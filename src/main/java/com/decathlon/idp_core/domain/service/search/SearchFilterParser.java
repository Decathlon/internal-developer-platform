package com.decathlon.idp_core.domain.service.search;

import java.util.List;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.constant.SearchConstraints;
import com.decathlon.idp_core.domain.constant.ValidationMessages;
import com.decathlon.idp_core.domain.exception.search.InvalidSearchQueryException;
import com.decathlon.idp_core.domain.model.entity.RawSearchFilterNode;
import com.decathlon.idp_core.domain.model.entity.SearchFilterNode;
import com.decathlon.idp_core.domain.model.enums.LogicalConnector;
import com.decathlon.idp_core.domain.model.enums.SearchOperator;

/// Domain service that converts a [RawSearchFilterNode] tree into a validated [SearchFilterNode] tree.
///
/// **Responsibility:** Parses the raw string representation produced by the infrastructure mapper and
/// enforces all structural and safety business rules:
/// <ul>
///   <li>Maximum nesting depth ([SearchConstraints#MAX_NESTING_DEPTH])</li>
///   <li>Maximum total criteria count ([SearchConstraints#MAX_TOTAL_CRITERIA])</li>
///   <li>Required fields on criterion nodes (field, operation, value)</li>
///   <li>Required fields on group nodes (connector, non-empty criteria)</li>
///   <li>Valid enum values for connectors and operators</li>
/// </ul>
///
/// Semantic validation (field name grammar, numeric operator constraints, query length) is
/// handled separately by [SearchFilterValidationService].
///
/// Throws [InvalidSearchQueryException] for all structural and safety violations.
@Service
public class SearchFilterParser {

    /// Parses and validates a [RawSearchFilterNode] tree into a typed [SearchFilterNode] tree.
    ///
    /// @param raw the root of the raw filter tree; null is treated as "no filter" and returns an empty AND group
    /// @return the validated, type-safe domain filter tree
    /// @throws InvalidSearchQueryException when the raw tree contains structural errors or exceeds safety limits
    public SearchFilterNode parse(RawSearchFilterNode raw) {
        if (raw == null) {
            return new SearchFilterNode.Group(LogicalConnector.AND, List.of());
        }
        return convertNode(raw, 0, new int[]{0});
    }

    private SearchFilterNode convertNode(RawSearchFilterNode raw, int depth, int[] criteriaCounter) {
        if (depth > SearchConstraints.MAX_NESTING_DEPTH) {
            throw new InvalidSearchQueryException(
                    ValidationMessages.SEARCH_NESTING_TOO_DEEP.formatted(SearchConstraints.MAX_NESTING_DEPTH));
        }
        return switch (raw) {
            case RawSearchFilterNode.Group group -> convertGroup(group, depth, criteriaCounter);
            case RawSearchFilterNode.Criterion criterion -> convertCriterion(criterion, criteriaCounter);
        };
    }

    private SearchFilterNode.Group convertGroup(RawSearchFilterNode.Group raw, int depth, int[] criteriaCounter) {
        if (raw.connector() == null || raw.connector().isBlank()) {
            throw new InvalidSearchQueryException(ValidationMessages.SEARCH_GROUP_MISSING_CONNECTOR);
        }
        if (raw.nodes() == null || raw.nodes().isEmpty()) {
            throw new InvalidSearchQueryException(ValidationMessages.SEARCH_GROUP_MISSING_CRITERIA);
        }

        var connector = parseConnector(raw.connector());
        List<SearchFilterNode> children = raw.nodes().stream()
                .map(child -> convertNode(child, depth + 1, criteriaCounter))
                .toList();

        return new SearchFilterNode.Group(connector, children);
    }

    private SearchFilterNode.Criterion convertCriterion(RawSearchFilterNode.Criterion raw, int[] criteriaCounter) {
        if (raw.field() == null || raw.field().isBlank()) {
            throw new InvalidSearchQueryException(ValidationMessages.SEARCH_CRITERION_MISSING_FIELD);
        }
        if (raw.operation() == null || raw.operation().isBlank()) {
            throw new InvalidSearchQueryException(ValidationMessages.SEARCH_CRITERION_MISSING_OPERATION);
        }
        if (raw.value() == null || raw.value().isBlank()) {
            throw new InvalidSearchQueryException(ValidationMessages.SEARCH_CRITERION_MISSING_VALUE);
        }

        criteriaCounter[0]++;
        if (criteriaCounter[0] > SearchConstraints.MAX_TOTAL_CRITERIA) {
            throw new InvalidSearchQueryException(
                    ValidationMessages.SEARCH_TOO_MANY_CRITERIA.formatted(SearchConstraints.MAX_TOTAL_CRITERIA));
        }

        var operator = parseOperator(raw.operation());
        return new SearchFilterNode.Criterion(raw.field(), operator, raw.value());
    }

    private LogicalConnector parseConnector(String raw) {
        try {
            return LogicalConnector.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException _) {
            throw new InvalidSearchQueryException(
                    ValidationMessages.SEARCH_INVALID_CONNECTOR.formatted(raw));
        }
    }

    private SearchOperator parseOperator(String raw) {
        try {
            return SearchOperator.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException _) {
            throw new InvalidSearchQueryException(
                    ValidationMessages.SEARCH_INVALID_OPERATOR.formatted(raw));
        }
    }
}
