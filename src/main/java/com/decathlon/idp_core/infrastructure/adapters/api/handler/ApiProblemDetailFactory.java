package com.decathlon.idp_core.infrastructure.adapters.api.handler;

import java.net.URI;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/// Creates RFC 9457 error bodies with the legacy fields retained for clients
/// that still consume the previous error format.
public final class ApiProblemDetailFactory {

  private ApiProblemDetailFactory() {
  }

  /// Creates a Problem Detail response and adds the backward-compatible fields.
  ///
  /// @param status HTTP status for the response
  /// @param detail human-readable explanation of the problem
  /// @param instance URI identifying the request that produced the problem
  /// @return the fully populated problem detail
  public static ProblemDetail create(HttpStatusCode status, String detail, URI instance) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
    addCompatibilityProperties(problemDetail, status, instance);
    return problemDetail;
  }

  /// Adds legacy compatibility properties and request context to a Spring
  /// Problem Detail that was created by Spring MVC.
  ///
  /// @param problemDetail problem response to enrich
  /// @param status HTTP status for the response
  /// @param instance URI identifying the request that produced the problem
  public static void addCompatibilityProperties(ProblemDetail problemDetail, HttpStatusCode status,
      URI instance) {
    if (problemDetail.getType() == null) {
      problemDetail.setType(URI.create("about:blank"));
    }
    HttpStatus httpStatus = HttpStatus.resolve(status.value());
    String detail = problemDetail.getDetail();
    if (detail == null || detail.isBlank()) {
      detail = httpStatus != null ? httpStatus.getReasonPhrase() : status.toString();
      problemDetail.setDetail(detail);
    }
    if (httpStatus != null) {
      problemDetail.setTitle(httpStatus.getReasonPhrase());
    }
    problemDetail.setInstance(instance);
    problemDetail.setProperty("error", httpStatus != null ? httpStatus.name() : status.toString());
    problemDetail.setProperty("error_description", detail);
    problemDetail.setProperty("timestamp", Instant.now().toString());
  }
}
