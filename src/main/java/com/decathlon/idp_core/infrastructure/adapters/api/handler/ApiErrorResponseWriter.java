package com.decathlon.idp_core.infrastructure.adapters.api.handler;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

/// Writes the standard ProblemDetail response from servlet filters.
@Component
@RequiredArgsConstructor
public class ApiErrorResponseWriter {

  private final ObjectMapper objectMapper;

  /// Writes an error status and the standard ProblemDetail response.
  ///
  /// @param request servlet request that produced the error
  /// @param response servlet response to populate
  /// @param status HTTP status for the error
  /// @param description human-readable explanation of the error
  /// @throws IOException if the response body cannot be written
  public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
      String description) throws IOException {
    ProblemDetail problemDetail = ProblemDetail.forStatus(status);
    problemDetail.setType(URI.create("about:blank"));
    problemDetail.setTitle(status.getReasonPhrase());
    problemDetail.setDetail(description);
    problemDetail.setInstance(URI.create(request.getRequestURI()));
    problemDetail.setProperty("error", status.name());
    problemDetail.setProperty("error_description", description);
    problemDetail.setProperty("timestamp", Instant.now().toString());
    ObjectNode responseBody = objectMapper.valueToTree(problemDetail);
    responseBody.remove("properties");
    problemDetail.getProperties()
        .forEach((name, value) -> responseBody.set(name, objectMapper.valueToTree(value)));

    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    objectMapper.writeValue(response.getOutputStream(), responseBody);
  }
}
