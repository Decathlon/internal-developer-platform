package com.decathlon.idp_core.infrastructure.adapters.api.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.stream.Stream;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.decathlon.idp_core.domain.exception.entity.EntityAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.exception.entity_template.RelationNameNotFoundEntityTemplateRelationsException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;

/// Comprehensive unit tests for [ApiExceptionHandler].
///
/// Tests the RFC 9457 (Problem Details) responses produced by the
/// consolidated domain exception handler, the dedicated handlers, and the
/// overridden Spring MVC exception handling methods.
@DisplayName("ApiExceptionHandler Tests")
class ApiExceptionHandlerTest {

  private ApiExceptionHandler exceptionHandler;
  private final HttpHeaders headers = new HttpHeaders();
  private final WebRequest request = mock(WebRequest.class);

  @BeforeEach
  void setUp() {
    exceptionHandler = new ApiExceptionHandler();
  }

  private ProblemDetail bodyOf(ResponseEntity<ProblemDetail> response) {
    ProblemDetail body = response.getBody();
    assertNotNull(body);
    return body;
  }

  @Nested
  @DisplayName("Domain Exception Handling")
  class DomainExceptionTests {

    /// Provides one representative domain exception per status bucket of
    /// [ApiExceptionHandler#STATUS_BY_EXCEPTION], to validate the map-driven
    /// dispatch without duplicating a test per exception class.
    static Stream<Arguments> domainExceptionTestData() {
      return Stream.of(
          Arguments.of(new EntityTemplateNotFoundException("Template 'test-id' not found"),
              HttpStatus.NOT_FOUND),
          Arguments.of(new EntityAlreadyExistsException("web-service", "api-gateway"),
              HttpStatus.CONFLICT),
          Arguments.of(new EntityValidationException(java.util.List.of("Invalid property")),
              HttpStatus.BAD_REQUEST),
          Arguments.of(new WebhookAuthenticationException("Invalid signature"),
              HttpStatus.UNAUTHORIZED),
          Arguments.of(new RelationNameNotFoundEntityTemplateRelationsException(
              "Relation name github_repository not found"), HttpStatus.UNPROCESSABLE_CONTENT));
    }

    @ParameterizedTest
    @MethodSource("domainExceptionTestData")
    @DisplayName("Should map each domain exception to its registered status and preserve its message")
    void shouldMapDomainExceptionToRegisteredStatus(RuntimeException exception,
        HttpStatus expectedStatus) {
      ResponseEntity<ProblemDetail> response = exceptionHandler.handleDomainException(exception);

      assertEquals(expectedStatus, response.getStatusCode());
      ProblemDetail body = bodyOf(response);
      assertEquals(expectedStatus.value(), body.getStatus());
      assertEquals(expectedStatus.getReasonPhrase(), body.getTitle());
      assertEquals(exception.getMessage(), body.getDetail());
    }

    /// A domain exception not present in the status map must fall back to a
    /// generic HTTP 500 response that never leaks the original message.
    @Test
    @DisplayName("Should fall back to 500 for an unmapped exception")
    void shouldFallBackToInternalServerErrorForUnmappedException() {
      RuntimeException exception = new IllegalStateException("Unexpected internal failure");

      ResponseEntity<ProblemDetail> response = exceptionHandler.handleDomainException(exception);

      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
      ProblemDetail body = bodyOf(response);
      assertEquals("An unexpected error occurred. Please try again later.", body.getDetail());
    }
  }

  @Nested
  @DisplayName("Constraint Violation Handling")
  class ConstraintViolationTests {

    @Test
    @DisplayName("Should handle ConstraintViolationException with a single violation")
    void shouldHandleSingleViolation() {
      ConstraintViolation<Object> violation = createMockConstraintViolation(
          "Field must not be null");
      ConstraintViolationException exception = new ConstraintViolationException("Validation failed",
          Set.of(violation));

      ResponseEntity<ProblemDetail> response = exceptionHandler
          .handleConstraintViolationException(exception);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      assertEquals("Field must not be null", bodyOf(response).getDetail());
    }

    @Test
    @DisplayName("Should handle ConstraintViolationException with multiple violations")
    void shouldHandleMultipleViolations() {
      ConstraintViolation<Object> violation1 = createMockConstraintViolation(
          "Field1 must not be null");
      ConstraintViolation<Object> violation2 = createMockConstraintViolation(
          "Field2 must not be blank");
      ConstraintViolationException exception = new ConstraintViolationException("Validation failed",
          Set.of(violation1, violation2));

      ResponseEntity<ProblemDetail> response = exceptionHandler
          .handleConstraintViolationException(exception);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      String detail = bodyOf(response).getDetail();
      assertTrue(detail.contains("Field1 must not be null"));
      assertTrue(detail.contains("Field2 must not be blank"));
      assertTrue(detail.contains(", "));
    }

    @SuppressWarnings("unchecked")
    private ConstraintViolation<Object> createMockConstraintViolation(String message) {
      ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
      when(violation.getMessage()).thenReturn(message);
      return violation;
    }
  }

  @Nested
  @DisplayName("Data Integrity Violation Handling")
  class DataIntegrityViolationTests {

    /// Technical SQL details must never leak into the response; only a generic
    /// conflict message is returned regardless of the original cause.
    @Test
    @DisplayName("Should hide technical details behind a generic 409 conflict message")
    void shouldHandleDataIntegrityViolationException() {
      DataIntegrityViolationException exception = new DataIntegrityViolationException(
          "duplicate key value violates unique constraint");

      ResponseEntity<ProblemDetail> response = exceptionHandler
          .handleDataIntegrityViolationException(exception);

      assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
      assertEquals("The request conflicts with the current state of the resource",
          bodyOf(response).getDetail());
    }
  }

  @Nested
  @DisplayName("Spring MVC Exception Handling Overrides")
  class SpringMvcExceptionTests {

    @Test
    @DisplayName("Should aggregate field errors for MethodArgumentNotValidException")
    void shouldHandleMethodArgumentNotValidException() throws Exception {
      Object target = new Object();
      BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "testObject");
      bindingResult.addError(new FieldError("testObject", "field1", "Field1 is required"));
      bindingResult.addError(new FieldError("testObject", "field2", "Field2 must be valid"));

      MethodParameter methodParameter = mock(MethodParameter.class);
      when(methodParameter.getExecutable()).thenReturn(this.getClass().getMethod("testMethod"));
      MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
          methodParameter, bindingResult);

      ResponseEntity<Object> response = exceptionHandler.handleMethodArgumentNotValid(exception,
          headers, HttpStatus.BAD_REQUEST, request);

      assertNotNull(response);
      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      String detail = ((ProblemDetail) response.getBody()).getDetail();
      assertTrue(detail.contains("Field1 is required"));
      assertTrue(detail.contains("Field2 must be valid"));
      assertTrue(detail.contains(", "));
    }

    // Helper method referenced through reflection above.
    public void testMethod() {
      // Empty method used solely as a MethodParameter source.
    }

    @Test
    @DisplayName("Should force HTTP 400 for MissingPathVariableException")
    void shouldHandleMissingPathVariableException() throws Exception {
      MethodParameter methodParameter = mock(MethodParameter.class);
      when(methodParameter.getExecutable()).thenReturn(this.getClass().getMethod("testMethod"));
      doReturn(String.class).when(methodParameter).getNestedParameterType();
      MissingPathVariableException exception = new MissingPathVariableException("identifier",
          methodParameter);

      ResponseEntity<Object> response = exceptionHandler.handleMissingPathVariable(exception,
          headers, HttpStatus.INTERNAL_SERVER_ERROR, request);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      assertEquals("Missing required path variable: identifier",
          ((ProblemDetail) response.getBody()).getDetail());
    }

    @Test
    @DisplayName("Should return a stable message for NoHandlerFoundException")
    void shouldHandleNoHandlerFoundException() {
      NoHandlerFoundException exception = new NoHandlerFoundException("GET", "/api/v1/unknown",
          headers);

      ResponseEntity<Object> response = exceptionHandler.handleNoHandlerFoundException(exception,
          headers, HttpStatus.NOT_FOUND, request);

      assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
      assertEquals("Malformed request URL or missing path variable.",
          ((ProblemDetail) response.getBody()).getDetail());
    }
  }

  @Nested
  @DisplayName("HTTP Message Not Readable Handling")
  class HttpMessageExceptionTests {

    /// Provides test data for [HttpMessageNotReadableException] scenarios. Each
    /// argument contains: input message and expected detail message.
    static Stream<Arguments> httpMessageNotReadableExceptionTestData() {
      return Stream.of(
          Arguments.of("Required request body is missing: public ResponseEntity",
              "Request body is required"),
          Arguments.of("JSON parse error: Unexpected character",
              "Invalid JSON format in request body"),
          Arguments.of(
              "Cannot deserialize value of type `PropertyType` from String \"INVALID_TYPE\": not one of the values accepted for Enum class",
              "Invalid value 'INVALID_TYPE' for property 'type'"),
          Arguments.of(
              "Cannot deserialize value of type `PropertyFormat` from String \"INVALID_FORMAT\": not one of the values accepted for Enum class",
              "Invalid value 'INVALID_FORMAT' for property 'format'"),
          Arguments.of(
              "Cannot deserialize value of type `UnknownEnum` from String \"VALUE\": not one of the values accepted for Enum class",
              "Invalid enum value in request body"),
          Arguments.of("Cannot deserialize value of type `com.example.SomeType`: some other error",
              "Invalid type: expected SomeType"),
          Arguments.of("Something completely unexpected happened", "Invalid request body format"),
          Arguments.of(
              "Cannot deserialize value of type `PropertyType`: not one of the values accepted for Enum class",
              "Invalid value for property 'type'"));
    }

    @Test
    @DisplayName("Should default to a generic message when the exception message is null")
    void shouldHandleNullMessage() {
      HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
      when(exception.getMessage()).thenReturn(null);

      ResponseEntity<Object> response = exceptionHandler.handleHttpMessageNotReadable(exception,
          headers, HttpStatus.BAD_REQUEST, request);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      assertEquals("Invalid request body format", ((ProblemDetail) response.getBody()).getDetail());
    }

    @ParameterizedTest
    @MethodSource("httpMessageNotReadableExceptionTestData")
    @DisplayName("Should translate technical Jackson messages into readable ones")
    void shouldHandleVariousErrorTypes(String originalMessage, String expectedDetail) {
      HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
      when(exception.getMessage()).thenReturn(originalMessage);

      ResponseEntity<Object> response = exceptionHandler.handleHttpMessageNotReadable(exception,
          headers, HttpStatus.BAD_REQUEST, request);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      assertEquals(expectedDetail, ((ProblemDetail) response.getBody()).getDetail());
    }
  }
}
