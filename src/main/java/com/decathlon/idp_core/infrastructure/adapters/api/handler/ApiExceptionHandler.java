package com.decathlon.idp_core.infrastructure.adapters.api.handler;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.decathlon.idp_core.domain.exception.entity.EntityAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity.EntityDeletionBlockedException;
import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.*;
import com.decathlon.idp_core.domain.exception.entity_template.*;
import com.decathlon.idp_core.domain.exception.filter.InvalidFilterDslException;
import com.decathlon.idp_core.domain.exception.search.InvalidSearchQueryException;
import com.decathlon.idp_core.domain.exception.webhook.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/// Global exception handler providing centralized error handling for all API
/// endpoints.
///
/// **Infrastructure error handling strategy:** Intercepts domain and validation
/// exceptions and converts them to appropriate HTTP responses with consistent
/// error formatting. Ensures API consumers receive standardized error messages
/// regardless of internal failures.
///
/// **Exception mapping approach:**
/// - Domain exceptions → HTTP 404/409 with business-meaningful messages
/// - Validation exceptions → HTTP 400 with field-specific error details
/// - JSON parsing errors → HTTP 400 with user-friendly parsing messages
/// - Generic exceptions → HTTP 500 with safe internal error responses
///
/// **Error response standardization:** All errors follow consistent
/// [ErrorResponse] format with appropriate HTTP status codes and logged for
/// monitoring/debugging purposes.
@Slf4j
@ControllerAdvice
public class ApiExceptionHandler {

  private ApiExceptionHandler() {
  }

  /// Handles domain exception when entity templates are not found.
  ///
  /// **HTTP mapping:** Maps domain EntityTemplateNotFoundException to HTTP 404
  /// status with business-meaningful error message for API consumers.
  @ExceptionHandler(EntityTemplateNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleTemplateNotFoundException(
      EntityTemplateNotFoundException ex) {
    log.warn("Template not found: {}", ex.getMessage());
    ErrorResponse errorResponse = new ErrorResponse(NOT_FOUND.name(), ex.getMessage());
    return ResponseEntity.status(NOT_FOUND).body(errorResponse);
  }

  /// Handles domain exception for malformed filter query strings (`q=` DSL).
  ///
  /// **HTTP mapping:** Maps domain [InvalidFilterDslException] to HTTP 400 Bad
  /// Request
  /// so API consumers receive clear feedback about invalid `q` parameter syntax.
  @ExceptionHandler(InvalidFilterDslException.class)
  public ResponseEntity<ErrorResponse> handleInvalidFilterDslException(
      InvalidFilterDslException ex) {
    log.warn("Invalid filter query: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /// Handles domain exception for malformed search filter trees or free-text
  /// query strings.
  ///
  /// **HTTP mapping:** Maps domain [InvalidSearchQueryException] to HTTP 400 Bad
  /// Request
  /// so API consumers receive clear feedback about invalid search request syntax.
  @ExceptionHandler(InvalidSearchQueryException.class)
  public ResponseEntity<ErrorResponse> handleInvalidSearchQueryException(
      InvalidSearchQueryException ex) {
    log.warn("Invalid search query: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /// Handles domain exception when entity templates already exist.
  ///
  /// **HTTP mapping:** Maps domain EntityTemplateAlreadyExistsException to HTTP
  /// 409 status indicating business rule conflict for duplicate identifiers.
  @ExceptionHandler(EntityTemplateAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleEntityTemplateAlreadyExistsException(
      EntityTemplateAlreadyExistsException ex) {
    log.warn("Entity entityTemplateIdentifier already exists: {}", ex.getMessage());
    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.CONFLICT.name(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }

  /// Handles domain exception when entity entityTemplateIdentifier names already
  /// exist.
  ///
  /// **HTTP mapping:** Maps domain EntityTemplateNameAlreadyExistsException to
  /// HTTP 409 status indicating business rule conflict for duplicate
  /// entityTemplateIdentifier names.
  @ExceptionHandler(EntityTemplateNameAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleEntityTemplateNameAlreadyExistsException(
      EntityTemplateNameAlreadyExistsException ex) {
    log.warn("Entity entityTemplateIdentifier name already exists: {}", ex.getMessage());
    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.CONFLICT.name(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }

  /// Handles domain exception when attempting to change an entity
  /// entityTemplateIdentifier
  /// identifier.
  ///
  /// **HTTP mapping:** Maps domain EntityTemplateIdentifierCannotChangeException
  /// to HTTP 400 status indicating validation error for immutable
  /// identifier field.
  @ExceptionHandler(EntityTemplateIdentifierCannotChangeException.class)
  public ResponseEntity<ErrorResponse> handleEntityTemplateIdentifierCannotChangeException(
      EntityTemplateIdentifierCannotChangeException ex) {
    log.warn("Entity entityTemplateIdentifier identifier cannot be changed: {}", ex.getMessage());
    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.name(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  /// Handles domain exception for wrong entity entityTemplateIdentifier property
  /// rules.
  ///
  /// **HTTP mapping:** Maps domain PropertyDefinitionRulesConflictException to
  /// HTTP 400 status indicating validation error for wrong property rules.
  @ExceptionHandler(PropertyDefinitionRulesConflictException.class)
  public ResponseEntity<ErrorResponse> handleWrongPropertyRulesException(
      PropertyDefinitionRulesConflictException ex) {
    log.warn("Wrong Entity entityTemplateIdentifier property rules: {}", ex.getMessage());
    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.name(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  /// Handles domain exception when property names are duplicated within a
  /// entityTemplateIdentifier.
  ///
  /// **HTTP mapping:** Maps domain PropertyNameAlreadyExistsException to HTTP 400
  /// status indicating validation error for duplicate property names.
  @ExceptionHandler(PropertyNameAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handlePropertyNameAlreadyExistsException(
      PropertyNameAlreadyExistsException ex) {
    log.warn("Duplicate property name: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /// Handles domain exception when relation names are duplicated within a
  /// entityTemplateIdentifier.
  ///
  /// **HTTP mapping:** Maps domain RelationNameAlreadyExistsException to HTTP 400
  /// status indicating validation error for duplicate relation names.
  @ExceptionHandler(RelationNameAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleRelationNameAlreadyExistsException(
      RelationNameAlreadyExistsException ex) {
    log.warn("Duplicate relation name: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /// Handles domain exception when a relation references a non-existent target
  /// entityTemplateIdentifier.
  ///
  /// **HTTP mapping:** Maps domain TargetTemplateNotFoundException to HTTP 400
  /// status indicating validation error for missing target
  /// entityTemplateIdentifier.
  @ExceptionHandler(TargetTemplateNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleTargetTemplateNotFoundException(
      TargetTemplateNotFoundException ex) {
    log.warn("Target entityTemplateIdentifier not found: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /// Handles domain exception when type changes are attempted.
  ///
  /// **HTTP mapping:** Maps domain PropertyTypeChangeException to HTTP 400 status
  /// indicating validation error for type changes.
  @ExceptionHandler(PropertyTypeChangeException.class)
  public ResponseEntity<ErrorResponse> handleTypeChangeException(PropertyTypeChangeException ex) {
    log.warn("Type change error: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /// Handles domain exception when relation target entityTemplateIdentifier
  /// changes are
  /// attempted.
  ///
  /// **HTTP mapping:** Maps domain RelationTargetTemplateChangeException to HTTP
  /// 400 status indicating validation error for immutable target
  /// entityTemplateIdentifier field.
  @ExceptionHandler(RelationTargetTemplateChangeException.class)
  public ResponseEntity<ErrorResponse> handleRelationTargetTemplateChangeException(
      RelationTargetTemplateChangeException ex) {
    log.warn("Relation target entityTemplateIdentifier change error: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /// Handles domain exception when a relation's target entityTemplateIdentifier
  /// identifier is the
  /// entityTemplateIdentifier itself.
  ///
  /// **HTTP mapping:** Maps domain RelationCannotTargetItselfException to HTTP
  /// 400
  /// status indicating validation error for self-referential relations.
  @ExceptionHandler(RelationCannotTargetItselfException.class)
  public ResponseEntity<ErrorResponse> handleRelationCannotTargetItselfException(
      RelationCannotTargetItselfException ex) {
    log.warn("Relation self-reference error: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /// Handles validation exceptions from Spring MVC handler method parameters.
  ///
  /// **Error aggregation:** Combines multiple validation error messages into a
  /// single user-friendly response with HTTP 400 status for client correction.
  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex) {
    log.warn("Handler method validation error: {}", ex.getMessage());
    String errorMessage = ex.getAllErrors().stream()
        .map(org.springframework.context.MessageSourceResolvable::getDefaultMessage)
        .collect(Collectors.joining(", "));
    return createErrorResponse(HttpStatus.BAD_REQUEST, errorMessage);
  }

  /// Handles domain exception when entities already exist.
  ///
  /// **HTTP mapping:** Maps domain EntityAlreadyExistsException to HTTP 409
  /// status
  /// indicating business rule conflict for duplicate entities.
  @ExceptionHandler(EntityAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleEntityAlreadyExistsException(
      EntityAlreadyExistsException ex) {
    log.warn("Entity already exists: {}", ex.getMessage());
    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.CONFLICT.name(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }

  /// Handles domain exception when entity validation fails.
  ///
  /// **HTTP mapping:** Maps domain EntityValidationException to HTTP 400 status
  /// with aggregated validation error messages for client correction.
  @ExceptionHandler(EntityValidationException.class)
  public ResponseEntity<ErrorResponse> handleEntityValidationException(
      EntityValidationException ex) {
    log.warn("Entity validation failed: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /// Handles Spring MVC request body validation failures.
  ///
  /// **Field-level errors:** Extracts and aggregates field validation errors from
  /// request body binding into comprehensive HTTP 400 error response.
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException ex) {
    log.warn("Method argument validation error: {}", ex.getMessage());

    String errorMessage = ex.getBindingResult().getFieldErrors().stream()
        .map(org.springframework.context.MessageSourceResolvable::getDefaultMessage)
        .collect(Collectors.joining(", "));

    return createErrorResponse(HttpStatus.BAD_REQUEST, errorMessage);
  }

  /// Handles JSON parsing and deserialization errors from request bodies.
  ///
  /// **User-friendly parsing:** Converts technical JSON parsing errors into
  /// readable messages, especially for enum validation and format issues.
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException ex) {
    log.warn("HTTP message not readable: {}", ex.getMessage());

    String errorMessage = parseHttpMessageNotReadableError(ex.getMessage());
    return createErrorResponse(HttpStatus.BAD_REQUEST, errorMessage);
  }

  /// Handles invalid dynamic mapping expressions (JSLT) provided in webhook
  /// configuration.
  ///
  /// **HTTP mapping:** Maps domain mapping configuration failures to HTTP 400,
  /// because clients can fix these expressions and retry.
  @ExceptionHandler(EntityDynamicMappingConfigurationException.class)
  public ResponseEntity<ErrorResponse> handleEntityDynamicMappingConfigurationException(
      EntityDynamicMappingConfigurationException ex) {
    log.warn("Invalid entity dynamic mapping configuration: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(PropertyNameNotFoundEntityTemplatePropertiesException.class)
  public ResponseEntity<ErrorResponse> handlePropertyNameNotFoundEntityTemplatePropertiesException(
      PropertyNameNotFoundEntityTemplatePropertiesException ex) {
    log.warn("Webhook mapping references unknown property: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(RelationNameNotFoundEntityTemplateRelationsException.class)
  public ResponseEntity<ErrorResponse> handleRelationNameNotFoundEntityTemplateRelationsException(
      RelationNameNotFoundEntityTemplateRelationsException ex) {
    log.warn("Webhook mapping references unknown relation: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(WebhookSecurityConfigurationException.class)
  public ResponseEntity<ErrorResponse> handleWebhookSecurityConfigurationException(
      WebhookSecurityConfigurationException ex) {
    log.warn("Invalid webhook security configuration: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  /// Handles domain exception when entities are not found.
  ///
  /// **HTTP mapping:** Maps domain EntityNotFoundException to HTTP 404 status
  /// with
  /// specific entity context for API consumers.
  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex) {
    ErrorResponse errorResponse = new ErrorResponse(NOT_FOUND.name(), ex.getMessage());
    return ResponseEntity.status(NOT_FOUND).body(errorResponse);
  }

  /// Handles Bean Validation constraint violations from domain model validation.
  ///
  /// **Error aggregation:** Combines multiple constraint violation messages into
  /// single user-friendly response with HTTP 400 status for client correction.
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolationException(
      ConstraintViolationException ex) {
    log.warn("Validation constraint violation: {}", ex.getMessage());

    String errorMessage = ex.getConstraintViolations().stream().map(ConstraintViolation::getMessage)
        .collect(Collectors.joining(", "));
    return createErrorResponse(HttpStatus.BAD_REQUEST, errorMessage);
  }
  /// Handles domain exception when entity deletion is blocked by required
  /// relations.
  ///
  /// **HTTP mapping:** Maps domain EntityDeletionBlockedException to HTTP 409
  /// status indicating business rule conflict where required relations prevent
  /// deletion.
  @ExceptionHandler(EntityDeletionBlockedException.class)
  public ResponseEntity<ErrorResponse> handleEntityDeletionBlockedException(
      EntityDeletionBlockedException ex) {
    log.warn("Entity deletion blocked: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
  }

  /// Handles missing path variables in the request URL.
  ///
  /// **HTTP mapping:** Maps MissingPathVariableException to HTTP 400
  /// status indicating a malformed request URL from the client.
  @ExceptionHandler(MissingPathVariableException.class)
  public ResponseEntity<ErrorResponse> handleMissingPathVariableException(
      MissingPathVariableException ex) {
    log.warn("Missing path variable: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.BAD_REQUEST,
        "Missing required path variable: " + ex.getVariableName());
  }

  /// Handles cases where a truncated URL matches no route, often caused by
  /// missing trailing path variables.
  ///
  /// **HTTP mapping:** Maps NoHandlerFoundException to HTTP 400
  /// to align with missing identifier logic and pass integration tests.
  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(NoHandlerFoundException ex) {
    log.warn("No handler found or missing path variable: {}", ex.getMessage());
    return createErrorResponse(NOT_FOUND, "Malformed request URL or missing path variable.");
  }

  private String parseHttpMessageNotReadableError(String originalMessage) {
    if (originalMessage == null) {
      return "Invalid request body format";
    }

    if (originalMessage.contains("Cannot deserialize value")) {
      return parseDeserializationError(originalMessage);
    } else if (originalMessage.contains("Required request body is missing")) {
      return "Request body is required";
    } else if (originalMessage.contains("JSON parse error")) {
      return "Invalid JSON format in request body";
    }

    return "Invalid request body format";
  }

  private String parseDeserializationError(String originalMessage) {
    if (originalMessage.contains("not one of the values accepted for Enum class")) {
      return parseEnumDeserializationError(originalMessage);
    }
    return parseTypeDeserializationError(originalMessage);
  }

  private String parseTypeDeserializationError(String originalMessage) {
    String targetType = extractTargetType(originalMessage);
    String invalidValue = extractInvalidValueFromString(originalMessage);

    if (!targetType.isEmpty() && !invalidValue.isEmpty()) {
      return "Invalid value '" + invalidValue + "' for property, expected " + targetType;
    } else if (!targetType.isEmpty()) {
      return "Invalid type: expected " + targetType;
    }
    return "Cannot deserialize request body property";
  }

  private String extractTargetType(String message) {
    Pattern typePattern = Pattern.compile("Cannot deserialize value of type `([^`]+)`");
    Matcher matcher = typePattern.matcher(message);
    if (matcher.find()) {
      String fullType = matcher.group(1);
      return fullType.substring(fullType.lastIndexOf('.') + 1);
    }
    return "";
  }

  private String extractInvalidValueFromString(String message) {
    Pattern valuePattern = Pattern.compile("from String \"([^\"]+)\"");
    Matcher matcher = valuePattern.matcher(message);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return "";
  }

  private String parseEnumDeserializationError(String originalMessage) {
    String enumTypeName = getPropertyNameFromEnumType(originalMessage);
    String invalidValue = extractInvalidValueFromString(originalMessage);

    if (!enumTypeName.isEmpty() && !invalidValue.isEmpty()) {
      return "Invalid value '" + invalidValue + "' for property '" + enumTypeName + "'";
    } else if (!enumTypeName.isEmpty()) {
      return "Invalid value for property '" + enumTypeName + "'";
    }
    return "Invalid enum value in request body";
  }

  private static final Map<String, String> ENUM_TYPE_TO_PROPERTY = Map.of("PropertyType", "type",
      "PropertyFormat", "format");

  private static final Pattern ENUM_CLASS_PATTERN = Pattern
      .compile("Cannot deserialize value of type `(?:[\\w.]+\\.)?(\\w+)`");

  private String getPropertyNameFromEnumType(String message) {
    Matcher matcher = ENUM_CLASS_PATTERN.matcher(message);
    if (matcher.find()) {
      String enumType = matcher.group(1);
      return ENUM_TYPE_TO_PROPERTY.getOrDefault(enumType, "");
    }
    return "";
  }

  /// Handles all unexpected exceptions as safety fallback.
  ///
  /// **Security consideration:** Returns generic error message to prevent
  /// information leakage while logging full exception details for
  /// internal debugging.
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
    log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

    String errorMessage = "An unexpected error occurred. Please try again later.";
    return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
  }

  /// Handles webhook signature and credential validation failures.
  ///
  /// HTTP mapping: Maps WebhookAuthenticationException to HTTP 401 Unauthorized.
  @ExceptionHandler(WebhookAuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleWebhookAuthenticationException(
      WebhookAuthenticationException ex) {
    log.warn("Webhook authentication failed: {}", ex.getMessage());
    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.UNAUTHORIZED.name(),
        ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
  }

  /// Handles missing webhook connector configuration.
  ///
  /// HTTP mapping: Maps WebhookConnectorNotFoundException to HTTP 404 Not Found.
  @ExceptionHandler(WebhookConnectorNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleWebhookConnectorNotFoundException(
      WebhookConnectorNotFoundException ex) {
    log.warn("Webhook connector not found: {}", ex.getMessage());
    ErrorResponse errorResponse = new ErrorResponse(NOT_FOUND.name(), ex.getMessage());
    return ResponseEntity.status(NOT_FOUND).body(errorResponse);
  }

  /// Handles a webhook connector referencing a non-existent entity dynamic
  /// mapping.
  ///
  /// HTTP mapping: Maps EntityDynamicMappingNotFoundException to HTTP 404 Not
  /// Found, because the referenced mapping must be created beforehand.
  @ExceptionHandler(EntityDynamicMappingNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleEntityDynamicMappingNotFoundException(
      EntityDynamicMappingNotFoundException ex) {
    log.warn("Referenced entity dynamic mapping not found: {}", ex.getMessage());
    ErrorResponse errorResponse = new ErrorResponse(NOT_FOUND.name(), ex.getMessage());
    return ResponseEntity.status(NOT_FOUND).body(errorResponse);
  }

  /// Handles creation of a dynamic mapping whose identifier already exists.
  ///
  /// HTTP mapping: Maps EntityDynamicMappingAlreadyExistsException to HTTP 409
  /// Conflict, surfacing the uniqueness violation with business meaning.
  @ExceptionHandler(EntityDynamicMappingAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleEntityDynamicMappingAlreadyExistsException(
      EntityDynamicMappingAlreadyExistsException ex) {
    log.warn("Entity dynamic mapping identifier conflict: {}", ex.getMessage());
    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.CONFLICT.name(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }

  /// Handles low-level database integrity violations (for example, unique
  /// constraint breaches) that were not caught earlier by domain validation.
  ///
  /// HTTP mapping: Maps DataIntegrityViolationException to HTTP 409 Conflict to
  /// avoid leaking technical SQL details while signaling a conflicting state.
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
      DataIntegrityViolationException ex) {
    log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.CONFLICT.name(),
        "The request conflicts with the current state of the resource");
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }

  @ExceptionHandler(EntityDynamicMappingAlreadyInUseException.class)
  public ResponseEntity<ErrorResponse> handleEntityDynamicMappingAlreadyInUseException(
      EntityDynamicMappingAlreadyInUseException ex) {
    log.warn("Entity dynamic mapping already in use: {}", ex.getMessage());
    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.CONFLICT.name(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }

  /// Handles webhook connector identifier duplication conflicts.
  @ExceptionHandler(WebhookConnectorAlreadyExistException.class)
  public ResponseEntity<ErrorResponse> handleWebhookConnectorAlreadyExistException(
      WebhookConnectorAlreadyExistException ex) {
    log.warn("Webhook connector identifier conflict: {}", ex.getMessage());
    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.CONFLICT.name(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }

  @ExceptionHandler(EntityTemplateUsedByDynamicMappingException.class)
  public ResponseEntity<ErrorResponse> handleTemplateAlreadyMappedInWebhookConfiguration(
      EntityTemplateUsedByDynamicMappingException ex) {
    log.warn("Entity entityTemplateIdentifier in use by webhook mapping conflict: {}",
        ex.getMessage());
    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.CONFLICT.name(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }

  /// Handles webhook connector name duplication conflicts.
  @ExceptionHandler(WebhookConnectorTitleAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleWebhookConnectorTitleAlreadyExistsException(
      WebhookConnectorTitleAlreadyExistsException ex) {
    log.warn("Webhook connector name conflict: {}", ex.getMessage());
    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.CONFLICT.name(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }
  @ExceptionHandler(EntityDynamicMappingHasNoPropertiesException.class)
  public ResponseEntity<ErrorResponse> handleEntityDynamicMappingHasNoPropertiesException(
      EntityDynamicMappingHasNoPropertiesException ex) {
    log.warn("Entity dynamic mapping has no properties: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(EntityDynamicMappingHasNoRelationsException.class)
  public ResponseEntity<ErrorResponse> handleEntityDynamicMappingHasNoRelationsException(
      EntityDynamicMappingHasNoRelationsException ex) {
    log.warn("Entity dynamic mapping has no relations: {}", ex.getMessage());
    return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  private static ResponseEntity<ErrorResponse> createErrorResponse(HttpStatus httpStatus,
      String errorMessage) {
    return new ResponseEntity<>(new ErrorResponse(httpStatus.name(), errorMessage), httpStatus);
  }

  @Getter
  @AllArgsConstructor
  @NoArgsConstructor(force = true)
  public static class ErrorResponse {
    private String error;
    private String errorDescription;
  }
}
