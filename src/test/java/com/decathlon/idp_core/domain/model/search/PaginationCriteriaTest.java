package com.decathlon.idp_core.domain.model.search;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.exception.search.InvalidSearchQueryException;

class PaginationCriteriaTest {

  @Test
  @DisplayName("validateBounds accepts a page size equal to the configured maximum")
  void validateAcceptsMaxSize() {
    assertThatCode(() -> new PaginationCriteria(0, 500, null).validateBounds(500))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("validateBounds rejects a page size above the configured maximum")
  void validateRejectsSizeAboveMax() {
    assertThatThrownBy(() -> new PaginationCriteria(0, 501, null).validateBounds(500))
        .isInstanceOf(InvalidSearchQueryException.class);
  }

  @Test
  @DisplayName("validateBounds uses the configured maximum instead of the default")
  void validateUsesConfiguredMax() {
    assertThatThrownBy(() -> new PaginationCriteria(0, 51, null).validateBounds(50))
        .isInstanceOf(InvalidSearchQueryException.class);
  }

  @Test
  @DisplayName("validateBounds rejects a negative page index")
  void validateRejectsNegativePage() {
    assertThatThrownBy(() -> new PaginationCriteria(-1, 10, null).validateBounds(500))
        .isInstanceOf(InvalidSearchQueryException.class);
  }

  @Test
  @DisplayName("validateBounds rejects a non-positive page size")
  void validateRejectsNonPositiveSize() {
    assertThatThrownBy(() -> new PaginationCriteria(0, 0, null).validateBounds(500))
        .isInstanceOf(InvalidSearchQueryException.class);
  }

  @Test
  @DisplayName("validateRange accepts any positive size without an upper bound")
  void validateRangeAcceptsLargeSize() {
    assertThatCode(() -> new PaginationCriteria(0, 100_000, null).validateRange())
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("validateRange rejects a negative page index and a non-positive size")
  void validateRangeRejectsInvalidValues() {
    assertThatThrownBy(() -> new PaginationCriteria(-1, 10, null).validateRange())
        .isInstanceOf(InvalidSearchQueryException.class);
    assertThatThrownBy(() -> new PaginationCriteria(0, 0, null).validateRange())
        .isInstanceOf(InvalidSearchQueryException.class);
  }
}
