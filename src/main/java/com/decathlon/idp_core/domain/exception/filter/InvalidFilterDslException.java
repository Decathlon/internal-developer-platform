package com.decathlon.idp_core.domain.exception.filter;

/// Domain exception thrown when the `q` filter query string contains invalid syntax.
///
/// **Business semantics:** Signals that the caller provided a malformed filter query
/// for the `GET /api/v1/entities/{template}?q=` endpoint. This exception should be
/// mapped to HTTP 400 Bad Request by the infrastructure layer.
public class InvalidFilterDslException extends RuntimeException {

  public InvalidFilterDslException(String message) {
    super(message);
  }
}
