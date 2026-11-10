package com.umc.pediupatrao.dto.dashboard;

import com.umc.pediupatrao.entity.StatusPedido;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

/** Parâmetros de consulta comuns a todos os endpoints do dashboard. */
public record ParametrosDashboard(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
        List<StatusPedido> status,
        String tipoProduto,
        Integer hora,
        String produtoId) {

    public FiltroDashboard paraFiltro(LocalDate hoje) {
        return FiltroDashboard.criar(dataInicio, dataFim, status, tipoProduto, hora, produtoId, hoje);
    }
}
