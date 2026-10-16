package com.decathlon.idp_core.infrastructure.adapters.api.handler;

import static org.springframework.http.HttpStatus.NOT_FOUND;

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
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

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

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  private static final String PROPERTY_ERROR = "error";
  private static final String PROPERTY_ERROR_DESCRIPTION = "error_description";
  private static final String PROPERTY_TIMESTAMP = "timestamp";

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

  /** Maps a missing entity template to an HTTP 404 problem detail. */
  @ExceptionHandler(EntityTemplateNotFoundException.class)
  public ProblemDetail handleTemplateNotFoundException(EntityTemplateNotFoundException ex) {
    log.warn("Template not found: {}", ex.getMessage());
    return buildProblemDetail(NOT_FOUND, ex.getMessage(), null);
  }

  /** Maps an invalid filter expression to an HTTP 400 problem detail. */
  @ExceptionHandler(InvalidFilterDslException.class)
  public ProblemDetail handleInvalidFilterDslException(InvalidFilterDslException ex) {
    log.warn("Invalid filter query: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
  }

  /** Maps an invalid search query to an HTTP 400 problem detail. */
  @ExceptionHandler(InvalidSearchQueryException.class)
  public ProblemDetail handleInvalidSearchQueryException(InvalidSearchQueryException ex) {
    log.warn("Invalid search query: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
  }

  /**
   * Maps a duplicate entity template identifier to an HTTP 409 problem detail.
   */
  @ExceptionHandler(EntityTemplateAlreadyExistsException.class)
  public ProblemDetail handleEntityTemplateAlreadyExistsException(
      EntityTemplateAlreadyExistsException ex) {
    log.warn("Entity entityTemplateIdentifier already exists: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), null);
  }

  /** Maps a duplicate entity template name to an HTTP 409 problem detail. */
  @ExceptionHandler(EntityTemplateNameAlreadyExistsException.class)
  public ProblemDetail handleEntityTemplateNameAlreadyExistsException(
      EntityTemplateNameAlreadyExistsException ex) {
    log.warn("Entity entityTemplateIdentifier name already exists: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), null);
  }

  /** Reports an attempt to change an immutable entity template identifier. */
  @ExceptionHandler(EntityTemplateIdentifierCannotChangeException.class)
  public ProblemDetail handleEntityTemplateIdentifierCannotChangeException(
      EntityTemplateIdentifierCannotChangeException ex) {
    log.warn("Entity entityTemplateIdentifier identifier cannot be changed: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
  }

  /**
   * Reports conflicting validation rules in an entity template property
   * definition.
   */
  @ExceptionHandler(PropertyDefinitionRulesConflictException.class)
  public ProblemDetail handleWrongPropertyRulesException(
      PropertyDefinitionRulesConflictException ex) {
    log.warn("Wrong Entity entityTemplateIdentifier property rules: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
  }

  /** Reports a duplicate property name in an entity template. */
  @ExceptionHandler(PropertyNameAlreadyExistsException.class)
  public ProblemDetail handlePropertyNameAlreadyExistsException(
      PropertyNameAlreadyExistsException ex) {
    log.warn("Duplicate property name: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
  }

  /** Reports a duplicate relation name in an entity template. */
  @ExceptionHandler(RelationNameAlreadyExistsException.class)
  public ProblemDetail handleRelationNameAlreadyExistsException(
      RelationNameAlreadyExistsException ex) {
    log.warn("Duplicate relation name: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
  }

  /** Reports a relation whose target entity template does not exist. */
  @ExceptionHandler(TargetTemplateNotFoundException.class)
  public ProblemDetail handleTargetTemplateNotFoundException(TargetTemplateNotFoundException ex) {
    log.warn("Target entityTemplateIdentifier not found: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
  }

  /**
   * Reports that an entity template cannot be deleted while targeted by a
   * relation.
   */
  @ExceptionHandler(EntityTemplateIsRelationTargetException.class)
  public ProblemDetail handleEntityTemplateIsRelationTargetException(
      EntityTemplateIsRelationTargetException ex) {
    log.warn("Template deletion blocked – still a relation target: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
  }

  /** Reports an unsupported change to an entity template property type. */
  @ExceptionHandler(PropertyTypeChangeException.class)
  public ProblemDetail handleTypeChangeException(PropertyTypeChangeException ex) {
    log.warn("Type change error: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
  }

  /** Reports an unsupported change to the target of an existing relation. */
  @ExceptionHandler(RelationTargetTemplateChangeException.class)
  public ProblemDetail handleRelationTargetTemplateChangeException(
      RelationTargetTemplateChangeException ex) {
    log.warn("Relation target entityTemplateIdentifier change error: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
  }

  /** Reports a relation that illegally targets its own entity template. */
  @ExceptionHandler(RelationCannotTargetItselfException.class)
  public ProblemDetail handleRelationCannotTargetItselfException(
      RelationCannotTargetItselfException ex) {
    log.warn("Relation self-reference error: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
  }

  /**
   * Converts method-parameter validation failures into a structured error
   * response.
   */
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

  /** Maps a duplicate entity to an HTTP 409 problem detail. */
  @ExceptionHandler(EntityAlreadyExistsException.class)
  public ProblemDetail handleEntityAlreadyExistsException(EntityAlreadyExistsException ex) {
    log.warn("Entity already exists: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), null);
  }

  /** Maps semantic entity validation failures to an HTTP 400 problem detail. */
  @ExceptionHandler(EntityValidationException.class)
  public ProblemDetail handleEntityValidationException(EntityValidationException ex) {
    log.warn("Entity validation failed: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
  }

  /** Reports an invalid dynamic-mapping configuration. */
  @ExceptionHandler(EntityDynamicMappingConfigurationException.class)
  public ProblemDetail handleEntityDynamicMappingConfigurationException(
      EntityDynamicMappingConfigurationException ex) {
    log.warn("Invalid entity dynamic mapping configuration: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
  }

  /** Reports a dynamic-mapping expression that could not be evaluated. */
  @ExceptionHandler(ExpressionEvaluationFailedException.class)
  public ProblemDetail handleExpressionEvaluationFailedException(
      ExpressionEvaluationFailedException ex) {
    log.warn("Expression evaluation failed for '{}': {}", ex.getExpression(), ex.getReason());
    return buildProblemDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), null);
  }

  /** Reports a JSLT transformation error in a dynamic mapping. */
  @ExceptionHandler(EntityDynamicMappingJsltErrorException.class)
  public ProblemDetail handleEntityDynamicMappingJsltErrorException(
      EntityDynamicMappingJsltErrorException ex) {
    log.warn("JSLT error in entity dynamic mapping: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), null);
  }

  /** Reports a webhook mapping that references an unknown template property. */
  @ExceptionHandler(PropertyNameNotFoundEntityTemplatePropertiesException.class)
  public ProblemDetail handlePropertyNameNotFoundEntityTemplatePropertiesException(
      PropertyNameNotFoundEntityTemplatePropertiesException ex) {
    log.warn("Webhook mapping references unknown property: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), null);
  }

  /** Reports a webhook mapping that references an unknown template relation. */
  @ExceptionHandler(RelationNameNotFoundEntityTemplateRelationsException.class)
  public ProblemDetail handleRelationNameNotFoundEntityTemplateRelationsException(
      RelationNameNotFoundEntityTemplateRelationsException ex) {
    log.warn("Webhook mapping references unknown relation: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), null);
  }

  /** Reports a dynamic mapping that does not define any property mapping. */
  @ExceptionHandler(EntityDynamicMappingHasNoPropertiesException.class)
  public ProblemDetail handleEntityDynamicMappingHasNoPropertiesException(
      EntityDynamicMappingHasNoPropertiesException ex) {
    log.warn("Dynamic mapping is missing required properties: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), null);
  }

  /** Reports a dynamic mapping that does not define any relation mapping. */
  @ExceptionHandler(EntityDynamicMappingHasNoRelationsException.class)
  public ProblemDetail handleEntityDynamicMappingHasNoRelationsException(
      EntityDynamicMappingHasNoRelationsException ex) {
    log.warn("Dynamic mapping is missing required relations: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), null);
  }

  /** Reports invalid security settings for a webhook connector. */
  @ExceptionHandler(WebhookSecurityConfigurationException.class)
  public ProblemDetail handleWebhookSecurityConfigurationException(
      WebhookSecurityConfigurationException ex) {
    log.warn("Invalid webhook security configuration: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
  }

  /** Maps a missing catalog entity to an HTTP 404 problem detail. */
  @ExceptionHandler(EntityNotFoundException.class)
  public ProblemDetail handleEntityNotFoundException(EntityNotFoundException ex) {
    return buildProblemDetail(NOT_FOUND, ex.getMessage(), null);
  }

  /// Handles principal not found exception when JIT provisioning fails.
  ///
  /// **HTTP mapping:** Maps PrincipalNotFoundException to HTTP 404 status
  /// indicating the authenticated principal does not have a catalog entry.
  @ExceptionHandler(PrincipalNotFoundException.class)
  public ProblemDetail handlePrincipalNotFoundException(PrincipalNotFoundException ex) {
    log.warn("Principal not found: {}", ex.getMessage());
    return buildProblemDetail(NOT_FOUND, ex.getMessage(), null);
  }

  /// Handles Bean Validation constraint violations from domain model validation.
  ///
  /// **Error aggregation:** Combines multiple constraint violation messages into
  /// single user-friendly response with HTTP 400 status for client correction.
  @ExceptionHandler(ConstraintViolationException.class)
  public ProblemDetail handleConstraintViolationException(ConstraintViolationException ex) {
    log.warn("Validation constraint violation: {}", ex.getMessage());
    String errorMessage = ex.getConstraintViolations().stream().map(ConstraintViolation::getMessage)
        .collect(Collectors.joining(", "));
    return buildProblemDetail(HttpStatus.BAD_REQUEST, errorMessage, null);
  }

  /** Maps a blocked entity deletion to an HTTP 409 problem detail. */
  @ExceptionHandler(EntityDeletionBlockedException.class)
  public ProblemDetail handleEntityDeletionBlockedException(EntityDeletionBlockedException ex) {
    log.warn("Entity deletion blocked: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), null);
  }

  /** Maps failed webhook authentication to an HTTP 401 problem detail. */
  @ExceptionHandler(WebhookAuthenticationException.class)
  public ProblemDetail handleWebhookAuthenticationException(WebhookAuthenticationException ex) {
    log.warn("Webhook authentication failed: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
  }

  /** Maps a missing webhook connector to an HTTP 404 problem detail. */
  @ExceptionHandler(WebhookConnectorNotFoundException.class)
  public ProblemDetail handleWebhookConnectorNotFoundException(
      WebhookConnectorNotFoundException ex) {
    log.warn("Webhook connector not found: {}", ex.getMessage());
    return buildProblemDetail(NOT_FOUND, ex.getMessage(), null);
  }

  /** Maps a missing entity dynamic mapping to an HTTP 404 problem detail. */
  @ExceptionHandler(EntityDynamicMappingNotFoundException.class)
  public ProblemDetail handleEntityDynamicMappingNotFoundException(
      EntityDynamicMappingNotFoundException ex) {
    log.warn("Referenced entity dynamic mapping not found: {}", ex.getMessage());
    return buildProblemDetail(NOT_FOUND, ex.getMessage(), null);
  }

  /** Maps a duplicate entity dynamic mapping to an HTTP 409 problem detail. */
  @ExceptionHandler(EntityDynamicMappingAlreadyExistsException.class)
  public ProblemDetail handleEntityDynamicMappingAlreadyExistsException(
      EntityDynamicMappingAlreadyExistsException ex) {
    log.warn("Entity dynamic mapping identifier conflict: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), null);
  }

  /** Reports an entity dynamic mapping that cannot be changed while in use. */
  @ExceptionHandler(EntityDynamicMappingAlreadyInUseException.class)
  public ProblemDetail handleEntityDynamicMappingAlreadyInUseException(
      EntityDynamicMappingAlreadyInUseException ex) {
    log.warn("Entity dynamic mapping already in use: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), null);
  }

  /**
   * Maps a duplicate webhook connector identifier to an HTTP 409 problem detail.
   */
  @ExceptionHandler(WebhookConnectorAlreadyExistException.class)
  public ProblemDetail handleWebhookConnectorAlreadyExistException(
      WebhookConnectorAlreadyExistException ex) {
    log.warn("Webhook connector identifier conflict: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), null);
  }

  /**
   * Maps an entity template used by a dynamic mapping to an HTTP 409 problem
   * detail.
   */
  @ExceptionHandler(EntityTemplateUsedByDynamicMappingException.class)
  public ProblemDetail handleTemplateAlreadyMappedInWebhookConfiguration(
      EntityTemplateUsedByDynamicMappingException ex) {
    log.warn("Entity entityTemplateIdentifier in use by webhook mapping conflict: {}",
        ex.getMessage());
    return buildProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), null);
  }

  /** Maps a duplicate webhook connector title to an HTTP 409 problem detail. */
  @ExceptionHandler(WebhookConnectorTitleAlreadyExistsException.class)
  public ProblemDetail handleWebhookConnectorTitleAlreadyExistsException(
      WebhookConnectorTitleAlreadyExistsException ex) {
    log.warn("Webhook connector name conflict: {}", ex.getMessage());
    return buildProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), null);
  }

  /** Converts database integrity conflicts into a safe HTTP 409 response. */
  @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
    log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());

    return buildProblemDetail(HttpStatus.CONFLICT,
        "The request conflicts with the current state of the resource", null);
  }

  /**
   * Converts unexpected failures into a safe response without exposing internal
   * details.
   */
  @ExceptionHandler(Exception.class)
  public ProblemDetail handleGenericException(Exception ex) {
    log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

    return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred. Please try again later.", null);
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
