package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.dashboard.Agrupamento;
import com.umc.pediupatrao.dto.dashboard.CriterioRanking;
import com.umc.pediupatrao.dto.dashboard.OpcoesFiltro;
import com.umc.pediupatrao.dto.dashboard.ParametrosDashboard;
import com.umc.pediupatrao.dto.dashboard.PontoVenda;
import com.umc.pediupatrao.dto.dashboard.ProdutoVendido;
import com.umc.pediupatrao.dto.dashboard.ResumoVendas;
import com.umc.pediupatrao.dto.dashboard.SituacaoPedidos;
import com.umc.pediupatrao.dto.dashboard.VendaPorHora;
import com.umc.pediupatrao.dto.dashboard.VendaPorTipo;
import com.umc.pediupatrao.service.DashboardService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Somente leitura. Acesso restrito a ADMIN e GERENTE na SecurityConfig. */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumo")
    public ResumoVendas resumo(ParametrosDashboard parametros) {
        return dashboardService.resumo(parametros.paraFiltro(LocalDate.now()));
    }

    @GetMapping("/vendas")
    public List<PontoVenda> vendas(ParametrosDashboard parametros,
            @RequestParam(defaultValue = "DIA") Agrupamento agrupamento) {
        return dashboardService.vendasNoTempo(parametros.paraFiltro(LocalDate.now()), agrupamento);
    }

    @GetMapping("/vendas-por-hora")
    public List<VendaPorHora> vendasPorHora(ParametrosDashboard parametros) {
        return dashboardService.vendasPorHora(parametros.paraFiltro(LocalDate.now()));
    }

    @GetMapping("/vendas-por-tipo")
    public List<VendaPorTipo> vendasPorTipo(ParametrosDashboard parametros) {
        return dashboardService.vendasPorTipo(parametros.paraFiltro(LocalDate.now()));
    }

    @GetMapping("/situacao-pedidos")
    public SituacaoPedidos situacao(ParametrosDashboard parametros) {
        return dashboardService.situacao(parametros.paraFiltro(LocalDate.now()));
    }

    @GetMapping("/produtos-mais-vendidos")
    public List<ProdutoVendido> produtosMaisVendidos(ParametrosDashboard parametros,
            @RequestParam(defaultValue = "10") int limite,
            @RequestParam(defaultValue = "QUANTIDADE") CriterioRanking criterio) {
        return dashboardService.produtosMaisVendidos(parametros.paraFiltro(LocalDate.now()), limite, criterio);
    }

    @GetMapping("/filtros")
    public OpcoesFiltro filtros() {
        return dashboardService.opcoes();
    }
}
