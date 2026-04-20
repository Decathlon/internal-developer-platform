package com.decathlon.idp_core.domain.model.entity_template;

import java.util.List;
import java.util.UUID;

import com.decathlon.idp_core.domain.model.enums.PropertyFormat;

/// Business rules and constraints for property validation in [EntityTemplate].
///
/// Forms part of the ubiquitous language where properties must conform to business rules
/// such as format validation, value boundaries, and enumerated constraints. These rules
/// ensure data integrity at the domain boundary before persistence.
///
/// **Business invariants:**
/// - Format constraints are enforced through [PropertyFormat] enum values
/// - String constraints: `minLength` ≤ actual length ≤ `maxLength`
/// - Numeric constraints: `minValue` ≤ actual value ≤ `maxValue`
/// - Enumeration constraints: values must be in `enumValues` list when specified
/// - Regular expression patterns provide additional validation when `regex` is defined
public record PropertyRules(
    UUID id,
    PropertyFormat format,
    List<String> enumValues,
    String regex,
    Integer maxLength,
    Integer minLength,
    Integer maxValue,
    Integer minValue
) {
    /// Ensures immutable defensive copying of enumeration values.
    ///
    /// **Why this exists:** Prevents external mutation of enum constraints after construction,
    /// maintaining business rule integrity throughout the entity lifecycle.
    public PropertyRules {
        enumValues = enumValues != null ? List.copyOf(enumValues) : null;
    }
}
