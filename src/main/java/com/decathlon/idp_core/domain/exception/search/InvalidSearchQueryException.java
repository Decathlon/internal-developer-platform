package com.decathlon.idp_core.domain.exception.search;

/// Domain exception thrown when a search filter tree or free-text query contains invalid syntax.
///
/// **Business semantics:** Signals that the caller provided a malformed search request
/// for the `POST /api/v1/entities/search` endpoint. This exception should be mapped to
/// HTTP 400 Bad Request by the infrastructure layer.
public class InvalidSearchQueryException extends RuntimeException {

  public InvalidSearchQueryException(String message) {
    super(message);
  }
}
