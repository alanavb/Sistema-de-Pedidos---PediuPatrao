package com.umc.pediupatrao.dto.dashboard;

import java.time.LocalDate;
import java.util.List;

public record OpcoesFiltro(List<String> status, List<String> tiposProduto, List<String> agrupamentos,
        List<String> criteriosRanking, LocalDate primeiroPedido, LocalDate ultimoPedido,
        int periodoPadraoDias, int periodoMaximoDias) {
}
