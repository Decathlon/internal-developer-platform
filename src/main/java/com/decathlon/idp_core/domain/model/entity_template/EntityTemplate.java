package com.decathlon.idp_core.domain.model.entity_template;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_DEFINITIONS_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.TEMPLATE_IDENTIFIER_MANDATORY;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/// Business template defining the structure and constraints for creating [Entity] instances.
///
/// Core aggregate root in the template-driven entity creation domain. EntityTemplates
/// establish the "schema" for dynamic entity types, defining what properties and relations
/// are allowed, required, and how they should be validated.
///
/// **Business invariants:**
/// - Template identifiers must be unique across the system
/// - At least one property definition is required for meaningful entities
/// - Property names must be unique within the template
/// - Relation names must be unique within the template
/// - All property definitions must have valid types and constraints
/// - Relations must reference valid target entity identifiers
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
