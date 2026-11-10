package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.dashboard.Agrupamento;
import com.umc.pediupatrao.dto.dashboard.CriterioRanking;
import com.umc.pediupatrao.dto.dashboard.FiltroDashboard;
import com.umc.pediupatrao.dto.dashboard.OpcoesFiltro;
import com.umc.pediupatrao.dto.dashboard.PontoVenda;
import com.umc.pediupatrao.dto.dashboard.ProdutoVendido;
import com.umc.pediupatrao.dto.dashboard.ResumoVendas;
import com.umc.pediupatrao.dto.dashboard.SituacaoPedidos;
import com.umc.pediupatrao.dto.dashboard.SituacaoPedidos.QuantidadePorStatus;
import com.umc.pediupatrao.dto.dashboard.VendaPorHora;
import com.umc.pediupatrao.dto.dashboard.VendaPorTipo;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    static final String SEM_TIPO = "Sem tipo";
    static final int LIMITE_MAXIMO_RANKING = 50;
    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final MongoTemplate mongoTemplate;
    private final ProdutoRepository produtoRepository;

    public DashboardService(MongoTemplate mongoTemplate, ProdutoRepository produtoRepository) {
        this.mongoTemplate = mongoTemplate;
        this.produtoRepository = produtoRepository;
    }

    public ResumoVendas resumo(FiltroDashboard filtro) {
        Consulta atual = consultar(filtro);
        Consulta anterior = consultar(filtro.periodoAnterior());

        List<Venda> validas = atual.validas();
        BigDecimal faturamento = somar(validas, Venda::valor);
        BigDecimal faturamentoAnterior = somar(anterior.validas(), Venda::valor);
        long cancelados = atual.vendas().size() - validas.size();
        long emAndamento = atual.vendas().stream().filter(v -> emAndamento(v.status())).count();

        return new ResumoVendas(
                filtro.inicio(), filtro.fim(),
                faturamento,
                faturamentoAnterior,
                variacao(faturamento, faturamentoAnterior),
                atual.vendas().size(),
                cancelados,
                percentual(cancelados, atual.vendas().size()),
                validas.isEmpty() ? ZERO
                        : faturamento.divide(BigDecimal.valueOf(validas.size()), 2, RoundingMode.HALF_UP),
                validas.stream().flatMap(v -> v.itens().stream()).mapToLong(ItemVendido::quantidade).sum(),
                somar(validas, v -> v.bruto().subtract(v.valor())),
                emAndamento,
                atual.desconsiderados(),
                LocalDateTime.now());
    }

    public List<PontoVenda> vendasNoTempo(FiltroDashboard filtro, Agrupamento agrupamento) {
        Map<String, Acumulado> serie = new LinkedHashMap<>();
        for (LocalDate dia = inicioDoPeriodo(filtro.inicio(), agrupamento); !dia.isAfter(filtro.fim());
                dia = proximo(dia, agrupamento)) {
            serie.put(rotulo(dia, agrupamento), new Acumulado());
        }
        for (Venda v : consultar(filtro).validas()) {
            serie.get(rotulo(v.pedido().getDataEntrada().toLocalDate(), agrupamento)).somar(v.valor());
        }
        return serie.entrySet().stream()
                .map(e -> new PontoVenda(e.getKey(), e.getValue().pedidos, e.getValue().total))
                .toList();
    }

    public List<VendaPorHora> vendasPorHora(FiltroDashboard filtro) {
        Acumulado[] horas = new Acumulado[24];
        Arrays.setAll(horas, h -> new Acumulado());
        for (Venda v : consultar(filtro).validas()) {
            horas[v.pedido().getDataEntrada().getHour()].somar(v.valor());
        }
        List<VendaPorHora> resultado = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            resultado.add(new VendaPorHora(h, horas[h].pedidos, horas[h].total));
        }
        return resultado;
    }

    public List<VendaPorTipo> vendasPorTipo(FiltroDashboard filtro) {
        Map<String, long[]> quantidades = new HashMap<>();
        Map<String, BigDecimal> valores = new HashMap<>();
        for (Venda v : consultar(filtro).validas()) {
            for (ItemVendido item : v.itens()) {
                quantidades.computeIfAbsent(item.tipo(), t -> new long[1])[0] += item.quantidade();
                valores.merge(item.tipo(), item.valorLiquido(), BigDecimal::add);
            }
        }
        BigDecimal total = valores.values().stream().reduce(ZERO, BigDecimal::add);
        return valores.entrySet().stream()
                .map(e -> new VendaPorTipo(e.getKey(), quantidades.get(e.getKey())[0], e.getValue(),
                        percentual(e.getValue(), total)))
                .sorted(Comparator.comparing(VendaPorTipo::faturamento).reversed()
                        .thenComparing(VendaPorTipo::tipo))
                .toList();
    }

    public SituacaoPedidos situacao(FiltroDashboard filtro) {
        List<Venda> vendas = consultar(filtro).vendas();
        Map<String, Long> contagem = new HashMap<>();
        vendas.forEach(v -> contagem.merge(v.status(), 1L, Long::sum));

        List<QuantidadePorStatus> porStatus = Arrays.stream(StatusPedido.values())
                .map(s -> {
                    long qtd = contagem.getOrDefault(s.name(), 0L);
                    return new QuantidadePorStatus(s.name(), qtd, percentual(qtd, vendas.size()));
                })
                .toList();

        List<Long> tempos = vendas.stream()
                .filter(v -> v.pedido().getDataSaida() != null && !v.cancelado())
                .map(v -> Duration.between(v.pedido().getDataEntrada(), v.pedido().getDataSaida()).toMinutes())
                .filter(m -> m >= 0)
                .toList();
        Long tempoMedio = tempos.isEmpty() ? null
                : Math.round(tempos.stream().mapToLong(Long::longValue).average().orElse(0));

        return new SituacaoPedidos(porStatus,
                vendas.stream().filter(v -> emAndamento(v.status())).count(), tempoMedio);
    }

    public List<ProdutoVendido> produtosMaisVendidos(FiltroDashboard filtro, int limite, CriterioRanking criterio) {
        if (limite < 1 || limite > LIMITE_MAXIMO_RANKING) {
            throw new IllegalArgumentException("O limite deve estar entre 1 e " + LIMITE_MAXIMO_RANKING + ".");
        }
        Map<String, Ranking> produtos = new LinkedHashMap<>();
        for (Venda v : consultar(filtro).validas()) {
            for (ItemVendido item : v.itens()) {
                produtos.computeIfAbsent(item.chave(), k -> new Ranking(item)).somar(item, v.pedido().getId());
            }
        }
        Comparator<Ranking> ordem = criterio == CriterioRanking.FATURAMENTO
                ? Comparator.comparing((Ranking r) -> r.faturamento).thenComparingLong(r -> r.quantidade)
                : Comparator.comparingLong((Ranking r) -> r.quantidade).thenComparing(r -> r.faturamento);
        List<Ranking> ordenados = produtos.values().stream()
                .sorted(ordem.reversed().thenComparing(r -> r.nome))
                .limit(limite)
                .toList();

        List<ProdutoVendido> resultado = new ArrayList<>();
        for (int i = 0; i < ordenados.size(); i++) {
            Ranking r = ordenados.get(i);
            resultado.add(new ProdutoVendido(i + 1, r.produtoId, r.nome, r.tipo, r.quantidade, r.faturamento,
                    r.pedidos.size()));
        }
        return resultado;
    }

    public OpcoesFiltro opcoes() {
        Set<String> tipos = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        tiposPorProduto().values().stream().filter(t -> !SEM_TIPO.equals(t)).forEach(tipos::add);
        return new OpcoesFiltro(
                Arrays.stream(StatusPedido.values()).map(Enum::name).toList(),
                List.copyOf(tipos),
                Arrays.stream(Agrupamento.values()).map(Enum::name).toList(),
                Arrays.stream(CriterioRanking.values()).map(Enum::name).toList(),
                dataDoPedido(Sort.Direction.ASC),
                dataDoPedido(Sort.Direction.DESC),
                FiltroDashboard.PERIODO_PADRAO_DIAS,
                FiltroDashboard.PERIODO_MAXIMO_DIAS);
    }

    // ---------- consulta ao banco e preparação dos dados ----------

    List<Pedido> buscarPedidos(FiltroDashboard filtro) {
        Criteria criteria = Criteria.where("dataEntrada").gte(filtro.desde()).lt(filtro.ate());
        if (!filtro.status().isEmpty()) {
            criteria = criteria.and("status").in(filtro.status().stream().map(Enum::name).toList());
        }
        return mongoTemplate.find(new Query(criteria), Pedido.class);
    }

    private Consulta consultar(FiltroDashboard filtro) {
        Map<String, String> tipos = tiposPorProduto();
        String tipoFiltro = tipoValidado(filtro.tipoProduto(), tipos);

        List<Pedido> pedidos = buscarPedidos(filtro);
        List<Venda> vendas = new ArrayList<>();
        long desconsiderados = 0;
        for (Pedido pedido : pedidos) {
            if (!completo(pedido)) {
                desconsiderados++;
                continue;
            }
            List<ItemVendido> itens = pedido.getItens().stream()
                    .map(i -> new ItemVendido(i, tipos.getOrDefault(i.produtoId(), SEM_TIPO),
                            liquido(i.subtotal(), pedido.getDescontoPercentual())))
                    .filter(i -> tipoFiltro == null || tipoFiltro.equalsIgnoreCase(i.tipo()))
                    .toList();
            if (itens.isEmpty()) {
                continue;
            }
            BigDecimal valor = tipoFiltro == null ? pedido.getValorTotal() : somarItens(itens, ItemVendido::valorLiquido);
            BigDecimal bruto = tipoFiltro == null
                    ? Objects.requireNonNullElse(pedido.getSubtotal(), pedido.getValorTotal())
                    : somarItens(itens, i -> i.item().subtotal());
            vendas.add(new Venda(pedido, itens, valor.setScale(2, RoundingMode.HALF_UP),
                    bruto.setScale(2, RoundingMode.HALF_UP)));
        }
        return new Consulta(vendas, desconsiderados);
    }

    /** Pedidos antigos (antes da evolução do cadastro) não têm itens nem valores e ficam fora dos indicadores. */
    private static boolean completo(Pedido p) {
        return p.getDataEntrada() != null && p.getValorTotal() != null && p.getStatus() != null
                && p.getItens() != null && !p.getItens().isEmpty()
                && p.getItens().stream().allMatch(i -> i != null && i.subtotal() != null && i.quantidade() > 0);
    }

    private Map<String, String> tiposPorProduto() {
        Map<String, String> tipos = new HashMap<>();
        for (Produto p : produtoRepository.findAll()) {
            if (p.getId() != null) {
                tipos.put(p.getId(), p.getTipo() == null || p.getTipo().isBlank() ? SEM_TIPO : p.getTipo().trim());
            }
        }
        return tipos;
    }

    private static String tipoValidado(String tipo, Map<String, String> tipos) {
        if (tipo == null) {
            return null;
        }
        boolean existe = SEM_TIPO.equalsIgnoreCase(tipo)
                || tipos.values().stream().anyMatch(t -> t.equalsIgnoreCase(tipo));
        if (!existe) {
            throw new IllegalArgumentException("Tipo de produto inexistente: " + tipo);
        }
        return tipo;
    }

    private LocalDate dataDoPedido(Sort.Direction direcao) {
        Query query = new Query(Criteria.where("dataEntrada").ne(null))
                .with(Sort.by(direcao, "dataEntrada")).limit(1);
        Pedido pedido = mongoTemplate.findOne(query, Pedido.class);
        return pedido == null ? null : pedido.getDataEntrada().toLocalDate();
    }

    // ---------- cálculos auxiliares ----------

    private static boolean emAndamento(String status) {
        return !StatusPedido.FINALIZADO.name().equals(status) && !StatusPedido.CANCELADO.name().equals(status);
    }

    private static BigDecimal liquido(BigDecimal subtotal, BigDecimal desconto) {
        if (desconto == null || desconto.signum() == 0) {
            return subtotal;
        }
        return subtotal.multiply(CEM.subtract(desconto)).divide(CEM, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal somar(List<Venda> vendas, Function<Venda, BigDecimal> valor) {
        return vendas.stream().map(valor).reduce(ZERO, BigDecimal::add);
    }

    private static BigDecimal somarItens(List<ItemVendido> itens, Function<ItemVendido, BigDecimal> valor) {
        return itens.stream().map(valor).reduce(ZERO, BigDecimal::add);
    }

    private static BigDecimal percentual(long parte, long total) {
        return percentual(BigDecimal.valueOf(parte), BigDecimal.valueOf(total));
    }

    private static BigDecimal percentual(BigDecimal parte, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return parte.multiply(CEM).divide(total, 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal variacao(BigDecimal atual, BigDecimal anterior) {
        if (anterior.signum() == 0) {
            return null;
        }
        return atual.subtract(anterior).multiply(CEM).divide(anterior, 1, RoundingMode.HALF_UP);
    }

    private static LocalDate inicioDoPeriodo(LocalDate dia, Agrupamento agrupamento) {
        return switch (agrupamento) {
            case DIA -> dia;
            case SEMANA -> dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MES -> dia.withDayOfMonth(1);
        };
    }

    private static LocalDate proximo(LocalDate dia, Agrupamento agrupamento) {
        return switch (agrupamento) {
            case DIA -> dia.plusDays(1);
            case SEMANA -> dia.plusWeeks(1);
            case MES -> dia.plusMonths(1);
        };
    }

    // Semana identificada pela segunda-feira em que começa
    private static String rotulo(LocalDate dia, Agrupamento agrupamento) {
        return switch (agrupamento) {
            case DIA -> dia.toString();
            case SEMANA -> inicioDoPeriodo(dia, Agrupamento.SEMANA).toString();
            case MES -> YearMonth.from(dia).toString();
        };
    }

    // ---------- estruturas internas ----------

    private record Consulta(List<Venda> vendas, long desconsiderados) {
        List<Venda> validas() {
            return vendas.stream().filter(v -> !v.cancelado()).toList();
        }
    }

    private record Venda(Pedido pedido, List<ItemVendido> itens, BigDecimal valor, BigDecimal bruto) {
        String status() {
            return pedido.getStatus();
        }

        boolean cancelado() {
            return StatusPedido.CANCELADO.name().equals(pedido.getStatus());
        }
    }

    private record ItemVendido(ItemPedido item, String tipo, BigDecimal valorLiquido) {
        String chave() {
            return item.produtoId() != null ? item.produtoId() : item.nomeProduto();
        }

        int quantidade() {
            return item.quantidade();
        }
    }

    private static final class Acumulado {
        long pedidos;
        BigDecimal total = ZERO;

        void somar(BigDecimal valor) {
            pedidos++;
            total = total.add(valor);
        }
    }

    private static final class Ranking {
        final String produtoId;
        final String nome;
        final String tipo;
        long quantidade;
        BigDecimal faturamento = ZERO;
        final Set<String> pedidos = new HashSet<>();

        Ranking(ItemVendido item) {
            this.produtoId = item.item().produtoId();
            this.nome = item.item().nomeProduto();
            this.tipo = item.tipo();
        }

        void somar(ItemVendido item, String pedidoId) {
            quantidade += item.quantidade();
            faturamento = faturamento.add(item.valorLiquido());
            pedidos.add(pedidoId);
        }
    }
}
