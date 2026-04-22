package com.decathlon.idp_core.infrastructure.adapters.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.decathlon.idp_core.domain.model.entity.RelationAsTargetSummary;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.RelationJpaEntity;

@Repository
public interface JpaRelationRepository extends JpaRepository<RelationJpaEntity, UUID> {

    @Query("""
            SELECT tei AS targetEntityIdentifier, r.name AS relationName, e.identifier AS sourceEntityIdentifier, e.name AS sourceEntityName
            FROM EntityJpaEntity e
            JOIN e.relations r
            JOIN r.targetEntityIdentifiers tei
            WHERE tei IN :targetEntityIdentifiers
            """)
    List<RelationAsTargetSummary> findRelationsSummariesByTargetEntityIdentifiers(
            @Param("targetEntityIdentifiers") List<String> targetEntityIdentifiers);
}
