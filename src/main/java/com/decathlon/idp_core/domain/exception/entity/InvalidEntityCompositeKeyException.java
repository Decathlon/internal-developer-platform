package com.decathlon.idp_core.domain.exception.entity;

/// Exception thrown when an entity composite key format is invalid.
///
/// Composite keys must follow the format "templateIdentifier:identifier"
/// where both parts are non-empty strings separated by a single colon.
///
/// **Business context:**
/// - Composite keys are used to uniquely identify entities across templates
/// - The same identifier can exist in multiple templates, so both parts are required
/// - This exception indicates a malformed key that cannot be parsed
///
/// **HTTP mapping:** 400 Bad Request (client error — invalid input format)
public class InvalidEntityCompositeKeyException extends RuntimeException {

  private final String invalidKey;

  public InvalidEntityCompositeKeyException(String invalidKey) {
    super(String.format(
        "Invalid entity composite key format: '%s'. Expected format: 'templateIdentifier:identifier'",
        invalidKey));
    this.invalidKey = invalidKey;
  }

  public String getInvalidKey() {
    return invalidKey;
  }
}
