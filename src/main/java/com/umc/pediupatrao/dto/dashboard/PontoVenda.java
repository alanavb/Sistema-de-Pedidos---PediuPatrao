package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;

public record PontoVenda(String periodo, long pedidos, BigDecimal faturamento) {
}
