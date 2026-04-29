package com.decathlon.idp_core.domain.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationMessages {

    // Entity Template validation messages
    public static final String TEMPLATE_ALREADY_EXISTS = "An Entity Template already exists with the same identifier";
    public static final String TEMPLATE_IDENTIFIER_MANDATORY = "Entity Template identifier is mandatory and cannot be blank";
    public static final String TEMPLATE_NAME_ALREADY_EXISTS = "The entity template name %s already exists";
    public static final String TEMPLATE_NAME_MANDATORY = "Entity template name is mandatory and cannot be blank";
    public static final String TEMPLATE_NAME_MAX_SIZE = "Entity template name must not exceed 255 characters";
    public static final String TEMPLATE_NAME_FORMAT = "Entity template name must only use alphanumeric characters, spaces, hyphens or underscores";

    // Property Definition validation messages
    public static final String PROPERTY_NAME_MANDATORY = "Property name is mandatory and cannot be blank";
    public static final String PROPERTY_DESCRIPTION_MANDATORY = "Property description is mandatory and cannot be blank";
    public static final String PROPERTY_TYPE_MANDATORY = "Property type is mandatory";
    public static final String PROPERTY_VALUE_MANDATORY = "Property value is mandatory and cannot be blank";

    // Relation Definition validation messages
    public static final String RELATION_NAME_MANDATORY = "Relation name is mandatory and cannot be blank";
    public static final String RELATION_TARGET_IDENTIFIER_MANDATORY = "Target template identifier is mandatory and cannot be blank";
    public static final String RELATION_NAME_MANDATORY_SIMPLE = "Relation name is mandatory";
    public static final String RELATION_TARGET_IDENTIFIER_MANDATORY_SIMPLE = "Relation target identifier is mandatory";
    public static final String RELATION_TARGET_IDENTIFIERS_NOT_NULL = "Target entity identifiers cannot be null";

    // Property Rules validation messages - templates and specific constraints
    public static final String PROPERTY_RULES_RULE_NOT_ALLOWED_FOR_TYPE = "{rule} rule is not allowed for {type} property type";
    public static final String PROPERTY_RULES_MIN_MAX_CONSTRAINT_VIOLATED = "min_{constraint} must be less than or equal to max_{constraint}";
    public static final String PROPERTY_RULES_MIN_LENGTH_NON_NEGATIVE = "min_length must be greater than or equal to 0";
    public static final String PROPERTY_RULES_MAX_LENGTH_POSITIVE = "max_length must be greater than 0";
    public static final String PROPERTY_RULES_BOOLEAN_NOT_ALLOWED = "Boolean properties do not accept any rules";
    public static final String PROPERTY_RULES_NUMERIC_RULE_NOT_ALLOWED = "Numeric rule {rule} is not allowed for STRING properties";

    // Helper method to construct rule-not-allowed message
    public static String ruleNotAllowed(String rule, String propertyType) {
        return PROPERTY_RULES_RULE_NOT_ALLOWED_FOR_TYPE
                .replace("{rule}", rule)
                .replace("{type}", propertyType);
    }

    // Helper method to construct min/max constraint violation message
    public static String minMaxConstraintViolated(String constraint) {
        return PROPERTY_RULES_MIN_MAX_CONSTRAINT_VIOLATED
                .replace("{constraint}", constraint);
    }
}
