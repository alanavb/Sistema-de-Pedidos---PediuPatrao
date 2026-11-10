package com.umc.pediupatrao.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeradorPedidosDemonstracaoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 10, 20, 30);

    private List<Produto> produtos;
    private List<Cliente> clientes;
    private List<Pedido> pedidos;

    @BeforeEach
    void setUp() {
        produtos = List.of(produto("p1", "Pizza Calabresa", "Pizza", 52.90),
                produto("p2", "Esfiha de Carne", "Esfiha", 6.50),
                produto("p3", "Coca-Cola 2L", "Bebida", 14.00));
        Cliente cliente = new Cliente();
        cliente.setId("c1");
        clientes = List.of(cliente);
        pedidos = new GeradorPedidosDemonstracao(1L, produtos, clientes).gerar(AGORA, 120);
    }

    private static Produto produto(String id, String nome, String tipo, double preco) {
        Produto p = new Produto();
        p.setId(id);
        p.setNome(nome);
        p.setTipo(tipo);
        p.setPreco(preco);
        return p;
    }

    @Test
    void geraVolumeCompativelComUmaPizzaria() {
        assertTrue(pedidos.size() > 800 && pedidos.size() < 2500, "gerados: " + pedidos.size());
    }

    @Test
    void mesmaSementeGeraAMesmaCarga() {
        List<Pedido> outra = new GeradorPedidosDemonstracao(1L, produtos, clientes).gerar(AGORA, 120);
        assertEquals(pedidos.size(), outra.size());
        assertEquals(pedidos.get(100).getValorTotal(), outra.get(100).getValorTotal());
    }

    @Test
    void nenhumPedidoNoFuturoOuForaDoPeriodo() {
        LocalDateTime inicio = AGORA.toLocalDate().minusDays(119).atStartOfDay();
        assertTrue(pedidos.stream().allMatch(p ->
                !p.getDataEntrada().isAfter(AGORA) && !p.getDataEntrada().isBefore(inicio)));
    }

    @Test
    void valoresSeguemAsRegrasDoPedidoService() {
        for (Pedido p : pedidos) {
            assertFalse(p.getItens().isEmpty());
            for (ItemPedido item : p.getItens()) {
                Produto produto = produtos.stream().filter(x -> x.getId().equals(item.produtoId())).findFirst().orElseThrow();
                assertEquals(BigDecimal.valueOf(produto.getPreco()).setScale(2), item.precoUnitario());
                assertEquals(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())), item.subtotal());
            }
            BigDecimal subtotal = p.getItens().stream().map(ItemPedido::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(subtotal, p.getSubtotal());
            assertTrue(p.getDescontoPercentual().compareTo(new BigDecimal("20")) <= 0);
            BigDecimal esperado = subtotal.multiply(new BigDecimal("100").subtract(p.getDescontoPercentual()))
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            assertEquals(esperado, p.getValorTotal());
        }
    }

    @Test
    void saidaECancelamentoSaoCoerentesComOStatus() {
        Set<StatusPedido> comSaida = EnumSet.of(StatusPedido.SAIU_PARA_ENTREGA, StatusPedido.RETIRADO,
                StatusPedido.FINALIZADO);
        for (Pedido p : pedidos) {
            StatusPedido status = StatusPedido.valueOf(p.getStatus());
            if (comSaida.contains(status)) {
                assertNotNull(p.getDataSaida());
                assertNotNull(p.getResponsavelSaida());
                assertTrue(p.getDataSaida().isAfter(p.getDataEntrada()));
                assertFalse(p.getDataSaida().isAfter(AGORA));
            } else {
                assertNull(p.getDataSaida());
            }
            if (status == StatusPedido.CANCELADO) {
                assertNotNull(p.getJustificativaCancelamento());
                assertEquals("gerente", p.getCanceladoPor());
                assertFalse(p.getDataCancelamento().isAfter(AGORA));
            }
        }
    }

    @Test
    void pedidosAntigosEstaoConcluidosOuCancelados() {
        LocalDateTime ontem = AGORA.minusDays(1);
        assertTrue(pedidos.stream().filter(p -> p.getDataEntrada().isBefore(ontem))
                .allMatch(p -> p.getStatus().equals("FINALIZADO") || p.getStatus().equals("CANCELADO")));
    }

    @Test
    void sempreHaPedidosEmAndamentoNaUltimaHora() {
        Set<String> emAndamento = Set.of("RECEBIDO", "EM_PREPARACAO", "PRONTO", "SAIU_PARA_ENTREGA", "RETIRADO");
        long recentes = pedidos.stream()
                .filter(p -> p.getDataEntrada().isAfter(AGORA.minusHours(1)))
                .filter(p -> emAndamento.contains(p.getStatus()))
                .count();
        assertTrue(recentes >= 3, "em andamento: " + recentes);

        LocalDateTime manha = LocalDateTime.of(2026, 11, 10, 9, 0);
        List<Pedido> cargaPelaManha = new GeradorPedidosDemonstracao(1L, produtos, clientes).gerar(manha, 30);
        assertTrue(cargaPelaManha.stream().anyMatch(p -> p.getStatus().equals("RECEBIDO")));
    }

    @Test
    void temCanceladosEDescontosSuficientesParaOsIndicadores() {
        Map<String, Long> porStatus = pedidos.stream()
                .collect(Collectors.groupingBy(Pedido::getStatus, Collectors.counting()));
        double cancelados = porStatus.getOrDefault("CANCELADO", 0L) * 100.0 / pedidos.size();
        long comDesconto = pedidos.stream().filter(p -> p.getDescontoPercentual().signum() > 0).count();

        assertTrue(cancelados > 3 && cancelados < 12, "cancelados: " + cancelados + "%");
        assertTrue(comDesconto > 0);
    }

    @Test
    void fimDeSemanaTemMaisMovimentoQueSegunda() {
        Map<DayOfWeek, Long> porDia = pedidos.stream()
                .collect(Collectors.groupingBy(p -> p.getDataEntrada().getDayOfWeek(), Collectors.counting()));
        assertTrue(porDia.get(DayOfWeek.SATURDAY) > 2 * porDia.get(DayOfWeek.MONDAY));
    }

    @Test
    void semProdutosOuClientesNaoGera() {
        assertThrows(IllegalArgumentException.class,
                () -> new GeradorPedidosDemonstracao(1L, List.of(), clientes));
    }
}
