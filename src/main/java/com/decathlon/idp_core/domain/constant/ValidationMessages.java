package com.decathlon.idp_core.domain.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationMessages {

    // Entity Template validation messages
    public static final String TEMPLATE_ALREADY_EXISTS = "An Entity Template already exists with the same identifier";
    public static final String TEMPLATE_IDENTIFIER_NOT_FOUND = "Target template with identifier '%s' does not exist.";
    public static final String TEMPLATE_IDENTIFIER_MANDATORY = "Entity Template identifier is mandatory and cannot be blank";
    public static final String TEMPLATE_IDENTIFIER_CANNOT_CHANGE = "Entity Template identifier cannot be changed. Current identifier: ";
    public static final String TEMPLATE_NAME_ALREADY_EXISTS = "The entity template name %s already exists";
    public static final String TEMPLATE_NAME_MANDATORY = "Entity template name is mandatory and cannot be blank";
    public static final String TEMPLATE_NAME_MAX_SIZE = "Entity template name must not exceed 255 characters";
    public static final String TEMPLATE_NAME_FORMAT = "Entity template name must only use alphanumeric characters, spaces, hyphens or underscores";

    // Property Definition validation messages
    public static final String PROPERTY_NAME_MANDATORY = "Property name is mandatory and cannot be blank";
    public static final String PROPERTY_NAME_ALREADY_EXISTS = "Property name '%s' already exists within the template. Property names must be unique.";
    public static final String PROPERTY_DESCRIPTION_MANDATORY = "Property description is mandatory and cannot be blank";
    public static final String PROPERTY_TYPE_MANDATORY = "Property type is mandatory";
    public static final String PROPERTY_VALUE_MANDATORY = "Property value is mandatory and cannot be blank";
    public static final String PROPERTY_REQUIRED_MISSING = "Property '%s' is required by template '%s'";
    public static final String PROPERTY_TYPE_MISMATCH = "Property '%s' must be of type %s";
    public static final String PROPERTY_MIN_LENGTH_VIOLATION = "Property '%s' length must be greater than or equal to %d";
    public static final String PROPERTY_MAX_LENGTH_VIOLATION = "Property '%s' length must be lower than or equal to %d";
    public static final String PROPERTY_MIN_VALUE_VIOLATION = "Property '%s' value must be greater than or equal to %d";
    public static final String PROPERTY_MAX_VALUE_VIOLATION = "Property '%s' value must be lower than or equal to %d";
    public static final String PROPERTY_REGEX_VIOLATION = "Property '%s' does not match expected format";
    public static final String PROPERTY_ENUM_VIOLATION = "Property '%s' must be one of %s";
    public static final String PROPERTY_FORMAT_VIOLATION = "Property '%s' does not match required format %s";
    public static final String PROPERTY_TYPE_CANNOT_CHANGE = "Cannot change type of property '%s' from %s to %s. Property types cannot be modified after creation. Please delete and recreate the property instead.";

    // Property Rules validation messages - templates and specific constraints
    public static final String PROPERTY_RULES_RULE_NOT_ALLOWED_FOR_TYPE = "{rule} rule is not allowed for {type} property type";
    public static final String PROPERTY_RULES_MIN_MAX_CONSTRAINT_VIOLATED = "min_{constraint} must be less than or equal to max_{constraint}";
    public static final String PROPERTY_RULES_MIN_LENGTH_NON_NEGATIVE = "min_length must be greater than or equal to 0";
    public static final String PROPERTY_RULES_MAX_LENGTH_POSITIVE = "max_length must be greater than 0";
    public static final String PROPERTY_RULES_BOOLEAN_NOT_ALLOWED = "Boolean properties do not accept any rules";
    public static final String PROPERTY_RULES_NUMERIC_RULE_NOT_ALLOWED = "Numeric rule {rule} is not allowed for STRING properties";
    public static final String PROPERTY_RULES_MUTUALLY_EXCLUSIVE = "{rule1} and {rule2} are mutually exclusive for STRING properties";
    public static final String PROPERTY_RULES_REGEX_INVALID = "Invalid regex pattern: %s";

    // Relation Definition validation messages
    public static final String RELATION_NAME_MANDATORY = "Relation name is mandatory and cannot be blank";
    public static final String RELATION_TARGET_IDENTIFIER_MANDATORY = "Target template identifier is mandatory and cannot be blank";
    public static final String RELATION_NAME_MANDATORY_SIMPLE = "Relation name is mandatory";
    public static final String RELATION_NAME_ALREADY_EXISTS = "Relation name '%s' already exists within the template. Relation names must be unique.";
    public static final String RELATION_TARGET_IDENTIFIER_MANDATORY_SIMPLE = "Relation target identifier is mandatory";
    public static final String RELATION_TARGET_IDENTIFIERS_NOT_NULL = "Target entity identifiers cannot be null";
    public static final String RELATION_NOT_DEFINED_IN_TEMPLATE = "Relation '%s' is not defined in template '%s'";
    public static final String RELATION_REQUIRED_MISSING = "Relation '%s' is required by template '%s'";
    public static final String RELATION_TOO_MANY_TARGETS = "Relation '%s' allows only one target in template '%s'";
    public static final String RELATION_TARGET_TEMPLATE_CANNOT_CHANGE = "Cannot change target template of relation '%s' from '%s' to '%s'. Target template cannot be modified after creation. Please delete and recreate the relation instead.";
    public static final String RELATION_CANNOT_TARGET_ITSELF = "Relation '%s' cannot reference its own template '%s' as the target.";

    // Entity input validation messages
    public static final String ENTITY_NAME_MANDATORY = "Entity name is mandatory and cannot be blank";
    public static final String ENTITY_IDENTIFIER_MANDATORY = "Entity identifier is mandatory and cannot be blank";
    public static final String ENTITY_IDENTIFIER_MUST_MATCH_PATH = "Entity identifier in body must match path identifier";

    // Entity creation validation messages
    public static final String ENTITY_NOT_FOUND = "Entity not found with template identifier %s and entity identifier '%s'";
    public static final String ENTITY_ALREADY_EXISTS = "Entity with name '%s' already exists for template '%s'";
    public static final String ENTITY_VALIDATION_FAILED = "Entity validation failed: ";

    // Helper method to construct rules incompatibility message
    public static String rulesAreIncompatible(String rule1, String rule2) {
        return PROPERTY_RULES_MUTUALLY_EXCLUSIVE
                .replace("{rule1}", rule1)
                .replace("{rule2}", rule2);
    }

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

    // Search filter validation messages
    public static final String SEARCH_INVALID_CONNECTOR = "Invalid connector '%s'. Supported values: AND, OR";
    public static final String SEARCH_INVALID_OPERATOR = "Invalid operation '%s'. Supported values: EQ, NEQ, CONTAINS, NOT_CONTAINS, STARTS_WITH, ENDS_WITH, GT, GTE, LT, LTE";
    public static final String SEARCH_INVALID_FIELD = "Unknown field '%s'. Supported fields: template, identifier, name, relation, property.{name}, relation.{name}, relation.{name}.identifier, relation.{name}.name, relations_as_target, relations_as_target.{name}.identifier, relations_as_target.{name}.name";
    public static final String SEARCH_TOO_MANY_CRITERIA = "Search filter exceeds maximum of %d total criteria";
    public static final String SEARCH_NESTING_TOO_DEEP = "Search filter exceeds maximum nesting depth of %d";
    public static final String SEARCH_CRITERION_MISSING_FIELD = "A criterion node must have a non-blank 'field'";
    public static final String SEARCH_CRITERION_MISSING_OPERATION = "A criterion node must have a non-blank 'operation'";
    public static final String SEARCH_CRITERION_MISSING_VALUE = "A criterion node must have a non-blank 'value'";
    public static final String SEARCH_GROUP_MISSING_CONNECTOR = "A group node must have a non-blank 'connector'";
    public static final String SEARCH_GROUP_MISSING_CRITERIA = "A group node must have a non-empty 'criteria' list";
    public static final String SEARCH_QUERY_TOO_LONG = "Search query must not exceed %d characters";
    public static final String SEARCH_NUMERIC_OPERATOR_REQUIRES_PROPERTY = "Operator '%s' is only valid for property.{name} fields";
    public static final String SEARCH_INVALID_SORT_FIELD = "Invalid sort field '%s'. Supported fields: identifier, name, templateIdentifier";
    public static final String SEARCH_PAGE_SIZE_TOO_LARGE = "Page size must not exceed %d";
    public static final String SEARCH_NUMERIC_OPERATOR_INVALID_VALUE = "Value '%s' is not a valid number for operator '%s'";
    public static final String SEARCH_NUMERIC_OPERATOR_PROPERTY_TYPE_MISMATCH = "Property '%s' in template '%s' is of type %s; operators GT, GTE, LT, LTE require type NUMBER";

}
