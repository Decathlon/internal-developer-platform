package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import io.swagger.v3.oas.annotations.media.Schema;

/// Request body for the {@code POST /api/v1/entities/search} endpoint.
///
/// Supports two complementary search modes that can be combined:
/// <ul>
///   <li>{@code query} — a free-text string searched across identifier, name,
///       templateIdentifier, and all property values (case-insensitive CONTAINS).</li>
///   <li>{@code filter} — a structured, nested filter tree for precise queries.</li>
/// </ul>
/// When both are provided the results must satisfy both (AND semantics).
///
/// <h3>Free-text search example</h3>
/// <pre>{@code
/// { "query": "checkout", "page": 0, "size": 20 }
/// }</pre>
///
/// <h3>Structured filter example</h3>
/// <pre>{@code
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
/// }</pre>
@Schema(description = "Request body for the POST /api/v1/entities/search endpoint")
public record EntitySearchRequestDtoIn(

        @Schema(description = "Free-text search string. When present, returns entities whose identifier, name, templateIdentifier, or any property value contains this string (case-insensitive). Can be combined with filter.", example = "checkout")
        String query,

        @Schema(description = "Root node of the search filter tree. May be omitted or null to return all entities.")
        FilterNodeDtoIn filter,

        @Schema(description = "Zero-based page index. Defaults to 0.", defaultValue = "0", example = "0")
        Integer page,

        @Schema(description = "Number of entities per page. Defaults to 20.", defaultValue = "20", example = "20")
        Integer size,

        @Schema(description = "Sort expression in the form field:asc|desc, e.g. identifier:asc.", example = "identifier:asc")
        String sort
) {
    public EntitySearchRequestDtoIn {
        if (size == null || size <= 0) {
            size = 20;
        }
        if (page == null || page < 0) {
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
