package com.decathlon.idp_core.domain.model.enums;

import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;

/// Fundamental data types supported for entity properties in [PropertyDefinition].
///
/// Defines the basic business data types that can be stored and validated within
/// the dynamic entity system. These types align with common business data patterns
/// and provide the foundation for type-safe property validation.
///
/// **Business purpose:**
/// - Ensures type safety for dynamic entity properties
/// - Provides consistent data representation across persistence and APIs
/// - Supports validation rule application based on data type
public enum PropertyType {
    STRING,
    NUMBER,
    BOOLEAN
}
