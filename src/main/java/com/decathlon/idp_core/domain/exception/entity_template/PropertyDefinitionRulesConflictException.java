package com.decathlon.idp_core.domain.exception.entity_template;

import com.decathlon.idp_core.domain.model.enums.PropertyType;

/// Domain exception for property rule validation violations.
///
/// **Business purpose:** Represents the business rule violation when property rules
/// conflict with their assigned property type. This ensures data integrity
/// by preventing invalid rule configurations before persistence.
///
/// **Usage patterns:**
/// - Property template creation with invalid rules
/// - Property template updates introducing rule conflicts
public class PropertyDefinitionRulesConflictException extends RuntimeException {

    /// Constructs a new exception for rule type conflict.
    ///
    /// @param propertyName     the name of the property with invalid rules
    /// @param propertyType     the data type of the property
    /// @param violationMessage detailed explanation of what rule is invalid
    public PropertyDefinitionRulesConflictException(String propertyName, PropertyType propertyType, String violationMessage) {
        super("Property '" + propertyName + "' of type " + propertyType +
                ": " + violationMessage);
    }
}
