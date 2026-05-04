package com.decathlon.idp_core.domain.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationMessages {

    // Entity Template validation messages
    public static final String TEMPLATE_ALREADY_EXISTS = "An Entity Template already exists with the same identifier";
    public static final String TEMPLATE_IDENTIFIER_MANDATORY = "Entity Template identifier is mandatory and cannot be blank";
    public static final String PROPERTY_DEFINITIONS_MANDATORY = "Entity Template property definitions are mandatory and cannot be empty";
    public static final String TEMPLATE_NAME_ALREADY_EXISTS = "The entity template name %s already exists";
    public static final String TEMPLATE_NAME_MANDATORY = "Entity template name is mandatory and cannot be blank";
    public static final String TEMPLATE_NAME_MAX_SIZE = "Entity template name must not exceed 255 characters";
    public static final String TEMPLATE_NAME_FORMAT = "Entity template name must only use alphanumeric characters, spaces, hyphens or underscores";

    // Property Definition validation messages
    public static final String PROPERTY_NAME_MANDATORY = "Property name is mandatory and cannot be blank";
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

    // Relation Definition validation messages
    public static final String RELATION_NAME_MANDATORY = "Relation name is mandatory and cannot be blank";
    public static final String RELATION_TARGET_IDENTIFIER_MANDATORY = "Target entity identifier is mandatory and cannot be blank";
    public static final String RELATION_NAME_MANDATORY_SIMPLE = "Relation name is mandatory";
    public static final String RELATION_TARGET_IDENTIFIER_MANDATORY_SIMPLE = "Relation target identifier is mandatory";
    public static final String RELATION_TARGET_IDENTIFIERS_NOT_NULL = "Target entity identifiers cannot be null";

    // Entity input validation messages
    public static final String ENTITY_NAME_MANDATORY = "Entity name is mandatory and cannot be blank";
    public static final String ENTITY_IDENTIFIER_MANDATORY = "Entity identifier is mandatory and cannot be blank";

    // Entity creation validation messages
    public static final String ENTITY_NOT_FOUND = "Entity not found with template identifier %s and entity identifier '%s'";
    public static final String ENTITY_ALREADY_EXISTS = "Entity with name '%s' already exists for template '%s'";
    public static final String ENTITY_VALIDATION_FAILED = "Entity validation failed: ";
}
