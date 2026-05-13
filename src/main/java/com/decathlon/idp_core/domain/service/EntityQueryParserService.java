package com.decathlon.idp_core.domain.service;

import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.constant.ValidationMessages;
import com.decathlon.idp_core.domain.exception.InvalidQueryException;
import com.decathlon.idp_core.domain.model.entity.EntityFilter;
import com.decathlon.idp_core.domain.model.entity.FilterCriterion;
import com.decathlon.idp_core.domain.model.enums.FilterKeyType;
import com.decathlon.idp_core.domain.model.enums.FilterOperator;

/// Parses the entity filter query string `q` into an [EntityFilter].
///
/// **Query syntax:**
/// - Criteria are separated by `;` and combined with implicit AND logic.
/// - Each criterion has the form `<key><operator><value>`
/// - Supported operators: `=` (equals), `:` (contains), `<` (less than), `>` (greater than)
/// - Key types:
///   - `identifier` or `name` - attribute filters
///   - `property.<name>` - property value filter
///   - `relation` - filter by relation name (e.g., `relation=api-link`)
///   - `relation.<name>` - relation target entity identifier filter
///   - `relation.<name>.<identifier|name>` - relation property filter; `<property>` must be `identifier` or `name`
///     (e.g., `relation.api-link.identifier=microservice-1`)
///   - `relations_as_target.<name>.<property>` - filter by a property of the source entity
///     in a reverse relation; `<property>` must be `identifier` or `name`
///     (e.g. `relations_as_target.api-link.name:microservice`)
///
/// **Security constraints:**
/// - Maximum MAX_CRITERIA_COUNT criteria per query (DoS prevention)
/// - Key names and values limited to MAX_KEY_VALUE_LENGTH characters
///
/// **Example:** `name:API;property.language=JAVA;relation=api-link;relation.database=my-db;relation.api-link.identifier=microservice-1`
@Component
public class EntityQueryParserService {

    private static final String RELATION = "relation";
    private static final String RELATIONS_AS_TARGET = "relations_as_target";
    private static final String PROPERTY_PREFIX = "property.";
    private static final String RELATION_PREFIX = "relation.";
    private static final String RELATIONS_AS_TARGET_PREFIX = "relations_as_target.";
    private static final Set<String> VALID_ATTRIBUTE_NAMES = Set.of("identifier", "name");

    private static final Set<FilterKeyType> COMPARISON_INCOMPATIBLE_TYPES = Set.of(
            FilterKeyType.RELATION_NAME,
            FilterKeyType.RELATION_ENTITY,
            FilterKeyType.RELATION_PROPERTY,
            FilterKeyType.RELATIONS_AS_TARGET_NAME,
            FilterKeyType.RELATIONS_AS_TARGET_PROPERTY);

    static final int MAX_CRITERIA_COUNT = 10;
    static final int MAX_KEY_VALUE_LENGTH = 255;

    /// Parses a query string into an [EntityFilter].
    ///
    /// @param query the raw `q` parameter value; may be null or blank
    /// @return an [EntityFilter] with parsed criteria, or [EntityFilter#empty()] when query is blank
    /// @throws InvalidQueryException when the query string is malformed or exceeds safety limits
    public EntityFilter parse(String query) {
        if (query == null || query.isBlank()) {
            return EntityFilter.empty();
        }

        List<FilterCriterion> criteria = Stream.of(query.split(";"))
                .filter(token -> !token.isBlank())
                .map(token -> parseCriterion(token.trim()))
                .toList();

        if (criteria.size() > MAX_CRITERIA_COUNT) {
            throw new InvalidQueryException(
                    ValidationMessages.FILTER_TOO_MANY_CRITERIA.formatted(MAX_CRITERIA_COUNT));
        }

        validateNoDuplicates(criteria);

        return new EntityFilter(criteria);
    }

    private FilterCriterion parseCriterion(String token) {
        int operatorIndex = findOperatorIndex(token)
                .orElseThrow(() -> new InvalidQueryException(ValidationMessages.FILTER_INVALID_FORMAT));

        var rawKey = token.substring(0, operatorIndex);
        var operatorChar = token.charAt(operatorIndex);
        var value = token.substring(operatorIndex + 1);

        if (rawKey.isBlank()) {
            throw new InvalidQueryException(
                    "Invalid filter criterion '%s': key must not be blank".formatted(token));
        }
        if (value.isBlank()) {
            throw new InvalidQueryException(
                    "Invalid filter criterion '%s': value must not be blank".formatted(token));
        }

        validateLength(rawKey, value, token);

        var operator = toOperator(operatorChar);
        var criterion = buildCriterion(rawKey, operator, value, token);
        validateOperatorCompatibility(criterion.keyType(), operator, rawKey);
        return criterion;
    }

    private OptionalInt findOperatorIndex(String token) {
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c == '=' || c == ':' || c == '<' || c == '>') {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    private FilterOperator toOperator(char c) {
        return switch (c) {
            case '=' -> FilterOperator.EQUALS;
            case ':' -> FilterOperator.CONTAINS;
            case '<' -> FilterOperator.LESS_THAN;
            case '>' -> FilterOperator.GREATER_THAN;
            default -> throw new InvalidQueryException("Unknown operator character: " + c);
        };
    }

    private FilterCriterion buildCriterion(String rawKey, FilterOperator operator, String value, String token) {
        // Direct attribute filters (relation=X means filter by relation name)
        if (RELATION.equals(rawKey)) {
            validateKeyName(value, token);
            return new FilterCriterion(FilterKeyType.RELATION_NAME, "", operator, value);
        }

        if (RELATIONS_AS_TARGET.equals(rawKey)) {
            validateKeyName(value, token);
            return new FilterCriterion(FilterKeyType.RELATIONS_AS_TARGET_NAME, "", operator, value);
        }

        if (rawKey.startsWith(PROPERTY_PREFIX)) {
            var keyName = rawKey.substring(PROPERTY_PREFIX.length());
            validateKeyName(keyName, token);
            return new FilterCriterion(FilterKeyType.PROPERTY, keyName, operator, value);
        }

        if (rawKey.startsWith(RELATIONS_AS_TARGET_PREFIX)) {
            var relationPart = rawKey.substring(RELATIONS_AS_TARGET_PREFIX.length());
            validateNonBlankKeyName(relationPart, token);
            return buildRelationsAsTargetCriterion(relationPart, operator, value, token);
        }

        if (rawKey.startsWith(RELATION_PREFIX)) {
            var relationPart = rawKey.substring(RELATION_PREFIX.length());
            validateNonBlankKeyName(relationPart, token);
            return buildRelationCriterion(relationPart, operator, value, token);
        }

        if (!VALID_ATTRIBUTE_NAMES.contains(rawKey)) {
            throw new InvalidQueryException(
                    "Unknown attribute '%s' in filter criterion '%s'. Valid attributes: %s"
                            .formatted(rawKey, token, VALID_ATTRIBUTE_NAMES));
        }
        return new FilterCriterion(FilterKeyType.ATTRIBUTE, rawKey, operator, value);
    }

    private FilterCriterion buildRelationsAsTargetCriterion(String relationPart, FilterOperator operator, String value, String token) {
        int dotIndex = relationPart.indexOf('.');
        if (dotIndex <= 0) {
            throw new InvalidQueryException(
                    "Invalid filter criterion '%s': relations_as_target requires the form 'relations_as_target.<relationName>.<identifier|name>'"
                            .formatted(token));
        }

        var relationName = relationPart.substring(0, dotIndex);
        var propertyName = relationPart.substring(dotIndex + 1);
        validateKeyName(relationName, token);
        if (!VALID_ATTRIBUTE_NAMES.contains(propertyName)) {
            throw new InvalidQueryException(
                    "Invalid property '%s' in criterion '%s': only 'identifier' and 'name' are supported for relations_as_target"
                            .formatted(propertyName, token));
        }
        var compositeKey = relationName + "." + propertyName;
        return new FilterCriterion(FilterKeyType.RELATIONS_AS_TARGET_PROPERTY, compositeKey, operator, value);
    }

    private FilterCriterion buildRelationCriterion(String relationPart, FilterOperator operator, String value, String token) {
        int dotIndex = relationPart.indexOf('.');
        if (dotIndex > 0) {
            var relationName = relationPart.substring(0, dotIndex);
            var propertyName = relationPart.substring(dotIndex + 1);
            validateKeyName(relationName, token);
            if (!VALID_ATTRIBUTE_NAMES.contains(propertyName)) {
                throw new InvalidQueryException(
                        "Invalid property '%s' in criterion '%s': only 'identifier' and 'name' are supported for relation"
                                .formatted(propertyName, token));
            }
            var compositeKey = relationName + "." + propertyName;
            return new FilterCriterion(FilterKeyType.RELATION_PROPERTY, compositeKey, operator, value);
        }

        // Default: relation entity filter
        validateKeyName(relationPart, token);
        return new FilterCriterion(FilterKeyType.RELATION_ENTITY, relationPart, operator, value);
    }

    private void validateNoDuplicates(List<FilterCriterion> criteria) {
        Set<String> seen = new HashSet<>();
        for (FilterCriterion criterion : criteria) {
            String dedupeKey = criterion.keyType().name() + ":" + criterion.key();
            if (!seen.add(dedupeKey)) {
                throw new InvalidQueryException(ValidationMessages.FILTER_DUPLICATE_CRITERION);
            }
        }
    }

    private void validateOperatorCompatibility(FilterKeyType keyType, FilterOperator operator, String rawKey) {
        if (COMPARISON_INCOMPATIBLE_TYPES.contains(keyType) &&
                (operator == FilterOperator.LESS_THAN || operator == FilterOperator.GREATER_THAN)) {
            var opSymbol = operator == FilterOperator.LESS_THAN ? "<" : ">";
            throw new InvalidQueryException(ValidationMessages.FILTER_TYPE_MISMATCH.formatted(opSymbol, rawKey));
        }
    }

    private void validateNonBlankKeyName(String keyName, String token) {
        if (keyName.isBlank()) {
            throw new InvalidQueryException(
                    "Invalid filter criterion '%s': key name must not be blank after prefix".formatted(token));
        }
    }

    private void validateKeyName(String keyName, String token) {
        validateNonBlankKeyName(keyName, token);
    }

    private void validateLength(String rawKey, String value, String token) {
        if (rawKey.length() > MAX_KEY_VALUE_LENGTH) {
            throw new InvalidQueryException(
                    ValidationMessages.FILTER_KEY_TOO_LONG.formatted(MAX_KEY_VALUE_LENGTH, token));
        }
        if (value.length() > MAX_KEY_VALUE_LENGTH) {
            throw new InvalidQueryException(
                    ValidationMessages.FILTER_VALUE_TOO_LONG.formatted(MAX_KEY_VALUE_LENGTH, token));
        }
    }
}
