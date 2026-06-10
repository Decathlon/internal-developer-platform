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

  /**
   * Find relation summaries where the given entity identifiers are targets. Uses
   * a native query to efficiently join through the relation_target_entities
   * table.
   *
   * @param targetEntityIdentifiers
   *          List of entity identifiers to search for
   * @return List of relation summaries where these entities are targets
   */
  @Query(value = """
      SELECT
          rte.target_entity_identifier AS targetIdentifier,
          r.name AS relationName,
          e.identifier AS sourceIdentifier,
          e.name AS sourceName
      FROM idp_core.entity e
      JOIN idp_core.entity_relations er ON er.entity_id = e.id
      JOIN idp_core.relation r ON r.id = er.relation_id
      JOIN idp_core.relation_target_entities rte ON rte.relation_id = r.id
      WHERE rte.target_entity_identifier IN :targetEntityIdentifiers
      """, nativeQuery = true)
  List<RelationAsTargetSummary> findRelationsSummariesByTargetEntityIdentifiers(
      @Param("targetEntityIdentifiers") List<String> targetEntityIdentifiers);
}
