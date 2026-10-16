package com.umc.pediupatrao.entity;

import java.math.BigDecimal;

public record ItemPedido(String produtoId, String nomeProduto, int quantidade,
        BigDecimal precoUnitario, BigDecimal subtotal) {
}
