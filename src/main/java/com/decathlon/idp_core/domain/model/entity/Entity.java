package com.decathlon.idp_core.domain.model.entity;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.ENTITY_IDENTIFIER_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.ENTITY_NAME_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.TEMPLATE_IDENTIFIER_MANDATORY;

import java.util.List;
import java.util.UUID;

import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;

import jakarta.validation.constraints.NotBlank;

/// Domain entity representing a concrete instance of an [EntityTemplate].
///
/// Business invariants:
/// - [templateIdentifier] must reference an existing template definition
/// - [identifier] serves as the unique business key within the template scope
/// - [properties] must conform to the template's property definitions
/// - [relations] must satisfy the template's relation constraints
///
/// Ubiquitous language: An Entity is a materialized instance of a template schema,
/// containing actual values that comply with the template's structure and rules.
public record Entity(
        UUID id,

        @NotBlank(message = TEMPLATE_IDENTIFIER_MANDATORY)
        String templateIdentifier,
        @NotBlank(message = ENTITY_NAME_MANDATORY)
        String name,
        @NotBlank(message = ENTITY_IDENTIFIER_MANDATORY)
        String identifier,

        List<Property> properties,

        List<Relation> relations
) {
}
