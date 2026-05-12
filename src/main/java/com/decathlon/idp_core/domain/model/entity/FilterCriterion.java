package com.decathlon.idp_core.domain.model.entity;

import com.decathlon.idp_core.domain.model.enums.FilterKeyType;
import com.decathlon.idp_core.domain.model.enums.FilterOperator;

/// A filter condition in an entity query.
///
/// **Business semantics:** Represents one predicate of the form
/// `<key> <operator> <value>`. The key type and name together identify which
/// field to filter on:
/// - [FilterKeyType#ATTRIBUTE] — direct entity attribute (`identifier`, `name`)
/// - [FilterKeyType#PROPERTY] — entity property value identified by `key`
/// - [FilterKeyType#RELATION_ENTITY] — target entity identifiers of the relation named `key`
/// - [FilterKeyType#RELATION_TEMPLATE] — target template identifier of the relation named `key`
/// - [FilterKeyType#RELATION_PROPERTY] — a property of relation instances. `key` format:
///   `relationName.propertyName` (e.g., `api-link.identifier`)
///
/// Multiple [FilterCriterion] instances combined in an [EntityFilter] are applied
/// with implicit AND logic.
public record FilterCriterion(
        FilterKeyType keyType,
        String key,
        FilterOperator operator,
        String value
) {
}
