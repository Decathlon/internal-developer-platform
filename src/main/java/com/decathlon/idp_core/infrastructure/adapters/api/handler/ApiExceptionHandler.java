package com.decathlon.idp_core.infrastructure.adapters.api.handler;

import static java.util.Map.entry;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
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
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.decathlon.idp_core.domain.constant.ValidationMessages;
import com.decathlon.idp_core.domain.exception.authorization.PrincipalNotAuthorizedException;
import com.decathlon.idp_core.domain.exception.entity.EntityAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity.EntityDeletionBlockedException;
import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.*;
import com.decathlon.idp_core.domain.exception.entity_template.*;
import com.decathlon.idp_core.domain.exception.filter.InvalidFilterDslException;
import com.decathlon.idp_core.domain.exception.mock.MockSecurityConfigurationException;
import com.decathlon.idp_core.domain.exception.principal.PrincipalNotFoundException;
import com.decathlon.idp_core.domain.exception.search.InvalidSearchQueryException;
import com.decathlon.idp_core.domain.exception.webhook.*;
import com.decathlon.idp_core.infrastructure.adapters.api.exception.MissingPrincipalIdentifierException;

import lombok.extern.slf4j.Slf4j;

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
/// exclusively here, in [#STATUS_BY_EXCEPTION]. Adding support for a new domain
/// exception only requires adding one entry to that map; no new handler method
/// is needed.
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  /// Maps each domain exception type to the HTTP status it represents.
  ///
  /// **Why a map instead of one `@ExceptionHandler` per exception:** all these
  /// exceptions are handled identically (log + status + `ex.getMessage()` as
  /// `detail`); only the resulting status differs. Centralizing the mapping
  /// here keeps [#handleMappedException] as the single place that builds the
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
          entry(EntityTemplateIsRelationTargetException.class, BAD_REQUEST),
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
          entry(MissingPrincipalIdentifierException.class, UNAUTHORIZED),
          entry(WebhookAuthenticationException.class, UNAUTHORIZED),
          // 403 Forbidden — identity is valid but not authorized
          entry(PrincipalNotAuthorizedException.class, HttpStatus.FORBIDDEN),
          // 404 Not Found — the referenced resource does not exist
          entry(EntityTemplateNotFoundException.class, NOT_FOUND),
          entry(EntityNotFoundException.class, NOT_FOUND),
          entry(PrincipalNotFoundException.class, NOT_FOUND),
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

  /// Handles registered domain exceptions and unexpected exceptions centrally.
  ///
  /// **Safety net:** any exception not present in the map is reported as HTTP
  /// 500 with a generic message, so internal details never leak to API consumers.
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleMappedException(Exception ex, WebRequest request) {
    HttpStatus status = statusFor(ex);
    if (status == null) {
      log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
      return problemResponse(INTERNAL_SERVER_ERROR,
          "An unexpected error occurred. Please try again later.", request);
    }
    log.warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
    return problemResponse(status, ex.getMessage(), request);
  }

  private static HttpStatus statusFor(Exception exception) {
    if (!(exception instanceof RuntimeException)) {
      return null;
    }
    return STATUS_BY_EXCEPTION.entrySet().stream()
        .filter(entry -> entry.getKey().isInstance(exception)).map(Map.Entry::getValue).findFirst()
        .orElse(null);
  }

  /// Handles Bean Validation constraint violations thrown outside Spring MVC's
  /// own method-validation flow (for example, domain-level manual validation).
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolationException(
      ConstraintViolationException ex, WebRequest request) {
    String detail = ex.getConstraintViolations().stream().map(ConstraintViolation::getMessage)
        .collect(Collectors.joining(", "));
    log.warn("Validation constraint violation: {}", detail);
    return problemResponse(BAD_REQUEST, detail, request);
  }

  /// Handles low-level database integrity violations (for example, unique
  /// constraint breaches) not already caught by domain validation, hiding
  /// technical SQL details behind a generic conflict message.
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetail> handleDataIntegrityViolationException(
      DataIntegrityViolationException ex, WebRequest request) {
    log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
    return problemResponse(CONFLICT, "The request conflicts with the current state of the resource",
        request);
  }

  private static ResponseEntity<ProblemDetail> problemResponse(HttpStatus status, String detail,
      WebRequest request) {
    ProblemDetail problemDetail = ApiProblemDetailFactory.create(status, detail,
        requestInstance(request));
    return ResponseEntity.status(status).body(problemDetail);
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

  private static URI requestInstance(WebRequest request) {
    if (request instanceof ServletWebRequest servletWebRequest) {
      return URI.create(servletWebRequest.getRequest().getRequestURI());
    }
    return null;
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
      HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, status,
        request);
    if (response != null && response.getBody()instanceof ProblemDetail problemDetail) {
      ApiProblemDetailFactory.addCompatibilityProperties(problemDetail, status,
          requestInstance(request));
    }
    return response;
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

  /// Preserves the API's actionable messages for pagination and other typed
  /// request parameters while delegating unrelated type mismatches to Spring.
  @Override
  protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
      HttpStatusCode status, WebRequest request) {
    if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
      String detail = switch (mismatch.getName()) {
        case "page" -> ValidationMessages.PAGE_INDEX_NOT_INTEGER;
        case "size" -> ValidationMessages.PAGE_SIZE_NOT_INTEGER;
        default -> "Invalid value '%s' for parameter '%s'.".formatted(mismatch.getValue(),
            mismatch.getName());
      };
      if (!"page".equals(mismatch.getName()) && !"size".equals(mismatch.getName())) {
        Class<?> requiredType = mismatch.getRequiredType();
        if (requiredType != null && requiredType.isEnum()) {
          detail += " Allowed values: " + Arrays.toString(requiredType.getEnumConstants()) + ".";
        } else if (requiredType != null) {
          detail += " Expected type: " + requiredType.getSimpleName() + ".";
        }
      }
      log.warn("Request parameter type mismatch: {}", detail);
      return handleExceptionInternal(ex, problemDetail(status, detail), headers, status, request);
    }
    return super.handleTypeMismatch(ex, headers, status, request);
  }
}
