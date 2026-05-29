package com.decathlon.idp_core.domain.exception.entity_template;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_TYPE_CANNOT_CHANGE;

import com.decathlon.idp_core.domain.model.enums.PropertyType;

/// Exception thrown when attempting any property type change.
///
/// This exception represents a business rule violation where type changes are blocked
/// to prevent data loss or corruption.
///
/// **Why this exception exists:**
/// - Protects data integrity by preventing type conversions
/// - Provides domain-specific error information for clear API feedback
/// - Mapped to HTTP 400 Bad Request by [ApiExceptionHandler]
public class PropertyTypeChangeException extends RuntimeException {

  /// Constructs a new exception for a type conversion.
  ///
  /// @param propertyName the name of the property whose type is being changed
  /// @param fromType the current property type
  /// @param toType the requested new property type
  public PropertyTypeChangeException(String propertyName, PropertyType fromType,
      PropertyType toType) {
    super(String.format(PROPERTY_TYPE_CANNOT_CHANGE, propertyName, fromType, toType));
  }
}
