package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity;

import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.enums.PropertyType;

/// Utility for converting property values to their typed representations.
///
/// **Purpose:** Centralized property type conversion logic used across multiple
/// infrastructure mappers to ensure consistent type handling and reduce code
/// duplication.
///
/// **Design:** Stateless utility class with no side effects, suitable for use
/// across multiple adapters and mappers.
public final class PropertyValueConverter {

  private PropertyValueConverter() {
    // Prevent instantiation of utility class
  }

  /// Converts a property value to its typed representation based on the property
  /// definition.
  ///
  /// **Type conversion strategy:**
  /// - `NUMBER`: Attempts to parse as `Double`; falls back to string if invalid
  /// - `BOOLEAN`: Parses using `Boolean.valueOf()`
  /// - Other types: Returns the raw string value
  ///
  /// **Null safety:** If no definition is provided, returns the raw string value
  /// as fallback for schema evolution tolerance.
  ///
  /// @param property the property to convert
  /// @param definition the property definition for type information (may be null)
  /// @return the typed value, or the raw string value if type conversion fails
  public static Object convert(Property property, PropertyDefinition definition) {
    String value = property.value();

    if (definition == null) {
      return value;
    }

    PropertyType type = definition.type();

    if (PropertyType.NUMBER.equals(type)) {
      try {
        return Double.valueOf(value);
      } catch (NumberFormatException _) {
        return value;
      }
    } else if (PropertyType.BOOLEAN.equals(type)) {
      return Boolean.valueOf(value);
    }

    return value;
  }
}
