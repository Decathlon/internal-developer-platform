package com.decathlon.idp_core.infrastructure.adapters.api.handler;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.decathlon.idp_core.domain.exception.entity_template.PropertyDefinitionRulesConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.decathlon.idp_core.domain.exception.EntityNotFoundException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateIdentifierCannotChangeException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNameAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNotFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.NoHandlerFoundException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/// Global exception handler providing centralized error handling for all API endpoints.
///
/// **Infrastructure error handling strategy:** Intercepts domain and validation exceptions
/// and converts them to appropriate HTTP responses with consistent error formatting.
/// Ensures API consumers receive standardized error messages regardless of internal failures.
///
/// **Exception mapping approach:**
/// - Domain exceptions → HTTP 404/409 with business-meaningful messages
/// - Validation exceptions → HTTP 400 with field-specific error details
/// - JSON parsing errors → HTTP 400 with user-friendly parsing messages
/// - Generic exceptions → HTTP 500 with safe internal error responses
///
/// **Error response standardization:** All errors follow consistent [ErrorResponse] format
/// with appropriate HTTP status codes and logged for monitoring/debugging purposes.
@Slf4j
@ControllerAdvice
public class ApiExceptionHandler {

    private ApiExceptionHandler() {
    }

    /// Handles domain exception when entity templates are not found.
    ///
    /// **HTTP mapping:** Maps domain EntityTemplateNotFoundException to HTTP 404 status
    /// with business-meaningful error message for API consumers.
    @ExceptionHandler(EntityTemplateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTemplateNotFoundException(EntityTemplateNotFoundException ex) {
        log.warn("Template not found: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(NOT_FOUND.name(), ex.getMessage());
        return ResponseEntity.status(NOT_FOUND).body(errorResponse);
    }

    /// Handles domain exception when entity templates already exist.
    ///
    /// **HTTP mapping:** Maps domain EntityTemplateAlreadyExistsException to HTTP 409
    /// status indicating business rule conflict for duplicate identifiers.
    @ExceptionHandler(EntityTemplateAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEntityTemplateAlreadyExistsException(
            EntityTemplateAlreadyExistsException ex) {
        log.warn("Entity template already exists: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.CONFLICT.name(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /// Handles domain exception when entity template names already exist.
    ///
    /// **HTTP mapping:** Maps domain EntityTemplateNameAlreadyExistsException to HTTP 409
    /// status indicating business rule conflict for duplicate template names.
    @ExceptionHandler(EntityTemplateNameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEntityTemplateNameAlreadyExistsException(
            EntityTemplateNameAlreadyExistsException ex) {
        log.warn("Entity template name already exists: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.CONFLICT.name(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /// Handles domain exception when attempting to change an entity template identifier.
    ///
    /// **HTTP mapping:** Maps domain EntityTemplateIdentifierCannotChangeException to HTTP 400
    /// status indicating validation error for immutable identifier field.
    @ExceptionHandler(EntityTemplateIdentifierCannotChangeException.class)
    public ResponseEntity<ErrorResponse> handleEntityTemplateIdentifierCannotChangeException(
            EntityTemplateIdentifierCannotChangeException ex) {
        log.warn("Entity template identifier cannot be changed: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.name(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /// Handles domain exception for wrong entity template property rules.
    ///
    /// **HTTP mapping:** Maps domain PropertyDefinitionRulesConflictException to HTTP 400
    /// status indicating validation error for wrong property rules.
    @ExceptionHandler(PropertyDefinitionRulesConflictException.class)
    public ResponseEntity<ErrorResponse> handleWrongPropertyRulesException(
            PropertyDefinitionRulesConflictException ex) {
        log.warn("Wrong Entity template property rules: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.name(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /// Handles Bean Validation constraint violations from domain model validation.
    ///
    /// **Error aggregation:** Combines multiple constraint violation messages into
    /// single user-friendly response with HTTP 400 status for client correction.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        log.warn("Validation constraint violation: {}", ex.getMessage());

        String errorMessage = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        return createErrorResponse(HttpStatus.BAD_REQUEST, errorMessage);
    }

    /// Handles Spring MVC request body validation failures.
    ///
    /// **Field-level errors:** Extracts and aggregates field validation errors from
    /// request body binding into comprehensive HTTP 400 error response.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
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
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("HTTP message not readable: {}", ex.getMessage());

        String errorMessage = parseHttpMessageNotReadableError(ex.getMessage());
        return createErrorResponse(HttpStatus.BAD_REQUEST, errorMessage);
    }


    /// Handles domain exception when entities are not found.
    ///
    /// **HTTP mapping:** Maps domain EntityNotFoundException to HTTP 404 status
    /// with specific entity context for API consumers.
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse(NOT_FOUND.name(), ex.getMessage());
        return ResponseEntity.status(NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoHandlerFoundException e) {
        return createErrorResponse(NOT_FOUND, "Resource not found: " + e.getRequestURL());
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

    private static final Map<String, String> ENUM_TYPE_TO_PROPERTY = Map.of(
            "PropertyType", "type",
            "PropertyFormat", "format");

    private static final Pattern ENUM_CLASS_PATTERN = Pattern.compile("Cannot deserialize value of type `(?:[\\w.]+\\.)?(\\w+)`");

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
    /// **Security consideration:** Returns generic error message to prevent information
    /// leakage while logging full exception details for internal debugging.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        String errorMessage = "An unexpected error occurred. Please try again later.";
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
    }

    private static ResponseEntity<ErrorResponse> createErrorResponse(HttpStatus httpStatus, String errorMessage) {
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
