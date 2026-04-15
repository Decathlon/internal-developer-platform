package com.decathlon.idp_core.domain.model.entity_template;

import static com.decathlon.idp_core.domain.constant.ValidationsMessages.RELATION_NAME_MANDATORY_SIMPLE;
import static com.decathlon.idp_core.domain.constant.ValidationsMessages.RELATION_TARGET_IDENTIFIER_MANDATORY_SIMPLE;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

/**
 * Pure domain model representing a RelationDefinition within an EntityTemplate.
 * Immutable value object.
 */
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
