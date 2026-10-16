package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.AuditoriaEvento;
import java.util.List;
import org.springframework.data.repository.Repository;

/** Estende Repository (e não MongoRepository) para não expor update/delete dos registros. */
public interface AuditoriaRepository extends Repository<AuditoriaEvento, String> {

    AuditoriaEvento save(AuditoriaEvento evento);

    // shortcut: sem paginação, só os 500 mais recentes; paginar se o volume crescer
    List<AuditoriaEvento> findTop500ByOrderByDataHoraDesc();
}
