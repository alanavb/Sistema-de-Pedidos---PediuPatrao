package com.umc.pediupatrao.dto;

import java.util.List;

/** Entrada de um novo pedido: preços, descontos e total nunca vêm do cliente HTTP. */
public record PedidoRequest(String clienteId, List<Item> itens) {

    public record Item(String produtoId, int quantidade) {
    }
}
