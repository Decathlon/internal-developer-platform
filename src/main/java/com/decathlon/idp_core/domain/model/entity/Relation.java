package com.decathlon.idp_core.domain.model.entity;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Pure domain model representing a Relation between entities.
 * Immutable value object.
 */
public record Relation(
    UUID id,

    @NotBlank(message = "Relation name is mandatory")
    String name,

    @NotBlank(message = "Target template identifier is mandatory")
    String targetTemplateIdentifier,

    @NotNull(message = "Target entity identifiers cannot be null")
    List<String> targetEntityIdentifiers
) {
    /**
     * Compact constructor for defensive copying.
     * Ensures the list is immutable to prevent external modification.
     */
    public Relation {
        targetEntityIdentifiers = targetEntityIdentifiers != null
            ? List.copyOf(targetEntityIdentifiers)
            : List.of();
    }
}
