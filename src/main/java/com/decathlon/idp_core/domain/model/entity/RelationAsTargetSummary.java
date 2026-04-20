package com.decathlon.idp_core.domain.model.entity;

/// Summary view of incoming relationships where the current entity is the target.
///
/// Provides reverse relationship navigation for business scenarios where you need
/// to understand "what points to this entity". Essential for relationship management
/// and dependency analysis in the domain model.
///
/// **Business purpose:**
/// - Display entities that reference the current entity
/// - Dependency impact analysis before entity deletion
/// - Bidirectional relationship navigation
/// - Audit trails for relationship changes
public record RelationAsTargetSummary(
    String targetEntityIdentifier,
    String relationName,
    String sourceEntityIdentifier,
    String sourceEntityName
) {}
