package com.decathlon.idp_core.infrastructure.adapters.api.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

class ApiProblemDetailFactoryTest {

  @Test
  void shouldCreateProblemDetailWithLegacyPropertiesAndInstance() {
    URI instance = URI.create("/api/v1/entities/test");

    ProblemDetail problemDetail = ApiProblemDetailFactory.create(HttpStatus.NOT_FOUND,
        "Entity not found", instance);

    assertEquals(URI.create("about:blank"), problemDetail.getType());
    assertEquals("Not Found", problemDetail.getTitle());
    assertEquals(HttpStatus.NOT_FOUND.value(), problemDetail.getStatus());
    assertEquals("Entity not found", problemDetail.getDetail());
    assertEquals(instance, problemDetail.getInstance());
    assertEquals("NOT_FOUND", problemDetail.getProperties().get("error"));
    assertEquals("Entity not found", problemDetail.getProperties().get("error_description"));
    assertEquals("java.lang.String",
        problemDetail.getProperties().get("timestamp").getClass().getName());
  }

  @Test
  void shouldRestoreTypeAndFallbackDetailForUnknownStatus() {
    HttpStatusCode unknownStatus = HttpStatusCode.valueOf(599);
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(unknownStatus, " ");
    problemDetail.setType(null);

    ApiProblemDetailFactory.addCompatibilityProperties(problemDetail, unknownStatus, null);

    assertEquals(URI.create("about:blank"), problemDetail.getType());
    assertNull(problemDetail.getTitle());
    assertEquals("599", problemDetail.getDetail());
    assertNull(problemDetail.getInstance());
    assertEquals("599", problemDetail.getProperties().get("error"));
    assertEquals("599", problemDetail.getProperties().get("error_description"));
  }

  @Test
  void shouldUseReasonPhraseWhenDetailIsMissing() {
    ProblemDetail problemDetail = ApiProblemDetailFactory.create(HttpStatus.BAD_REQUEST, null,
        null);

    assertEquals("Bad Request", problemDetail.getDetail());
    assertEquals("BAD_REQUEST", problemDetail.getProperties().get("error"));
    assertEquals("Bad Request", problemDetail.getProperties().get("error_description"));
  }
}
