package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Gera pedidos fictícios para alimentar o dashboard. Segue as mesmas regras do PedidoService:
 * preço vindo do cadastro do produto, desconto de até 20% e datas de saída/cancelamento coerentes.
 * A semente fixa faz a mesma carga ser gerada sempre.
 */
class GeradorPedidosDemonstracao {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final int[] DESCONTOS = {5, 10, 15, 20};
    private static final String[] JUSTIFICATIVAS = {
        "Cliente desistiu do pedido",
        "Endereço fora da área de entrega",
        "Pagamento não confirmado",
        "Pedido duplicado",
        "Falta de ingrediente"
    };

    private final Random random;
    private final List<Produto> produtos;
    private final List<Cliente> clientes;

    GeradorPedidosDemonstracao(long semente, List<Produto> produtos, List<Cliente> clientes) {
        if (produtos.isEmpty() || clientes.isEmpty()) {
            throw new IllegalArgumentException("É preciso ter produtos e clientes para gerar pedidos.");
        }
        this.random = new Random(semente);
        this.produtos = produtos;
        this.clientes = clientes;
    }

    List<Pedido> gerar(LocalDateTime agora, int dias) {
        List<Pedido> pedidos = new ArrayList<>();
        LocalDate primeiro = agora.toLocalDate().minusDays(dias - 1);
        for (int d = 0; d < dias; d++) {
            LocalDate dia = primeiro.plusDays(d);
            // o movimento cresce ao longo do período, para o dashboard mostrar tendência
            double crescimento = 0.8 + 0.4 * d / Math.max(1, dias - 1);
            int quantidade = (int) Math.round(pedidosNoDia(dia) * crescimento);
            for (int i = 0; i < quantidade; i++) {
                LocalDateTime entrada = dia.atTime(horaDoPedido(), random.nextInt(60), random.nextInt(60));
                if (!entrada.isAfter(agora)) {
                    pedidos.add(pedido(entrada, agora));
                }
            }
        }
        // Pedidos da última hora, ainda no fluxo, independente do horário em que a carga roda
        for (int minutos = 3; minutos <= 55; minutos += 8) {
            pedidos.add(pedido(agora.minusMinutes(minutos), agora));
        }
        pedidos.sort((a, b) -> a.getDataEntrada().compareTo(b.getDataEntrada()));
        return pedidos;
    }

    private int pedidosNoDia(LocalDate dia) {
        DayOfWeek semana = dia.getDayOfWeek();
        if (semana == DayOfWeek.FRIDAY || semana == DayOfWeek.SATURDAY) {
            return 16 + random.nextInt(8);
        }
        if (semana == DayOfWeek.SUNDAY) {
            return 13 + random.nextInt(6);
        }
        if (semana == DayOfWeek.MONDAY) {
            return 4 + random.nextInt(4);
        }
        return 7 + random.nextInt(6);
    }

    // Pico no jantar, movimento menor no almoço
    private int horaDoPedido() {
        int sorteio = random.nextInt(100);
        if (sorteio < 15) {
            return 11 + random.nextInt(3);
        }
        if (sorteio < 25) {
            return 18;
        }
        if (sorteio < 85) {
            return 19 + random.nextInt(3);
        }
        return 22 + random.nextInt(2);
    }

    private Pedido pedido(LocalDateTime entrada, LocalDateTime agora) {
        List<ItemPedido> itens = itens();
        BigDecimal subtotal = itens.stream().map(ItemPedido::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        Pedido p = new Pedido();
        p.setClienteId(clientes.get(random.nextInt(clientes.size())).getId());
        p.setItens(itens);
        p.setSubtotal(subtotal);
        p.setDataEntrada(entrada);
        p.setResponsavelRegistro(random.nextInt(4) == 0 ? "gerente" : "atendente");

        BigDecimal desconto = random.nextInt(10) == 0
                ? new BigDecimal(DESCONTOS[random.nextInt(DESCONTOS.length)]) : BigDecimal.ZERO;
        p.setDescontoPercentual(desconto.setScale(2));
        p.setValorTotal(subtotal.multiply(CEM.subtract(desconto)).divide(CEM, 2, RoundingMode.HALF_UP));

        definirSituacao(p, entrada, agora);
        return p;
    }

    private List<ItemPedido> itens() {
        List<Produto> embaralhados = new ArrayList<>(produtos);
        Collections.shuffle(embaralhados, random);
        int distintos = 1 + random.nextInt(Math.min(3, embaralhados.size()));
        List<ItemPedido> itens = new ArrayList<>();
        for (Produto produto : embaralhados.subList(0, distintos)) {
            int quantidade = quantidade(produto.getTipo());
            BigDecimal preco = BigDecimal.valueOf(produto.getPreco()).setScale(2, RoundingMode.HALF_UP);
            itens.add(new ItemPedido(produto.getId(), produto.getNome(), quantidade, preco,
                    preco.multiply(BigDecimal.valueOf(quantidade))));
        }
        return itens;
    }

    private int quantidade(String tipo) {
        if ("Esfiha".equalsIgnoreCase(tipo)) {
            return 2 + random.nextInt(9);
        }
        if ("Bebida".equalsIgnoreCase(tipo)) {
            return 1 + random.nextInt(3);
        }
        return 1 + random.nextInt(2);
    }

    private void definirSituacao(Pedido p, LocalDateTime entrada, LocalDateTime agora) {
        long minutos = Duration.between(entrada, agora).toMinutes();
        boolean retirada = random.nextInt(3) == 0;
        StatusPedido saida = retirada ? StatusPedido.RETIRADO : StatusPedido.SAIU_PARA_ENTREGA;
        int tempoAteSaida = 20 + random.nextInt(41);

        if (random.nextInt(100) < 7 && minutos >= 10) {
            p.setStatus(StatusPedido.CANCELADO.name());
            p.setJustificativaCancelamento(JUSTIFICATIVAS[random.nextInt(JUSTIFICATIVAS.length)]);
            p.setCanceladoPor("gerente");
            p.setDataCancelamento(entrada.plusMinutes(5 + random.nextInt(Math.min(10, (int) minutos - 4))));
            return;
        }
        // Pedidos recentes ainda estão no fluxo; os demais já foram concluídos
        if (minutos < 10) {
            p.setStatus(StatusPedido.RECEBIDO.name());
        } else if (minutos < 20) {
            p.setStatus(StatusPedido.EM_PREPARACAO.name());
        } else if (minutos < tempoAteSaida) {
            p.setStatus(StatusPedido.PRONTO.name());
        } else {
            p.setDataSaida(entrada.plusMinutes(tempoAteSaida));
            p.setResponsavelSaida(retirada ? "atendente" : "gerente");
            p.setStatus(minutos < tempoAteSaida + 30 ? saida.name() : StatusPedido.FINALIZADO.name());
        }
    }
}
