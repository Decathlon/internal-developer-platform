package com.decathlon.idp_core.infrastructure.adapters.api.handler;

import static java.util.Map.entry;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.decathlon.idp_core.domain.constant.ValidationMessages;
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
import com.decathlon.idp_core.domain.exception.mock.MockSecurityConfigurationException;
import com.decathlon.idp_core.domain.exception.principal.PrincipalCreationException;
import com.decathlon.idp_core.domain.exception.principal.PrincipalNotFoundException;
import com.decathlon.idp_core.domain.exception.search.InvalidSearchQueryException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConfigurationMissingException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorAlreadyExistException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorConfigurationException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorNotFoundException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorTitleAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookDisabledException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;

import lombok.extern.slf4j.Slf4j;

/**
 * Maps Spring MVC API exceptions to ProblemDetail responses. Camel webhook
 * ingestion uses its own handler to return the same response contract.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  private static final String PROPERTY_ERROR = "error";
  private static final String PROPERTY_ERROR_DESCRIPTION = "error_description";
  private static final String PROPERTY_TIMESTAMP = "timestamp";
  private static final String GENERIC_ERROR_DETAIL = "An unexpected error occurred. Please try again later.";

  /// Maps domain exceptions to HTTP statuses without placing HTTP semantics in
  /// the domain layer.
  private static final Map<Class<? extends RuntimeException>, HttpStatus> STATUS_BY_EXCEPTION = Map
      .ofEntries(entry(InvalidFilterDslException.class, BAD_REQUEST),
          entry(InvalidSearchQueryException.class, BAD_REQUEST),
          entry(EntityTemplateIdentifierCannotChangeException.class, BAD_REQUEST),
          entry(PropertyDefinitionRulesConflictException.class, BAD_REQUEST),
          entry(
              com.decathlon.idp_core.domain.exception.property.PropertyDefinitionRulesConflictException.class,
              BAD_REQUEST),
          entry(PropertyNameAlreadyExistsException.class, BAD_REQUEST),
          entry(RelationNameAlreadyExistsException.class, BAD_REQUEST),
          entry(TargetTemplateNotFoundException.class, BAD_REQUEST),
          entry(EntityTemplateIsRelationTargetException.class, BAD_REQUEST),
          entry(PropertyTypeChangeException.class, BAD_REQUEST),
          entry(RelationTargetTemplateChangeException.class, BAD_REQUEST),
          entry(RelationCannotTargetItselfException.class, BAD_REQUEST),
          entry(EntityValidationException.class, BAD_REQUEST),
          entry(EntityDynamicMappingConfigurationException.class, BAD_REQUEST),
          entry(PropertyNameNotFoundEntityTemplatePropertiesException.class, UNPROCESSABLE_CONTENT),
          entry(WebhookSecurityConfigurationException.class, BAD_REQUEST),
          entry(WebhookConnectorConfigurationException.class, BAD_REQUEST),
          entry(MockSecurityConfigurationException.class, BAD_REQUEST),
          entry(WebhookAuthenticationException.class, UNAUTHORIZED),
          entry(EntityTemplateNotFoundException.class, NOT_FOUND),
          entry(EntityNotFoundException.class, NOT_FOUND),
          entry(PrincipalNotFoundException.class, NOT_FOUND),
          entry(WebhookConnectorNotFoundException.class, NOT_FOUND),
          entry(EntityDynamicMappingNotFoundException.class, NOT_FOUND),
          entry(EntityTemplateAlreadyExistsException.class, CONFLICT),
          entry(EntityTemplateNameAlreadyExistsException.class, CONFLICT),
          entry(EntityAlreadyExistsException.class, CONFLICT),
          entry(EntityDeletionBlockedException.class, CONFLICT),
          entry(EntityDynamicMappingAlreadyExistsException.class, CONFLICT),
          entry(EntityDynamicMappingAlreadyInUseException.class, CONFLICT),
          entry(WebhookConnectorAlreadyExistException.class, CONFLICT),
          entry(EntityTemplateUsedByDynamicMappingException.class, CONFLICT),
          entry(WebhookConnectorTitleAlreadyExistsException.class, CONFLICT),
          entry(ExpressionEvaluationFailedException.class, UNPROCESSABLE_CONTENT),
          entry(EntityDynamicMappingJsltErrorException.class, UNPROCESSABLE_CONTENT),
          entry(RelationNameNotFoundEntityTemplateRelationsException.class, UNPROCESSABLE_CONTENT),
          entry(EntityDynamicMappingHasNoPropertiesException.class, UNPROCESSABLE_CONTENT),
          entry(EntityDynamicMappingHasNoRelationsException.class, UNPROCESSABLE_CONTENT),
          entry(PrincipalCreationException.class, INTERNAL_SERVER_ERROR),
          entry(WebhookConfigurationMissingException.class, INTERNAL_SERVER_ERROR),
          entry(WebhookDisabledException.class, FORBIDDEN));

  /**
   * Prevents direct instantiation because Spring manages this exception handler.
   */
  private ApiExceptionHandler() {
  }

  /**
   * Converts request-body validation errors into a structured bad-request
   * response.
   */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
      HttpHeaders headers, org.springframework.http.HttpStatusCode statusCode, WebRequest request) {
    log.warn("Method argument validation error: {}", ex.getMessage());
    String errorMessage = ex.getBindingResult().getFieldErrors().stream()
        .map(org.springframework.context.MessageSourceResolvable::getDefaultMessage)
        .collect(Collectors.joining(", "));
    return ResponseEntity.status(statusCode).headers(headers)
        .body(buildProblemDetail(HttpStatus.valueOf(statusCode.value()), errorMessage, request));
  }

  /**
   * Reports a required path variable that could not be resolved from the request.
   */
  @Override
  protected ResponseEntity<Object> handleMissingPathVariable(MissingPathVariableException ex,
      HttpHeaders headers, org.springframework.http.HttpStatusCode statusCode, WebRequest request) {
    log.warn("Missing path variable: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers)
        .body(buildProblemDetail(HttpStatus.BAD_REQUEST,
            "Missing required path variable: " + ex.getVariableName(), request));
  }

  /** Reports requests for which no matching HTTP handler exists. */
  @Override
  protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex,
      HttpHeaders headers, org.springframework.http.HttpStatusCode statusCode, WebRequest request) {
    log.warn("No handler found or missing path variable: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(headers).body(buildProblemDetail(
        HttpStatus.NOT_FOUND, "Malformed request URL or missing path variable.", request));
  }

  /** Maps registered domain exceptions to ProblemDetail responses. */
  @ExceptionHandler(RuntimeException.class)
  public ProblemDetail handleDomainException(RuntimeException ex) {
    HttpStatus status = STATUS_BY_EXCEPTION.get(ex.getClass());
    if (status == null || status.is5xxServerError()) {
      return handleUnexpectedException(ex);
    }
    log.warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
    return buildProblemDetail(status, ex.getMessage(), null);
  }

  /** Converts method-parameter validation failures into a structured response. */
  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode statusCode,
      WebRequest request) {
    log.warn("Handler method validation error: {}", ex.getMessage());
    String errorMessage = ex.getAllErrors().stream()
        .map(org.springframework.context.MessageSourceResolvable::getDefaultMessage)
        .collect(Collectors.joining(", "));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers)
        .body(buildProblemDetail(HttpStatus.BAD_REQUEST, errorMessage, request));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ProblemDetail handleConstraintViolationException(ConstraintViolationException ex) {
    log.warn("Validation constraint violation: {}", ex.getMessage());
    String errorMessage = ex.getConstraintViolations().stream().map(ConstraintViolation::getMessage)
        .collect(Collectors.joining(", "));
    return buildProblemDetail(HttpStatus.BAD_REQUEST, errorMessage, null);
  }

  /** Converts database integrity conflicts into a safe HTTP 409 response. */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
    log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
    return buildProblemDetail(HttpStatus.CONFLICT,
        "The request conflicts with the current state of the resource", null);
  }

  /** Returns a safe response for checked or otherwise unexpected exceptions. */
  @ExceptionHandler(Exception.class)
  public ProblemDetail handleGenericException(Exception ex) {
    return handleUnexpectedException(ex);
  }

  private ProblemDetail handleUnexpectedException(Exception ex) {
    log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
    return buildProblemDetail(INTERNAL_SERVER_ERROR, GENERIC_ERROR_DETAIL, null);
  }

  /**
   * Adds the API error contract fields to framework-generated problem details.
   */
  @Override
  protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
      HttpStatusCode statusCode, WebRequest request) {
    if (body instanceof ProblemDetail problemDetail) {
      enrichProblemDetail(problemDetail, statusCode, request);
    }
    return super.createResponseEntity(body, headers, statusCode, request);
  }

  private void enrichProblemDetail(ProblemDetail problemDetail, HttpStatusCode statusCode,
      WebRequest request) {
    Map<String, Object> properties = problemDetail.getProperties();
    if (properties == null || !properties.containsKey(PROPERTY_ERROR)) {
      HttpStatus status = HttpStatus.resolve(statusCode.value());
      problemDetail.setProperty(PROPERTY_ERROR,
          status != null ? status.name() : Integer.toString(statusCode.value()));
    }
    if (properties == null || !properties.containsKey(PROPERTY_ERROR_DESCRIPTION)) {
      problemDetail.setProperty(PROPERTY_ERROR_DESCRIPTION, problemDetail.getDetail());
    }
    if (properties == null || !properties.containsKey(PROPERTY_TIMESTAMP)) {
      problemDetail.setProperty(PROPERTY_TIMESTAMP, Instant.now());
    }
    if (problemDetail.getInstance() == null
        && request instanceof ServletWebRequest servletRequest) {
      problemDetail.setInstance(java.net.URI.create(servletRequest.getRequest().getRequestURI()));
    }
  }

  /**
   * Builds the common RFC 9457 problem-detail representation returned by this
   * handler.
   */
  private ProblemDetail buildProblemDetail(HttpStatus status, String detail, WebRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatus(status);
    problemDetail.setTitle(status.getReasonPhrase());
    problemDetail.setDetail(detail);
    String path = resolveRequestPath(request);
    if (path != null) {
      problemDetail.setInstance(java.net.URI.create(path));
    }
    problemDetail.setProperty(PROPERTY_ERROR, status.name());
    problemDetail.setProperty(PROPERTY_ERROR_DESCRIPTION, detail);
    problemDetail.setProperty(PROPERTY_TIMESTAMP, Instant.now());
    return problemDetail;
  }

  /**
   * Resolves the request path from the given request or, for handlers that do not
   * receive one, from the current servlet request context.
   */
  private static String resolveRequestPath(WebRequest request) {
    WebRequest effective = request;
    if (effective == null
        && RequestContextHolder.getRequestAttributes()instanceof ServletRequestAttributes attrs) {
      effective = new ServletWebRequest(attrs.getRequest());
    }
    return effective == null ? null : effective.getDescription(false).replace("uri=", "");
  }
}
