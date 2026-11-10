package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;

public record VendaPorTipo(String tipo, long quantidade, BigDecimal faturamento, BigDecimal percentual) {
}
