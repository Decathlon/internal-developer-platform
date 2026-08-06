package com.decathlon.idp_core.infrastructure.adapters.api.handler;

import static java.util.Map.entry;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.decathlon.idp_core.domain.exception.entity.EntityAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity.EntityDeletionBlockedException;
import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.*;
import com.decathlon.idp_core.domain.exception.entity_template.*;
import com.decathlon.idp_core.domain.exception.filter.InvalidFilterDslException;
import com.decathlon.idp_core.domain.exception.mock.MockSecurityConfigurationException;
import com.decathlon.idp_core.domain.exception.search.InvalidSearchQueryException;
import com.decathlon.idp_core.domain.exception.webhook.*;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

/// Global exception handler providing centralized, RFC 9457 (formerly RFC 7807)
/// "Problem Details" error responses for all API endpoints.
///
/// **Infrastructure error handling strategy:** Extends Spring's native
/// [ResponseEntityExceptionHandler] so that every response — whether raised by
/// Spring MVC itself or by a domain exception — is serialized as a
/// [ProblemDetail] (`title`, `type`, `status`, `detail`). This is enabled
/// globally by `spring.mvc.problemdetails.enabled=true`.
///
/// **Factorization:** Domain exceptions never carry HTTP semantics (see the
/// domain layer instructions), so the exception-to-status mapping lives
/// exclusively here, in [#STATUS_BY_EXCEPTION]. Adding support for a new
/// domain exception only requires adding one entry to that map; no new
/// handler method is needed.
@Slf4j
@ControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  /// Maps each domain exception type to the HTTP status it represents.
  ///
  /// **Why a map instead of one `@ExceptionHandler` per exception:** all these
  /// exceptions are handled identically (log + status + `ex.getMessage()` as
  /// `detail`); only the resulting status differs. Centralizing the mapping
  /// here keeps [#handleDomainException] as the single place that builds the
  /// response, and turns "support a new exception" into a one-line change.
  private static final Map<Class<? extends RuntimeException>, HttpStatus> STATUS_BY_EXCEPTION = Map
      .ofEntries(
          // 400 Bad Request — malformed input or a business rule violation the
          // client can fix and retry
          entry(InvalidFilterDslException.class, BAD_REQUEST),
          entry(InvalidSearchQueryException.class, BAD_REQUEST),
          entry(EntityTemplateIdentifierCannotChangeException.class, BAD_REQUEST),
          entry(PropertyDefinitionRulesConflictException.class, BAD_REQUEST),
          entry(PropertyNameAlreadyExistsException.class, BAD_REQUEST),
          entry(RelationNameAlreadyExistsException.class, BAD_REQUEST),
          entry(TargetTemplateNotFoundException.class, BAD_REQUEST),
          entry(PropertyTypeChangeException.class, BAD_REQUEST),
          entry(RelationTargetTemplateChangeException.class, BAD_REQUEST),
          entry(RelationCannotTargetItselfException.class, BAD_REQUEST),
          entry(EntityValidationException.class, BAD_REQUEST),
          entry(EntityDynamicMappingConfigurationException.class, BAD_REQUEST),
          entry(PropertyNameNotFoundEntityTemplatePropertiesException.class, BAD_REQUEST),
          entry(WebhookSecurityConfigurationException.class, BAD_REQUEST),
          entry(WebhookConnectorConfigurationException.class, BAD_REQUEST),
          entry(MockSecurityConfigurationException.class, BAD_REQUEST),
          // 401 Unauthorized — credentials could not be verified
          entry(WebhookAuthenticationException.class, UNAUTHORIZED),
          // 404 Not Found — the referenced resource does not exist
          entry(EntityTemplateNotFoundException.class, NOT_FOUND),
          entry(EntityNotFoundException.class, NOT_FOUND),
          entry(WebhookConnectorNotFoundException.class, NOT_FOUND),
          entry(EntityDynamicMappingNotFoundException.class, NOT_FOUND),
          // 409 Conflict — the request conflicts with the current state of the resource
          entry(EntityTemplateAlreadyExistsException.class, CONFLICT),
          entry(EntityTemplateNameAlreadyExistsException.class, CONFLICT),
          entry(EntityAlreadyExistsException.class, CONFLICT),
          entry(EntityDeletionBlockedException.class, CONFLICT),
          entry(EntityDynamicMappingAlreadyExistsException.class, CONFLICT),
          entry(EntityDynamicMappingAlreadyInUseException.class, CONFLICT),
          entry(WebhookConnectorAlreadyExistException.class, CONFLICT),
          entry(EntityTemplateUsedByDynamicMappingException.class, CONFLICT),
          entry(WebhookConnectorTitleAlreadyExistsException.class, CONFLICT),
          // 422 Unprocessable Content — syntactically valid request that cannot be
          // processed (mapping/expression evaluation failures)
          entry(ExpressionEvaluationFailedException.class, UNPROCESSABLE_CONTENT),
          entry(EntityDynamicMappingJsltErrorException.class, UNPROCESSABLE_CONTENT),
          entry(RelationNameNotFoundEntityTemplateRelationsException.class, UNPROCESSABLE_CONTENT),
          entry(EntityDynamicMappingHasNoPropertiesException.class, UNPROCESSABLE_CONTENT),
          entry(EntityDynamicMappingHasNoRelationsException.class, UNPROCESSABLE_CONTENT));

  /// Handles every domain exception through a single entry point, looking up
  /// its HTTP status in [#STATUS_BY_EXCEPTION].
  ///
  /// **Safety net:** any `RuntimeException` not present in the map (that is,
  /// truly unexpected) is reported as HTTP 500 with a generic message, so
  /// internal details never leak to API consumers.
  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ProblemDetail> handleDomainException(RuntimeException ex) {
    HttpStatus status = STATUS_BY_EXCEPTION.get(ex.getClass());
    if (status == null) {
      log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
      return problemResponse(INTERNAL_SERVER_ERROR,
          "An unexpected error occurred. Please try again later.");
    }
    log.warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
    return problemResponse(status, ex.getMessage());
  }

  /// Handles Bean Validation constraint violations thrown outside Spring MVC's
  /// own method-validation flow (for example, domain-level manual validation).
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolationException(
      ConstraintViolationException ex) {
    String detail = ex.getConstraintViolations().stream().map(ConstraintViolation::getMessage)
        .collect(Collectors.joining(", "));
    log.warn("Validation constraint violation: {}", detail);
    return problemResponse(BAD_REQUEST, detail);
  }

  /// Handles low-level database integrity violations (for example, unique
  /// constraint breaches) not already caught by domain validation, hiding
  /// technical SQL details behind a generic conflict message.
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetail> handleDataIntegrityViolationException(
      DataIntegrityViolationException ex) {
    log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
    return problemResponse(CONFLICT,
        "The request conflicts with the current state of the resource");
  }

  private static ResponseEntity<ProblemDetail> problemResponse(HttpStatus status, String detail) {
    return ResponseEntity.status(status).body(createProblemDetail(status, detail));
  }

  private static ProblemDetail problemDetail(HttpStatusCode status, String detail) {
    return createProblemDetail(status, detail);
  }

  private static ProblemDetail createProblemDetail(HttpStatusCode status, String detail) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
    HttpStatus httpStatus = HttpStatus.resolve(status.value());
    if (httpStatus != null) {
      problemDetail.setTitle(httpStatus.getReasonPhrase());
    }
    return problemDetail;
  }

  /// Customizes Spring's built-in handling of request body validation
  /// failures, aggregating field errors into the `detail` field while reusing
  /// the native RFC 9457 conversion.
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
      HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    String detail = ex.getBindingResult().getFieldErrors().stream()
        .map(MessageSourceResolvable::getDefaultMessage).collect(Collectors.joining(", "));
    log.warn("Method argument validation error: {}", detail);
    return handleExceptionInternal(ex, problemDetail(status, detail), headers, status, request);
  }

  /// Customizes Spring's built-in handling of validation errors on
  /// `@Validated` handler method parameters (for example, path/query params).
  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status,
      WebRequest request) {
    String detail = ex.getAllErrors().stream().map(MessageSourceResolvable::getDefaultMessage)
        .collect(Collectors.joining(", "));
    log.warn("Handler method validation error: {}", detail);
    return handleExceptionInternal(ex, problemDetail(status, detail), headers, status, request);
  }

  /// Customizes Spring's built-in handling of missing path variables. Forces
  /// HTTP 400 (Spring's default is 500, treating it as a routing bug) because,
  /// in this API, it is caused by a malformed client URL.
  @Override
  protected ResponseEntity<Object> handleMissingPathVariable(MissingPathVariableException ex,
      HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    String detail = "Missing required path variable: " + ex.getVariableName();
    log.warn("Missing path variable: {}", ex.getMessage());
    return handleExceptionInternal(ex, problemDetail(BAD_REQUEST, detail), headers, BAD_REQUEST,
        request);
  }

  /// Customizes Spring's built-in handling of unmatched routes, aligning the
  /// message with missing-identifier requests.
  @Override
  protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex,
      HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    log.warn("No handler found or missing path variable: {}", ex.getMessage());
    String detail = "Malformed request URL or missing path variable.";
    return handleExceptionInternal(ex, problemDetail(status, detail), headers, status, request);
  }

  /// Customizes Spring's built-in handling of JSON parsing/deserialization
  /// errors, translating technical Jackson messages into readable ones,
  /// especially for enum validation and type-format issues.
  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
      HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    log.warn("HTTP message not readable: {}", ex.getMessage());

    if (ex.getCause()instanceof MismatchedInputException mismatch
        && !(ex.getCause() instanceof InvalidFormatException)) {
      String fieldPath = extractLastFieldNameFromPath(mismatch.getPath());
      String targetType = extractTargetType(mismatch.getOriginalMessage());

      if (!targetType.isEmpty() && !fieldPath.isEmpty()) {
        String detail = "Invalid type for '" + fieldPath + "': expected " + targetType;
        return handleExceptionInternal(ex, problemDetail(status, detail), headers, status, request);
      }
    }

    String detail = parseHttpMessageNotReadableError(ex.getMessage());
    return handleExceptionInternal(ex, problemDetail(status, detail), headers, status, request);
  }

  /// Extracts the deepest (last) field name from a Jackson path reference list.
  ///
  /// Jackson populates `MismatchedInputException.getPath()` with references from
  /// the root to the failing field. The last named reference is the most specific
  /// field name to show in the error message.
  private String extractLastFieldNameFromPath(List<JacksonException.Reference> path) {
    if (path == null || path.isEmpty()) {
      return "";
    }
    return path.reversed().stream().map(JacksonException.Reference::getPropertyName)
        .filter(name -> name != null && !name.isBlank()).findFirst().orElse("");
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
      String rawType = fullType.contains("<")
          ? fullType.substring(0, fullType.indexOf('<'))
          : fullType;
      return rawType.substring(rawType.lastIndexOf('.') + 1);
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
}
