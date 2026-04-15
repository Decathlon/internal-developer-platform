package com.decathlon.idp_core.domain.model.entity_template;

import static com.decathlon.idp_core.domain.constant.ValidationsMessages.PROPERTY_DEFINITIONS_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationsMessages.TEMPLATE_IDENTIFIER_MANDATORY;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * Pure domain model representing an EntityTemplate.
 */
public record EntityTemplate(
        UUID id,

        @NotBlank(message = TEMPLATE_IDENTIFIER_MANDATORY)
        String identifier,

        String description,

        @NotEmpty(message = PROPERTY_DEFINITIONS_MANDATORY)
        List<PropertyDefinition> propertiesDefinitions,

        List<RelationDefinition> relationsDefinitions
) {
}
