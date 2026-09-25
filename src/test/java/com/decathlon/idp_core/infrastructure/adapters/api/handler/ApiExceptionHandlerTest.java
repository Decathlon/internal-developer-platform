package com.decathlon.idp_core.infrastructure.adapters.api.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.util.List;
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
import com.decathlon.idp_core.domain.exception.search.InvalidSearchQueryException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorAlreadyExistException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorNotFoundException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorTitleAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;
import com.decathlon.idp_core.infrastructure.adapters.common.model.ErrorResponse;

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
        .handleTemplateNotFoundException(new EntityTemplateNotFoundException("missing"));
    assertProblemDetail(body, HttpStatus.NOT_FOUND, "missing", "NOT_FOUND");
  }

  @Test
  void shouldHandleEntityTemplateAlreadyExistsException() {
    ProblemDetail body = exceptionHandler.handleEntityTemplateAlreadyExistsException(
        new EntityTemplateAlreadyExistsException("duplicate-id"));
    assertProblemDetail(body, HttpStatus.CONFLICT,
        "An Entity Template already exists with the same identifier:duplicate-id", "CONFLICT");
  }

  @Test
  void shouldHandleEntityAlreadyExistsException() {
    ProblemDetail body = exceptionHandler
        .handleEntityAlreadyExistsException(new EntityAlreadyExistsException("a", "b"));
    assertProblemDetail(body, HttpStatus.CONFLICT,
        "Entity with name 'b' already exists for template 'a'", "CONFLICT");
  }

  @Test
  void shouldHandleEntityValidationException() {
    ProblemDetail body = exceptionHandler.handleEntityValidationException(
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
  void shouldConvertUnreadableMessageToSafeError() {
    HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
        "Cannot deserialize value of type `PropertyType` from String \"INVALID_TYPE\": "
            + "not one of the values accepted for Enum class",
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
    ProblemDetail body = (ProblemDetail) exceptionHandler.handleHttpMessageNotReadable(ex,
        new HttpHeaders(), HttpStatus.BAD_REQUEST, mockWebRequest()).getBody();

    assertProblemDetail(body, HttpStatus.BAD_REQUEST,
        "Invalid value 'INVALID_TYPE' for property 'type'", "BAD_REQUEST");
  }

  @Test
  void shouldHandleGenericExceptionWithoutExposingInternalDetails() {
    ResponseEntity<ErrorResponse> response = exceptionHandler
        .handleGenericException(new RuntimeException("Sensitive internal details"));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getError());
    assertEquals("An unexpected error occurred. Please try again later.",
        response.getBody().getErrorDescription());
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
        .handleInvalidFilterDslException(new InvalidFilterDslException("bad filter"));
    assertProblemDetail(body, HttpStatus.BAD_REQUEST, "bad filter", "BAD_REQUEST");
  }

  @Test
  void shouldHandleInvalidSearchQueryException() {
    ProblemDetail body = exceptionHandler
        .handleInvalidSearchQueryException(new InvalidSearchQueryException("bad query"));
    assertProblemDetail(body, HttpStatus.BAD_REQUEST, "bad query", "BAD_REQUEST");
  }

  @Test
  void shouldHandleEntityTemplateNameAlreadyExistsException() {
    ProblemDetail body = exceptionHandler.handleEntityTemplateNameAlreadyExistsException(
        new EntityTemplateNameAlreadyExistsException("duplicate-name"));
    assertEquals(HttpStatus.CONFLICT.value(), body.getStatus());
    assertEquals("CONFLICT", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleEntityTemplateIdentifierCannotChangeException() {
    ProblemDetail body = exceptionHandler.handleEntityTemplateIdentifierCannotChangeException(
        new EntityTemplateIdentifierCannotChangeException("template-id"));
    assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
    assertEquals("BAD_REQUEST", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleWrongPropertyRulesException() {
    ProblemDetail body = exceptionHandler.handleWrongPropertyRulesException(
        new PropertyDefinitionRulesConflictException("property-test", "STRING", "not allowed"));
    assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
    assertEquals("BAD_REQUEST", body.getProperties().get("error"));
  }

  @Test
  void shouldHandlePropertyNameAlreadyExistsException() {
    ProblemDetail body = exceptionHandler.handlePropertyNameAlreadyExistsException(
        new PropertyNameAlreadyExistsException("property-test"));
    assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
    assertEquals("BAD_REQUEST", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleRelationNameAlreadyExistsException() {
    ProblemDetail body = exceptionHandler.handleRelationNameAlreadyExistsException(
        new RelationNameAlreadyExistsException("belongsto"));
    assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
    assertEquals("BAD_REQUEST", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleTargetTemplateNotFoundException() {
    ProblemDetail body = exceptionHandler
        .handleTargetTemplateNotFoundException(new TargetTemplateNotFoundException("non-existent"));
    assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
    assertEquals("BAD_REQUEST", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleEntityTemplateIsRelationTargetException() {
    ProblemDetail body = exceptionHandler.handleEntityTemplateIsRelationTargetException(
        new EntityTemplateIsRelationTargetException("microservice"));
    assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
    assertEquals("BAD_REQUEST", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleTypeChangeException() {
    ProblemDetail body = exceptionHandler
        .handleTypeChangeException(new PropertyTypeChangeException("name", "STRING", "NUMBER"));
    assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
    assertEquals("BAD_REQUEST", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleRelationTargetTemplateChangeException() {
    ProblemDetail body = exceptionHandler.handleRelationTargetTemplateChangeException(
        new RelationTargetTemplateChangeException("dependencies", "service", "service-modified"));
    assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
    assertEquals("BAD_REQUEST", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleRelationCannotTargetItselfException() {
    ProblemDetail body = exceptionHandler.handleRelationCannotTargetItselfException(
        new RelationCannotTargetItselfException("circular", "self-ref-template"));
    assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
    assertEquals("BAD_REQUEST", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleEntityDynamicMappingConfigurationException() {
    ProblemDetail body = exceptionHandler.handleEntityDynamicMappingConfigurationException(
        new EntityDynamicMappingConfigurationException("invalid configuration"));
    assertProblemDetail(body, HttpStatus.BAD_REQUEST, "invalid configuration", "BAD_REQUEST");
  }

  @Test
  void shouldHandleExpressionEvaluationFailedException() {
    ProblemDetail body = exceptionHandler.handleExpressionEvaluationFailedException(
        new ExpressionEvaluationFailedException(".foo", "syntax error", null));
    assertEquals(HttpStatus.UNPROCESSABLE_CONTENT.value(), body.getStatus());
    assertEquals("UNPROCESSABLE_CONTENT", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleEntityDynamicMappingJsltErrorException() {
    ProblemDetail body = exceptionHandler.handleEntityDynamicMappingJsltErrorException(
        new EntityDynamicMappingJsltErrorException("jslt failed"));
    assertProblemDetail(body, HttpStatus.UNPROCESSABLE_CONTENT, "jslt failed",
        "UNPROCESSABLE_CONTENT");
  }

  @Test
  void shouldHandlePropertyNameNotFoundEntityTemplatePropertiesException() {
    ProblemDetail body = exceptionHandler
        .handlePropertyNameNotFoundEntityTemplatePropertiesException(
            new PropertyNameNotFoundEntityTemplatePropertiesException("unknown property"));
    assertProblemDetail(body, HttpStatus.UNPROCESSABLE_CONTENT, "unknown property",
        "UNPROCESSABLE_CONTENT");
  }

  @Test
  void shouldHandleRelationNameNotFoundEntityTemplateRelationsException() {
    ProblemDetail body = exceptionHandler
        .handleRelationNameNotFoundEntityTemplateRelationsException(
            new RelationNameNotFoundEntityTemplateRelationsException("unknown relation"));
    assertProblemDetail(body, HttpStatus.UNPROCESSABLE_CONTENT, "unknown relation",
        "UNPROCESSABLE_CONTENT");
  }

  @Test
  void shouldHandleEntityDynamicMappingHasNoPropertiesException() {
    ProblemDetail body = exceptionHandler.handleEntityDynamicMappingHasNoPropertiesException(
        new EntityDynamicMappingHasNoPropertiesException("missing properties"));
    assertProblemDetail(body, HttpStatus.UNPROCESSABLE_CONTENT, "missing properties",
        "UNPROCESSABLE_CONTENT");
  }

  @Test
  void shouldHandleEntityDynamicMappingHasNoRelationsException() {
    ProblemDetail body = exceptionHandler.handleEntityDynamicMappingHasNoRelationsException(
        new EntityDynamicMappingHasNoRelationsException("missing relations"));
    assertProblemDetail(body, HttpStatus.UNPROCESSABLE_CONTENT, "missing relations",
        "UNPROCESSABLE_CONTENT");
  }

  @Test
  void shouldHandleWebhookSecurityConfigurationException() {
    ProblemDetail body = exceptionHandler.handleWebhookSecurityConfigurationException(
        new WebhookSecurityConfigurationException("invalid security"));
    assertProblemDetail(body, HttpStatus.BAD_REQUEST, "invalid security", "BAD_REQUEST");
  }

  @Test
  void shouldHandleEntityNotFoundException() {
    ProblemDetail body = exceptionHandler
        .handleEntityNotFoundException(new EntityNotFoundException("template", "entity-id"));
    assertEquals(HttpStatus.NOT_FOUND.value(), body.getStatus());
    assertEquals("NOT_FOUND", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleEntityDeletionBlockedException() {
    ProblemDetail body = exceptionHandler.handleEntityDeletionBlockedException(
        new EntityDeletionBlockedException("template", "entity-id", List.of("child-id")));
    assertEquals(HttpStatus.CONFLICT.value(), body.getStatus());
    assertEquals("CONFLICT", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleWebhookAuthenticationException() {
    ProblemDetail body = exceptionHandler.handleWebhookAuthenticationException(
        new WebhookAuthenticationException("invalid signature"));
    assertProblemDetail(body, HttpStatus.UNAUTHORIZED, "invalid signature", "UNAUTHORIZED");
  }

  @Test
  void shouldHandleWebhookConnectorNotFoundException() {
    ProblemDetail body = exceptionHandler.handleWebhookConnectorNotFoundException(
        new WebhookConnectorNotFoundException("connector-id"));
    assertEquals(HttpStatus.NOT_FOUND.value(), body.getStatus());
    assertEquals("NOT_FOUND", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleEntityDynamicMappingNotFoundException() {
    ProblemDetail body = exceptionHandler.handleEntityDynamicMappingNotFoundException(
        new EntityDynamicMappingNotFoundException("mapping-id"));
    assertEquals(HttpStatus.NOT_FOUND.value(), body.getStatus());
    assertEquals("NOT_FOUND", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleEntityDynamicMappingAlreadyExistsException() {
    ProblemDetail body = exceptionHandler.handleEntityDynamicMappingAlreadyExistsException(
        new EntityDynamicMappingAlreadyExistsException("mapping-id"));
    assertEquals(HttpStatus.CONFLICT.value(), body.getStatus());
    assertEquals("CONFLICT", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleEntityDynamicMappingAlreadyInUseException() {
    ProblemDetail body = exceptionHandler.handleEntityDynamicMappingAlreadyInUseException(
        new EntityDynamicMappingAlreadyInUseException(List.of("mapping-id")));
    assertEquals(HttpStatus.CONFLICT.value(), body.getStatus());
    assertEquals("CONFLICT", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleWebhookConnectorAlreadyExistException() {
    ProblemDetail body = exceptionHandler.handleWebhookConnectorAlreadyExistException(
        new WebhookConnectorAlreadyExistException("connector-id"));
    assertEquals(HttpStatus.CONFLICT.value(), body.getStatus());
    assertEquals("CONFLICT", body.getProperties().get("error"));
  }

  @Test
  void shouldHandleTemplateAlreadyMappedInWebhookConfiguration() {
    ProblemDetail body = exceptionHandler.handleTemplateAlreadyMappedInWebhookConfiguration(
        new EntityTemplateUsedByDynamicMappingException("template in use"));
    assertProblemDetail(body, HttpStatus.CONFLICT, "template in use", "CONFLICT");
  }

  @Test
  void shouldHandleWebhookConnectorTitleAlreadyExistsException() {
    ProblemDetail body = exceptionHandler.handleWebhookConnectorTitleAlreadyExistsException(
        new WebhookConnectorTitleAlreadyExistsException("connector-name"));
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

  @Test
  void shouldReturnEmptyMessageWhenHttpMessageNotReadableCauseIsNull() {
    HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Invalid JSON format",
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

    ProblemDetail body = (ProblemDetail) exceptionHandler.handleHttpMessageNotReadable(ex,
        new HttpHeaders(), HttpStatus.BAD_REQUEST, mockWebRequest()).getBody();

    assertProblemDetail(body, HttpStatus.BAD_REQUEST, "Invalid request body format", "BAD_REQUEST");
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
