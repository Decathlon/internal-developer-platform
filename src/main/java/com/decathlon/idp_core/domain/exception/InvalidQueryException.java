package com.decathlon.idp_core.domain.exception;

/// Domain exception thrown when a search filter or query contains invalid syntax.
///
/// **Business semantics:** Signals that the caller provided a malformed search request.
/// This exception should be mapped to HTTP 400 Bad Request by the infrastructure layer.
public class InvalidQueryException extends RuntimeException {

    public InvalidQueryException(String message) {
        super(message);
    }
}
