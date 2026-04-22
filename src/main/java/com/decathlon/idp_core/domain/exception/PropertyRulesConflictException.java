package com.decathlon.idp_core.domain.exception;

import com.decathlon.idp_core.domain.model.enums.PropertyType;

/// Domain exception for property rule validation violations.
///
/// **Business purpose:** Represents the business rule violation when property rules
/// conflict with their assigned property type. This ensures data integrity
/// by preventing invalid rule configurations before persistence.
///
/// **Exception design rationale:**
/// - Reports specific property name and type for debugging clarity
/// - Includes detailed violation message explaining the constraint failure
/// - Domain-level exception keeps business logic separate from HTTP concerns
///
/// **Usage patterns:**
/// - Property template creation with invalid rules
/// - Template updates introducing rule conflicts
/// - Batch validation of property definitions
public class PropertyRulesConflictException extends RuntimeException {

    /// Constructs a new exception for rule type conflict.
    ///
    /// **Why this exists:** Provides standardized error message format when
    /// a rule parameter (format, enum_values, regex, etc.) is not supported
    /// for the given property type.
    ///
    /// @param propertyName the name of the property with invalid rules
    /// @param propertyType the data type of the property
    /// @param violationMessage detailed explanation of what rule is invalid
    public PropertyRulesConflictException(String propertyName, PropertyType propertyType, String violationMessage) {
        super("Property '" + propertyName + "' of type " + propertyType + 
              ": " + violationMessage);
    }

    /// Constructs a new exception with a custom error message.
    ///
    /// **Why this exists:** Allows for flexible error messages when the standard
    /// format doesn't capture the specific validation scenario.
    ///
    /// @param message the detail message explaining the rule conflict
    public PropertyRulesConflictException(String message) {
        super(message);
    }

    /// Constructs a new exception with a message and underlying cause.
    ///
    /// **Why this exists:** Used when the exception wraps another exception or
    /// when additional context about the underlying cause is needed for debugging.
    ///
    /// @param message the detail message explaining the rule conflict
    /// @param cause the underlying cause of this exception
    public PropertyRulesConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
