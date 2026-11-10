package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;

public record VendaPorHora(int hora, long pedidos, BigDecimal faturamento) {
}
