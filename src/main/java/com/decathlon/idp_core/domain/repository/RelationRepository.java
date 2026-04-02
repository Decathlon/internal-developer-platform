package com.decathlon.idp_core.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity.RelationAsTargetSummary;

/**
 * Repository interface for accessing and managing {@link Relation} domain objects.
 * <p>
 * Provides custom queries for retrieving relation summaries where a given entity is the target.
 * </p>
 */
public interface RelationRepository extends JpaRepository<Relation, UUID> {

    /**
     * Finds relation summaries where the specified entity identifiers are the target.
     *
     * @param targetEntityIdentifiers List of target entity identifiers to search for
     * @return List of {@link RelationAsTargetSummary} projections containing relation and source entity info
     */
    @Query("""
            SELECT tei AS targetEntityIdentifier, r.name AS relationName, e.identifier AS sourceEntityIdentifier, e.name AS sourceEntityName
            FROM Entity e
            JOIN e.relations r
            JOIN r.targetEntityIdentifiers tei
            WHERE tei IN :targetEntityIdentifiers
            """)
    List<RelationAsTargetSummary> findRelationsSummariesByTargetEntityIdentifiers(
            @Param("targetEntityIdentifiers") List<String> targetEntityIdentifiers);

}
