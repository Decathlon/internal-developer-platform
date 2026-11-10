package com.decathlon.idp_core.domain.model.search;

/// A validated entity sort field and direction.
public record SortCriteria(String field, Direction direction) {

  public enum Direction {
    ASC, DESC
  }
}
