package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity;

import java.util.Collection;

import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.enums.PropertyType;

/// Utility for preserving property values in their JSON-compatible
/// representations.
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
  /// Native JSON values are already correctly typed by Jackson and persistence.
  /// Legacy scalar strings are converted only where the template still requires a
  /// numeric or boolean value. Collections are returned unchanged so their shape
  /// and element types cannot be collapsed into a scalar.
  ///
  /// **Null safety:** If no definition is provided, returns the raw value as a
  /// fallback for schema evolution tolerance.
  ///
  /// @param property the property to convert
  /// @param definition the property definition for type information (may be null)
  /// @return the JSON-compatible value
  public static Object convert(Property property, PropertyDefinition definition) {
    Object value = property.value();

    if (definition == null) {
      return value;
    }

    if (value.getClass().isArray() || value instanceof Collection) {
      return value;
    }

    PropertyType type = definition.type();

    if (PropertyType.NUMBER.equals(type)) {
      if (value instanceof Number) {
        return value;
      }
      try {
        return Double.valueOf(value.toString());
      } catch (NumberFormatException _) {
        return value;
      }
    } else if (PropertyType.BOOLEAN.equals(type)) {
      if (value instanceof Boolean) {
        return value;
      }
      if ("true".equalsIgnoreCase(value.toString()) || "false".equalsIgnoreCase(value.toString())) {
        return Boolean.valueOf(value.toString());
      }
    }

    return value;
  }
}
