package com.decathlon.idp_core.domain.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/// Domain constants for JSLT mapping safety limits (DoS prevention).
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsltConstraints {

  /// Maximum size (in UTF-8 bytes) of a raw payload accepted for mapping.
  public static final int MAX_PAYLOAD_BYTES = 1024 * 1024;

  /// Maximum JSON nesting depth of a raw payload accepted for mapping.
  public static final int MAX_JSON_DEPTH = 64;

  /// Maximum length (in characters) of a JSLT expression.
  public static final int MAX_EXPRESSION_LENGTH = 10_000;

  /// Maximum wall-clock time (in milliseconds) allowed for one JSLT evaluation.
  public static final long EVALUATION_TIMEOUT_MS = 5_000L;
}
