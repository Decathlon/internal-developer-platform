package com.decathlon.idp_core.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.model.entity.RelationAsTargetSummary;
import com.decathlon.idp_core.domain.port.RelationRepositoryPort;

import lombok.AllArgsConstructor;

/// Domain service for managing entity relationship queries and navigation.
///
/// **Business purpose:** Provides specialized relationship queries for understanding
/// entity interconnections and dependency analysis. Supports reverse relationship
/// navigation to identify which entities reference a given target entity.
///
/// **Key responsibilities:**
/// - Relationship impact analysis for entity deletion scenarios
/// - Reverse navigation through entity relationship graphs
/// - Bulk relationship lookups for performance optimization
@Service
@AllArgsConstructor
public class RelationService {

    private final RelationRepositoryPort relationRepository;

    /// Finds all incoming relationships where specified entities are targets.
    ///
    /// **Contract:** Returns relationship summaries for dependency analysis and
    /// impact assessment. Useful for understanding entity interconnections before
    /// deletion or modification operations.
    ///
    /// @param targetEntityIdentifiers business identifiers of entities to analyze
    /// @return relationship summaries showing incoming connections to target entities
    public List<RelationAsTargetSummary> findRelationsSummariesByTargetEntityIdentifiers(List<String> targetEntityIdentifiers) {
        return relationRepository.findRelationsSummariesByTargetEntityIdentifiers(targetEntityIdentifiers);
    }

}
