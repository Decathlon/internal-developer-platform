package com.decathlon.idp_core.domain.service.search;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.constant.SearchConstraints;
import com.decathlon.idp_core.domain.constant.ValidationMessages;
import com.decathlon.idp_core.domain.exception.search.InvalidSearchQueryException;
import com.decathlon.idp_core.domain.model.entity.SearchFilterNode;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.enums.PropertyType;
import com.decathlon.idp_core.domain.model.enums.SearchOperator;
import com.decathlon.idp_core.domain.port.EntityTemplateRepositoryPort;

import lombok.AllArgsConstructor;

/// Validates a [SearchFilterNode] tree and its accompanying free-text query string.
///
/// **Responsibilities (in order):**
/// 1. Query-string length — rejects query strings longer than [SearchConstraints#MAX_QUERY_LENGTH].
/// 2. Field name grammar — rejects unknown or malformed field names in each criterion.
/// 3. Numeric operator constraints — numeric operators (GT, GTE, LT, LTE) must target a
///    `property.{name}` field and the value must be parseable as a [java.math.BigDecimal].
/// 4. Template-scoped property-type check — when the filter contains a `template EQ <id>`
///    criterion, verifies that any numeric-operator property field is defined as
///    [PropertyType#NUMBER] in that template.
///
/// **Error handling:** Throws [InvalidSearchQueryException] (HTTP 400) for all validation failures.
@Service
@AllArgsConstructor
public class SearchFilterValidationService {

  private static final String PROPERTY_PREFIX = "property.";
  private static final String RELATION_PREFIX = "relation.";
  private static final String RELATIONS_AS_TARGET_PREFIX = "relations_as_target.";
  private static final String TEMPLATE_FIELD = "template";
  private static final Set<SearchOperator> NUMERIC_OPERATORS = Set.of(SearchOperator.GT,
      SearchOperator.GTE, SearchOperator.LT, SearchOperator.LTE);
  private static final Set<String> SIMPLE_FIELDS = Set.of(TEMPLATE_FIELD, "identifier", "name",
      "relation", "relations_as_target");

  private final EntityTemplateRepositoryPort entityTemplateRepository;

  /// Validates the filter tree and query string for semantic correctness.
  ///
  /// @param filter the root of the search filter tree to validate
  /// @param query optional free-text query string; may be null (no-op)
  /// @throws InvalidSearchQueryException when any validation rule is violated
  public void validate(SearchFilterNode filter, String query) {
    validateQuery(query);
    collectCriteria(filter).forEach(this::validateCriterion);
    validateTemplatePropertyTypes(filter);
  }

  private void validateQuery(String query) {
    if (query != null && query.length() > SearchConstraints.MAX_QUERY_LENGTH) {
      throw new InvalidSearchQueryException(
          ValidationMessages.SEARCH_QUERY_TOO_LONG.formatted(SearchConstraints.MAX_QUERY_LENGTH));
    }
  }

  private void validateCriterion(SearchFilterNode.Criterion criterion) {
    validateField(criterion.field());
    validateNumericConstraints(criterion.operation(), criterion.field(), criterion.value());
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
    throw new InvalidSearchQueryException(ValidationMessages.SEARCH_INVALID_FIELD.formatted(field));
  }

  private void validateRelationsAsTargetField(String field) {
    String rest = field.substring(RELATIONS_AS_TARGET_PREFIX.length());
    int dot = rest.indexOf('.');
    if (dot <= 0 || dot == rest.length() - 1) {
      throw new InvalidSearchQueryException(
          ValidationMessages.SEARCH_INVALID_FIELD.formatted(field));
    }
  }

  private void validateNumericConstraints(SearchOperator operator, String field, String value) {
    if (!NUMERIC_OPERATORS.contains(operator)) {
      return;
    }
    if (!field.startsWith(PROPERTY_PREFIX) || field.length() <= PROPERTY_PREFIX.length()) {
      throw new InvalidSearchQueryException(
          ValidationMessages.SEARCH_NUMERIC_OPERATOR_REQUIRES_PROPERTY.formatted(operator));
    }
    try {
      new BigDecimal(value);
    } catch (NumberFormatException _) {
      throw new InvalidSearchQueryException(
          ValidationMessages.SEARCH_NUMERIC_OPERATOR_INVALID_VALUE.formatted(value, operator));
    }
  }

  private void validateTemplatePropertyTypes(SearchFilterNode filter) {
    Set<String> numericPropertyNames = collectNumericPropertyCriteria(filter);
    if (numericPropertyNames.isEmpty()) {
      return;
    }

    Set<String> templateIdentifiers = collectTemplateIdentifiers(filter);
    if (templateIdentifiers.isEmpty()) {
      return;
    }

    for (String templateIdentifier : templateIdentifiers) {
      entityTemplateRepository.findByIdentifier(templateIdentifier)
          .ifPresent(template -> checkPropertyTypes(numericPropertyNames, template));
    }
  }

  private void checkPropertyTypes(Set<String> numericPropertyNames, EntityTemplate template) {
    template.propertiesDefinitions().stream().filter(pd -> numericPropertyNames.contains(pd.name()))
        .filter(pd -> pd.type() != PropertyType.NUMBER).findFirst().ifPresent(pd -> {
          throw new InvalidSearchQueryException(
              ValidationMessages.SEARCH_NUMERIC_OPERATOR_PROPERTY_TYPE_MISMATCH.formatted(pd.name(),
                  template.identifier(), pd.type()));
        });
  }

  private Set<String> collectNumericPropertyCriteria(SearchFilterNode filter) {
    Set<String> names = new HashSet<>();
    collectCriteria(filter).filter(c -> NUMERIC_OPERATORS.contains(c.operation()))
        .filter(c -> c.field().startsWith(PROPERTY_PREFIX))
        .map(c -> c.field().substring(PROPERTY_PREFIX.length())).forEach(names::add);
    return names;
  }

  private Set<String> collectTemplateIdentifiers(SearchFilterNode filter) {
    Set<String> identifiers = new HashSet<>();
    collectCriteria(filter)
        .filter(c -> TEMPLATE_FIELD.equals(c.field()) && c.operation() == SearchOperator.EQ)
        .map(SearchFilterNode.Criterion::value).forEach(identifiers::add);
    return identifiers;
  }

  private Stream<SearchFilterNode.Criterion> collectCriteria(SearchFilterNode node) {
        return switch (node) {
            case SearchFilterNode.Criterion c -> Stream.of(c);
            case SearchFilterNode.Group g -> g.nodes().stream().flatMap(this::collectCriteria);
        };
    }
}
