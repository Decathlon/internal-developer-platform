package com.decathlon.idp_core.domain.model.entity_template;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.TEMPLATE_IDENTIFIER_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.TEMPLATE_NAME_FORMAT;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.TEMPLATE_NAME_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.TEMPLATE_NAME_MAX_SIZE;
import static com.decathlon.idp_core.domain.constant.ValidationRegex.ENTITY_TEMPLATE_NAME_REGEX;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/// Business template defining the structure and constraints for creating [Entity] instances.
///
/// Core aggregate root in the template-driven entity creation domain. EntityTemplates
/// establish the "schema" for dynamic entity types, defining what properties and relations
/// are allowed, required, and how they should be validated.
///
/// **Business invariants:**
/// - Template identifiers must be unique across the system
/// - Template names must be unique across the system and respect the business regex
/// - Property names must be unique within the template (if any)
/// - Relation names must be unique within the template (if any)
/// - All property definitions must have valid types and constraints
/// - Relations must reference valid target template identifiers
public record EntityTemplate(
        UUID id,

        @NotBlank(message = TEMPLATE_IDENTIFIER_MANDATORY)
        String identifier,

        @Size(max = 255, message = TEMPLATE_NAME_MAX_SIZE)
        @NotBlank(message = TEMPLATE_NAME_MANDATORY)
        @Pattern(regexp = ENTITY_TEMPLATE_NAME_REGEX, message = TEMPLATE_NAME_FORMAT)
        String name,

        String description,

        List<PropertyDefinition> propertiesDefinitions,

        List<RelationDefinition> relationsDefinitions
) {
}
