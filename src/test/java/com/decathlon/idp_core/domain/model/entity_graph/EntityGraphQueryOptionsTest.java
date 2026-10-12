package com.decathlon.idp_core.domain.model.entity_graph;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EntityGraphQueryOptions tests")
class EntityGraphQueryOptionsTest {

  @Test
  @DisplayName("Should reject a null traversal mode")
  void shouldRejectNullTraversalMode() {
    assertThatNullPointerException()
        .isThrownBy(() -> new EntityGraphQueryOptions(1, false, false, Set.of(), Set.of(), null))
        .withMessage("mode");
  }
}