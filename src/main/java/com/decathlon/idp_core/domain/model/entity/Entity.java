package com.decathlon.idp_core.domain.model.entity;

import static com.decathlon.idp_core.domain.constant.ValidationsMessages.TEMPLATE_IDENTIFIER_MANDATORY;

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

        String name,

        String identifier,

        List<Property> properties,

        List<Relation> relations
) {
}
