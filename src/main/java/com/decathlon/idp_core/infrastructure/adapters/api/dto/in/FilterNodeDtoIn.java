package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import java.util.List;

import com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription;

import io.swagger.v3.oas.annotations.media.Schema;

/// A node in the search filter tree, used in the request body of
/// `POST /api/v1/entities/search`.
///
/// Each node is either a **group** or a **criterion** (leaf node):
/// A **group** must have a `connector` (AND/OR) and a non-empty `criteria` list.
/// A **criterion** must have a `field`, an `operation`, and a `value`.
///
/// Both types share the same JSON object shape; unused fields should be omitted or set to null.
@Schema(description = SwaggerDescription.SCHEMA_FILTER_NODE)
public record FilterNodeDtoIn(

    @Schema(description = SwaggerDescription.FIELD_FILTER_CONNECTOR, example = "AND") String connector,

    @Schema(description = SwaggerDescription.FIELD_FILTER_CRITERIA) List<FilterNodeDtoIn> criteria,

    @Schema(description = SwaggerDescription.FIELD_FILTER_FIELD, example = "template") String field,

    @Schema(description = SwaggerDescription.FIELD_FILTER_OPERATION, example = "EQ") String operation,

    @Schema(description = SwaggerDescription.FIELD_FILTER_VALUE, example = "microservice") String value) {
}
