package com.decathlon.idp_core.domain.model.enums;

/// Operators supported by the entity search query DSL.
///
/// **Business semantics:**
/// - [EQ] requires exact match (case-insensitive)
/// - [NEQ] requires the field to not exactly match (case-insensitive)
/// - [CONTAINS] requires the field to contain the value (case-insensitive substring)
/// - [NOT_CONTAINS] requires the field to not contain the value
/// - [STARTS_WITH] requires the field to start with the value (case-insensitive)
/// - [ENDS_WITH] requires the field to end with the value (case-insensitive)
/// - [GT] requires the field to be strictly greater than the value
/// - [GTE] requires the field to be greater than or equal to the value
/// - [LT] requires the field to be strictly less than the value
/// - [LTE] requires the field to be less than or equal to the value
public enum SearchOperator {
  EQ, NEQ, CONTAINS, NOT_CONTAINS, STARTS_WITH, ENDS_WITH, GT, GTE, LT, LTE
}
