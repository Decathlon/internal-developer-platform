package com.decathlon.idp_core.domain.service.entity_template;

import com.decathlon.idp_core.domain.exception.entity_template.PropertyDefinitionRulesConflictException;
import com.decathlon.idp_core.domain.exception.entity_template.PropertyNameAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity_template.UnsafeTypeConversionException;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.PropertyRules;
import com.decathlon.idp_core.domain.model.enums.PropertyType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_RULES_BOOLEAN_NOT_ALLOWED;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_RULES_MAX_LENGTH_POSITIVE;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_RULES_MIN_LENGTH_NON_NEGATIVE;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_RULES_NUMERIC_RULE_NOT_ALLOWED;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.minMaxConstraintViolated;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.ruleNotAllowed;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.rulesAreIncompatible;

/// Domain service for validating property definitions and their compatibility with property types.
///
/// **Business rules:**
/// - STRING: Allows format, enum_values, regex, max_length, min_length. Rejects numeric rules.
/// - NUMBER: Allows max_value, min_value. Rejects string and format rules.
/// - BOOLEAN: Rejects all rules; rules field must be null or empty.
///
/// **Key responsibilities:**
/// - Validate property name uniqueness within an entity template
/// - Enforce type conversion constraints considering existing entity data
/// - Apply type conversion safety rules (safe vs. unsafe conversions)
/// - Type-to-rule compatibility validation
/// - Constraint ordering validation (min ≤ max)
/// - Regex pattern validation (delegated to [PropertyRegexValidationService])
///
@Service
@RequiredArgsConstructor
public class PropertyDefinitionValidationService {

    private final PropertyRegexValidationService propertyRegexValidationService;

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

    /// Validates that all property names are unique within a template.
    ///
    /// **Contract:** Enforces the invariant that property names must be unique. Used
    /// during template creation and updates to prevent duplicate property
    /// definitions.
    ///
    /// @param properties the list of property definitions to validate
    /// @throws PropertyNameAlreadyExistsException if duplicate property names
    ///                                            are found
    public void validatePropertyNamesUniqueness(List<PropertyDefinition> properties) {
        Set<String> names = new HashSet<>();
        for (PropertyDefinition property : properties) {
            if (property.name() != null) {
                String normalizedName = property.name().toLowerCase();
                if (!names.add(normalizedName)) {
                    throw new PropertyNameAlreadyExistsException(property.name());
                }
            }
        }
    }

    /// Validates that property types are not changed on existing properties.
    ///
    /// **Contract:** Enforces the invariant that property types cannot be modified
    /// after initial creation. Any attempt to change a property type is forbidden.
    /// Users must delete and recreate the property if they need to change its type.
    ///
    /// @param existingProperties the existing property definitions
    /// @param updatedProperties  the new/updated property definitions
    /// @throws UnsafeTypeConversionException if any property type change is attempted
    public void validateTypeChanges(List<PropertyDefinition> existingProperties, List<PropertyDefinition> updatedProperties) {
        if (existingProperties == null || existingProperties.isEmpty() ||
                updatedProperties == null || updatedProperties.isEmpty()) {
            return;
        }
        Map<String, PropertyDefinition> updatedMap = updatedProperties.stream()
                .collect(Collectors.toMap(PropertyDefinition::name, p -> p));

        for (PropertyDefinition existing : existingProperties) {
            PropertyDefinition updated = updatedMap.get(existing.name());
            boolean propertyTypeChanged = updated != null && !existing.type().equals(updated.type());

            if (propertyTypeChanged) {
                throw new UnsafeTypeConversionException(
                        existing.name(),
                        existing.type(),
                        updated.type());
            }
        }
    }

    /// Validates property rules are compatible with the property's data type.
    ///
    /// **Contract:** Performs comprehensive validation including:
    /// - Rule type compatibility with property type
    /// - Numeric constraint ordering (min ≤ max)
    /// - Boolean properties reject all rules
    ///
    /// @param propertyDefinition the property definition containing type and rules
    /// @throws PropertyDefinitionRulesConflictException when rules violate business invariants
    public void validatePropertyDefinitionRules(PropertyDefinition propertyDefinition) {
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
    /// **Conflicting rules:** format, regex, and enum_values are mutually exclusive;
    ///   enum_values is also mutually exclusive with max_length and min_length
    /// **Constraints:** 0 ≤ min_length ≤ max_length, regex must be valid
    ///
    /// @param propertyName name of the property (for error reporting)
    /// @param rules the property rules to validate
    /// @throws PropertyDefinitionRulesConflictException when rules defined violate any of the above constraints
    private void validateStringPropertyRules(String propertyName, PropertyRules rules) {
        validateStringIncompatibleRules(propertyName, rules);
        validateStringConstraints(propertyName, rules);

        // Validate regex pattern is valid
        if (rules.regex() != null && !rules.regex().isBlank()) {
            propertyRegexValidationService.validateRegexPattern(propertyName, rules.regex());
        }
    }

    /// Validates numeric constraints for STRING property rules.
    ///
    /// **Constraints enforced:**
    /// - min_length must be non-negative (≥ 0)
    /// - max_length must be positive (> 0)
    /// - min_length must be less than or equal to max_length
    ///
    /// @param propertyName name of the property (for error reporting)
    /// @param rules the property rules to validate
    /// @throws PropertyDefinitionRulesConflictException when any constraint is violated
    private void validateStringConstraints(String propertyName, PropertyRules rules) {
        // Validate min_length is non-negative
        if (rules.minLength() != null && rules.minLength() < 0) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    PROPERTY_RULES_MIN_LENGTH_NON_NEGATIVE
            );
        }
        // Validate max_length is not zero or negative
        if (rules.maxLength() != null && rules.maxLength() <= 0) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    PROPERTY_RULES_MAX_LENGTH_POSITIVE
            );
        }
        // Validate min_length is below or equal to max_length
        if (rules.minLength() != null && rules.maxLength() != null && rules.minLength() > rules.maxLength()) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    minMaxConstraintViolated(LENGTH)
            );
        }
    }

    /// Validates rule compatibility and mutual exclusivity for STRING property rules.
    ///
    /// **Incompatibility rules enforced:**
    /// - Numeric rules (max_value, min_value) are not allowed for STRING type
    /// - format, regex, and enum_values are mutually exclusive
    /// - enum_values and length constraints (max_length, min_length) are mutually exclusive
    ///
    /// @param propertyName name of the property (for error reporting)
    /// @param rules the property rules to validate
    /// @throws PropertyDefinitionRulesConflictException when incompatible rules are both present
    private void validateStringIncompatibleRules(String propertyName, PropertyRules rules){
        // Reject numeric rules for STRING type
        if (rules.maxValue() != null || rules.minValue() != null) {
            String ruleName = rules.maxValue() != null ? MAX_VALUE : MIN_VALUE;
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    PROPERTY_RULES_NUMERIC_RULE_NOT_ALLOWED.replace("{rule}", ruleName)
            );
        }

        // format, regex, and enum_values are incompatible with each other
        if (rules.format() != null && rules.enumValues() != null) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    rulesAreIncompatible(FORMAT, ENUM_VALUES)
            );
        }
        if (rules.format() != null && rules.regex() != null) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    rulesAreIncompatible(FORMAT, REGEX)
            );
        }
        if (rules.regex() != null && rules.enumValues() != null) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    rulesAreIncompatible(REGEX, ENUM_VALUES)
            );
        }

        // enum_values and length constraints are incompatible with each other
        if (rules.enumValues() != null && rules.maxLength() != null) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    rulesAreIncompatible(ENUM_VALUES, MAX_LENGTH)
            );
        }
        if (rules.enumValues() != null && rules.minLength() != null) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.STRING,
                    rulesAreIncompatible(ENUM_VALUES, MIN_LENGTH)
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
    /// @throws PropertyDefinitionRulesConflictException when string rules are present
    ///         or min/max value constraints are violated
    private void validateNumberPropertyRules(String propertyName, PropertyRules rules) {
        if (rules.format() != null) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.NUMBER,
                    ruleNotAllowed(FORMAT, PropertyType.NUMBER.name())
            );
        }

        if (rules.enumValues() != null) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.NUMBER,
                    ruleNotAllowed(ENUM_VALUES, PropertyType.NUMBER.name())
            );
        }

        if (rules.regex() != null) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.NUMBER,
                    ruleNotAllowed(REGEX, PropertyType.NUMBER.name())
            );
        }

        if (rules.minLength() != null) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.NUMBER,
                    ruleNotAllowed(MIN_LENGTH, PropertyType.NUMBER.name())
            );
        }

        if (rules.maxLength() != null) {
            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.NUMBER,
                    ruleNotAllowed(MAX_LENGTH, PropertyType.NUMBER.name())
            );
        }

        if (rules.minValue() != null && rules.maxValue() != null && rules.minValue() > rules.maxValue()) {
            throw new PropertyDefinitionRulesConflictException(
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
    /// @throws PropertyDefinitionRulesConflictException when any rule is set for BOOLEAN
    private void validateBooleanPropertyRules(String propertyName, PropertyRules rules) {
        if (rules.format() != null ||
                rules.enumValues() != null ||
                rules.regex() != null ||
                rules.maxLength() != null ||
                rules.minLength() != null ||
                rules.maxValue() != null ||
                rules.minValue() != null) {

            throw new PropertyDefinitionRulesConflictException(
                    propertyName,
                    PropertyType.BOOLEAN,
                    PROPERTY_RULES_BOOLEAN_NOT_ALLOWED
            );
        }
    }

}
