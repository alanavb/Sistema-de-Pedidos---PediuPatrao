package com.umc.pediupatrao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.umc.pediupatrao.dto.dashboard.CriterioRanking;
import com.umc.pediupatrao.dto.dashboard.FiltroDashboard;
import com.umc.pediupatrao.dto.dashboard.ProdutoVendido;
import com.umc.pediupatrao.dto.dashboard.ResumoVendas;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.service.DashboardService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Executa as consultas do dashboard no MongoDB local (banco pizzaria_teste). */
@SpringBootTest(properties = {
        "spring.data.mongodb.uri=mongodb://127.0.0.1:27017/pizzaria_teste",
        "spring.data.mongodb.database=pizzaria_teste"})
@EnabledIf("mongoLocalDisponivel")
class DashboardIntegracaoTest {

    @Autowired DashboardService dashboardService;
    @Autowired PedidoRepository pedidoRepository;
    @Autowired ProdutoRepository produtoRepository;

    private String pizzaId;
    private String cocaId;

    static boolean mongoLocalDisponivel() {
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString("mongodb://127.0.0.1:27017"))
                .applyToClusterSettings(c -> c.serverSelectionTimeout(1, TimeUnit.SECONDS))
                .build();
        try (MongoClient client = MongoClients.create(settings)) {
            client.getDatabase("admin").runCommand(new Document("ping", 1));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeEach
    void setUp() {
        pedidoRepository.deleteAll();
        produtoRepository.deleteAll();
        pizzaId = produtoRepository.save(produto("Pizza Margherita", "Pizza", 40.0)).getId();
        cocaId = produtoRepository.save(produto("Coca-Cola", "Bebida", 8.0)).getId();

        // dentro do período
        salvar(LocalDateTime.of(2026, 11, 1, 0, 0), StatusPedido.FINALIZADO, item(pizzaId, "Pizza Margherita", 2, "40.00"));
        salvar(LocalDateTime.of(2026, 11, 3, 20, 0), StatusPedido.CANCELADO, item(pizzaId, "Pizza Margherita", 1, "40.00"));
        salvar(LocalDateTime.of(2026, 11, 7, 23, 59), StatusPedido.PRONTO, item(cocaId, "Coca-Cola", 3, "8.00"));
        // fora do período
        salvar(LocalDateTime.of(2026, 10, 31, 23, 59), StatusPedido.FINALIZADO, item(pizzaId, "Pizza Margherita", 5, "40.00"));
        salvar(LocalDateTime.of(2026, 11, 8, 0, 0), StatusPedido.FINALIZADO, item(cocaId, "Coca-Cola", 5, "8.00"));
    }

    private static Produto produto(String nome, String tipo, double preco) {
        Produto p = new Produto();
        p.setNome(nome);
        p.setTipo(tipo);
        p.setPreco(preco);
        p.setAtivo(true);
        return p;
    }

    private static ItemPedido item(String produtoId, String nome, int qtd, String preco) {
        BigDecimal unitario = new BigDecimal(preco);
        return new ItemPedido(produtoId, nome, qtd, unitario, unitario.multiply(BigDecimal.valueOf(qtd)));
    }

    private void salvar(LocalDateTime entrada, StatusPedido status, ItemPedido item) {
        Pedido p = new Pedido();
        p.setDataEntrada(entrada);
        p.setStatus(status.name());
        p.setItens(List.of(item));
        p.setSubtotal(item.subtotal());
        p.setDescontoPercentual(BigDecimal.ZERO);
        p.setValorTotal(item.subtotal());
        pedidoRepository.save(p);
    }

    private static FiltroDashboard periodo(StatusPedido... status) {
        return FiltroDashboard.criar(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 7),
                List.of(status), null, LocalDate.of(2026, 11, 10));
    }

    @Test
    void consideraSoOsPedidosDoPeriodoIncluindoAsBordas() {
        ResumoVendas r = dashboardService.resumo(periodo());

        assertEquals(3, r.totalPedidos());
        assertEquals(1, r.pedidosCancelados());
        assertEquals(new BigDecimal("104.00"), r.faturamento());
        assertEquals(new BigDecimal("200.00"), r.faturamentoPeriodoAnterior());
    }

    @Test
    void filtroDeStatusEAplicadoNoBanco() {
        ResumoVendas r = dashboardService.resumo(periodo(StatusPedido.PRONTO, StatusPedido.CANCELADO));

        assertEquals(2, r.totalPedidos());
        assertEquals(new BigDecimal("24.00"), r.faturamento());
    }

    @Test
    void rankingUsaOsTiposCadastrados() {
        List<ProdutoVendido> ranking = dashboardService.produtosMaisVendidos(periodo(), 5, CriterioRanking.QUANTIDADE);

        assertEquals(2, ranking.size());
        assertEquals("Coca-Cola", ranking.get(0).nome());
        assertEquals("Bebida", ranking.get(0).tipo());
        assertEquals(3, ranking.get(0).quantidade());
        assertEquals(2, ranking.get(1).quantidade());
    }

    @Test
    void opcoesTrazemOPeriodoComPedidos() {
        var opcoes = dashboardService.opcoes();

        assertEquals(LocalDate.of(2026, 10, 31), opcoes.primeiroPedido());
        assertEquals(LocalDate.of(2026, 11, 8), opcoes.ultimoPedido());
        assertEquals(List.of("Bebida", "Pizza"), opcoes.tiposProduto());
    }
}
