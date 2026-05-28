package com.decathlon.idp_core.domain.exception.entity_template;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_NAME_ALREADY_EXISTS;

/// Exception thrown when attempting to create or update an [EntityTemplate] with duplicate property names.
///
/// This exception represents a business rule violation where unique constraints on property
/// names within a template are enforced at the application level.
///
/// **Why this exception exists:**
/// - Enforces business invariant that property names must be unique within a template
/// - Provides domain-specific error information for clear API feedback
/// - Mapped to HTTP 400 Bad Request by [ApiExceptionHandler]
public class PropertyNameAlreadyExistsException extends RuntimeException {

  /// Constructs a new exception with the duplicate property name.
  ///
  /// @param propertyName the property name that appears more than once
  public PropertyNameAlreadyExistsException(String propertyName) {
    super(String.format(PROPERTY_NAME_ALREADY_EXISTS, propertyName));
  }
}
