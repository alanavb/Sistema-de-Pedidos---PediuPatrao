package com.umc.pediupatrao.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.service.AuditoriaService;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.DashboardService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.ProdutoService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import com.umc.pediupatrao.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest
@Import(SecurityConfig.class)
class AutorizacaoApiTest {

    @Autowired MockMvc mvc;

    @MockitoBean UsuarioDetailsService usuarioDetailsService;
    @MockitoBean PedidoService pedidoService;
    @MockitoBean ClienteService clienteService;
    @MockitoBean ProdutoService produtoService;
    @MockitoBean UsuarioService usuarioService;
    @MockitoBean AuditoriaService auditoriaService;
    @MockitoBean DashboardService dashboardService;

    private static RequestPostProcessor perfil(String role) {
        return user(role.toLowerCase()).roles(role);
    }

    private void esperaNegado(MockHttpServletRequestBuilder req, String role) throws Exception {
        mvc.perform(req.with(perfil(role)).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Acesso negado")));
    }

    private void esperaPermitido(MockHttpServletRequestBuilder req, String role) throws Exception {
        mvc.perform(req.with(perfil(role)).with(csrf()))
                .andExpect(status().is2xxSuccessful());
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder req, String corpo) {
        return req.contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    @Test
    void semLoginRedirecionaParaOLogin() throws Exception {
        mvc.perform(get("/api/pedidos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void perfilDesconhecidoNaoAcessaNada() throws Exception {
        esperaNegado(get("/api/pedidos"), "SEM_PERFIL");
    }

    // Pedidos: desconto e cancelamento só para o GERENTE, inclusive em relação ao ADMIN

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "ATENDENTE"})
    void descontoNegadoParaQuemNaoEGerente(String role) throws Exception {
        esperaNegado(post("/api/pedidos/p1/desconto").param("percentual", "10"), role);
    }

    @Test
    void gerenteAplicaDesconto() throws Exception {
        esperaPermitido(post("/api/pedidos/p1/desconto").param("percentual", "10"), "GERENTE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "ATENDENTE"})
    void cancelamentoNegadoParaQuemNaoEGerente(String role) throws Exception {
        esperaNegado(post("/api/pedidos/p1/cancelamento").param("justificativa", "cliente desistiu"), role);
    }

    @Test
    void gerenteCancelaPedido() throws Exception {
        esperaPermitido(post("/api/pedidos/p1/cancelamento").param("justificativa", "cliente desistiu"), "GERENTE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "ATENDENTE"})
    void mudancaDeStatusSoPeloGerente(String role) throws Exception {
        esperaNegado(put("/api/pedidos/p1/status").param("status", "EM_PREPARACAO"), role);
    }

    @Test
    void adminNaoRegistraPedido() throws Exception {
        esperaNegado(json(post("/api/pedidos"), "{}"), "ADMIN");
    }

    @Test
    void atendenteRegistraPedido() throws Exception {
        esperaPermitido(json(post("/api/pedidos"), "{}"), "ATENDENTE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "GERENTE", "ATENDENTE"})
    void todosOsPerfisConsultamPedidos(String role) throws Exception {
        esperaPermitido(get("/api/pedidos"), role);
    }

    // Usuários: somente ADMIN, para impedir que outro perfil se promova

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE", "ATENDENTE"})
    void somenteAdminListaUsuarios(String role) throws Exception {
        esperaNegado(get("/api/usuarios"), role);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE", "ATENDENTE"})
    void naoAdminNaoCriaUsuarioAdmin(String role) throws Exception {
        esperaNegado(json(post("/api/usuarios"),
                "{\"username\":\"intruso\",\"password\":\"123\",\"role\":\"ADMIN\"}"), role);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE", "ATENDENTE"})
    void naoAdminNaoAlteraPerfil(String role) throws Exception {
        esperaNegado(json(put("/api/usuarios/u1"), "{\"username\":\"gerente\",\"role\":\"ADMIN\"}"), role);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE", "ATENDENTE"})
    void telaDeUsuariosSoParaAdmin(String role) throws Exception {
        mvc.perform(post("/usuarios/salvar").param("username", "x").param("role", "ADMIN")
                        .with(perfil(role)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminGerenciaUsuarios() throws Exception {
        esperaPermitido(get("/api/usuarios"), "ADMIN");
    }

    // Auditoria: leitura para ADMIN e GERENTE, sem alteração ou exclusão por ninguém

    @Test
    void atendenteNaoVeAuditoria() throws Exception {
        esperaNegado(get("/api/auditoria"), "ATENDENTE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "GERENTE"})
    void adminEGerenteConsultamAuditoria(String role) throws Exception {
        esperaPermitido(get("/api/auditoria"), role);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "GERENTE", "ATENDENTE"})
    void auditoriaNaoPodeSerAlteradaNemExcluida(String role) throws Exception {
        esperaNegado(delete("/api/auditoria/a1"), role);
        esperaNegado(json(put("/api/auditoria/a1"), "{}"), role);
        esperaNegado(json(post("/api/auditoria"), "{}"), role);
    }

    // Clientes: ATENDENTE cadastra e altera; GERENTE consulta; ADMIN não acessa

    @Test
    void adminNaoConsultaClientes() throws Exception {
        esperaNegado(get("/api/clientes"), "ADMIN");
    }

    @Test
    void gerenteNaoAlteraCliente() throws Exception {
        esperaNegado(json(put("/api/clientes/c1"), "{\"nome\":\"Ana\"}"), "GERENTE");
    }

    @Test
    void atendenteAlteraCliente() throws Exception {
        esperaPermitido(json(put("/api/clientes/c1"), "{\"nome\":\"Ana\"}"), "ATENDENTE");
    }

    // CSRF

    @Test
    void requisicaoSemTokenCsrfERecusada() throws Exception {
        mvc.perform(post("/api/pedidos/p1/desconto").param("percentual", "10").with(perfil("GERENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenCsrfInvalidoERecusado() throws Exception {
        mvc.perform(post("/api/pedidos/p1/cancelamento").param("justificativa", "x")
                        .with(perfil("GERENTE")).with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden());
    }
}
