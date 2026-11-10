package com.umc.pediupatrao.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.dto.dashboard.Agrupamento;
import com.umc.pediupatrao.dto.dashboard.CriterioRanking;
import com.umc.pediupatrao.dto.dashboard.FiltroDashboard;
import com.umc.pediupatrao.dto.dashboard.PontoVenda;
import com.umc.pediupatrao.dto.dashboard.ResumoVendas;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.service.DashboardService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean DashboardService dashboardService;
    @MockitoBean UsuarioDetailsService usuarioDetailsService;

    private static RequestPostProcessor gerente() {
        return user("gerente").roles("GERENTE");
    }

    private static ResumoVendas resumoExemplo() {
        return new ResumoVendas(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 7),
                new BigDecimal("193.00"), new BigDecimal("100.00"), new BigDecimal("93.0"),
                4, 1, new BigDecimal("25.0"), new BigDecimal("64.33"), 10, new BigDecimal("7.00"),
                1, 1, LocalDateTime.of(2026, 11, 10, 9, 0));
    }

    // ---------- autorização ----------

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "GERENTE"})
    void adminEGerenteConsultamODashboard(String role) throws Exception {
        when(dashboardService.resumo(any())).thenReturn(resumoExemplo());

        mvc.perform(get("/api/dashboard/resumo").with(user("u").roles(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faturamento").value(193.00))
                .andExpect(jsonPath("$.ticketMedio").value(64.33))
                .andExpect(jsonPath("$.dataInicio").value("2026-11-01"));
    }

    @Test
    void atendenteNaoConsultaODashboard() throws Exception {
        mvc.perform(get("/api/dashboard/resumo").with(user("atendente").roles("ATENDENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro").value(containsString("Acesso negado")));
        verifyNoInteractions(dashboardService);
    }

    @Test
    void semLoginRedirecionaParaOLogin() throws Exception {
        mvc.perform(get("/api/dashboard/resumo")).andExpect(status().is3xxRedirection());
    }

    @Test
    void dashboardEsomenteLeitura() throws Exception {
        mvc.perform(post("/api/dashboard/resumo").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/dashboard/resumo").with(gerente()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ---------- parâmetros ----------

    @Test
    void filtrosSaoRepassadosAoServico() throws Exception {
        when(dashboardService.resumo(any())).thenReturn(resumoExemplo());

        mvc.perform(get("/api/dashboard/resumo").with(gerente())
                        .param("dataInicio", "2026-11-01").param("dataFim", "2026-11-07")
                        .param("status", "FINALIZADO,CANCELADO").param("tipoProduto", "Pizza"))
                .andExpect(status().isOk());

        ArgumentCaptor<FiltroDashboard> captor = ArgumentCaptor.forClass(FiltroDashboard.class);
        verify(dashboardService).resumo(captor.capture());
        FiltroDashboard filtro = captor.getValue();
        assertEquals(LocalDate.of(2026, 11, 1), filtro.inicio());
        assertEquals(LocalDate.of(2026, 11, 7), filtro.fim());
        assertEquals(Set.of(StatusPedido.FINALIZADO, StatusPedido.CANCELADO), filtro.status());
        assertEquals("Pizza", filtro.tipoProduto());
    }

    @Test
    void agrupamentoECriterioTemValorPadrao() throws Exception {
        when(dashboardService.vendasNoTempo(any(), eq(Agrupamento.DIA)))
                .thenReturn(List.of(new PontoVenda("2026-11-01", 1, new BigDecimal("120.00"))));

        mvc.perform(get("/api/dashboard/vendas").with(gerente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].periodo").value("2026-11-01"));
        mvc.perform(get("/api/dashboard/produtos-mais-vendidos").with(gerente()))
                .andExpect(status().isOk());
        verify(dashboardService).produtosMaisVendidos(any(), eq(10), eq(CriterioRanking.QUANTIDADE));
    }

    @Test
    void dataMalFormatadaRetorna400() throws Exception {
        mvc.perform(get("/api/dashboard/resumo").with(gerente()).param("dataInicio", "31/12/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("dataInicio")));
        verify(dashboardService, never()).resumo(any());
    }

    @Test
    void statusInexistenteRetorna400() throws Exception {
        mvc.perform(get("/api/dashboard/resumo").with(gerente()).param("status", "ENTREGUE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("status")));
    }

    @Test
    void periodoInvertidoRetorna400() throws Exception {
        mvc.perform(get("/api/dashboard/resumo").with(gerente())
                        .param("dataInicio", "2026-11-07").param("dataFim", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("data inicial")));
    }

    @Test
    void agrupamentoInvalidoRetorna400() throws Exception {
        mvc.perform(get("/api/dashboard/vendas").with(gerente()).param("agrupamento", "ANO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("agrupamento")));
    }

    @Test
    void limiteNaoNumericoRetorna400() throws Exception {
        mvc.perform(get("/api/dashboard/produtos-mais-vendidos").with(gerente()).param("limite", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("limite")));
    }

    @Test
    void regraDoServicoViraErro400() throws Exception {
        when(dashboardService.produtosMaisVendidos(any(), anyInt(), any()))
                .thenThrow(new IllegalArgumentException("O limite deve estar entre 1 e 50."));

        mvc.perform(get("/api/dashboard/produtos-mais-vendidos").with(gerente()).param("limite", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O limite deve estar entre 1 e 50."));
    }
}
