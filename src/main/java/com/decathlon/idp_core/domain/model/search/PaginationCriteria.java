package com.decathlon.idp_core.domain.model.search;

import com.decathlon.idp_core.domain.constant.SearchConstraints;
import com.decathlon.idp_core.domain.constant.ValidationMessages;
import com.decathlon.idp_core.domain.exception.search.InvalidSearchQueryException;

public record PaginationCriteria(int page, int size, String sort) {

  /// Checks that page is non-negative and size is between 1 and
  /// [SearchConstraints#MAX_PAGE_SIZE].
  ///
  /// @throws InvalidSearchQueryException when page or size is out of bounds
  public void validateBounds() {
    if (page < 0) {
      throw new InvalidSearchQueryException(ValidationMessages.SEARCH_PAGE_INVALID);
    }
    if (size <= 0) {
      throw new InvalidSearchQueryException(ValidationMessages.SEARCH_SIZE_INVALID);
    }
    if (size > SearchConstraints.MAX_PAGE_SIZE) {
      throw new InvalidSearchQueryException(
          ValidationMessages.SEARCH_PAGE_SIZE_TOO_LARGE.formatted(SearchConstraints.MAX_PAGE_SIZE));
    }
  }
}
