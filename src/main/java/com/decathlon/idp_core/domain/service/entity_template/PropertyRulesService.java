package com.decathlon.idp_core.domain.service.entity_template;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_RULES_BOOLEAN_NOT_ALLOWED;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_RULES_MAX_LENGTH_POSITIVE;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_RULES_MIN_LENGTH_NON_NEGATIVE;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_RULES_NUMERIC_RULE_NOT_ALLOWED;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.minMaxConstraintViolated;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.ruleNotAllowed;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.springframework.stereotype.Service;
import com.decathlon.idp_core.domain.exception.PropertyRulesConflictException;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.PropertyRules;
import com.decathlon.idp_core.domain.model.enums.PropertyType;

/// Domain service for validating property rule compatibility with property types.
///
/// **Business rules:**
/// - STRING: Allows format, enum_values, regex, max_length, min_length. Rejects numeric rules.
/// - NUMBER: Allows max_value, min_value. Rejects string and format rules.
/// - BOOLEAN: Rejects all rules; rules field must be null or empty.
///
@Service
public class PropertyRulesService {

    // Rule name constants
    public static final String REGEX = "regex";
    public static final String LENGTH = "length";
    public static final String VALUE = "value";
    public static final String FORMAT = "format";
    public static final String ENUM_VALUES = "enum_values";
    public static final String MAX_LENGTH = "max_length";
    public static final String MIN_LENGTH = "min_length";
    public static final String MAX_VALUE = "max_value";
    public static final String MIN_VALUE = "min_value";

    /// Validates property rules are compatible with the property's data type.
    ///
    /// **Contract:** Performs comprehensive validation including:
    /// - Rule type compatibility with property type
    /// - Numeric constraint ordering (min ≤ max)
    /// - Boolean properties reject all rules
    ///
    /// @param propertyDefinition the property definition containing type and rules
    /// @throws PropertyRulesConflictException when rules violate business invariants
    public void validatePropertyRules(PropertyDefinition propertyDefinition) {
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
    /// **Constraints:** 0 ≤ min_length ≤ max_length, regex must be valid
    ///
    /// @param propertyName name of the property (for error reporting)
    /// @param rules the property rules to validate
    /// @throws PropertyRulesConflictException when numeric rules are present
    ///         or min/max length constraints are violated or regex is invalid
    private void validateStringPropertyRules(String propertyName, PropertyRules rules) {
        // Reject numeric rules for STRING type
        if (rules.maxValue() != null || rules.minValue() != null) {
            String ruleName = rules.maxValue() != null ? MAX_VALUE : MIN_VALUE;
            throw new PropertyRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    PROPERTY_RULES_NUMERIC_RULE_NOT_ALLOWED.replace("{rule}", ruleName)
            );
        }

        // Validate regex pattern is valid
        if (rules.regex() != null && !rules.regex().isBlank()) {
            validateRegexPattern(propertyName, rules.regex());
        }

        // Validate min_length is non-negative
        if (rules.minLength() != null && rules.minLength() < 0) {
            throw new PropertyRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    PROPERTY_RULES_MIN_LENGTH_NON_NEGATIVE
            );
        }
        // Validate max_length is not zero or negative
        if (rules.maxLength() != null && rules.maxLength() <= 0) {
            throw new PropertyRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    PROPERTY_RULES_MAX_LENGTH_POSITIVE
            );
        }
        // Validate min_length is below or equal to max_length
        if (rules.minLength() != null && rules.maxLength() != null && rules.minLength() > rules.maxLength()) {
            throw new PropertyRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    minMaxConstraintViolated(LENGTH)
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
    private void validateNumberPropertyRules(String propertyName, PropertyRules rules) {
        if (rules.format() != null) {
            throw new PropertyRulesConflictException(
                    propertyName,
                    PropertyType.NUMBER,
                    ruleNotAllowed(FORMAT, PropertyType.NUMBER.name())
            );
        }

        if (rules.enumValues() != null) {
            throw new PropertyRulesConflictException(
                    propertyName,
                    PropertyType.NUMBER,
                    ruleNotAllowed(ENUM_VALUES, PropertyType.NUMBER.name())
            );
        }

        if (rules.regex() != null) {
            throw new PropertyRulesConflictException(
                    propertyName,
                    PropertyType.NUMBER,
                    ruleNotAllowed(REGEX, PropertyType.NUMBER.name())
            );
        }

        if (rules.minLength() != null) {
            throw new PropertyRulesConflictException(
                    propertyName,
                    PropertyType.NUMBER,
                    ruleNotAllowed(MIN_LENGTH, PropertyType.NUMBER.name())
            );
        }

        if (rules.maxLength() != null) {
            throw new PropertyRulesConflictException(
                    propertyName,
                    PropertyType.NUMBER,
                    ruleNotAllowed(MAX_LENGTH, PropertyType.NUMBER.name())
            );
        }

        if (rules.minValue() != null && rules.maxValue() != null && rules.minValue() > rules.maxValue()) {
            throw new PropertyRulesConflictException(
                    propertyName,
                    PropertyType.NUMBER,
                    minMaxConstraintViolated(VALUE)
            );
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
    private void validateBooleanPropertyRules(String propertyName, PropertyRules rules) {
        if (rules.format() != null ||
                rules.enumValues() != null ||
                rules.regex() != null ||
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

    /// Validates that the provided regex pattern is syntactically valid.
    ///
    /// @param propertyName name of the property (for error reporting)
    /// @param regexPattern the regex pattern to validate
    /// @throws PropertyRulesConflictException if the pattern is syntactically invalid
    private void validateRegexPattern(String propertyName, String regexPattern) {
        try {
            Pattern.compile(regexPattern);
        } catch (PatternSyntaxException e) {
            throw new PropertyRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    "Invalid regex pattern: " + e.getMessage()
            );
        }
    }

}
