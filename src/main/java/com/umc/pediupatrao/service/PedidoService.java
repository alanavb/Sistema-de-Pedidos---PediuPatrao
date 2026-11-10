package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.PedidoRequest;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {

    static final BigDecimal DESCONTO_MAXIMO = new BigDecimal("20");
    private static final BigDecimal CEM = new BigDecimal("100");

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AuditoriaService auditoria;

    @Transactional
    public Pedido criarPedido(PedidoRequest request) {
        if (request == null || request.clienteId() == null
                || !clienteRepository.existsById(request.clienteId())) {
            throw new IllegalArgumentException("Cliente inválido ou não encontrado.");
        }
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new IllegalArgumentException("O pedido precisa ter ao menos um item.");
        }

        List<ItemPedido> itens = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (PedidoRequest.Item item : request.itens()) {
            if (item.quantidade() <= 0) {
                throw new IllegalArgumentException("A quantidade de cada item deve ser maior que zero.");
            }
            Produto produto = produtoRepository.findById(String.valueOf(item.produtoId()))
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + item.produtoId()));
            if (Boolean.FALSE.equals(produto.getAtivo()) || produto.getPreco() == null) {
                throw new IllegalArgumentException("Produto indisponível: " + produto.getNome());
            }
            BigDecimal preco = BigDecimal.valueOf(produto.getPreco()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotalItem = preco.multiply(BigDecimal.valueOf(item.quantidade()));
            itens.add(new ItemPedido(produto.getId(), produto.getNome(), item.quantidade(), preco, subtotalItem));
            subtotal = subtotal.add(subtotalItem);
        }

        Pedido pedido = new Pedido();
        pedido.setClienteId(request.clienteId());
        pedido.setItens(itens);
        pedido.setSubtotal(subtotal);
        pedido.setDescontoPercentual(BigDecimal.ZERO.setScale(2));
        pedido.setValorTotal(subtotal);
        pedido.setStatus(StatusPedido.RECEBIDO.name());
        pedido.setResponsavelRegistro(AuditoriaService.usuarioAtual());
        pedido.setDataEntrada(LocalDateTime.now());
        Pedido salvo = pedidoRepository.save(pedido);

        auditoria.registrar("CRIACAO_PEDIDO", "Pedido", salvo.getId(), Map.of(), campos(salvo), null);
        return salvo;
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    /** Avança o pedido no fluxo normal; cancelamento tem operação própria. */
    @Transactional
    public Pedido atualizarStatus(String id, String status) {
        StatusPedido novo;
        try {
            novo = StatusPedido.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Status inválido: " + status);
        }
        if (novo == StatusPedido.CANCELADO) {
            throw new IllegalArgumentException("Use a operação de cancelamento (exige justificativa).");
        }

        Pedido pedido = buscar(id);
        StatusPedido atual = statusAtual(pedido);
        if (atual == StatusPedido.CANCELADO) {
            throw new IllegalStateException("Pedido cancelado não pode mais ser alterado.");
        }
        if (!atual.proximos().contains(novo)) {
            throw new IllegalStateException("Transição inválida: " + atual + " → " + novo + ".");
        }

        Map<String, Object> antes = campos(pedido);
        pedido.setStatus(novo.name());
        if (novo.saida()) {
            pedido.setDataSaida(LocalDateTime.now());
            pedido.setResponsavelSaida(AuditoriaService.usuarioAtual());
        }
        Pedido salvo = pedidoRepository.save(pedido);
        auditoria.registrar("MUDANCA_STATUS", "Pedido", id, antes, campos(salvo), null);
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    @Transactional
    public Pedido aplicarDesconto(String id, BigDecimal percentual) {
        if (percentual == null || percentual.signum() <= 0 || percentual.compareTo(DESCONTO_MAXIMO) > 0) {
            throw new IllegalArgumentException("O desconto deve ser maior que 0% e no máximo 20%.");
        }
        Pedido pedido = buscar(id);
        if (!statusAtual(pedido).antesDaSaida()) {
            throw new IllegalStateException("Desconto só pode ser aplicado antes da saída e em pedido não cancelado.");
        }
        if (pedido.getSubtotal() == null) {
            throw new IllegalStateException("Pedido antigo, sem itens: não é possível aplicar desconto.");
        }

        Map<String, Object> antes = campos(pedido);
        BigDecimal desconto = percentual.setScale(2, RoundingMode.HALF_UP);
        pedido.setDescontoPercentual(desconto);
        pedido.setValorTotal(pedido.getSubtotal()
                .multiply(CEM.subtract(desconto)).divide(CEM, 2, RoundingMode.HALF_UP));
        Pedido salvo = pedidoRepository.save(pedido);
        auditoria.registrar("APLICACAO_DESCONTO", "Pedido", id, antes, campos(salvo), null);
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    @Transactional
    public Pedido cancelar(String id, String justificativa) {
        if (justificativa == null || justificativa.isBlank()) {
            throw new IllegalArgumentException("A justificativa do cancelamento é obrigatória.");
        }
        Pedido pedido = buscar(id);
        if (!statusAtual(pedido).antesDaSaida()) {
            throw new IllegalStateException("Só é possível cancelar antes da saída e se o pedido não estiver cancelado.");
        }

        Map<String, Object> antes = campos(pedido);
        pedido.setStatus(StatusPedido.CANCELADO.name());
        pedido.setJustificativaCancelamento(justificativa.trim());
        pedido.setCanceladoPor(AuditoriaService.usuarioAtual());
        pedido.setDataCancelamento(LocalDateTime.now());
        Pedido salvo = pedidoRepository.save(pedido);
        auditoria.registrar("CANCELAMENTO_PEDIDO", "Pedido", id, antes, campos(salvo), justificativa.trim());
        return salvo;
    }

    private Pedido buscar(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));
    }

    private StatusPedido statusAtual(Pedido pedido) {
        try {
            return StatusPedido.valueOf(pedido.getStatus());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalStateException("Pedido com status desconhecido: " + pedido.getStatus());
        }
    }

    /** Campos auditáveis do pedido. */
    private Map<String, Object> campos(Pedido p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("clienteId", p.getClienteId());
        m.put("itens", p.getItens());
        m.put("subtotal", p.getSubtotal());
        m.put("descontoPercentual", p.getDescontoPercentual());
        m.put("valorTotal", p.getValorTotal());
        m.put("status", p.getStatus());
        m.put("responsavelRegistro", p.getResponsavelRegistro());
        m.put("dataEntrada", p.getDataEntrada());
        m.put("responsavelSaida", p.getResponsavelSaida());
        m.put("dataSaida", p.getDataSaida());
        m.put("justificativaCancelamento", p.getJustificativaCancelamento());
        return m;
    }
}
