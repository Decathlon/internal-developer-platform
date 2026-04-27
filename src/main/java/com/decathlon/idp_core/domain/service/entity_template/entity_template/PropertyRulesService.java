package com.decathlon.idp_core.domain.service.entity_template.entity_template;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_RULES_BOOLEAN_NOT_ALLOWED;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_RULES_MIN_VALUE_NON_NEGATIVE;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.minMaxConstraintViolated;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.ruleNotAllowed;

import com.decathlon.idp_core.domain.exception.PropertyRulesConflictException;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.PropertyRules;
import com.decathlon.idp_core.domain.model.enums.PropertyType;

/// Domain service for validating property rule compatibility with property types.
///
/// Provides pure validation functions ensuring property rules conform to their assigned
/// data types. Enforces business invariants around which rules apply to each type and
/// validates numeric constraints (min ≤ max).
///
/// **Business rules:**
/// - STRING: Allows format, enum_values, regex, max_length, min_length. Rejects numeric rules.
/// - NUMBER: Allows max_value, min_value. Rejects string and format rules.
/// - BOOLEAN: Rejects all rules; rules field must be null or empty.
///
/// **Design principles:**
/// - Pure functions: No side effects, no state mutation
/// - Single responsibility: Only validates rules, doesn't throw or log
/// - Testable: Can be tested independently without Spring context
public final class PropertyRulesService {

    private PropertyRulesService() {
        // Utility class - prevent instantiation
    }

    /// Validates property rules are compatible with the property's data type.
    ///
    /// **Contract:** Performs comprehensive validation including:
    /// - Rule type compatibility with property type
    /// - Numeric constraint ordering (min ≤ max)
    /// - Boolean properties reject all rules
    ///
    /// @param propertyDefinition the property definition containing type and rules
    /// @throws PropertyRulesConflictException when rules violate business invariants
    public static void validatePropertyRules(PropertyDefinition propertyDefinition) {
        if (propertyDefinition.rules() == null) {
            return;
        }

        PropertyRules rules = propertyDefinition.rules();
        PropertyType type = propertyDefinition.type();

        switch (type) {
            case STRING:
                validateStringPropertyRules(propertyDefinition.name(), rules);
                break;
            case NUMBER:
                validateNumberPropertyRules(propertyDefinition.name(), rules);
                break;
            case BOOLEAN:
                validateBooleanPropertyRules(propertyDefinition.name(), rules);
                break;
            default:
                throw new IllegalArgumentException("Unknown property type: " + type);
        }
    }

    /// Validates rules for STRING property type.
    ///
    /// **Allowed rules:** format, enum_values, regex, max_length, min_length
    /// **Rejected rules:** max_value, min_value (numeric)
    /// **Constraints:** 0 ≤ min_length ≤ max_length
    ///
    /// @param propertyName name of the property (for error reporting)
    /// @param rules the property rules to validate
    /// @throws PropertyRulesConflictException when numeric rules are present
    ///         or min/max length constraints are violated
    private static void validateStringPropertyRules(String propertyName, PropertyRules rules) {
        // Reject numeric rules for STRING type
        if (rules.maxValue() != null || rules.minValue() != null) {
            String violation = rules.maxValue() != null ?
                    "Numeric rule maxValue is not allowed for STRING properties" :
                    "Numeric rule minValue is not allowed for STRING properties";
            throw new PropertyRulesConflictException(propertyName, PropertyType.STRING, violation);
        }

        // Validate min_length and max_length constraints
        if (rules.minLength() != null && rules.maxLength() != null) {
            if (rules.minLength() > rules.maxLength()) {
                throw new PropertyRulesConflictException(
                        propertyName,
                        PropertyType.STRING,
                        minMaxConstraintViolated("length")
                );
            }
        }

        // Validate min_length is non-negative
        if (rules.minLength() != null && rules.minLength() < 0) {
            throw new PropertyRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    PROPERTY_RULES_MIN_VALUE_NON_NEGATIVE
            );
        }
    }

    /// Validates rules for NUMBER property type.
    ///
    /// **Allowed rules:** max_value, min_value
    /// **Rejected rules:** format, enum_values, regex, max_length, min_length (string)
    /// **Constraints:** min_value ≤ max_value
    ///
    /// @param propertyName name of the property (for error reporting)
    /// @param rules the property rules to validate
    /// @throws PropertyRulesConflictException when string rules are present
    ///         or min/max value constraints are violated
    private static void validateNumberPropertyRules(String propertyName, PropertyRules rules) {
        // Reject string-related rules for NUMBER type
        if (rules.format() != null) {
            throw new PropertyRulesConflictException(
                    propertyName,
                    PropertyType.NUMBER,
                    ruleNotAllowed("format", "NUMBER")
            );
        }

        if (rules.enumValues() != null && !rules.enumValues().isEmpty()) {
            throw new PropertyRulesConflictException(
                    propertyName,
                    PropertyType.NUMBER,
                    ruleNotAllowed("enum_values", "NUMBER")
            );
        }

        if (rules.regex() != null && !rules.regex().isBlank()) {
            throw new PropertyRulesConflictException(
                    propertyName,
                    PropertyType.NUMBER,
                    ruleNotAllowed("regex", "NUMBER")
            );
        }

        if (rules.minLength() != null) {
            throw new PropertyRulesConflictException(
                    propertyName,
                    PropertyType.NUMBER,
                    ruleNotAllowed("min_length", "NUMBER")
            );
        }

        if (rules.maxLength() != null) {
            throw new PropertyRulesConflictException(
                    propertyName,
                    PropertyType.NUMBER,
                    ruleNotAllowed("max_length", "NUMBER")
            );
        }

        // Validate min_value and max_value constraints
        if (rules.minValue() != null && rules.maxValue() != null) {
            if (rules.minValue() > rules.maxValue()) {
                throw new PropertyRulesConflictException(
                        propertyName,
                        PropertyType.NUMBER,
                        minMaxConstraintViolated("value")
                );
            }
        }
    }

    /// Validates rules for BOOLEAN property type.
    ///
    /// **Allowed rules:** None
    /// **Rejected rules:** All rules must be null or empty
    ///
    /// @param propertyName name of the property (for error reporting)
    /// @param rules the property rules to validate
    /// @throws PropertyRulesConflictException when any rule is set for BOOLEAN
    private static void validateBooleanPropertyRules(String propertyName, PropertyRules rules) {
        // Check if any rule field is set
        if (rules.format() != null ||
                (rules.enumValues() != null && !rules.enumValues().isEmpty()) ||
                (rules.regex() != null && !rules.regex().isBlank()) ||
                rules.maxLength() != null ||
                rules.minLength() != null ||
                rules.maxValue() != null ||
                rules.minValue() != null) {

            throw new PropertyRulesConflictException(
                    propertyName,
                    PropertyType.BOOLEAN,
                    PROPERTY_RULES_BOOLEAN_NOT_ALLOWED
            );
        }
    }
}
