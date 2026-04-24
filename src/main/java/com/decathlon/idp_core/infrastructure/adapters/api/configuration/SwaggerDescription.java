package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/// Centralized OpenAPI documentation constants for consistent API descriptions.
///
/// **Documentation standardization rationale:** Maintains consistency across all API
/// endpoints by centralizing descriptions, response messages, and field documentation.
/// Prevents duplication and ensures uniform language throughout the API.
///
/// **Organization strategy:**
/// - HTTP status codes and standard responses
/// - Endpoint summaries and descriptions by domain (templates, entities)
/// - Schema and field descriptions for comprehensive API documentation
/// - Pagination parameter descriptions for consistent query interfaces
///
/// **Maintenance benefits:** Single point of truth for API documentation strings,
/// enabling easy updates and internationalization if needed in the future.

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SwaggerDescription {
    /// HTTP response status codes for OpenAPI documentation
    public static final String OK_CODE = "200";
    public static final String CREATED_CODE = "201";
    public static final String NO_CONTENT_CODE = "204";
    public static final String PARTIAL_CONTENT_CODE = "206";
    public static final String BAD_REQUEST_CODE = "400";
    public static final String UNAUTHORIZED_CODE = "401";
    public static final String FORBIDDEN_CODE = "403";
    public static final String NOT_FOUND_CODE = "404";
    public static final String CONFLICT_CODE = "409";
    public static final String SERVICE_UNAVAILABLE_CODE = "503";
    public static final String INTERNAL_SERVER_ERROR_CODE = "500";

    /// Entity Template API endpoint constants
    public static final String ENDPOINT_GET_TEMPLATES_SUMMARY = "Get all templates";
    public static final String ENDPOINT_GET_TEMPLATES_DESCRIPTION = "Retrieve a list of all available templates in the system";

    public static final String ENDPOINT_GET_TEMPLATES_PAGINATED_SUMMARY = "Get paginated templates";
    public static final String ENDPOINT_GET_TEMPLATES_PAGINATED_DESCRIPTION = "Retrieve a paginated list of templates with optional sorting";

    public static final String ENDPOINT_GET_TEMPLATE_BY_ID_SUMMARY = "Get template by ID";
    public static final String ENDPOINT_GET_TEMPLATE_BY_ID_DESCRIPTION = "Retrieve a specific template using its unique identifier";

    public static final String ENDPOINT_GET_TEMPLATE_BY_IDENTIFIER_SUMMARY = "Get template by identifier";
    public static final String ENDPOINT_GET_TEMPLATE_BY_IDENTIFIER_DESCRIPTION = "Retrieve a specific template using its string identifier";

    public static final String ENDPOINT_POST_TEMPLATE_SUMMARY = "Create a new template";
    public static final String ENDPOINT_POST_TEMPLATE_DESCRIPTION = "Create a new template in the system with the provided information";
    public static final String ENDPOINT_PUT_TEMPLATE_SUMMARY = "Update an existing template by template identifier";
    public static final String ENDPOINT_PUT_TEMPLATE_DESCRIPTION = "Update the details of an existing template identified by its unique string identifier";

    public static final String ENDPOINT_DELETE_TEMPLATE_SUMMARY = "Delete template by identifier";
    public static final String ENDPOINT_DELETE_TEMPLATE_DESCRIPTION = "Remove a template from the system using its unique identifier";

    /// Entity API endpoint constants
    public static final String ENDPOINT_GET_ENTITIES_SUMMARY = "Get entities by template identifier";
    public static final String ENDPOINT_GET_ENTITIES_DESCRIPTION = "Retrieve a list of all available entities in the system";

    public static final String ENDPOINT_GET_ENTITIES_PAGINATED_SUMMARY = "Get paginated entities";
    public static final String ENDPOINT_GET_ENTITIES_PAGINATED_DESCRIPTION = "Retrieve a paginated list of entities with optional sorting";

    public static final String ENDPOINT_GET_ENTITY_BY_IDENTIFIER_SUMMARY = "Get entity by entity template and identifier";
    public static final String ENDPOINT_GET_ENTITY_BY_IDENTIFIER_DESCRIPTION = "Retrieve a specific entity using its string identifier and its template identifier";

    public static final String ENDPOINT_POST_ENTITY_SUMMARY = "Create a new entity";
    public static final String ENDPOINT_POST_ENTITY_DESCRIPTION = "Create a new entity in the system with the provided information";


    /// API response description constants
    public static final String RESPONSE_TEMPLATES_PAGINATED_SUCCESS = "Paginated templates retrieved successfully";
    public static final String RESPONSE_TEMPLATES_PARTIAL_CONTENT = "Partial content - paginated templates retrieved (subset of total data)";
    public static final String RESPONSE_TEMPLATE_FOUND = "Template found";
    public static final String RESPONSE_TEMPLATE_CREATED = "Template created successfully";
    public static final String RESPONSE_TEMPLATE_UPDATED = "Template update successfully";
    public static final String RESPONSE_TEMPLATE_DELETED = "Template deleted successfully";
    public static final String RESPONSE_TEMPLATE_NOT_FOUND_ID = "Template not found with the provided ID";
    public static final String RESPONSE_TEMPLATE_NOT_FOUND_IDENTIFIER = "Template not found with the provided identifier";
    public static final String RESPONSE_INVALID_UUID = "Invalid UUID format";
    public static final String RESPONSE_INVALID_TEMPLATE_DATA = "Invalid template data provided";
    public static final String RESPONSE_INVALID_PAGINATION = "Invalid pagination parameters";
    public static final String RESPONSE_TEMPLATE_CONFLICT = "Template with this identifier already exists";
    public static final String RESPONSE_ENTITY_CONFLICT = "Entity already exists in this template";
    public static final String RESPONSE_ENTITIES_PAGINATED_SUCCESS = "Paginated entities retrieved successfully";
    public static final String RESPONSE_ENTITY_FOUND = "Entity found";
    public static final String RESPONSE_ENTITY_NOT_FOUND_IDENTIFIER = "Entity not found with the provided identifier";
    public static final String RESPONSE_ENTITY_CREATED = "Entity created successfully";
    public static final String RESPONSE_INVALID_ENTITY_DATA = "Invalid entity data provided";
    public static final String RESPONSE_UNEXPECTED_SERVER_ERROR = "Unexpected server-side failure";
    public static final String RESPONSE_INSUFFICIENT_RIGHTS = "Insufficient rights";
    public static final String RESPONSE_UNAUTHORIZED = "Unauthorized - Missing or invalid token";


    // --- Schema (class) descriptions ---
    public static final String SCHEMA_ENTITY_TEMPLATE_CREATE_IN = "Input DTO for creating an entity template";
    public static final String SCHEMA_ENTITY_TEMPLATE_UPDATE_IN = "Input DTO for updating an entity template";
    public static final String SCHEMA_PROPERTY_DEFINITION_IN = "Input DTO for creating or updating a property definition";
    public static final String SCHEMA_RELATION_DEFINITION_IN = "Input DTO for creating or updating a relation definition";
    public static final String SCHEMA_PROPERTY_RULES_IN = "Input DTO for property validation rules";
    public static final String SCHEMA_ENTITY_TEMPLATE_OUT = "Output DTO for entity template";
    public static final String SCHEMA_PROPERTY_DEFINITION_OUT = "Output DTO for property definition";
    public static final String SCHEMA_RELATION_DEFINITION_OUT = "Output DTO for relation definition";
    public static final String SCHEMA_PROPERTY_RULES_OUT = "Output DTO for property validation rules";
    public static final String SCHEMA_ENTITY_IN = "Input DTO for creating or updating an entity";
    public static final String SCHEMA_ENTITY_RELATION_IN = "Input DTO for an entity relation instance";

    // --- Field descriptions (shared) ---
    public static final String FIELD_TEMPLATE_ID = "Unique generated identifier of the entity template";
    public static final String FIELD_TEMPLATE_IDENTIFIER = "Unique Entity Template identifier";
    public static final String FIELD_TEMPLATE_NAME = "Unique Entity Template name";
    public static final String FIELD_TEMPLATE_DESCRIPTION = "Entity Template description";
    public static final String FIELD_TEMPLATE_PROPERTIES = "List of property definitions for this template";
    public static final String FIELD_TEMPLATE_RELATIONS = "List of relation definitions for this template";

    public static final String FIELD_ENTITY_NAME = "Name of the entity";
    public static final String FIELD_ENTITY_IDENTIFIER = "Unique identifier of the entity within the template scope";
    public static final String FIELD_ENTITY_PROPERTIES = "Map of property name to value for this entity";
    public static final String FIELD_ENTITY_RELATIONS = "List of relations for this entity";
    public static final String FIELD_ENTITY_RELATION_NAME = "Name of the relation (must match a template relation definition)";
    public static final String FIELD_ENTITY_RELATION_TARGETS = "List of target entity identifiers for this relation";

    public static final String FIELD_PROPERTY_ID = "Unique identifier of the property definition";
    public static final String FIELD_PROPERTY_NAME = "Property name";
    public static final String FIELD_PROPERTY_DESCRIPTION = "Property description";
    public static final String FIELD_PROPERTY_TYPE = "Property data type";
    public static final String FIELD_PROPERTY_REQUIRED = "Whether this property is required";
    public static final String FIELD_PROPERTY_RULES = "Property validation rules";

    public static final String FIELD_PROPERTY_RULES_ID = "Unique identifier of the property rules";
    public static final String FIELD_PROPERTY_RULES_FORMAT = "Format of the property";
    public static final String FIELD_PROPERTY_RULES_ENUM_VALUES = "Allowed enum values for the property";
    public static final String FIELD_PROPERTY_RULES_REGEX = "Regular expression for property validation";
    public static final String FIELD_PROPERTY_RULES_MAX_LENGTH = "Maximum length of the property";
    public static final String FIELD_PROPERTY_RULES_MIN_LENGTH = "Minimum length of the property";
    public static final String FIELD_PROPERTY_RULES_MAX_VALUE = "Maximum value for the property";
    public static final String FIELD_PROPERTY_RULES_MIN_VALUE = "Minimum value for the property";
    public static final String FIELD_CREATED_AT = "Creation timestamp";
    public static final String FIELD_UPDATED_AT = "Last update timestamp";

    public static final String FIELD_RELATION_ID = "Unique identifier of the relation definition";
    public static final String FIELD_RELATION_NAME = "Name of the relation";
    public static final String FIELD_RELATION_TARGET_IDENTIFIER = "Identifier of the target template";
    public static final String FIELD_RELATION_REQUIRED = "Whether this relation is required";
    public static final String FIELD_RELATION_TO_MANY = "Whether this relation can have multiple targets";

    // --- Pagination and sorting parameter descriptions ---
    public static final String PARAM_PAGE_DESCRIPTION = "Page number for pagination. Defaults to 0.";
    public static final String PARAM_SIZE_DESCRIPTION = "Number of items per page. Defaults to 20.";
    public static final String PARAM_SORT_DESCRIPTION = "Sorting criteria in the format: property(,asc|desc). Defaults to identifier,asc.";
}
