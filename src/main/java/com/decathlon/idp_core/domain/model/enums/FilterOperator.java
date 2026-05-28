package com.decathlon.idp_core.domain.model.enums;

/// Operators supported by the entity filter query DSL.
///
/// **Business semantics:**
/// - [EQUALS] requires exact match (case-insensitive)
/// - [CONTAINS] requires the field to contain the value (case-insensitive)
/// - [LESS_THAN] requires the field to be less than the value
/// - [GREATER_THAN] requires the field to be greater than the value
public enum FilterOperator {
  EQUALS, CONTAINS, LESS_THAN, GREATER_THAN
}
