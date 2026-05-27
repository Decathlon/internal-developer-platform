package com.decathlon.idp_core.domain.model.entity;

import java.util.List;

/// Aggregates zero or more [FilterCriterion] instances that together define a filter
/// query for entities.
///
/// **Business semantics:** All criteria are combined with implicit AND logic. When the
/// criteria list is empty no filtering is applied and all entities for a given template
/// are returned.
///
/// Use [EntityFilter#empty()] to represent the absence of any filter constraint.
public record EntityFilter(List<FilterCriterion> criteria) {

    /// Constructs an [EntityFilter] with a defensive copy of the criteria list.
    public EntityFilter {
        criteria = criteria != null ? List.copyOf(criteria) : List.of();
    }

    /// Returns an [EntityFilter] with no criteria (matches all entities).
    public static EntityFilter empty() {
        return new EntityFilter(List.of());
    }

    /// Returns true when no criteria have been defined.
    public boolean isEmpty() {
        return criteria.isEmpty();
    }
}
