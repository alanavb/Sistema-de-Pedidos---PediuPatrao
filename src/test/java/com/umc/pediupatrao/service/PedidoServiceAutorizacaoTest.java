package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

// Segunda camada: mesmo que uma rota seja liberada por engano, o serviço recusa quem não é GERENTE
@SpringJUnitConfig(PedidoServiceAutorizacaoTest.Config.class)
class PedidoServiceAutorizacaoTest {

    @Configuration
    @EnableMethodSecurity
    @Import(PedidoService.class)
    static class Config {
    }

    @MockitoBean PedidoRepository pedidoRepository;
    @MockitoBean ProdutoRepository produtoRepository;
    @MockitoBean ClienteRepository clienteRepository;
    @MockitoBean AuditoriaService auditoriaService;

    @Autowired PedidoService service;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminNaoAplicaDesconto() {
        assertThrows(AccessDeniedException.class, () -> service.aplicarDesconto("p1", BigDecimal.TEN));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoCancela() {
        assertThrows(AccessDeniedException.class, () -> service.cancelar("p1", "motivo"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminNaoCancela() {
        assertThrows(AccessDeniedException.class, () -> service.cancelar("p1", "motivo"));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerentePassaPelaAutorizacao() {
        // passa pela segurança e para na regra de negócio (pedido inexistente)
        assertThrows(IllegalArgumentException.class, () -> service.aplicarDesconto("p1", BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> service.cancelar("p1", "motivo"));
    }
}
