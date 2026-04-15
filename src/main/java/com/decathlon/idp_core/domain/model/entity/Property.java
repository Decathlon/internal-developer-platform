package com.decathlon.idp_core.domain.model.entity;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

/**
 * Pure domain model representing a Property of an Entity.
 * Immutable value object.
 */
public record Property(
    UUID id,

    @NotBlank(message = "Property name is mandatory")
    String name,

    @NotBlank(message = "Property value is mandatory")
    String value
) {
}
