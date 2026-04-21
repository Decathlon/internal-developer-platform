package com.decathlon.idp_core.domain.model.entity_template;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_DESCRIPTION_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_NAME_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_TYPE_MANDATORY;

import java.util.UUID;

import com.decathlon.idp_core.domain.model.enums.PropertyType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/// Defines the structure and constraints for a property within an [EntityTemplate].
///
/// Part of the domain's ubiquitous language where each property represents a business
/// attribute that entities can possess. Properties define both the data structure
/// (name, type, description) and behavioral constraints (required flag, validation rules).
///
/// **Business invariants:**
/// - Property names must be unique within an EntityTemplate context
/// - Required properties cannot be null/empty when creating entities
/// - Validation rules in [PropertyRules] are enforced for all property values
/// - Property descriptions support business documentation and user guidance
public record PropertyDefinition(
    UUID id,

    @NotBlank(message = PROPERTY_NAME_MANDATORY)
    String name,

    @NotBlank(message = PROPERTY_DESCRIPTION_MANDATORY)
    String description,

    @NotNull(message = PROPERTY_TYPE_MANDATORY)
    PropertyType type,

    boolean required,

    PropertyRules rules
) {
}
