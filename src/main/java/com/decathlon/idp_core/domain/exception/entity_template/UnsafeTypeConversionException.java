package com.decathlon.idp_core.domain.exception.entity_template;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_UNSAFE_TYPE_CONVERSION;

import com.decathlon.idp_core.domain.model.enums.PropertyType;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiExceptionHandler;

/// Exception thrown when attempting an unsafe property type change on a property that already has data.
///
/// Unsafe type conversions are those where existing values cannot be safely converted:
/// - STRING → NUMBER
/// - STRING → BOOLEAN
/// - NUMBER → BOOLEAN
/// - BOOLEAN → NUMBER
///
/// This exception represents a business rule violation where type changes are blocked
/// to prevent data loss or corruption.
///
/// **Why this exception exists:**
/// - Protects data integrity by preventing unsafe type conversions
/// - Provides domain-specific error information for clear API feedback
/// - Mapped to HTTP 400 Bad Request by [ApiExceptionHandler]
public class UnsafeTypeConversionException extends RuntimeException {

    /// Constructs a new exception for an unsafe type conversion.
    ///
    /// @param propertyName the name of the property whose type is being changed
    /// @param fromType the current property type
    /// @param toType the requested new property type
    public UnsafeTypeConversionException(String propertyName, PropertyType fromType, PropertyType toType) {
        super(String.format(PROPERTY_UNSAFE_TYPE_CONVERSION, propertyName, fromType, toType));
    }
}
