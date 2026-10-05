package com.decathlon.idp_core.infrastructure.adapters.api.handler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.infrastructure.adapters.common.model.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/// Writes the standard API error body from servlet filters.
@Component
@RequiredArgsConstructor
public class ApiErrorResponseWriter {

  private final ObjectMapper objectMapper;

  /// Writes an error status and the standard JSON error response.
  ///
  /// @param response servlet response to populate
  /// @param status HTTP status for the error
  /// @param description human-readable explanation of the error
  /// @throws IOException if the response body cannot be written
  public void write(HttpServletResponse response, HttpStatus status, String description)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    objectMapper.writeValue(response.getOutputStream(),
        new ErrorResponse(status.name(), description));
  }
}
