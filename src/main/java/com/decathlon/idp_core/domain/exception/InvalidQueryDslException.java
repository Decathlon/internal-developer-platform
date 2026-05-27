package com.decathlon.idp_core.domain.exception;

/// Domain exception thrown when the `q` filter query string contains invalid syntax.
///
/// **Business semantics:** Signals that the caller provided a malformed filter query.
/// This exception should be mapped to HTTP 400 Bad Request by the infrastructure layer.
public class InvalidQueryDslException extends RuntimeException {

    public InvalidQueryDslException(String message) {
        super(message);
    }
}
