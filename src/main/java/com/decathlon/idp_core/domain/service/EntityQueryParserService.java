package com.decathlon.idp_core.domain.service;

import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.constant.ValidationMessages;
import com.decathlon.idp_core.domain.exception.InvalidQueryDslException;
import com.decathlon.idp_core.domain.model.entity.EntityFilter;
import com.decathlon.idp_core.domain.model.entity.FilterCriterion;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.enums.FilterKeyType;
import com.decathlon.idp_core.domain.model.enums.FilterOperator;
import com.decathlon.idp_core.domain.model.enums.PropertyType;

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
@Service
public class EntityQueryParserService {

  private static final String RELATION = "relation";
  private static final String RELATIONS_AS_TARGET = "relations_as_target";
  private static final String PROPERTY_PREFIX = "property.";
  private static final String RELATION_PREFIX = "relation.";
  private static final String RELATIONS_AS_TARGET_PREFIX = "relations_as_target.";
  private static final Set<String> VALID_ATTRIBUTE_NAMES = Set.of("identifier", "name");

  private static final Set<FilterKeyType> COMPARISON_INCOMPATIBLE_TYPES = Set.of(
      FilterKeyType.ATTRIBUTE, FilterKeyType.RELATION_NAME, FilterKeyType.RELATION_ENTITY,
      FilterKeyType.RELATION_PROPERTY, FilterKeyType.RELATIONS_AS_TARGET_NAME,
      FilterKeyType.RELATIONS_AS_TARGET_PROPERTY);

  static final int MAX_CRITERIA_COUNT = 10;
  static final int MAX_KEY_VALUE_LENGTH = 255;

  /// Parses a query string into an [EntityFilter].
  ///
  /// @param query the raw `q` parameter value; may be null or blank
  /// @return an [EntityFilter] with parsed criteria, or [EntityFilter#empty()]
  /// when query is blank
  /// @throws InvalidQueryDslException when the query string is malformed or
  /// exceeds safety limits
  public EntityFilter parse(String query) {
    if (query == null || query.isBlank()) {
      return EntityFilter.empty();
    }

    List<FilterCriterion> criteria = Stream.of(query.split(";")).filter(token -> !token.isBlank())
        .map(token -> parseCriterion(token.trim())).toList();

    if (criteria.size() > MAX_CRITERIA_COUNT) {
      throw new InvalidQueryDslException(
          ValidationMessages.FILTER_TOO_MANY_CRITERIA.formatted(MAX_CRITERIA_COUNT));
    }

    validateNoDuplicates(criteria);

    return new EntityFilter(criteria);
  }

  private FilterCriterion parseCriterion(String token) {
    int operatorIndex = findOperatorIndex(token)
        .orElseThrow(() -> new InvalidQueryDslException(ValidationMessages.FILTER_INVALID_FORMAT));

    var rawKey = token.substring(0, operatorIndex);
    var operatorChar = token.charAt(operatorIndex);
    var value = token.substring(operatorIndex + 1);

    validateKey(rawKey, token);
    validateValue(value, token);
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
      default -> throw new InvalidQueryDslException("Unknown operator character: " + c);
    };
  }

  private FilterCriterion buildCriterion(String rawKey, FilterOperator operator, String value,
      String token) {
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
      validateKey(relationPart, token);
      return buildRelationsAsTargetCriterion(relationPart, operator, value, token);
    }

    if (rawKey.startsWith(RELATION_PREFIX)) {
      var relationPart = rawKey.substring(RELATION_PREFIX.length());
      validateKey(relationPart, token);
      return buildRelationCriterion(relationPart, operator, value, token);
    }

    if (!VALID_ATTRIBUTE_NAMES.contains(rawKey)) {
      throw new InvalidQueryDslException(
          "Unknown attribute '%s' in filter criterion '%s'. Valid attributes: %s".formatted(rawKey,
              token, VALID_ATTRIBUTE_NAMES));
    }
    return new FilterCriterion(FilterKeyType.ATTRIBUTE, rawKey, operator, value);
  }

  private FilterCriterion buildRelationsAsTargetCriterion(String relationPart,
      FilterOperator operator, String value, String token) {
    int dotIndex = relationPart.indexOf('.');
    if (dotIndex <= 0) {
      throw new InvalidQueryDslException(
          "Invalid filter criterion '%s': relations_as_target requires the form 'relations_as_target.<relationName>.<identifier|name>'"
              .formatted(token));
    }

    var relationName = relationPart.substring(0, dotIndex);
    var propertyName = relationPart.substring(dotIndex + 1);
    validateKeyName(relationName, token);
    validatePropertyName(propertyName, RELATIONS_AS_TARGET, token);
    var compositeKey = relationName + "." + propertyName;
    return new FilterCriterion(FilterKeyType.RELATIONS_AS_TARGET_PROPERTY, compositeKey, operator,
        value);
  }

  private FilterCriterion buildRelationCriterion(String relationPart, FilterOperator operator,
      String value, String token) {
    int dotIndex = relationPart.indexOf('.');
    if (dotIndex > 0) {
      var relationName = relationPart.substring(0, dotIndex);
      var propertyName = relationPart.substring(dotIndex + 1);
      validateKeyName(relationName, token);
      validatePropertyName(propertyName, RELATION, token);
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
        throw new InvalidQueryDslException(ValidationMessages.FILTER_DUPLICATE_CRITERION);
      }
    }
  }

  private void validateOperatorCompatibility(FilterKeyType keyType, FilterOperator operator,
      String rawKey) {
    if (COMPARISON_INCOMPATIBLE_TYPES.contains(keyType)
        && (operator == FilterOperator.LESS_THAN || operator == FilterOperator.GREATER_THAN)) {
      var opSymbol = operator == FilterOperator.LESS_THAN ? "<" : ">";
      throw new InvalidQueryDslException(
          ValidationMessages.FILTER_TYPE_MISMATCH.formatted(opSymbol, rawKey));
    }
  }

  /// Validates that all PROPERTY criteria using `<` or `>` operators
  /// correspond to a NUMBER-typed property in the given template.
  ///
  /// This is a semantic check that requires the template to be available (i.e.,
  /// it
  /// cannot be performed in [#parse] which has no template context).
  ///
  /// @param filter the parsed query filter
  /// @param template the entity template providing property type information
  /// @throws InvalidQueryDslException when a comparison operator is used on a
  /// non-NUMBER property
  public void validateFilterPropertyTypes(EntityFilter filter, EntityTemplate template) {
    filter.criteria().stream().filter(c -> c.keyType() == FilterKeyType.PROPERTY)
        .filter(c -> c.operator() == FilterOperator.LESS_THAN
            || c.operator() == FilterOperator.GREATER_THAN)
        .forEach(c -> {
          var propertyDef = template.propertiesDefinitions().stream()
              .filter(p -> p.name().equals(c.key())).findFirst();
          if (propertyDef.isEmpty() || propertyDef.get().type() != PropertyType.NUMBER) {
            var opSymbol = c.operator() == FilterOperator.LESS_THAN ? "<" : ">";
            throw new InvalidQueryDslException(
                ValidationMessages.FILTER_PROPERTY_TYPE_NOT_NUMERIC.formatted(opSymbol, c.key()));
          }
        });
  }

  private void validateKey(String key, String token) {
    if (key.isBlank()) {
      throw new InvalidQueryDslException(
          "Invalid filter criterion '%s': key must not be blank".formatted(token));
    }
  }

  private void validateKeyName(String keyName, String token) {
    if (keyName.isBlank()) {
      throw new InvalidQueryDslException(
          "Invalid filter criterion '%s': key name must not be blank".formatted(token));
    }
  }

  private void validateValue(String value, String token) {
    if (value.isBlank()) {
      throw new InvalidQueryDslException(
          "Invalid filter criterion '%s': value must not be blank".formatted(token));
    }
  }

  private void validatePropertyName(String propertyName, String contextType, String token) {
    if (!VALID_ATTRIBUTE_NAMES.contains(propertyName)) {
      throw new InvalidQueryDslException(
          "Invalid property '%s' in criterion '%s': only 'identifier' and 'name' are supported for %s"
              .formatted(propertyName, token, contextType));
    }
  }

  private void validateLength(String rawKey, String value, String token) {
    if (rawKey.length() > MAX_KEY_VALUE_LENGTH) {
      throw new InvalidQueryDslException(
          ValidationMessages.FILTER_KEY_TOO_LONG.formatted(MAX_KEY_VALUE_LENGTH, token));
    }
    if (value.length() > MAX_KEY_VALUE_LENGTH) {
      throw new InvalidQueryDslException(
          ValidationMessages.FILTER_VALUE_TOO_LONG.formatted(MAX_KEY_VALUE_LENGTH, token));
    }
  }
}
