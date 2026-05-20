package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.constant.ValidationMessages;
import com.decathlon.idp_core.domain.exception.InvalidQueryException;
import com.decathlon.idp_core.domain.model.entity.SearchFilterNode;
import com.decathlon.idp_core.domain.model.enums.LogicalConnector;
import com.decathlon.idp_core.domain.model.enums.SearchOperator;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.FilterNodeDtoIn;

/// Maps a [FilterNodeDtoIn] tree to its domain counterpart [SearchFilterNode].
///
/// **Validation responsibilities:**
/// - Validates that each node has the required fields for its type (group vs. criterion).
/// - Validates connector and operation values against known enums.
/// - Validates field names against the supported field syntax.
/// - Enforces safety limits: maximum nesting depth and maximum total criteria count.
///
/// Throws [InvalidQueryException] for any validation failure so that the caller
/// (the [ApiExceptionHandler]) can translate it to HTTP 400.
@Component
public class EntitySearchDomainMapper {

    public static final int MAX_NESTING_DEPTH = 5;
    public static final int MAX_TOTAL_CRITERIA = 50;
    public static final int MAX_QUERY_LENGTH = 255;
    public static final int MAX_PAGE_SIZE = 500;

    private static final String PROPERTY_PREFIX = "property.";
    private static final String RELATION_PREFIX = "relation.";
    private static final String RELATIONS_AS_TARGET_PREFIX = "relations_as_target.";
    private static final Set<String> SIMPLE_FIELDS = Set.of("template", "identifier", "name", "relation");
    private static final Set<SearchOperator> NUMERIC_OPERATORS =
            Set.of(SearchOperator.GT, SearchOperator.GTE, SearchOperator.LT,
                   SearchOperator.LTE);

    /// Validates the free-text `query` string from the search request.
    ///
    /// @param query the query string to validate; may be null (no-op)
    /// @throws InvalidQueryException when the query exceeds the maximum length
    public void validateQuery(String query) {
        if (query != null && query.length() > MAX_QUERY_LENGTH) {
            throw new InvalidQueryException(
                    ValidationMessages.SEARCH_QUERY_TOO_LONG.formatted(MAX_QUERY_LENGTH));
        }
    }

    /// Converts a nullable [FilterNodeDtoIn] to a [SearchFilterNode].
    ///
    /// @param dto the root node DTO; may be null, in which case an empty group is returned
    /// @return the domain representation of the filter tree
    /// @throws InvalidQueryException when the DTO tree contains validation errors
    public SearchFilterNode toDomain(FilterNodeDtoIn dto) {
        if (dto == null) {
            return new SearchFilterNode.Group(LogicalConnector.AND, List.of());
        }
        var counter = new int[]{0};
        return convertNode(dto, 0, counter);
    }

    private SearchFilterNode convertNode(FilterNodeDtoIn dto, int depth, int[] criteriaCounter) {
        if (depth > MAX_NESTING_DEPTH) {
            throw new InvalidQueryException(
                    ValidationMessages.SEARCH_NESTING_TOO_DEEP.formatted(MAX_NESTING_DEPTH));
        }
        if (isGroupNode(dto)) {
            return convertGroup(dto, depth, criteriaCounter);
        }
        return convertCriterion(dto, criteriaCounter);
    }

    private boolean isGroupNode(FilterNodeDtoIn dto) {
        return dto.connector() != null || dto.criteria() != null;
    }

    private SearchFilterNode.Group convertGroup(FilterNodeDtoIn dto, int depth, int[] criteriaCounter) {
        if (dto.connector() == null || dto.connector().isBlank()) {
            throw new InvalidQueryException(ValidationMessages.SEARCH_GROUP_MISSING_CONNECTOR);
        }
        if (dto.criteria() == null || dto.criteria().isEmpty()) {
            throw new InvalidQueryException(ValidationMessages.SEARCH_GROUP_MISSING_CRITERIA);
        }

        var connector = parseConnector(dto.connector());
        List<SearchFilterNode> children = dto.criteria().stream()
                .map(child -> convertNode(child, depth + 1, criteriaCounter))
                .toList();

        return new SearchFilterNode.Group(connector, children);
    }

    private SearchFilterNode.Criterion convertCriterion(FilterNodeDtoIn dto, int[] criteriaCounter) {
        if (dto.field() == null || dto.field().isBlank()) {
            throw new InvalidQueryException(ValidationMessages.SEARCH_CRITERION_MISSING_FIELD);
        }
        if (dto.operation() == null || dto.operation().isBlank()) {
            throw new InvalidQueryException(ValidationMessages.SEARCH_CRITERION_MISSING_OPERATION);
        }
        if (dto.value() == null || dto.value().isBlank()) {
            throw new InvalidQueryException(ValidationMessages.SEARCH_CRITERION_MISSING_VALUE);
        }

        criteriaCounter[0]++;
        if (criteriaCounter[0] > MAX_TOTAL_CRITERIA) {
            throw new InvalidQueryException(
                    ValidationMessages.SEARCH_TOO_MANY_CRITERIA.formatted(MAX_TOTAL_CRITERIA));
        }

        var operator = parseOperator(dto.operation());
        validateField(dto.field());
        validateNumericOperatorConstraints(operator, dto.field(), dto.value());

        return new SearchFilterNode.Criterion(dto.field(), operator, dto.value());
    }

    private LogicalConnector parseConnector(String raw) {
        try {
            return LogicalConnector.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException _) {
            throw new InvalidQueryException(
                    ValidationMessages.SEARCH_INVALID_CONNECTOR.formatted(raw));
        }
    }

    private SearchOperator parseOperator(String raw) {
        try {
            return SearchOperator.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException _) {
            throw new InvalidQueryException(
                    ValidationMessages.SEARCH_INVALID_OPERATOR.formatted(raw));
        }
    }

    private void validateField(String field) {
        if (SIMPLE_FIELDS.contains(field)) {
            return;
        }
        if (field.startsWith(PROPERTY_PREFIX) && field.length() > PROPERTY_PREFIX.length()) {
            return;
        }
        if (field.startsWith(RELATIONS_AS_TARGET_PREFIX)) {
            validateRelationsAsTargetField(field);
            return;
        }
        if (field.startsWith(RELATION_PREFIX) && field.length() > RELATION_PREFIX.length()) {
            return;
        }
        throw new InvalidQueryException(ValidationMessages.SEARCH_INVALID_FIELD.formatted(field));
    }

    private void validateNumericOperatorConstraints(SearchOperator operator, String field, String value) {
        if (!NUMERIC_OPERATORS.contains(operator)) {
            return;
        }
        if (!field.startsWith(PROPERTY_PREFIX) || field.length() <= PROPERTY_PREFIX.length()) {
            throw new InvalidQueryException(
                    ValidationMessages.SEARCH_NUMERIC_OPERATOR_REQUIRES_PROPERTY.formatted(operator));
        }
        try {
            new BigDecimal(value);
        } catch (NumberFormatException _) {
            throw new InvalidQueryException(
                    ValidationMessages.SEARCH_NUMERIC_OPERATOR_INVALID_VALUE.formatted(value, operator));
        }
    }

    private void validateRelationsAsTargetField(String field) {
        String rest = field.substring(RELATIONS_AS_TARGET_PREFIX.length());
        int dot = rest.indexOf('.');
        if (dot <= 0 || dot == rest.length() - 1) {
            throw new InvalidQueryException(ValidationMessages.SEARCH_INVALID_FIELD.formatted(field));
        }
    }
}
