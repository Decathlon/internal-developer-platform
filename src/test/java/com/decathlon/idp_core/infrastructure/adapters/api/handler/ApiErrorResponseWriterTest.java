package com.decathlon.idp_core.infrastructure.adapters.api.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

class ApiErrorResponseWriterTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ApiErrorResponseWriter writer = new ApiErrorResponseWriter(objectMapper);

  @ParameterizedTest
  @MethodSource("authorizationErrors")
  void shouldWriteProblemDetailFieldsAndLegacyExtensionsAtTopLevel(HttpStatus status)
      throws Exception {
    var request = new MockHttpServletRequest("GET", "/api/v1/entity-templates");
    var response = new MockHttpServletResponse();
    String description = "Principal is not authorized to perform this operation";

    writer.write(request, response, status, description);

    var body = objectMapper.readTree(response.getContentAsByteArray());
    assertThat(response.getStatus()).isEqualTo(status.value());
    assertThat(response.getContentType()).startsWith("application/problem+json");
    assertThat(body.get("type").asText()).isEqualTo("about:blank");
    assertThat(body.get("title").asText()).isEqualTo(status.getReasonPhrase());
    assertThat(body.get("status").asInt()).isEqualTo(status.value());
    assertThat(body.get("detail").asText()).isEqualTo(description);
    assertThat(body.get("instance").asText()).isEqualTo("/api/v1/entity-templates");
    assertThat(body.get("error").asText()).isEqualTo(status.name());
    assertThat(body.get("error_description").asText()).isEqualTo(description);
    assertThat(body.get("timestamp").asText()).isNotBlank();
  }

  private static Stream<Arguments> authorizationErrors() {
    return Stream.of(Arguments.of(HttpStatus.UNAUTHORIZED), Arguments.of(HttpStatus.FORBIDDEN));
  }
}
