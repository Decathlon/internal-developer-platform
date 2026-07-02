package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import java.util.List;

import com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription;

import io.swagger.v3.oas.annotations.media.Schema;

/// Request body for the `POST /api/v1/entities/search` endpoint.
///
/// Supports two complementary search modes that can be combined:
/// - `query` — a free-text string searched across identifier, name,
///   templateIdentifier, and all property values (case-insensitive CONTAINS).
/// - `filter` — a structured, nested filter tree for precise queries.
///
/// ### Free-text search example
/// ```
/// { "query": "checkout", "page": 0, "size": 20 }
/// ```
///
/// ### Structured filter example
/// ```
/// {
///   "filter": {
///     "connector": "AND",
///     "criteria": [
///       { "field": "template",  "operation": "EQ", "value": "microservice" },
///       { "field": "property.language", "operation": "EQ", "value": "JAVA" }
///     ]
///   },
///   "page": 0,
///   "size": 20,
///   "sort": "identifier:asc"
/// }
/// ```
@Schema(description = SwaggerDescription.SCHEMA_ENTITY_SEARCH_REQUEST_IN)
public record EntitySearchDepthRequestDtoIn(

    @Schema(description = SwaggerDescription.FIELD_SEARCH_FILTER) List<FilterNodeDtoIn> filters,

    @Schema(description = SwaggerDescription.PARAM_PAGE_DESCRIPTION, defaultValue = "0", example = "0") Integer page,

    @Schema(description = SwaggerDescription.PARAM_SIZE_DESCRIPTION, defaultValue = "20", example = "20") Integer size,

    @Schema(description = SwaggerDescription.PARAM_SORT_DESCRIPTION, example = "identifier:asc") String sort,

    Integer depth,

    List<String> allowedRelations

) {
}
