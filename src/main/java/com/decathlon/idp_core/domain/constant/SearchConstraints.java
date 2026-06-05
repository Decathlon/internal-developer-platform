package com.decathlon.idp_core.domain.constant;

import java.util.Set;

import com.decathlon.idp_core.domain.model.search.SearchFilterNode;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/// Domain constants for search and filter query safety limits.
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SearchConstraints {

  /// Maximum number of entities returned per page in a search request.
  public static final int MAX_PAGE_SIZE = 500;

  /// Maximum length (in characters) of the free-text `query` parameter.
  public static final int MAX_QUERY_LENGTH = 255;

  /// Maximum nesting depth of a
  /// [SearchFilterNode] tree.
  public static final int MAX_NESTING_DEPTH = 5;

  /// Maximum total number of criterion nodes across a
  /// [SearchFilterNode] tree.
  public static final int MAX_TOTAL_CRITERIA = 50;

  /// Fields on which search results may be sorted.
  public static final Set<String> ALLOWED_SORT_FIELDS = Set.of("identifier", "name",
      "templateIdentifier");
}
