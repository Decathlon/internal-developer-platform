package com.decathlon.idp_core.domain.model.entity_template;

import java.util.List;
import java.util.UUID;

import com.decathlon.idp_core.domain.model.enums.PropertyFormat;

/**
 * Pure domain model representing validation rules for a PropertyDefinition.
 * Immutable value object.
 */
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
    /**
     * Compact constructor for defensive copying.
     * Ensures the list is immutable to prevent external modification.
     */
    public PropertyRules {
        enumValues = enumValues != null ? List.copyOf(enumValues) : null;
    }
}
