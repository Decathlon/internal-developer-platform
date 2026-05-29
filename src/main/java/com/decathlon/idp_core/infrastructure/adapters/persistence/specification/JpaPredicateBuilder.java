package com.decathlon.idp_core.infrastructure.adapters.persistence.specification;

import java.math.BigDecimal;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;

import com.decathlon.idp_core.domain.model.enums.SearchOperator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/// Shared predicate-building utilities used by [EntitySpecification] and [EntitySearchSpecification].
///
/// **Responsibilities:**
/// - LIKE wildcard escaping for user-supplied values
/// - String-comparison predicates using `ILIKE` (case-insensitive, GIN-index-friendly) for
///   [SearchOperator#CONTAINS], [SearchOperator#NOT_CONTAINS], [SearchOperator#STARTS_WITH],
///   [SearchOperator#ENDS_WITH], and `LOWER()` equality for [SearchOperator#EQ] / [SearchOperator#NEQ]
/// - Numeric-comparison predicates (GT, GTE, LT, LTE) via explicit SQL `CAST(field AS NUMERIC)`
///   so that VARCHAR property-value columns can be compared to numeric literals without
///   PostgreSQL rejecting the query.
///
/// **Why Hibernate-specific?** The codebase already targets PostgreSQL through Hibernate.
/// `ILIKE` requires [HibernateCriteriaBuilder] which is a Hibernate extension; this is
/// intentional and consistent with the rest of the specification layer.
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class JpaPredicateBuilder {

  static final char LIKE_ESCAPE_CHAR = '\\';

  /// Builds a [Predicate] for the given [SearchOperator] against the provided
  /// field expression.
  ///
  /// - EQ / NEQ use `LOWER(field) = LOWER(value)` to leverage functional btree
  /// indexes.
  /// - CONTAINS / NOT_CONTAINS / STARTS_WITH / ENDS_WITH use `ILIKE` to leverage
  /// GIN
  /// trigram indexes and avoid redundant client-side lowercasing.
  /// - GT / GTE / LT / LTE cast the field to `NUMERIC` before comparing.
  ///
  /// @param cb the JPA criteria builder (must be a [HibernateCriteriaBuilder] at
  /// runtime)
  /// @param field the expression to filter on
  /// @param operator the comparison operator
  /// @param value the user-supplied value (not yet escaped or lowercased)
  /// @return a [Predicate] representing the comparison
  static Predicate buildPredicate(CriteriaBuilder cb, Expression<?> field, SearchOperator operator,
      String value) {
    if (isNumericOperator(operator)) {
      return buildNumericPredicate(cb, field, operator, new BigDecimal(value));
    }
    HibernateCriteriaBuilder hcb = (HibernateCriteriaBuilder) cb;
    Expression<String> stringField = field.as(String.class);
    return switch (operator) {
      case EQ -> cb.equal(cb.lower(stringField), value.toLowerCase());
      case NEQ -> cb.notEqual(cb.lower(stringField), value.toLowerCase());
      case CONTAINS -> {
        String escaped = escapeLikeWildcards(value);
        yield hcb.ilike(stringField, "%" + escaped + "%", LIKE_ESCAPE_CHAR);
      }
      case NOT_CONTAINS -> {
        String escaped = escapeLikeWildcards(value);
        yield hcb.notIlike(stringField, "%" + escaped + "%", LIKE_ESCAPE_CHAR);
      }
      case STARTS_WITH -> {
        String escaped = escapeLikeWildcards(value);
        yield hcb.ilike(stringField, escaped + "%", LIKE_ESCAPE_CHAR);
      }
      case ENDS_WITH -> {
        String escaped = escapeLikeWildcards(value);
        yield hcb.ilike(stringField, "%" + escaped, LIKE_ESCAPE_CHAR);
      }
      default -> throw new IllegalStateException("Unhandled operator: " + operator);
    };
  }

  static boolean isNumericOperator(SearchOperator operator) {
    return switch (operator) {
      case GT, GTE, LT, LTE -> true;
      default -> false;
    };
  }

  static Predicate buildNumericPredicate(CriteriaBuilder cb, Expression<?> field,
      SearchOperator operator, BigDecimal numericValue) {
    // Explicit SQL CAST(field AS NUMERIC): the property value column is VARCHAR;
    // without
    // this cast PostgreSQL would reject the comparison with a numeric literal.
    Expression<BigDecimal> numericField = ((HibernateCriteriaBuilder) cb)
        .cast((org.hibernate.query.criteria.JpaExpression<?>) field, BigDecimal.class);
    return switch (operator) {
      case GT -> cb.greaterThan(numericField, numericValue);
      case GTE -> cb.greaterThanOrEqualTo(numericField, numericValue);
      case LT -> cb.lessThan(numericField, numericValue);
      case LTE -> cb.lessThanOrEqualTo(numericField, numericValue);
      default -> throw new IllegalStateException("Not a numeric operator: " + operator);
    };
  }

  /// Escapes SQL LIKE wildcards (`%` and `_`) in the given value so they are
  /// treated as
  /// literal characters rather than pattern metacharacters.
  ///
  /// Used by all ILIKE-based operators. The value does not need to be
  /// pre-lowercased
  /// because `ILIKE` handles case-insensitivity natively.
  static String escapeLikeWildcards(String value) {
    return value
        .replace(String.valueOf(LIKE_ESCAPE_CHAR),
            LIKE_ESCAPE_CHAR + String.valueOf(LIKE_ESCAPE_CHAR))
        .replace("%", LIKE_ESCAPE_CHAR + "%").replace("_", LIKE_ESCAPE_CHAR + "_");
  }
}
