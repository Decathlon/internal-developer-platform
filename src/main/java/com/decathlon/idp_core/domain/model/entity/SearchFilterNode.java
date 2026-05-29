package com.decathlon.idp_core.domain.model.entity;

import java.util.List;

import com.decathlon.idp_core.domain.model.enums.LogicalConnector;
import com.decathlon.idp_core.domain.model.enums.SearchOperator;

/// A node in the search filter tree for entity search queries.
///
/// **Business semantics:** A filter tree is composed of two types of nodes:
/// - [Group] — a logical group that combines child nodes with a [LogicalConnector]
///   (AND / OR / IN). Children may themselves be groups or leaf criteria, allowing
///   arbitrarily deep nesting.
/// - [Criterion] — a leaf predicate: field <operator> value.
///
/// The root of the tree must be either a [Group] or a single [Criterion].
/// An empty [Group] matches all entities.
///
/// **Supported fields for [Criterion]:**
/// - `template` — filters by the entity template identifier
/// - `identifier` — filters by the entity identifier
/// - `name` — filters by the entity name
/// - `property.{name}` — filters by a named property value
/// - `relation.{name}` — filters by target entity identifier of a named relation
/// - `relation.{name}.identifier` — explicit form of the above
/// - `relation.{name}.name` — filters by target entity name of a named relation
/// - `relations_as_target` — filters by the presence or absence of any reverse relation by name
/// - `relations_as_target.{name}.identifier` — filters by source entity identifier in a reverse relation
/// - `relations_as_target.{name}.name` — filters by source entity name in a reverse relation
public sealed interface SearchFilterNode {

    /// A logical group combining multiple child [SearchFilterNode]s with a connector.
    ///
    /// @param connector how child nodes are logically combined
    /// @param nodes     child nodes; an empty list matches all entities
    record Group(LogicalConnector connector, List<SearchFilterNode> nodes) implements SearchFilterNode {
        public Group {
            nodes = nodes != null ? List.copyOf(nodes) : List.of();
        }
    }

    /// A leaf predicate in the search filter tree.
    ///
    /// @param field     the entity field to filter on (see [SearchFilterNode] for supported fields)
    /// @param operation the comparison operator to apply
    /// @param value     the value to compare against
    record Criterion(String field, SearchOperator operation, String value) implements SearchFilterNode {
    }
}
