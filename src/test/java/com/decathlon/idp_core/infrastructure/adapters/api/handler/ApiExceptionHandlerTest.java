package com.decathlon.idp_core.infrastructure.adapters.api.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Min;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.decathlon.idp_core.domain.constant.ValidationMessages;
import com.decathlon.idp_core.domain.exception.authorization.PrincipalNotAuthorizedException;
import com.decathlon.idp_core.domain.exception.entity.EntityAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateIsRelationTargetException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.exception.entity_template.PropertyNameNotFoundEntityTemplatePropertiesException;
import com.decathlon.idp_core.domain.exception.entity_template.RelationNameNotFoundEntityTemplateRelationsException;
import com.decathlon.idp_core.domain.exception.principal.PrincipalNotFoundException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;
import com.decathlon.idp_core.infrastructure.adapters.api.exception.MissingPrincipalIdentifierException;

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

  private void assertCompatibilityProperties(ProblemDetail body, HttpStatus status, String detail) {
    assertEquals("about:blank", body.getType().toString());
    assertEquals(status.getReasonPhrase(), body.getTitle());
    assertEquals(status.value(), body.getStatus());
    assertEquals(detail, body.getDetail());
    assertEquals(status.name(), body.getProperties().get("error"));
    assertEquals(detail, body.getProperties().get("error_description"));
    assertDoesNotThrow(() -> Instant.parse((String) body.getProperties().get("timestamp")));
  }

  @Test
  @DisplayName("Should use Spring's default unreadable-body handling and add compatibility fields")
  void shouldUseSpringDefaultForUnreadableBody() {
    RestTestClient client = RestTestClient.bindToController(new RequestBodyController())
        .configureServer(builder -> builder.setControllerAdvice(exceptionHandler)).build();

    client.post().uri("/request-body").contentType(MediaType.APPLICATION_JSON).body("{").exchange()
        .expectStatus().isBadRequest().expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON).expectBody()
        .jsonPath("$.type").isEqualTo("about:blank").jsonPath("$.title").isEqualTo("Bad Request")
        .jsonPath("$.status").isEqualTo(400).jsonPath("$.detail").exists().jsonPath("$.error")
        .isEqualTo("BAD_REQUEST").jsonPath("$.error_description").exists().jsonPath("$.timestamp")
        .exists().jsonPath("$.instance").isEqualTo("/request-body");
  }

  @Test
  @DisplayName("Should route domain exceptions through the Problem Detail handler")
  void shouldRouteDomainExceptionThroughAdvice() {
    RestTestClient client = RestTestClient.bindToController(new RequestBodyController())
        .configureServer(builder -> builder.setControllerAdvice(exceptionHandler)).build();

    client.get().uri("/domain-error").exchange().expectStatus().isNotFound().expectBody()
        .jsonPath("$.type").isEqualTo("about:blank").jsonPath("$.title").isEqualTo("Not Found")
        .jsonPath("$.status").isEqualTo(404).jsonPath("$.detail").isEqualTo("Missing template")
        .jsonPath("$.error").isEqualTo("NOT_FOUND").jsonPath("$.error_description")
        .isEqualTo("Missing template").jsonPath("$.timestamp").exists();
  }

  @Test
  @DisplayName("Should fall back to 500 for a checked exception")
  void shouldFallBackToInternalServerErrorForCheckedException() {
    ResponseEntity<ProblemDetail> response = exceptionHandler
        .handleMappedException(new Exception("Unexpected checked failure"), request);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertCompatibilityProperties(bodyOf(response), HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred. Please try again later.");
  }

  @Test
  @DisplayName("Should handle method parameter validation errors")
  void shouldHandleHandlerMethodValidationException() {
    RestTestClient client = RestTestClient.bindToController(new MethodValidationController())
        .configureServer(builder -> builder.setControllerAdvice(exceptionHandler)).build();

    client.get().uri("/validated?count=0").exchange().expectStatus().isBadRequest().expectBody()
        .jsonPath("$.type").isEqualTo("about:blank").jsonPath("$.status").isEqualTo(400)
        .jsonPath("$.detail").exists().jsonPath("$.error").isEqualTo("BAD_REQUEST")
        .jsonPath("$.error_description").exists().jsonPath("$.timestamp").exists();
  }

  @Test
  @DisplayName("Should preserve pagination parameter messages")
  void shouldHandlePaginationTypeMismatches() {
    assertTypeMismatchDetail("page", Integer.class, ValidationMessages.PAGE_INDEX_NOT_INTEGER);
    assertTypeMismatchDetail("size", Integer.class, ValidationMessages.PAGE_SIZE_NOT_INTEGER);
  }

  @Test
  @DisplayName("Should include allowed enum values in type mismatch messages")
  void shouldHandleEnumTypeMismatch() {
    ResponseEntity<Object> response = exceptionHandler.handleTypeMismatch(
        typeMismatch("mode", "invalid", TestMode.class), headers, HttpStatus.BAD_REQUEST, request);

    assertEquals("Invalid value 'invalid' for parameter 'mode'. Allowed values: [FAST, SAFE].",
        ((ProblemDetail) response.getBody()).getDetail());
  }

  @Test
  @DisplayName("Should include the expected type in scalar mismatch messages")
  void shouldHandleScalarTypeMismatch() {
    ResponseEntity<Object> response = exceptionHandler.handleTypeMismatch(
        typeMismatch("count", "many", Integer.class), headers, HttpStatus.BAD_REQUEST, request);

    assertEquals("Invalid value 'many' for parameter 'count'. Expected type: Integer.",
        ((ProblemDetail) response.getBody()).getDetail());
  }

  @Test
  @DisplayName("Should handle type mismatches without a known required type")
  void shouldHandleTypeMismatchWithoutRequiredType() {
    ResponseEntity<Object> response = exceptionHandler.handleTypeMismatch(
        typeMismatch("value", "bad", null), headers, HttpStatus.BAD_REQUEST, request);

    assertEquals("Invalid value 'bad' for parameter 'value'.",
        ((ProblemDetail) response.getBody()).getDetail());
  }

  @Test
  @DisplayName("Should delegate non-method argument mismatches to Spring")
  void shouldDelegateOtherTypeMismatchesToSpring() {
    TypeMismatchException exception = new TypeMismatchException("not-an-integer", Integer.class);

    ResponseEntity<Object> response = exceptionHandler.handleTypeMismatch(exception, headers,
        HttpStatus.BAD_REQUEST, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody() instanceof ProblemDetail);
  }

  private void assertTypeMismatchDetail(String name, Class<?> requiredType, String expectedDetail) {
    ResponseEntity<Object> response = exceptionHandler.handleTypeMismatch(
        typeMismatch(name, "invalid", requiredType), headers, HttpStatus.BAD_REQUEST, request);

    assertEquals(expectedDetail, ((ProblemDetail) response.getBody()).getDetail());
  }

  private org.springframework.web.method.annotation.MethodArgumentTypeMismatchException typeMismatch(
      String name, Object value, Class<?> requiredType) {
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException exception = mock(
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class);
    when(exception.getName()).thenReturn(name);
    when(exception.getValue()).thenReturn(value);
    doReturn(requiredType).when(exception).getRequiredType();
    return exception;
  }

  @RestController
  static class RequestBodyController {

    @PostMapping("/request-body")
    void create(@RequestBody RequestBodyDto requestBody) {
    }

    @GetMapping("/domain-error")
    void domainError() {
      throw new EntityTemplateNotFoundException("Missing template");
    }
  }

  record RequestBodyDto(String name) {
  }

  @RestController
  static class MethodValidationController {

    @GetMapping("/validated")
    void validate(@RequestParam @Min(1) int count) {
    }
  }

  private enum TestMode {
    FAST, SAFE
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
          Arguments.of(new EntityTemplateIsRelationTargetException("used-template"),
              HttpStatus.BAD_REQUEST),
          Arguments.of(new MissingPrincipalIdentifierException("id"), HttpStatus.UNAUTHORIZED),
          Arguments.of(new WebhookAuthenticationException("Invalid signature"),
              HttpStatus.UNAUTHORIZED),
          Arguments.of(new PrincipalNotAuthorizedException("platform-user"), HttpStatus.FORBIDDEN),
          Arguments.of(new PrincipalNotFoundException("platform-user"), HttpStatus.NOT_FOUND),
          Arguments.of(new PropertyNameNotFoundEntityTemplatePropertiesException(
              "Mapped property does not exist in template"), HttpStatus.UNPROCESSABLE_CONTENT),
          Arguments.of(new RelationNameNotFoundEntityTemplateRelationsException(
              "Relation name github_repository not found"), HttpStatus.UNPROCESSABLE_CONTENT));
    }

    @ParameterizedTest
    @MethodSource("domainExceptionTestData")
    @DisplayName("Should map each domain exception to its registered status and preserve its message")
    void shouldMapDomainExceptionToRegisteredStatus(RuntimeException exception,
        HttpStatus expectedStatus) {
      ResponseEntity<ProblemDetail> response = exceptionHandler.handleMappedException(exception,
          request);

      assertEquals(expectedStatus, response.getStatusCode());
      ProblemDetail body = bodyOf(response);
      assertCompatibilityProperties(body, expectedStatus, exception.getMessage());
    }

    /// A domain exception not present in the status map must fall back to a
    /// generic HTTP 500 response that never leaks the original message.
    @Test
    @DisplayName("Should fall back to 500 for an unmapped exception")
    void shouldFallBackToInternalServerErrorForUnmappedException() {
      RuntimeException exception = new IllegalStateException("Unexpected internal failure");

      ResponseEntity<ProblemDetail> response = exceptionHandler.handleMappedException(exception,
          request);

      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
      ProblemDetail body = bodyOf(response);
      assertCompatibilityProperties(body, HttpStatus.INTERNAL_SERVER_ERROR,
          "An unexpected error occurred. Please try again later.");
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
          .handleConstraintViolationException(exception, request);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      assertCompatibilityProperties(bodyOf(response), HttpStatus.BAD_REQUEST,
          "Field must not be null");
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
          .handleConstraintViolationException(exception, request);

      assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
      String detail = bodyOf(response).getDetail();
      assertTrue(detail.contains("Field1 must not be null"));
      assertTrue(detail.contains("Field2 must not be blank"));
      assertTrue(detail.contains(", "));
      assertCompatibilityProperties(bodyOf(response), HttpStatus.BAD_REQUEST, detail);
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
          .handleDataIntegrityViolationException(exception, request);

      assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
      assertCompatibilityProperties(bodyOf(response), HttpStatus.CONFLICT,
          "The request conflicts with the current state of the resource");
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
      assertCompatibilityProperties((ProblemDetail) response.getBody(), HttpStatus.BAD_REQUEST,
          detail);
    }

    @Test
    @DisplayName("Should preserve unknown HTTP status codes when handling validation errors")
    void shouldHandleValidationErrorForUnknownStatus() throws Exception {
      Object target = new Object();
      BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "testObject");
      bindingResult.addError(new FieldError("testObject", "field", "Field is invalid"));
      MethodParameter methodParameter = mock(MethodParameter.class);
      when(methodParameter.getExecutable()).thenReturn(this.getClass().getMethod("testMethod"));
      MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
          methodParameter, bindingResult);
      HttpStatusCode unknownStatus = HttpStatusCode.valueOf(599);

      ResponseEntity<Object> response = exceptionHandler.handleMethodArgumentNotValid(exception,
          headers, unknownStatus, request);

      assertEquals(599, response.getStatusCode().value());
      ProblemDetail body = (ProblemDetail) response.getBody();
      assertEquals(599, body.getStatus());
      assertNull(body.getTitle());
      assertEquals("Field is invalid", body.getDetail());
      assertEquals("599", body.getProperties().get("error"));
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
      assertCompatibilityProperties((ProblemDetail) response.getBody(), HttpStatus.BAD_REQUEST,
          "Missing required path variable: identifier");
    }

    @Test
    @DisplayName("Should return a stable message for NoHandlerFoundException")
    void shouldHandleNoHandlerFoundException() {
      NoHandlerFoundException exception = new NoHandlerFoundException("GET", "/api/v1/unknown",
          headers);

      ResponseEntity<Object> response = exceptionHandler.handleNoHandlerFoundException(exception,
          headers, HttpStatus.NOT_FOUND, request);

      assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
      assertCompatibilityProperties((ProblemDetail) response.getBody(), HttpStatus.NOT_FOUND,
          "Malformed request URL or missing path variable.");
    }
  }
}
