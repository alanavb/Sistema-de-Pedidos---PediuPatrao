package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.dashboard.Agrupamento;
import com.umc.pediupatrao.dto.dashboard.CriterioRanking;
import com.umc.pediupatrao.dto.dashboard.FiltroDashboard;
import com.umc.pediupatrao.dto.dashboard.OpcoesFiltro;
import com.umc.pediupatrao.dto.dashboard.PontoVenda;
import com.umc.pediupatrao.dto.dashboard.ProdutoVendido;
import com.umc.pediupatrao.dto.dashboard.ResumoVendas;
import com.umc.pediupatrao.dto.dashboard.SituacaoPedidos;
import com.umc.pediupatrao.dto.dashboard.VendaPorHora;
import com.umc.pediupatrao.dto.dashboard.VendaPorTipo;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

/**
 * Período de 01 a 07/11/2026 com:
 *  A 01/11 19:30 FINALIZADO  2 pizzas (100) + 2 cocas (20)           = 120,00  saída em 40 min
 *  B 02/11 20:15 FINALIZADO  1 pizza (50) + 4 esfihas (20), 10% desc = 63,00   saída em 20 min
 *  C 03/11 19:05 CANCELADO   3 pizzas                                = 150,00
 *  D 07/11 21:00 EM_PREPARACAO 1 coca                                = 10,00
 *  E 04/11 pedido antigo, sem itens nem valores
 * Período anterior (25 a 31/10): F 28/10 FINALIZADO 2 pizzas = 100,00
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 10);

    @Mock MongoTemplate mongoTemplate;
    @Mock ProdutoRepository produtoRepository;
    @InjectMocks DashboardService service;

    private Pedido pedidoA;

    @BeforeEach
    void setUp() {
        when(produtoRepository.findAll()).thenReturn(List.of(
                produto("p1", "Pizza Calabresa", "Pizza"),
                produto("p2", "Coca-Cola", "Bebida"),
                produto("p3", "Esfiha de Carne", "Esfiha")));

        pedidoA = pedido("A", LocalDateTime.of(2026, 11, 1, 19, 30), StatusPedido.FINALIZADO, 0,
                item("p1", "Pizza Calabresa", 2, "50.00"), item("p2", "Coca-Cola", 2, "10.00"));
        pedidoA.setDataSaida(LocalDateTime.of(2026, 11, 1, 20, 10));
        Pedido b = pedido("B", LocalDateTime.of(2026, 11, 2, 20, 15), StatusPedido.FINALIZADO, 10,
                item("p1", "Pizza Calabresa", 1, "50.00"), item("p3", "Esfiha de Carne", 4, "5.00"));
        b.setDataSaida(LocalDateTime.of(2026, 11, 2, 20, 35));
        Pedido c = pedido("C", LocalDateTime.of(2026, 11, 3, 19, 5), StatusPedido.CANCELADO, 0,
                item("p1", "Pizza Calabresa", 3, "50.00"));
        Pedido d = pedido("D", LocalDateTime.of(2026, 11, 7, 21, 0), StatusPedido.EM_PREPARACAO, 0,
                item("p2", "Coca-Cola", 1, "10.00"));
        Pedido antigo = new Pedido();
        antigo.setId("E");
        antigo.setStatus("RECEBIDO");
        antigo.setDataEntrada(LocalDateTime.of(2026, 11, 4, 12, 0));

        Pedido f = pedido("F", LocalDateTime.of(2026, 10, 28, 20, 0), StatusPedido.FINALIZADO, 0,
                item("p1", "Pizza Calabresa", 2, "50.00"));

        when(mongoTemplate.find(any(Query.class), eq(Pedido.class)))
                .thenReturn(List.of(pedidoA, b, c, d, antigo), List.of(f));
    }

    private static FiltroDashboard filtro(String tipo, StatusPedido... status) {
        return FiltroDashboard.criar(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 7),
                List.of(status), tipo, HOJE);
    }

    private static Produto produto(String id, String nome, String tipo) {
        Produto p = new Produto();
        p.setId(id);
        p.setNome(nome);
        p.setTipo(tipo);
        return p;
    }

    private static ItemPedido item(String produtoId, String nome, int qtd, String preco) {
        BigDecimal unitario = new BigDecimal(preco);
        return new ItemPedido(produtoId, nome, qtd, unitario, unitario.multiply(BigDecimal.valueOf(qtd)));
    }

    private static Pedido pedido(String id, LocalDateTime entrada, StatusPedido status, int desconto,
            ItemPedido... itens) {
        BigDecimal subtotal = List.of(itens).stream()
                .map(ItemPedido::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        Pedido p = new Pedido();
        p.setId(id);
        p.setDataEntrada(entrada);
        p.setStatus(status.name());
        p.setItens(List.of(itens));
        p.setSubtotal(subtotal);
        p.setDescontoPercentual(BigDecimal.valueOf(desconto));
        p.setValorTotal(subtotal.multiply(BigDecimal.valueOf(100 - desconto))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        return p;
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void resumoIgnoraCanceladosEPedidosIncompletos() {
        ResumoVendas r = service.resumo(filtro(null));

        assertEquals(bd("193.00"), r.faturamento());
        assertEquals(4, r.totalPedidos());
        assertEquals(1, r.pedidosCancelados());
        assertEquals(bd("25.0"), r.taxaCancelamento());
        assertEquals(bd("64.33"), r.ticketMedio());
        assertEquals(10, r.itensVendidos());
        assertEquals(bd("7.00"), r.descontosConcedidos());
        assertEquals(1, r.pedidosEmAndamento());
        assertEquals(1, r.registrosDesconsiderados());
    }

    @Test
    void resumoComparaComOPeriodoAnterior() {
        ResumoVendas r = service.resumo(filtro(null));

        assertEquals(bd("100.00"), r.faturamentoPeriodoAnterior());
        assertEquals(bd("93.0"), r.variacaoFaturamento());
    }

    @Test
    void semVendasNoPeriodoAnteriorAVariacaoFicaNula() {
        when(mongoTemplate.find(any(Query.class), eq(Pedido.class))).thenReturn(List.of(pedidoA), List.of());

        assertNull(service.resumo(filtro(null)).variacaoFaturamento());
    }

    @Test
    void periodoSemPedidosRetornaZeros() {
        when(mongoTemplate.find(any(Query.class), eq(Pedido.class))).thenReturn(List.of());

        ResumoVendas r = service.resumo(filtro(null));
        assertEquals(bd("0.00"), r.faturamento());
        assertEquals(bd("0.00"), r.ticketMedio());
        assertEquals(bd("0.0"), r.taxaCancelamento());
        assertEquals(0, r.totalPedidos());
    }

    @Test
    void filtroPorTipoConsideraSoOsItensDoTipo() {
        ResumoVendas r = service.resumo(filtro("pizza"));

        assertEquals(3, r.totalPedidos());
        assertEquals(bd("145.00"), r.faturamento());
        assertEquals(1, r.pedidosCancelados());
        assertEquals(3, r.itensVendidos());
        assertEquals(bd("5.00"), r.descontosConcedidos());
    }

    @Test
    void tipoInexistenteERecusado() {
        assertThrows(IllegalArgumentException.class, () -> service.resumo(filtro("Sushi")));
    }

    @Test
    void vendasPorDiaPreenchemDiasSemVenda() {
        List<PontoVenda> serie = service.vendasNoTempo(filtro(null), Agrupamento.DIA);

        assertEquals(7, serie.size());
        assertEquals(new PontoVenda("2026-11-01", 1, bd("120.00")), serie.get(0));
        assertEquals(new PontoVenda("2026-11-02", 1, bd("63.00")), serie.get(1));
        assertEquals(new PontoVenda("2026-11-03", 0, bd("0.00")), serie.get(2));
        assertEquals(new PontoVenda("2026-11-07", 1, bd("10.00")), serie.get(6));
    }

    @Test
    void vendasPorSemanaComecamNaSegunda() {
        List<PontoVenda> serie = service.vendasNoTempo(filtro(null), Agrupamento.SEMANA);

        assertEquals(List.of(
                new PontoVenda("2026-10-26", 1, bd("120.00")),
                new PontoVenda("2026-11-02", 2, bd("73.00"))), serie);
    }

    @Test
    void vendasPorMes() {
        assertEquals(List.of(new PontoVenda("2026-11", 3, bd("193.00"))),
                service.vendasNoTempo(filtro(null), Agrupamento.MES));
    }

    @Test
    void vendasPorHoraTem24Faixas() {
        List<VendaPorHora> horas = service.vendasPorHora(filtro(null));

        assertEquals(24, horas.size());
        assertEquals(new VendaPorHora(19, 1, bd("120.00")), horas.get(19));
        assertEquals(new VendaPorHora(20, 1, bd("63.00")), horas.get(20));
        assertEquals(new VendaPorHora(21, 1, bd("10.00")), horas.get(21));
        assertEquals(0, horas.get(12).pedidos());
    }

    @Test
    void vendasPorTipoComDescontoAplicadoNosItens() {
        assertEquals(List.of(
                new VendaPorTipo("Pizza", 3, bd("145.00"), bd("75.1")),
                new VendaPorTipo("Bebida", 3, bd("30.00"), bd("15.5")),
                new VendaPorTipo("Esfiha", 4, bd("18.00"), bd("9.3"))),
                service.vendasPorTipo(filtro(null)));
    }

    @Test
    void rankingPorQuantidadeDesempataPeloFaturamento() {
        List<ProdutoVendido> ranking = service.produtosMaisVendidos(filtro(null), 10, CriterioRanking.QUANTIDADE);

        assertEquals(List.of("Esfiha de Carne", "Pizza Calabresa", "Coca-Cola"),
                ranking.stream().map(ProdutoVendido::nome).toList());
        assertEquals(new ProdutoVendido(2, "p1", "Pizza Calabresa", "Pizza", 3, bd("145.00"), 2), ranking.get(1));
    }

    @Test
    void rankingPorFaturamentoRespeitaOLimite() {
        List<ProdutoVendido> ranking = service.produtosMaisVendidos(filtro(null), 2, CriterioRanking.FATURAMENTO);

        assertEquals(List.of("Pizza Calabresa", "Coca-Cola"), ranking.stream().map(ProdutoVendido::nome).toList());
    }

    @Test
    void limiteDoRankingEValidado() {
        assertThrows(IllegalArgumentException.class,
                () -> service.produtosMaisVendidos(filtro(null), 0, CriterioRanking.QUANTIDADE));
        assertThrows(IllegalArgumentException.class,
                () -> service.produtosMaisVendidos(filtro(null), 51, CriterioRanking.QUANTIDADE));
    }

    @Test
    void situacaoContaTodosOsStatusETempoMedioDeSaida() {
        SituacaoPedidos s = service.situacao(filtro(null));

        assertEquals(StatusPedido.values().length, s.porStatus().size());
        assertEquals(new SituacaoPedidos.QuantidadePorStatus("FINALIZADO", 2, bd("50.0")), s.porStatus().get(5));
        assertEquals(new SituacaoPedidos.QuantidadePorStatus("CANCELADO", 1, bd("25.0")), s.porStatus().get(6));
        assertEquals(new SituacaoPedidos.QuantidadePorStatus("PRONTO", 0, bd("0.0")), s.porStatus().get(2));
        assertEquals(1, s.emAndamento());
        assertEquals(30L, s.tempoMedioAteSaidaMinutos());
    }

    @Test
    void consultaAoBancoFiltraPeriodoEStatus() {
        service.buscarPedidos(filtro(null, StatusPedido.FINALIZADO, StatusPedido.CANCELADO));

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(Pedido.class));
        Document consulta = captor.getValue().getQueryObject();
        Document periodo = (Document) consulta.get("dataEntrada");
        assertEquals(LocalDateTime.of(2026, 11, 1, 0, 0), periodo.get("$gte"));
        assertEquals(LocalDateTime.of(2026, 11, 8, 0, 0), periodo.get("$lt"));
        assertEquals(List.of("FINALIZADO", "CANCELADO"), ((Document) consulta.get("status")).get("$in"));
    }

    @Test
    void opcoesDeFiltroListamTiposEPeriodoDisponivel() {
        when(mongoTemplate.findOne(any(Query.class), eq(Pedido.class))).thenReturn(pedidoA);

        OpcoesFiltro opcoes = service.opcoes();
        assertEquals(List.of("Bebida", "Esfiha", "Pizza"), opcoes.tiposProduto());
        assertEquals(StatusPedido.values().length, opcoes.status().size());
        assertEquals(LocalDate.of(2026, 11, 1), opcoes.primeiroPedido());
        assertEquals(List.of("DIA", "SEMANA", "MES"), opcoes.agrupamentos());
    }
}
