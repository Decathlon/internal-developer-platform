package com.decathlon.idp_core.infrastructure.adapters.api.handler;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/// Writes the standard API error body from servlet filters.
@Component
public class ApiErrorResponseWriter {

  private final ObjectWriter problemDetailWriter;

  public ApiErrorResponseWriter(ObjectMapper objectMapper) {
    this.problemDetailWriter = objectMapper.copy()
        .addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
        .writerFor(ProblemDetail.class);
  }

  /// Writes an error status and the standard Problem Details response.
  ///
  /// @param request servlet request that produced the error
  /// @param response servlet response to populate
  /// @param status HTTP status for the error
  /// @param description human-readable explanation of the error
  /// @throws IOException if the response body cannot be written
  public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
      String description) throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    problemDetailWriter.writeValue(response.getOutputStream(),
        ApiProblemDetailFactory.create(status, description, URI.create(request.getRequestURI())));
  }
}
