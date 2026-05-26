package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/// A node in the search filter tree, used in the request body of
/// `POST /api/v1/entities/search`.
///
/// Each node is either a **group** or a **criterion** (leaf node):
/// A **group** must have a `connector` (AND/OR) and a non-empty `criteria` list.
/// A **criterion** must have a `field`, an `operation`, and a `value`.
///
/// Both types share the same JSON object shape; unused fields should be omitted or set to null.
@Schema(description = "A node in the search filter tree. Either a logical group (connector + criteria) or a leaf criterion (field + operation + value).")
public record FilterNodeDtoIn(

        @Schema(description = "Logical connector for a group node. One of: AND, OR. Required for group nodes.", example = "AND")
        String connector,

        @Schema(description = "Child filter nodes for a group node. Required for group nodes (must be non-empty).")
        List<FilterNodeDtoIn> criteria,

        @Schema(description = "Field to filter on for a criterion node. Required for leaf nodes. Examples: template, identifier, name, relation, property.language, relation.api-link, relation.api-link.identifier, relations_as_target.api-link.name", example = "template")
        String field,

        @Schema(description = "Filter operation for a criterion node. One of: EQ, NEQ, CONTAINS, NOT_CONTAINS, STARTS_WITH, ENDS_WITH, GT, GTE, LT, LTE. Required for leaf nodes.", example = "EQ")
        String operation,

        @Schema(description = "Value to compare against for a criterion node. Required for leaf nodes.", example = "microservice")
        String value) {

    public FilterNodeDtoIn {
        criteria = criteria == null ? null : Collections.unmodifiableList(List.copyOf(criteria));
    }

    @Override
    public List<FilterNodeDtoIn> criteria() {
        return criteria == null ? null : Collections.unmodifiableList(criteria);
    }
}
