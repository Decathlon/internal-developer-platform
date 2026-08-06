package com.decathlon.idp_core.domain.service.relation;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.model.entity.RelationAsTargetSummary;
import com.decathlon.idp_core.domain.port.RelationRepositoryPort;

import lombok.AllArgsConstructor;

/**
 * Domain service for managing entity relationship queries and navigation.
 *
 * **Business purpose:** Provides specialized relationship queries for
 * understanding entity interconnections and dependency analysis. Supports
 * reverse relationship navigation to identify which entities reference a given
 * target entity.
 *
 * **Key responsibilities:** - Relationship impact analysis for entity deletion
 * scenarios - Reverse navigation through entity relationship graphs - Bulk
 * relationship lookups for performance optimization using entity UUIDs
 */
@Service
@AllArgsConstructor
public class RelationService {

  private final RelationRepositoryPort relationRepository;

  /**
   * Finds all incoming relationships where specified entities (by UUID) are
   * targets.
   *
   * **Contract:** Accepts entity UUIDs (database primary keys). UUID-based
   * matching guarantees correct entity identification regardless of identifier
   * duplicates across templates.
   *
   * @param targetEntityUuids
   *          list of entity UUIDs to find inbound relations for
   * @return list of relationship summaries containing source entity details
   *         (identifier, name, template identifier) and relation metadata
   *         (relation name)
   */
  public List<RelationAsTargetSummary> findRelationsSummariesByTargetEntityUuids(
      List<UUID> targetEntityUuids) {
    return relationRepository.findRelationsSummariesByTargetEntityUuids(targetEntityUuids);
  }
}
