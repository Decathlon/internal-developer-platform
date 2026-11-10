package com.decathlon.idp_core.domain.service.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.exception.search.InvalidSearchQueryException;
import com.decathlon.idp_core.domain.model.search.SortCriteria;
import com.decathlon.idp_core.domain.model.search.SortCriteria.Direction;

@DisplayName("SortParameterParser Tests")
class SortParameterParserTest {

  @Test
  @DisplayName("Should parse comma-delimited sort expression")
  void shouldParseCommaDelimitedExpression() {
    assertThat(SortParameterParser.parse("identifier,desc"))
        .contains(new SortCriteria("identifier", Direction.DESC));
  }

  @Test
  @DisplayName("Should parse colon-delimited sort expression")
  void shouldParseColonDelimitedExpression() {
    assertThat(SortParameterParser.parse("name:asc"))
        .contains(new SortCriteria("name", Direction.ASC));
  }

  @Test
  @DisplayName("Should default direction to ascending")
  void shouldDefaultDirectionToAscending() {
    assertThat(SortParameterParser.parse("templateIdentifier"))
        .contains(new SortCriteria("templateIdentifier", Direction.ASC));
  }

  @Test
  @DisplayName("Should return empty when sort is not provided")
  void shouldReturnEmptyForMissingSort() {
    assertThat(SortParameterParser.parse(null)).isEmpty();
    assertThat(SortParameterParser.parse("  ")).isEmpty();
  }

  @Test
  @DisplayName("Should reject unsupported sort field")
  void shouldRejectUnsupportedField() {
    assertThatThrownBy(() -> SortParameterParser.parse("badField,asc"))
        .isInstanceOf(InvalidSearchQueryException.class).hasMessageContaining("badField");
  }

  @Test
  @DisplayName("Should reject invalid direction and malformed expressions")
  void shouldRejectInvalidExpressions() {
    assertThatThrownBy(() -> SortParameterParser.parse("identifier:sideways"))
        .isInstanceOf(InvalidSearchQueryException.class);
    assertThatThrownBy(() -> SortParameterParser.parse("identifier,asc:desc"))
        .isInstanceOf(InvalidSearchQueryException.class);
    assertThatThrownBy(() -> SortParameterParser.parse("identifier,"))
        .isInstanceOf(InvalidSearchQueryException.class);
  }
}