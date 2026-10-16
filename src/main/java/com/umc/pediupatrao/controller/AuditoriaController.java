package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.AuditoriaEvento;
import com.umc.pediupatrao.service.AuditoriaService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Somente leitura: não há endpoints de alteração ou exclusão da auditoria. */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public List<AuditoriaEvento> listar() {
        return auditoriaService.listar();
    }
}
