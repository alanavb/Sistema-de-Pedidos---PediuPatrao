package com.umc.pediupatrao.entity;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Registro imutável: sem setters e sem operações de alteração/exclusão no repositório. */
@Document(collection = "auditoria")
public record AuditoriaEvento(@Id String id, String usuario, String perfil, LocalDateTime dataHora,
        String operacao, String entidade, String entidadeId, List<Alteracao> alteracoes,
        String justificativa) {
}
