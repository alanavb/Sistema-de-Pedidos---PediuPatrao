package com.umc.pediupatrao.dto.dashboard;

import com.umc.pediupatrao.entity.StatusPedido;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public record FiltroDashboard(LocalDate inicio, LocalDate fim, Set<StatusPedido> status, String tipoProduto) {

    public static final int PERIODO_PADRAO_DIAS = 30;
    public static final int PERIODO_MAXIMO_DIAS = 366;

    public static FiltroDashboard criar(LocalDate inicio, LocalDate fim, List<StatusPedido> status,
            String tipoProduto, LocalDate hoje) {
        LocalDate ate = fim != null ? fim : hoje;
        LocalDate de = inicio != null ? inicio : ate.minusDays(PERIODO_PADRAO_DIAS - 1);
        if (de.isAfter(ate)) {
            throw new IllegalArgumentException("A data inicial não pode ser posterior à data final.");
        }
        if (ChronoUnit.DAYS.between(de, ate) + 1 > PERIODO_MAXIMO_DIAS) {
            throw new IllegalArgumentException("O período consultado pode ter no máximo "
                    + PERIODO_MAXIMO_DIAS + " dias.");
        }
        Set<StatusPedido> situacoes = status == null || status.isEmpty()
                ? EnumSet.noneOf(StatusPedido.class) : EnumSet.copyOf(status);
        String tipo = tipoProduto == null || tipoProduto.isBlank() ? null : tipoProduto.trim();
        return new FiltroDashboard(de, ate, situacoes, tipo);
    }

    public long dias() {
        return ChronoUnit.DAYS.between(inicio, fim) + 1;
    }

    /** Mesmo número de dias, imediatamente antes do período atual. */
    public FiltroDashboard periodoAnterior() {
        return new FiltroDashboard(inicio.minusDays(dias()), inicio.minusDays(1), status, tipoProduto);
    }

    public LocalDateTime desde() {
        return inicio.atStartOfDay();
    }

    public LocalDateTime ate() {
        return fim.plusDays(1).atStartOfDay();
    }
}
