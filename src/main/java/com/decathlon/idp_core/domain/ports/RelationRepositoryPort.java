package com.decathlon.idp_core.domain.ports;

import java.util.List;

import com.decathlon.idp_core.domain.model.entity.RelationAsTargetSummary;

/**
 * Driven Port for Relation persistence operations.
 */
public interface RelationRepositoryPort {

    List<RelationAsTargetSummary> findRelationsSummariesByTargetEntityIdentifiers(
            List<String> targetEntityIdentifiers);
}
