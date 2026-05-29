package com.decathlon.idp_core.domain.model.entity;

import java.util.List;

/// A domain-native raw node of the search filter tree, produced by the infrastructure mapper
/// before domain parsing and validation.
///
/// **Responsibility:** Carries the unvalidated, string-only representation of a filter tree
/// from the API adapter into the domain parser. All fields are raw strings — no enums,
/// no framework types. Structural conversion from [com.decathlon.idp_core.infrastructure.adapters.api.dto.in.FilterNodeDtoIn]
/// to this type is handled by the infrastructure mapper; validation and enum resolution
/// are handled by the domain parser.
///
/// **Nodes:**
/// - [Group] — a logical group with a raw connector string and child nodes
/// - [Criterion] — a leaf predicate with raw field, operation, and value strings
public sealed interface RawSearchFilterNode {

    /// A logical group combining multiple child [RawSearchFilterNode]s.
    ///
    /// @param connector raw connector string (e.g. "AND", "OR"); may be null or blank until validated
    /// @param nodes     child nodes; may be null until validated
    record Group(String connector, List<RawSearchFilterNode> nodes) implements RawSearchFilterNode {
        public Group {
            nodes = nodes != null ? List.copyOf(nodes) : List.of();
        }
    }

    /// A leaf predicate in the filter tree.
    ///
    /// @param field     raw field name (e.g. "template", "property.language"); may be null until validated
    /// @param operation raw operation string (e.g. "EQ", "CONTAINS"); may be null until validated
    /// @param value     raw value string; may be null until validated
    record Criterion(String field, String operation, String value) implements RawSearchFilterNode {
    }
}
