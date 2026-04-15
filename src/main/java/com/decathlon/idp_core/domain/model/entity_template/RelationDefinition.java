package com.decathlon.idp_core.domain.model.entity_template;

import static com.decathlon.idp_core.domain.constant.ValidationsMessages.RELATION_NAME_MANDATORY_SIMPLE;
import static com.decathlon.idp_core.domain.constant.ValidationsMessages.RELATION_TARGET_IDENTIFIER_MANDATORY_SIMPLE;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

/// Defines relationship structure between entities within [EntityTemplate] contexts.
///
/// Captures the business relationships in the domain model, defining how entities
/// can be connected to form meaningful business object graphs. Relations represent
/// the "lines" in entity-relationship diagrams at the business level.
///
/// **Business invariants:**
/// - Relation names must be unique within an EntityTemplate context
/// - Target entities must exist before relations can be established
/// - Required relations cannot be null when creating entities
/// - `toMany` relationships allow multiple target connections (one-to-many/many-to-many)
/// - `!toMany` relationships enforce single target connections (one-to-one/many-to-one)
public record RelationDefinition(
    UUID id,

    @NotBlank(message = RELATION_NAME_MANDATORY_SIMPLE)
    String name,

    @NotBlank(message = RELATION_TARGET_IDENTIFIER_MANDATORY_SIMPLE)
    String targetEntityIdentifier,

    boolean required,

    boolean toMany
) {
}
