package com.decathlon.idp_core.domain.model.entity;

import static com.decathlon.idp_core.domain.constant.ValidationsMessages.TEMPLATE_IDENTIFIER_MANDATORY;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

/**
 * Pure domain model representing an Entity in the system.
 * <p>
 * Free of persistence annotations. JPA mapping is in the infrastructure layer.
 * </p>
 */
public record Entity(
        UUID id,

        @NotBlank(message = TEMPLATE_IDENTIFIER_MANDATORY)
        String templateIdentifier,

        String name,

        String identifier,

        List<Property> properties,

        List<Relation> relations
) {
}
