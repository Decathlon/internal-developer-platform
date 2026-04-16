package com.decathlon.idp_core.domain.model.entity;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_NAME_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_VALUE_MANDATORY;

import java.util.UUID;

import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.PropertyRules;
import com.decathlon.idp_core.domain.model.enums.PropertyType;

import jakarta.validation.constraints.NotBlank;

/// A concrete property instance belonging to an [Entity].
///
/// Represents actual business data values that conform to the constraints defined
/// in the corresponding [PropertyDefinition] within the entity's template.
/// Properties are the "filled-in" values of the template's property schema.
///
/// **Business invariants:**
/// - Property names must match a [PropertyDefinition] name in the entity's template
/// - Property values must satisfy all validation rules from [PropertyRules]
/// - Required properties cannot have empty values
/// - Property types must align with the template's [PropertyType] definition
public record Property(
    UUID id,

    @NotBlank(message = PROPERTY_NAME_MANDATORY)
    String name,

    @NotBlank(message = PROPERTY_VALUE_MANDATORY)
    String value
) {
}
