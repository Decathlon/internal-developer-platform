package com.decathlon.idp_core.infrastructure.adapters.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.decathlon.idp_core.domain.model.entity.RelationAsTargetSummary;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.RelationJpaEntity;

@Repository
public interface JpaRelationRepository
    extends
      JpaRepository<RelationJpaEntity, UUID>,
      RevisionRepository<RelationJpaEntity, UUID, Long> {

  /**
   * Finds inbound relation summaries where specified entities (by UUID) are
   * targets.
   *
   * **Purpose:** Discovers all relationships where the given entities are
   * referenced as targets. Essential for bidirectional graph traversal and entity
   * deletion safety checks.
   *
   * **UUID-Based Lookup:** Matches by `target_entity_uuid` column (database
   * primary key reference) to guarantee correct entity matching regardless of
   * identifier duplicates across templates.
   *
   * **Design:** Uses native SQL with `DISTINCT` to eliminate duplicate results
   * when multiple relations point to the same target entity.
   *
   * **Performance:** Single query join across entity, entity_relations, relation,
   * and relation_target_entities tables. Filtered by IN clause on
   * target_entity_uuid.
   *
   * @param targetEntityUuids
   *          list of entity database UUIDs to query for inbound relations
   * @return list of relation summaries containing source entity details
   *         (identifier, name, template identifier) and relation metadata
   *         (relation name). Results are distinct to avoid duplicates when
   *         multiple relations target the same entity
   */
  @Query(value = """
      SELECT DISTINCT
          rte.target_entity_identifier AS targetEntityIdentifier,
          r.name AS relationName,
          e.identifier AS sourceEntityIdentifier,
          e.name AS sourceEntityName,
          e.template_identifier AS sourceTemplateIdentifier
      FROM idp_core.entity e
      JOIN idp_core.entity_relations er ON er.entity_id = e.id
      JOIN idp_core.relation r ON r.id = er.relation_id
      JOIN idp_core.relation_target_entities rte ON rte.relation_id = r.id
      WHERE rte.target_entity_uuid IN :targetEntityUuids
      """, nativeQuery = true)
  List<RelationAsTargetSummary> findRelationsSummariesByTargetEntityUuids(
      @Param("targetEntityUuids") List<UUID> targetEntityUuids);
}
