package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

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
public record EntitySearchRequestDtoIn(

    @Schema(description = SwaggerDescription.FIELD_SEARCH_QUERY, example = "checkout") String query,

    @Schema(description = SwaggerDescription.FIELD_SEARCH_FILTER) FilterNodeDtoIn filter,

    @Schema(description = SwaggerDescription.PARAM_PAGE_DESCRIPTION, defaultValue = "0", example = "0") Integer page,

    @Schema(description = SwaggerDescription.PARAM_SIZE_DESCRIPTION, defaultValue = "20", example = "20") Integer size,

    @Schema(description = SwaggerDescription.PARAM_SORT_DESCRIPTION, example = "identifier:asc") String sort) {
  public EntitySearchRequestDtoIn {
    if (size == null) {
      size = 20;
    }
    if (page == null) {
      page = 0;
    }
    if (query != null) {
      query = query.strip();
      if (query.isBlank()) {
        query = null;
      }
    }
  }
}
