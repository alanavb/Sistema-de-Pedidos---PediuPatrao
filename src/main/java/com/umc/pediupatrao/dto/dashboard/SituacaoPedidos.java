package com.umc.pediupatrao.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record SituacaoPedidos(List<QuantidadePorStatus> porStatus, long emAndamento,
        Long tempoMedioAteSaidaMinutos) {

    public record QuantidadePorStatus(String status, long quantidade, BigDecimal percentual) {
    }
}
