package com.decathlon.idp_core.domain.service.search;

import java.util.Locale;
import java.util.Optional;

import com.decathlon.idp_core.domain.constant.SearchConstraints;
import com.decathlon.idp_core.domain.constant.ValidationMessages;
import com.decathlon.idp_core.domain.exception.search.InvalidSearchQueryException;
import com.decathlon.idp_core.domain.model.search.SortCriteria;
import com.decathlon.idp_core.domain.model.search.SortCriteria.Direction;

/// Parses and validates entity sort parameters shared by search and listing APIs.
public final class SortParameterParser {

  private SortParameterParser() {
  }

  /// Parses a sort expression in either `field,direction` or `field:direction`
  /// form.
  /// A field without a direction defaults to ascending; null or blank input means
  /// no sort was requested.
  ///
  /// @param expression caller-provided sort expression
  /// @return validated sort criteria, or empty when no sort was requested
  /// @throws InvalidSearchQueryException when the expression or field is invalid
  public static Optional<SortCriteria> parse(String expression) {
    if (expression == null || expression.isBlank()) {
      return Optional.empty();
    }

    String[] parts = expression.trim().split("[,:]", -1);
    if (parts.length > 2 || parts[0].isBlank() || parts.length == 2 && parts[1].isBlank()) {
      throw invalidFormat(expression);
    }

    String field = parts[0].trim();
    if (!SearchConstraints.ALLOWED_SORT_FIELDS.contains(field)) {
      throw new InvalidSearchQueryException(
          ValidationMessages.SEARCH_INVALID_SORT_FIELD.formatted(field));
    }

    Direction direction = Direction.ASC;
    if (parts.length == 2) {
      direction = switch (parts[1].trim().toLowerCase(Locale.ROOT)) {
        case "asc" -> Direction.ASC;
        case "desc" -> Direction.DESC;
        default -> throw invalidFormat(expression);
      };
    }

    return Optional.of(new SortCriteria(field, direction));
  }

  private static InvalidSearchQueryException invalidFormat(String expression) {
    return new InvalidSearchQueryException(
        ValidationMessages.SEARCH_INVALID_SORT_FORMAT.formatted(expression));
  }
}