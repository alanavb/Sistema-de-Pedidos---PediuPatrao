package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.PedidoRequest;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PedidoServiceTest {

    @Mock PedidoRepository pedidoRepository;
    @Mock ProdutoRepository produtoRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock AuditoriaService auditoria;
    @InjectMocks PedidoService service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("gerente", "x", "ROLE_GERENTE"));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Pedido pedidoComStatus(String status) {
        Pedido p = new Pedido();
        p.setId("p1");
        p.setStatus(status);
        p.setSubtotal(new BigDecimal("100.00"));
        p.setValorTotal(new BigDecimal("100.00"));
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void totalVemDosPrecosDoServidor() {
        Produto produto = new Produto();
        produto.setId("prod1");
        produto.setNome("Pizza");
        produto.setPreco(25.50);
        produto.setAtivo(true);
        when(clienteRepository.existsById("c1")).thenReturn(true);
        when(produtoRepository.findById("prod1")).thenReturn(Optional.of(produto));

        Pedido criado = service.criarPedido(new PedidoRequest("c1", List.of(new PedidoRequest.Item("prod1", 2))));

        assertEquals(new BigDecimal("51.00"), criado.getValorTotal());
        assertEquals("RECEBIDO", criado.getStatus());
        assertEquals("gerente", criado.getResponsavelRegistro());
        assertNotNull(criado.getDataEntrada());
    }

    @Test
    void fluxoValidoRegistraSaida() {
        pedidoComStatus("PRONTO");
        Pedido p = service.atualizarStatus("p1", "RETIRADO");
        assertEquals("gerente", p.getResponsavelSaida());
        assertNotNull(p.getDataSaida());
    }

    @Test
    void transicaoInvalidaEBloqueada() {
        pedidoComStatus("EM_PREPARACAO");
        assertThrows(IllegalStateException.class, () -> service.atualizarStatus("p1", "FINALIZADO"));
    }

    @Test
    void descontoLimitadoA20PorCentoEAntesDaSaida() {
        pedidoComStatus("RECEBIDO");
        assertThrows(IllegalArgumentException.class, () -> service.aplicarDesconto("p1", new BigDecimal("20.01")));
        Pedido p = service.aplicarDesconto("p1", new BigDecimal("20"));
        assertEquals(new BigDecimal("80.00"), p.getValorTotal());

        pedidoComStatus("RETIRADO");
        assertThrows(IllegalStateException.class, () -> service.aplicarDesconto("p1", new BigDecimal("5")));
    }

    @Test
    void cancelamentoExigeJustificativaETravaOPedido() {
        pedidoComStatus("PRONTO");
        assertThrows(IllegalArgumentException.class, () -> service.cancelar("p1", "  "));
        service.cancelar("p1", "Cliente desistiu");

        assertThrows(IllegalStateException.class, () -> service.atualizarStatus("p1", "EM_PREPARACAO"));
        assertThrows(IllegalStateException.class, () -> service.cancelar("p1", "outra vez"));

        pedidoComStatus("SAIU_PARA_ENTREGA");
        assertThrows(IllegalStateException.class, () -> service.cancelar("p1", "tarde demais"));
    }
}
