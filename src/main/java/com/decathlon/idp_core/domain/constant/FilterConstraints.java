package com.decathlon.idp_core.domain.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/// Domain constants for the entity filter query DSL safety limits.
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FilterConstraints {

  /// Maximum number of filter criteria per `q` query string (DoS prevention).
  public static final int MAX_CRITERIA_COUNT = 10;

  /// Maximum length (in characters) of a key or value in a single filter
  /// criterion.
  public static final int MAX_KEY_VALUE_LENGTH = 255;
}
