package com.decathlon.idp_core.domain.service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.constant.ValidationMessages;
import com.decathlon.idp_core.domain.exception.InvalidQueryException;
import com.decathlon.idp_core.domain.model.entity.SearchFilterNode;
import com.decathlon.idp_core.domain.model.enums.PropertyType;
import com.decathlon.idp_core.domain.model.enums.SearchOperator;
import com.decathlon.idp_core.domain.port.EntityTemplateRepositoryPort;

import lombok.AllArgsConstructor;

/// Validates a [SearchFilterNode] tree for semantic correctness of numeric operators.
///
/// **Responsibility:** When a caller uses a numeric operator (GT, GTE, LT, LTE) on a
/// property.{name} field, this service verifies that the corresponding property is
/// defined as [PropertyType#NUMBER] in the relevant entity template(s).
///
/// **Template scope:** Template identifiers are inferred from template EQ <id> criteria
/// anywhere in the filter tree. If no template constraint is present (template-agnostic search),
/// the property-type check is skipped — syntactic validation in the mapper already ensures the
/// value is a valid number.
///
/// **Error handling:** Throws [InvalidQueryException] (HTTP 400) when a type mismatch is detected.
@Service
@AllArgsConstructor
public class EntitySearchService {

    private static final Set<SearchOperator> NUMERIC_OPERATORS = Set.of(SearchOperator.GT, SearchOperator.GTE, SearchOperator.LT, SearchOperator.LTE);
    private static final String PROPERTY_PREFIX = "property.";
    private static final String TEMPLATE_FIELD = "template";

    private final EntityTemplateRepositoryPort entityTemplateRepository;

    /// Validates the filter tree for numeric operator / property type compatibility.
    ///
    /// @param filter the root of the search filter tree to validate
    /// @throws InvalidQueryException when a numeric operator targets a non-NUMBER property
    public void validate(SearchFilterNode filter) {
        Set<String> numericPropertyNames = collectNumericPropertyCriteria(filter);
        if (numericPropertyNames.isEmpty()) {
            return;
        }

        Set<String> templateIdentifiers = collectTemplateIdentifiers(filter);
        if (templateIdentifiers.isEmpty()) {
            return; // no template scope — skip type check
        }

        for (String templateIdentifier : templateIdentifiers) {
            entityTemplateRepository.findByIdentifier(templateIdentifier)
                    .ifPresent(template -> template.propertiesDefinitions().stream()
                            .filter(pd -> numericPropertyNames.contains(pd.name()))
                            .filter(pd -> pd.type() != PropertyType.NUMBER)
                            .findFirst()
                            .ifPresent(pd -> {
                                throw new InvalidQueryException(
                                        ValidationMessages.SEARCH_NUMERIC_OPERATOR_PROPERTY_TYPE_MISMATCH
                                                .formatted(pd.name(), templateIdentifier, pd.type()));
                            }));
        }
    }

    private Set<String> collectNumericPropertyCriteria(SearchFilterNode filter) {
        Set<String> names = new HashSet<>();
        collectCriteria(filter)
                .filter(c -> NUMERIC_OPERATORS.contains(c.operation()))
                .filter(c -> c.field().startsWith(PROPERTY_PREFIX))
                .map(c -> c.field().substring(PROPERTY_PREFIX.length()))
                .forEach(names::add);
        return names;
    }

    private Set<String> collectTemplateIdentifiers(SearchFilterNode filter) {
        Set<String> identifiers = new HashSet<>();
        collectCriteria(filter)
                .filter(c -> TEMPLATE_FIELD.equals(c.field()) && c.operation() == SearchOperator.EQ)
                .map(SearchFilterNode.Criterion::value)
                .forEach(identifiers::add);
        return identifiers;
    }

    private Stream<SearchFilterNode.Criterion> collectCriteria(SearchFilterNode node) {
        return switch (node) {
            case SearchFilterNode.Criterion c -> Stream.of(c);
            case SearchFilterNode.Group g -> g.nodes().stream().flatMap(this::collectCriteria);
        };
    }
}
