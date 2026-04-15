package com.decathlon.idp_core.domain.model.entity;

import static com.decathlon.idp_core.domain.constant.ValidationsMessages.RELATION_NAME_MANDATORY_SIMPLE;
import static com.decathlon.idp_core.domain.constant.ValidationsMessages.RELATION_TARGET_IDENTIFIERS_NOT_NULL;
import static com.decathlon.idp_core.domain.constant.ValidationsMessages.RELATION_TARGET_IDENTIFIER_MANDATORY_SIMPLE;

import java.util.List;
import java.util.UUID;

import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/// A concrete relationship instance connecting entities in the business domain.
///
/// Represents actual business connections between entities that conform to the
/// relationship structure defined in [RelationDefinition] within entity templates.
/// Relations are the "filled-in" connections of the template's relationship schema.
///
/// **Business invariants:**
/// - Relation names must match a [RelationDefinition] name in the entity's template
/// - Target entities must exist before relations can be established
/// - Required relations cannot have empty target lists
/// - Multiple targets allowed only when template's `toMany` is true
/// - Target template identifiers must reference valid [EntityTemplate] identifiers
public record Relation(
    UUID id,

    @NotBlank(message = RELATION_NAME_MANDATORY_SIMPLE)
    String name,

    @NotBlank(message = RELATION_TARGET_IDENTIFIER_MANDATORY_SIMPLE)
    String targetTemplateIdentifier,

    @NotNull(message = RELATION_TARGET_IDENTIFIERS_NOT_NULL)
    List<String> targetEntityIdentifiers
) {
    /// Ensures immutable defensive copying of target entity identifiers.
    ///
    /// **Why this exists:** Prevents external mutation of relationship targets after
    /// construction, maintaining referential integrity in the business object graph.
    public Relation {
        targetEntityIdentifiers = targetEntityIdentifiers != null
            ? List.copyOf(targetEntityIdentifiers)
            : List.of();
    }
}
