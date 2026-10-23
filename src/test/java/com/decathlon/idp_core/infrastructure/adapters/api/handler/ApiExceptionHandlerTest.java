package com.decathlon.idp_core.infrastructure.adapters.api.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import com.decathlon.idp_core.domain.exception.entity.EntityAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity.EntityDeletionBlockedException;
import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingAlreadyInUseException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingConfigurationException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingHasNoPropertiesException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingHasNoRelationsException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingJsltErrorException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingNotFoundException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.ExpressionEvaluationFailedException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateIdentifierCannotChangeException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateIsRelationTargetException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNameAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateUsedByDynamicMappingException;
import com.decathlon.idp_core.domain.exception.entity_template.PropertyDefinitionRulesConflictException;
import com.decathlon.idp_core.domain.exception.entity_template.PropertyNameAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity_template.PropertyNameNotFoundEntityTemplatePropertiesException;
import com.decathlon.idp_core.domain.exception.entity_template.PropertyTypeChangeException;
import com.decathlon.idp_core.domain.exception.entity_template.RelationCannotTargetItselfException;
import com.decathlon.idp_core.domain.exception.entity_template.RelationNameAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity_template.RelationNameNotFoundEntityTemplateRelationsException;
import com.decathlon.idp_core.domain.exception.entity_template.RelationTargetTemplateChangeException;
import com.decathlon.idp_core.domain.exception.entity_template.TargetTemplateNotFoundException;
import com.decathlon.idp_core.domain.exception.filter.InvalidFilterDslException;
import com.decathlon.idp_core.domain.exception.principal.PrincipalNotFoundException;
import com.decathlon.idp_core.domain.exception.search.InvalidSearchQueryException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorAlreadyExistException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorNotFoundException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorTitleAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;

class ApiExceptionHandlerTest {

  private ApiExceptionHandler exceptionHandler;

  @BeforeEach
  void setUp() throws Exception {
    Constructor<ApiExceptionHandler> constructor = ApiExceptionHandler.class
        .getDeclaredConstructor();
    constructor.setAccessible(true);
    exceptionHandler = constructor.newInstance();
  }

  /**
   * Creates a mocked {@link WebRequest} with a stubbed description, matching the
   * framework's non-null contract for {@code getDescription(boolean)}.
   */
  private static WebRequest mockWebRequest() {
    WebRequest request = mock(WebRequest.class);
    when(request.getDescription(false)).thenReturn("uri=/api/v1/test");
    return request;
  }

  @Test
  void shouldHandleEntityTemplateNotFoundException() {
    ProblemDetail body = exceptionHandler
        .handleDomainException(new EntityTemplateNotFoundException("missing"));
    assertProblemDetail(body, HttpStatus.NOT_FOUND, "missing", "NOT_FOUND");
  }

  @Test
  void shouldHandleEntityTemplateAlreadyExistsException() {
    ProblemDetail body = exceptionHandler
        .handleDomainException(new EntityTemplateAlreadyExistsException("duplicate-id"));
    assertProblemDetail(body, HttpStatus.CONFLICT,
        "An Entity Template already exists with the same identifier:duplicate-id", "CONFLICT");
  }

  @Test
  void shouldHandleEntityAlreadyExistsException() {
    ProblemDetail body = exceptionHandler
        .handleDomainException(new EntityAlreadyExistsException("a", "b"));
    assertProblemDetail(body, HttpStatus.CONFLICT,
        "Entity with name 'b' already exists for template 'a'", "CONFLICT");
  }

  @Test
  void shouldHandleEntityValidationException() {
    ProblemDetail body = exceptionHandler.handleDomainException(
        new EntityValidationException(java.util.List.of("Invalid property")));
    assertProblemDetail(body, HttpStatus.BAD_REQUEST, "Entity validation failed: Invalid property",
        "BAD_REQUEST");
  }

  @Test
  void shouldHandleConstraintViolationException() {
    ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
    when(violation.getMessage()).thenReturn("Field must not be null");
    ProblemDetail body = exceptionHandler.handleConstraintViolationException(
        new ConstraintViolationException("failed", Set.of(violation)));
    assertProblemDetail(body, HttpStatus.BAD_REQUEST, "Field must not be null", "BAD_REQUEST");
  }

  @Test
  void shouldHandleMethodArgumentNotValidViaFrameworkOverride() throws Exception {
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "obj");
    bindingResult.addError(new FieldError("obj", "field", "Field is required"));
    MethodParameter methodParameter = new MethodParameter(
        ApiExceptionHandlerTest.class.getDeclaredMethod("setUp"), -1);
    MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter,
        bindingResult);
    ProblemDetail body = (ProblemDetail) exceptionHandler.handleMethodArgumentNotValid(ex,
        new HttpHeaders(), HttpStatus.BAD_REQUEST, mockWebRequest()).getBody();
    assertProblemDetail(body, HttpStatus.BAD_REQUEST, "Field is required", "BAD_REQUEST");
  }

  @Test
  void shouldHandleHandlerMethodValidationViaFrameworkOverride() {
    HandlerMethodValidationException ex = mock(HandlerMethodValidationException.class);
    MessageSourceResolvable error = mock(MessageSourceResolvable.class);
    when(error.getDefaultMessage()).thenReturn("Parameter is invalid");
    doReturn(List.of(error)).when(ex).getAllErrors();

    ResponseEntity<Object> response = exceptionHandler.handleHandlerMethodValidationException(ex,
        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, mockWebRequest());
    ProblemDetail body = (ProblemDetail) response.getBody();

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertProblemDetail(body, HttpStatus.BAD_REQUEST, "Parameter is invalid", "BAD_REQUEST");
  }

  @Test
  void shouldDeclareUnambiguousExceptionHandlerMappings() {
    assertDoesNotThrow(() -> new ExceptionHandlerMethodResolver(ApiExceptionHandler.class));
  }

  @Test
  void shouldUseNativeProblemDetailForUnreadableMessage() throws Exception {
    HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
        "Cannot deserialize value of type `PropertyType` from String \"INVALID_TYPE\"",
        new HttpInputMessage() {
          @Override
          public HttpHeaders getHeaders() {
            return new HttpHeaders();
          }

          @Override
          public java.io.InputStream getBody() {
            return java.io.InputStream.nullInputStream();
          }
        });
    ProblemDetail body = (ProblemDetail) exceptionHandler.handleException(ex, mockWebRequest())
        .getBody();

    assertProblemDetail(body, HttpStatus.BAD_REQUEST, "Failed to read request", "BAD_REQUEST");
  }

  @Test
  void shouldSetInstanceOnDomainErrorsFromCurrentRequest() {
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    servletRequest.setRequestURI("/api/v1/entities/x");
    org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
        new org.springframework.web.context.request.ServletRequestAttributes(servletRequest));
    try {
      ProblemDetail body = exceptionHandler
          .handleDomainException(new EntityAlreadyExistsException("a", "b"));
      assertEquals("/api/v1/entities/x", String.valueOf(body.getInstance()));
    } finally {
      org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
    }
  }

  @Test
  void shouldHandleGenericExceptionWithoutExposingInternalDetails() {
    ProblemDetail body = exceptionHandler
        .handleDomainException(new RuntimeException("Sensitive internal details"));

    assertProblemDetail(body, HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred. Please try again later.", "INTERNAL_SERVER_ERROR");
  }

  @Test
  void shouldNormalizeInheritedFrameworkProblemDetails() throws Exception {
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    servletRequest.setRequestURI("/api/v1/test");
    ServletWebRequest webRequest = new ServletWebRequest(servletRequest);

    ResponseEntity<Object> response = exceptionHandler.handleException(
        new MissingServletRequestParameterException("page", "integer"), webRequest);
    ProblemDetail body = (ProblemDetail) response.getBody();

    assertNotNull(body);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("BAD_REQUEST", body.getProperties().get("error"));
    assertEquals(body.getDetail(), body.getProperties().get("error_description"));
    assertNotNull(body.getProperties().get("timestamp"));
    assertEquals("/api/v1/test", body.getInstance().toString());
  }

  @Test
  void shouldPreserveExistingProblemDetailExtensionsAndInstance() {
    ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid input");
    Instant timestamp = Instant.parse("2026-10-14T08:00:00Z");
    body.setProperty("error", "CUSTOM_ERROR");
    body.setProperty("error_description", "Custom description");
    body.setProperty("timestamp", timestamp);
    body.setInstance(URI.create("/original"));
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/replacement");

    ResponseEntity<Object> response = exceptionHandler.createResponseEntity(body, new HttpHeaders(),
        HttpStatus.BAD_REQUEST, new ServletWebRequest(request));

    assertSame(body, response.getBody());
    assertEquals("CUSTOM_ERROR", body.getProperties().get("error"));
    assertEquals("Custom description", body.getProperties().get("error_description"));
    assertEquals(timestamp, body.getProperties().get("timestamp"));
    assertEquals(URI.create("/original"), body.getInstance());
  }

  @Test
  void shouldAddMissingExtensionsWithoutReplacingExistingProperties() {
    ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid input");
    body.setProperty("traceId", "trace-123");
    body.setProperty("error", "CUSTOM_ERROR");

    exceptionHandler.createResponseEntity(body, new HttpHeaders(), HttpStatus.BAD_REQUEST,
        mockWebRequest());

    assertEquals("trace-123", body.getProperties().get("traceId"));
    assertEquals("CUSTOM_ERROR", body.getProperties().get("error"));
    assertEquals("Invalid input", body.getProperties().get("error_description"));
    assertInstanceOf(Instant.class, body.getProperties().get("timestamp"));
    assertNull(body.getInstance());
  }

  @Test
  void shouldEnrichProblemDetailWithEmptyProperties() {
    ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid input");
    body.setProperties(new LinkedHashMap<>());

    exceptionHandler.createResponseEntity(body, new HttpHeaders(), HttpStatus.BAD_REQUEST,
        mockWebRequest());

    assertProblemDetail(body, HttpStatus.BAD_REQUEST, "Invalid input", "BAD_REQUEST");
  }

  @Test
  void shouldUseNumericErrorForNonStandardHttpStatus() {
    HttpStatusCode status = HttpStatusCode.valueOf(599);
    ProblemDetail body = ProblemDetail.forStatusAndDetail(status, "Non-standard error");

    ResponseEntity<Object> response = exceptionHandler.createResponseEntity(body, new HttpHeaders(),
        status, mockWebRequest());

    assertEquals(status, response.getStatusCode());
    assertEquals("599", body.getProperties().get("error"));
    assertEquals("Non-standard error", body.getProperties().get("error_description"));
    assertInstanceOf(Instant.class, body.getProperties().get("timestamp"));
  }

  @Test
  void shouldLeaveNonProblemDetailResponseUnchanged() {
    Map<String, String> body = Map.of("message", "original");
    HttpHeaders headers = new HttpHeaders();
    headers.add("X-Test", "unchanged");

    ResponseEntity<Object> response = exceptionHandler.createResponseEntity(body, headers,
        HttpStatus.BAD_REQUEST, mockWebRequest());

    assertSame(body, response.getBody());
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("unchanged", response.getHeaders().getFirst("X-Test"));
  }

  @Test
  void shouldAllowResponseWithoutBody() {
    ResponseEntity<Object> response = exceptionHandler.createResponseEntity(null, new HttpHeaders(),
        HttpStatus.NO_CONTENT, mockWebRequest());

    assertNull(response.getBody());
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  void shouldHandlePrincipalNotFoundException() {
    PrincipalNotFoundException ex = new PrincipalNotFoundException("missing-principal");

    ProblemDetail body = exceptionHandler.handleDomainException(ex);

    assertProblemDetail(body, HttpStatus.NOT_FOUND, ex.getMessage(), "NOT_FOUND");
  }

  @Test
  void shouldHandleMissingPathVariableViaFrameworkOverride() {
    ProblemDetail body = (ProblemDetail) exceptionHandler.handleMissingPathVariable(
        mock(org.springframework.web.bind.MissingPathVariableException.class), new HttpHeaders(),
        HttpStatus.BAD_REQUEST, mockWebRequest()).getBody();
    assertNotNull(body);
    assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
    assertEquals("BAD_REQUEST", body.getProperties().get("error"));
  }

  @Test
  void shouldSetInstanceWhenWebRequestIsPresent() {
    ServletWebRequest request = mock(ServletWebRequest.class);
    when(request.getDescription(false)).thenReturn("uri=/api/v1/test");
    ProblemDetail body = (ProblemDetail) exceptionHandler.handleMissingPathVariable(
        mock(org.springframework.web.bind.MissingPathVariableException.class), new HttpHeaders(),
        HttpStatus.BAD_REQUEST, request).getBody();

    assertNotNull(body);
    assertNotNull(body.getInstance());
    assertEquals("/api/v1/test", body.getInstance().toString());
  }

  @Test
  void shouldHandleInvalidFilterDslException() {
    ProblemDetail body = exceptionHandler
        .handleDomainException(new InvalidFilterDslException("bad filter"));
    assertProblemDetail(body, HttpStatus.BAD_REQUEST, "bad filter", "BAD_REQUEST");
  }

  @Test
  void shouldHandleInvalidSearchQueryException() {
    ProblemDetail body = exceptionHandler
        .handleDomainException(new InvalidSearchQueryException("bad query"));
    assertProblemDetail(body, HttpStatus.BAD_REQUEST, "bad query", "BAD_REQUEST");
  }

  @Test
  void shouldHandleEntityTemplateNameAlreadyExistsException() {
    ProblemDetail body = exceptionHandler
        .handleDomainException(new EntityTemplateNameAlreadyExistsException("duplicate-name"));
    assertEquals(HttpStatus.CONFLICT.value(), body.getStatus());
    assertEquals("CONFLICT", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleEntityTemplateIdentifierCannotChangeException() {
    ProblemDetail body = exceptionHandler
        .handleDomainException(new EntityTemplateIdentifierCannotChangeException("template-id"));
    assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
    assertEquals("BAD_REQUEST", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleWrongPropertyRulesException() {
    ProblemDetail body = exceptionHandler.handleDomainException(
        new PropertyDefinitionRulesConflictException("property-test", "STRING", "not allowed"));
    assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
    assertEquals("BAD_REQUEST", body.getProperties().get("error"));
  }

  @Test
  void shouldHandlePropertyNameAlreadyExistsException() {
    ProblemDetail body = exceptionHandler
        .handleDomainException(new PropertyNameAlreadyExistsException("property-test"));
    assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
    assertEquals("BAD_REQUEST", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleRelationNameAlreadyExistsException() {
    ProblemDetail body = exceptionHandler
        .handleDomainException(new RelationNameAlreadyExistsException("belongsto"));
    assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
    assertEquals("BAD_REQUEST", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleTargetTemplateNotFoundException() {
    ProblemDetail body = exceptionHandler
        .handleDomainException(new TargetTemplateNotFoundException("non-existent"));
    assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
    assertEquals("BAD_REQUEST", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleEntityTemplateIsRelationTargetException() {
    ProblemDetail body = exceptionHandler
        .handleDomainException(new EntityTemplateIsRelationTargetException("microservice"));
    assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
    assertEquals("BAD_REQUEST", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleTypeChangeException() {
    ProblemDetail body = exceptionHandler
        .handleDomainException(new PropertyTypeChangeException("name", "STRING", "NUMBER"));
    assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
    assertEquals("BAD_REQUEST", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleRelationTargetTemplateChangeException() {
    ProblemDetail body = exceptionHandler.handleDomainException(
        new RelationTargetTemplateChangeException("dependencies", "service", "service-modified"));
    assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
    assertEquals("BAD_REQUEST", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleRelationCannotTargetItselfException() {
    ProblemDetail body = exceptionHandler.handleDomainException(
        new RelationCannotTargetItselfException("circular", "self-ref-template"));
    assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
    assertEquals("BAD_REQUEST", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleEntityDynamicMappingConfigurationException() {
    ProblemDetail body = exceptionHandler.handleDomainException(
        new EntityDynamicMappingConfigurationException("invalid configuration"));
    assertProblemDetail(body, HttpStatus.BAD_REQUEST, "invalid configuration", "BAD_REQUEST");
  }

  @Test
  void shouldHandleExpressionEvaluationFailedException() {
    ProblemDetail body = exceptionHandler.handleDomainException(
        new ExpressionEvaluationFailedException(".foo", "syntax error", null));
    assertEquals(HttpStatus.UNPROCESSABLE_CONTENT.value(), body.getStatus());
    assertEquals("UNPROCESSABLE_CONTENT", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleEntityDynamicMappingJsltErrorException() {
    ProblemDetail body = exceptionHandler
        .handleDomainException(new EntityDynamicMappingJsltErrorException("jslt failed"));
    assertProblemDetail(body, HttpStatus.UNPROCESSABLE_CONTENT, "jslt failed",
        "UNPROCESSABLE_CONTENT");
  }

  @Test
  void shouldHandlePropertyNameNotFoundEntityTemplatePropertiesException() {
    ProblemDetail body = exceptionHandler.handleDomainException(
        new PropertyNameNotFoundEntityTemplatePropertiesException("unknown property"));
    assertProblemDetail(body, HttpStatus.UNPROCESSABLE_CONTENT, "unknown property",
        "UNPROCESSABLE_CONTENT");
  }

  @Test
  void shouldHandleRelationNameNotFoundEntityTemplateRelationsException() {
    ProblemDetail body = exceptionHandler.handleDomainException(
        new RelationNameNotFoundEntityTemplateRelationsException("unknown relation"));
    assertProblemDetail(body, HttpStatus.UNPROCESSABLE_CONTENT, "unknown relation",
        "UNPROCESSABLE_CONTENT");
  }

  @Test
  void shouldHandleEntityDynamicMappingHasNoPropertiesException() {
    ProblemDetail body = exceptionHandler.handleDomainException(
        new EntityDynamicMappingHasNoPropertiesException("missing properties"));
    assertProblemDetail(body, HttpStatus.UNPROCESSABLE_CONTENT, "missing properties",
        "UNPROCESSABLE_CONTENT");
  }

  @Test
  void shouldHandleEntityDynamicMappingHasNoRelationsException() {
    ProblemDetail body = exceptionHandler.handleDomainException(
        new EntityDynamicMappingHasNoRelationsException("missing relations"));
    assertProblemDetail(body, HttpStatus.UNPROCESSABLE_CONTENT, "missing relations",
        "UNPROCESSABLE_CONTENT");
  }

  @Test
  void shouldHandleWebhookSecurityConfigurationException() {
    ProblemDetail body = exceptionHandler
        .handleDomainException(new WebhookSecurityConfigurationException("invalid security"));
    assertProblemDetail(body, HttpStatus.BAD_REQUEST, "invalid security", "BAD_REQUEST");
  }

  @Test
  void shouldHandleEntityNotFoundException() {
    ProblemDetail body = exceptionHandler
        .handleDomainException(new EntityNotFoundException("template", "entity-id"));
    assertEquals(HttpStatus.NOT_FOUND.value(), body.getStatus());
    assertEquals("NOT_FOUND", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleEntityDeletionBlockedException() {
    ProblemDetail body = exceptionHandler.handleDomainException(
        new EntityDeletionBlockedException("template", "entity-id", List.of("child-id")));
    assertEquals(HttpStatus.CONFLICT.value(), body.getStatus());
    assertEquals("CONFLICT", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleWebhookAuthenticationException() {
    ProblemDetail body = exceptionHandler
        .handleDomainException(new WebhookAuthenticationException("invalid signature"));
    assertProblemDetail(body, HttpStatus.UNAUTHORIZED, "invalid signature", "UNAUTHORIZED");
  }

  @Test
  void shouldHandleWebhookConnectorNotFoundException() {
    ProblemDetail body = exceptionHandler
        .handleDomainException(new WebhookConnectorNotFoundException("connector-id"));
    assertEquals(HttpStatus.NOT_FOUND.value(), body.getStatus());
    assertEquals("NOT_FOUND", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleEntityDynamicMappingNotFoundException() {
    ProblemDetail body = exceptionHandler
        .handleDomainException(new EntityDynamicMappingNotFoundException("mapping-id"));
    assertEquals(HttpStatus.NOT_FOUND.value(), body.getStatus());
    assertEquals("NOT_FOUND", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleEntityDynamicMappingAlreadyExistsException() {
    ProblemDetail body = exceptionHandler
        .handleDomainException(new EntityDynamicMappingAlreadyExistsException("mapping-id"));
    assertEquals(HttpStatus.CONFLICT.value(), body.getStatus());
    assertEquals("CONFLICT", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleEntityDynamicMappingAlreadyInUseException() {
    ProblemDetail body = exceptionHandler.handleDomainException(
        new EntityDynamicMappingAlreadyInUseException(List.of("mapping-id")));
    assertEquals(HttpStatus.CONFLICT.value(), body.getStatus());
    assertEquals("CONFLICT", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleWebhookConnectorAlreadyExistException() {
    ProblemDetail body = exceptionHandler
        .handleDomainException(new WebhookConnectorAlreadyExistException("connector-id"));
    assertEquals(HttpStatus.CONFLICT.value(), body.getStatus());
    assertEquals("CONFLICT", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleTemplateAlreadyMappedInWebhookConfiguration() {
    ProblemDetail body = exceptionHandler
        .handleDomainException(new EntityTemplateUsedByDynamicMappingException("template in use"));
    assertProblemDetail(body, HttpStatus.CONFLICT, "template in use", "CONFLICT");
  }

  @Test
  void shouldHandleWebhookConnectorTitleAlreadyExistsException() {
    ProblemDetail body = exceptionHandler
        .handleDomainException(new WebhookConnectorTitleAlreadyExistsException("connector-name"));
    assertEquals(HttpStatus.CONFLICT.value(), body.getStatus());
    assertEquals("CONFLICT", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleDataIntegrityViolationException() {
    Throwable rootCause = new RuntimeException("constraint violation");
    org.springframework.dao.DataIntegrityViolationException ex = mock(
        org.springframework.dao.DataIntegrityViolationException.class);
    when(ex.getMostSpecificCause()).thenReturn(rootCause);

    ProblemDetail body = exceptionHandler.handleDataIntegrityViolationException(ex);

    assertProblemDetail(body, HttpStatus.CONFLICT,
        "The request conflicts with the current state of the resource", "CONFLICT");
  }

  @Test
  void shouldHandleNoHandlerFoundExceptionViaFrameworkOverride() {
    ProblemDetail body = (ProblemDetail) exceptionHandler.handleNoHandlerFoundException(
        mock(org.springframework.web.servlet.NoHandlerFoundException.class), new HttpHeaders(),
        HttpStatus.NOT_FOUND, mockWebRequest()).getBody();
    assertProblemDetail(body, HttpStatus.NOT_FOUND,
        "Malformed request URL or missing path variable.", "NOT_FOUND");
  }

  private static void assertProblemDetail(ProblemDetail body, HttpStatus status, String detail,
      String error) {
    assertNotNull(body);
    assertEquals(status.value(), body.getStatus());
    assertEquals(status.getReasonPhrase(), body.getTitle());
    assertEquals(detail, body.getDetail());
    assertEquals(error, body.getProperties().get("error"));
    assertEquals(detail, body.getProperties().get("error_description"));
    assertNotNull(body.getProperties().get("timestamp"));
  }
}
