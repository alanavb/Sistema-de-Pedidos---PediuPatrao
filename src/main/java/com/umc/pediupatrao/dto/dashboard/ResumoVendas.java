package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ResumoVendas(
        LocalDate dataInicio,
        LocalDate dataFim,
        BigDecimal faturamento,
        BigDecimal faturamentoPeriodoAnterior,
        BigDecimal variacaoFaturamento,
        long totalPedidos,
        long pedidosCancelados,
        BigDecimal taxaCancelamento,
        BigDecimal ticketMedio,
        long itensVendidos,
        BigDecimal descontosConcedidos,
        long pedidosEmAndamento,
        long registrosDesconsiderados,
        LocalDateTime atualizadoEm) {
}
