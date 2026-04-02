package com.decathlon.idp_core.domain.model.entity;

/**
 * Record representing a summary of a relation where the current entity is the target.
 * <p>
 * Contains the target entity identifier, relation name, source entity identifier, and source entity name.
 * </p>
 */
public record RelationAsTargetSummary(
    String targetEntityIdentifier,
    String relationName,
    String sourceEntityIdentifier,
    String sourceEntityName
) {}
