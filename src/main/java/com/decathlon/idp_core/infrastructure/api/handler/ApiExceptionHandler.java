package com.decathlon.idp_core.infrastructure.api.handler;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.decathlon.idp_core.domain.exception.EntityNotFoundException;
import com.decathlon.idp_core.domain.exception.EntityTemplateAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.EntityTemplateNotFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for the API layer that provides centralized error
 * handling
 * across all controllers in the application.
 *
 * <p>
 * This class uses Spring's {@code @ControllerAdvice} annotation to intercept
 * exceptions
 * thrown by controllers and convert them into appropriate HTTP responses with
 * consistent
 * error formatting.
 * </p>
 *
 * <p>
 * The handler covers various types of exceptions including:
 * </p>
 * <ul>
 * <li>Domain-specific exceptions (EntityTemplateNotFoundException,
 * EntityTemplateAlreadyExistsException)</li>
 * <li>Validation exceptions (ConstraintViolationException,
 * MethodArgumentNotValidException)</li>
 * <li>Request parsing exceptions (HttpMessageNotReadableException)</li>
 * <li>Generic exceptions as a fallback</li>
 * </ul>
 *
 * <p>
 * All exceptions are logged appropriately and converted to
 * {@link ErrorResponse} objects
 * with standardized HTTP status codes and user-friendly error messages.
 * </p>
 *
 * @author GitHub Copilot
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@ControllerAdvice
public class ApiExceptionHandler {

    private ApiExceptionHandler() {
    }

    /**
     * Handle TemplateNotFoundException
     *
     * @param ex TemplateNotFoundException
     * @return ErrorResponse with 404 status
     */
    @ExceptionHandler(EntityTemplateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTemplateNotFoundException(EntityTemplateNotFoundException ex) {
        log.warn("Template not found: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.name(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle EntityTemplateAlreadyExistsException
     *
     * @param ex EntityTemplateAlreadyExistsException
     * @return ErrorResponse with 409 status
     */
    @ExceptionHandler(EntityTemplateAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEntityTemplateAlreadyExistsException(
            EntityTemplateAlreadyExistsException ex) {
        log.warn("Entity template already exists: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.CONFLICT.name(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handle ConstraintViolationException (validation errors)
     *
     * @param ex ConstraintViolationException
     * @return ErrorResponse with 400 status
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        log.warn("Validation constraint violation: {}", ex.getMessage());

        String errorMessage = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        return createErrorResponse(HttpStatus.BAD_REQUEST, errorMessage);
    }

    /**
     * Handle MethodArgumentNotValidException (request body validation errors)
     *
     * @param ex MethodArgumentNotValidException
     * @return ErrorResponse with 400 status
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.warn("Method argument validation error: {}", ex.getMessage());

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(org.springframework.context.MessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return createErrorResponse(HttpStatus.BAD_REQUEST, errorMessage);
    }

    /**
     * Handle HttpMessageNotReadableException (JSON parsing errors)
     *
     * @param ex HttpMessageNotReadableException
     * @return ErrorResponse with 400 status
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("HTTP message not readable: {}", ex.getMessage());

        String errorMessage = parseHttpMessageNotReadableError(ex.getMessage());
        return createErrorResponse(HttpStatus.BAD_REQUEST, errorMessage);
    }


    /**
     * Handle generic EntityNotFoundException
     *
     * @param ex EntityNotFoundException
     * @return ErrorResponse with 404 status
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.name(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
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
        return "Cannot deserialize request body property";
    }

    private String parseEnumDeserializationError(String originalMessage) {
        String enumTypeName = getPropertyNameFromEnumType(originalMessage);
        String invalidValue = extractInvalidValue(originalMessage);

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

    private String extractInvalidValue(String message) {
        int valueStart = message.indexOf("from String \"") + 13;
        int valueEnd = message.indexOf("\"", valueStart);
        if (valueStart > 12 && valueEnd > valueStart) {
            return message.substring(valueStart, valueEnd);
        }
        return "";
    }

    /**
     * Handle all other exceptions (default fallback)
     *
     * @param ex Exception
     * @return ErrorResponse with 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        String errorMessage = "An unexpected error occurred. Please try again later.";
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
    }

    @SuppressWarnings("null")
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
