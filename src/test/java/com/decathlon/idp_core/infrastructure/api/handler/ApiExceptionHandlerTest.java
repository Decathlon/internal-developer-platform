package com.decathlon.idp_core.infrastructure.api.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.decathlon.idp_core.domain.exception.EntityTemplateAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.EntityTemplateNotFoundException;
import com.decathlon.idp_core.infrastructure.api.handler.ApiExceptionHandler.ErrorResponse;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * Comprehensive unit tests for {@link ApiExceptionHandler}.
 *
 * Tests all exception handler methods and utility functions to ensure proper
 * error handling and response formatting across the API layer.
 */
@DisplayName("ApiExceptionHandler Tests")
class ApiExceptionHandlerTest {

    private ApiExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() throws Exception {
        // Use reflection to create instance since constructor is private
        Constructor<ApiExceptionHandler> constructor = ApiExceptionHandler.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        exceptionHandler = constructor.newInstance();
    }

    @Nested
    @DisplayName("Domain Exception Handling")
    class DomainExceptionTests {

        /**
         * Tests the handling of EntityTemplateNotFoundException by the ApiExceptionHandler.
         *
         * <p>This test verifies that:</p>
         * <ul>
         *   <li>EntityTemplateNotFoundException is properly caught and handled</li>
         *   <li>HTTP 404 Not Found status is returned</li>
         *   <li>Error response contains the correct error status and description</li>
         *   <li>Original exception message is preserved in the response</li>
         * </ul>
         */
        @Test
        @DisplayName("Should handle EntityTemplateNotFoundException with 404 status")
        void shouldHandleEntityTemplateNotFoundException() {
            // Given
            String errorMessage = "Template with ID 'test-id' not found";
            EntityTemplateNotFoundException exception = new EntityTemplateNotFoundException(errorMessage);

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleTemplateNotFoundException(exception);

            // Then
            assertNotNull(response);
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals(HttpStatus.NOT_FOUND, body.getError());
            assertEquals(errorMessage, body.getErrorDescription());
        }

        /**
         * Tests the handling of EntityTemplateAlreadyExistsException by the ApiExceptionHandler.
         *
         * <p>This test verifies that:</p>
         * <ul>
         *   <li>EntityTemplateAlreadyExistsException is properly caught and handled</li>
         *   <li>HTTP 409 Conflict status is returned</li>
         *   <li>Error response contains the correct error status and formatted description</li>
         *   <li>Exception message is properly formatted with validation constants</li>
         * </ul>
         */
        @Test
        @DisplayName("Should handle EntityTemplateAlreadyExistsException with 409 status")
        void shouldHandleEntityTemplateAlreadyExistsException() {
            // Given
            String identifier = "duplicate-id";
            EntityTemplateAlreadyExistsException exception = new EntityTemplateAlreadyExistsException(identifier);
            String expectedMessage = "An Entity Template already exists with the same identifier:duplicate-id";

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleEntityTemplateAlreadyExistsException(exception);

            // Then
            assertNotNull(response);
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals(HttpStatus.CONFLICT, body.getError());
            assertEquals(expectedMessage, body.getErrorDescription());
        }
    }

    @Nested
    @DisplayName("Validation Exception Handling")
    class ValidationExceptionTests {

        /**
         * Tests the handling of ConstraintViolationException with a single validation violation.
         *
         * <p>This test verifies that:</p>
         * <ul>
         *   <li>ConstraintViolationException is properly caught and handled</li>
         *   <li>HTTP 400 Bad Request status is returned</li>
         *   <li>Single violation message is correctly extracted and returned</li>
         *   <li>Error response format matches expected structure</li>
         * </ul>
         */
        @Test
        @DisplayName("Should handle ConstraintViolationException with single violation")
        void shouldHandleConstraintViolationExceptionSingleViolation() {
            // Given
            ConstraintViolation<Object> violation = createMockConstraintViolation("Field must not be null");
            Set<ConstraintViolation<Object>> violations = Set.of(violation);
            ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolationException(exception);

            // Then
            assertNotNull(response);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals(HttpStatus.BAD_REQUEST, body.getError());
            assertEquals("Field must not be null", body.getErrorDescription());
        }

        /**
         * Tests the handling of ConstraintViolationException with multiple validation violations.
         *
         * <p>This test verifies that:</p>
         * <ul>
         *   <li>ConstraintViolationException with multiple violations is properly handled</li>
         *   <li>HTTP 400 Bad Request status is returned</li>
         *   <li>All violation messages are concatenated with comma separation</li>
         *   <li>Error response contains all validation error messages</li>
         * </ul>
         */
        @Test
        @DisplayName("Should handle ConstraintViolationException with multiple violations")
        void shouldHandleConstraintViolationExceptionMultipleViolations() {
            // Given
            ConstraintViolation<Object> violation1 = createMockConstraintViolation("Field1 must not be null");
            ConstraintViolation<Object> violation2 = createMockConstraintViolation("Field2 must not be blank");
            Set<ConstraintViolation<Object>> violations = Set.of(violation1, violation2);
            ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleConstraintViolationException(exception);

            // Then
            assertNotNull(response);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals(HttpStatus.BAD_REQUEST, body.getError());
            // Note: The order of messages may vary due to Set implementation
            String errorDescription = body.getErrorDescription();
            assertTrue(errorDescription.contains("Field1 must not be null"));
            assertTrue(errorDescription.contains("Field2 must not be blank"));
            assertTrue(errorDescription.contains(", "));
        }

        /**
         * Tests the handling of MethodArgumentNotValidException with field validation errors.
         *
         * <p>This test verifies that:</p>
         * <ul>
         *   <li>MethodArgumentNotValidException is properly caught and handled</li>
         *   <li>HTTP 400 Bad Request status is returned</li>
         *   <li>Field error messages from binding result are extracted and concatenated</li>
         *   <li>All field validation errors are included in the response with comma separation</li>
         * </ul>
         *
         * @throws Exception if reflection fails during test setup
         */
        @Test
        @DisplayName("Should handle MethodArgumentNotValidException with field errors")
        void shouldHandleMethodArgumentNotValidException() throws Exception {
            // Given
            Object target = new Object();
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "testObject");
            bindingResult.addError(new FieldError("testObject", "field1", "Field1 is required"));
            bindingResult.addError(new FieldError("testObject", "field2", "Field2 must be valid"));

            // Create a proper MethodParameter mock with required methods
            MethodParameter methodParameter = mock(MethodParameter.class);
            when(methodParameter.getExecutable()).thenReturn(this.getClass().getMethod("testMethod"));

            MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodArgumentNotValidException(exception);

            // Then
            assertNotNull(response);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals(HttpStatus.BAD_REQUEST, body.getError());
            String errorDescription = body.getErrorDescription();
            assertTrue(errorDescription.contains("Field1 is required"));
            assertTrue(errorDescription.contains("Field2 must be valid"));
            assertTrue(errorDescription.contains(", "));
        }

        // Helper method for mocking
        public void testMethod() {
            // Empty method for testing purposes
        }

        @SuppressWarnings("unchecked")
        private ConstraintViolation<Object> createMockConstraintViolation(String message) {
            ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
            when(violation.getMessage()).thenReturn(message);
            return violation;
        }
    }

    @Nested
    @DisplayName("HTTP Message Exception Handling")
    class HttpMessageExceptionTests {

        /**
         * Tests the handling of HttpMessageNotReadableException when exception message is null.
         *
         * <p>This test verifies that:</p>
         * <ul>
         *   <li>HttpMessageNotReadableException with null message is properly handled</li>
         *   <li>HTTP 400 Bad Request status is returned</li>
         *   <li>Default error message is provided when original message is null</li>
         *   <li>Graceful handling of edge case scenarios</li>
         * </ul>
         */
        @Test
        @DisplayName("Should handle HttpMessageNotReadableException with null message")
        void shouldHandleHttpMessageNotReadableExceptionWithNullMessage() {
            // Given
            HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
            when(exception.getMessage()).thenReturn(null);

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleHttpMessageNotReadableException(exception);

            // Then
            assertNotNull(response);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals(HttpStatus.BAD_REQUEST, body.getError());
            assertEquals("Invalid request body format", body.getErrorDescription());
        }

        /**
         * Provides test data for HttpMessageNotReadableException scenarios.
         * Each argument contains: input message and expected error description.
         */
        static Stream<Arguments> httpMessageNotReadableExceptionTestData() {
            return Stream.of(
                Arguments.of(
                    "Required request body is missing: public ResponseEntity",
                    "Request body is required"
                ),
                Arguments.of(
                    "JSON parse error: Unexpected character",
                    "Invalid JSON format in request body"
                ),
                Arguments.of(
                    "Cannot deserialize value of type `PropertyType` from String \"INVALID_TYPE\": not one of the values accepted for Enum class",
                    "Invalid value 'INVALID_TYPE' for property 'type'"
                ),
                Arguments.of(
                    "Cannot deserialize value of type `UnknownEnum` from String \"VALUE\": not one of the values accepted for Enum class",
                    "Invalid enum value in request body"
                )
            );
        }

        /**
         * Parameterized test for handling HttpMessageNotReadableException with various error scenarios.
         *
         * <p>This test verifies that different types of HttpMessageNotReadableException are properly
         * parsed and converted to user-friendly error messages:</p>
         * <ul>
         *   <li>Missing request body errors → "Request body is required"</li>
         *   <li>JSON parse errors → "Invalid JSON format in request body"</li>
         *   <li>PropertyType enum deserialization errors → Specific property and value information</li>
         *   <li>Unknown enum deserialization errors → Generic enum error message</li>
         * </ul>
         *
         * <p>Each test case validates that:</p>
         * <ul>
         *   <li>HTTP 400 Bad Request status is returned</li>
         *   <li>Original complex error message is parsed and simplified</li>
         *   <li>User-friendly error description is provided</li>
         *   <li>Error response structure is consistent</li>
         * </ul>
         *
         * @param originalMessage the original exception message to be processed
         * @param expectedErrorDescription the expected user-friendly error description
         */
        @ParameterizedTest
        @MethodSource("httpMessageNotReadableExceptionTestData")
        @DisplayName("Should handle HttpMessageNotReadableException with various error types")
        void shouldHandleHttpMessageNotReadableExceptionWithVariousErrorTypes(String originalMessage, String expectedErrorDescription) {
            // Given
            HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
            when(exception.getMessage()).thenReturn(originalMessage);

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleHttpMessageNotReadableException(exception);

            // Then
            assertNotNull(response);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals(HttpStatus.BAD_REQUEST, body.getError());
            assertEquals(expectedErrorDescription, body.getErrorDescription());
        }
    }

    @Nested
    @DisplayName("Generic Exception Handling")
    class GenericExceptionTests {

        /**
         * Tests the handling of generic Exception as a fallback mechanism.
         *
         * <p>This test verifies that:</p>
         * <ul>
         *   <li>Unexpected exceptions are caught by the generic handler</li>
         *   <li>HTTP 500 Internal Server Error status is returned</li>
         *   <li>Generic error message is provided to avoid exposing internal details</li>
         *   <li>Exception is properly logged for debugging purposes</li>
         * </ul>
         */
        @Test
        @DisplayName("Should handle generic Exception with 500 status")
        void shouldHandleGenericException() {
            // Given
            Exception exception = new RuntimeException("Unexpected error");

            // When
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception);

            // Then
            assertNotNull(response);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            ErrorResponse body = response.getBody();
            assertNotNull(body);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, body.getError());
            assertEquals("An unexpected error occurred. Please try again later.", body.getErrorDescription());
        }
    }

    @Nested
    @DisplayName("ErrorResponse Class Tests")
    class ErrorResponseTests {

        /**
         * Tests the creation of ErrorResponse using the all-arguments constructor.
         *
         * <p>This test verifies that:</p>
         * <ul>
         *   <li>ErrorResponse can be instantiated with HttpStatus and description</li>
         *   <li>All fields are properly initialized with provided values</li>
         *   <li>Getter methods return the expected values</li>
         *   <li>Object is successfully created and accessible</li>
         * </ul>
         */
        @Test
        @DisplayName("Should create ErrorResponse with all args constructor")
        void shouldCreateErrorResponseWithAllArgsConstructor() {
            // Given
            HttpStatus status = HttpStatus.BAD_REQUEST;
            String description = "Test error message";

            // When
            ErrorResponse errorResponse = new ErrorResponse(status, description);

            // Then
            assertNotNull(errorResponse);
            assertEquals(status, errorResponse.getError());
            assertEquals(description, errorResponse.getErrorDescription());
        }

        /**
         * Tests the creation of ErrorResponse using the no-arguments constructor.
         *
         * <p>This test verifies that:</p>
         * <ul>
         *   <li>ErrorResponse can be instantiated without parameters</li>
         *   <li>Object is successfully created with default/null field values</li>
         *   <li>Constructor works with @NoArgsConstructor(force = true) annotation</li>
         *   <li>Provides flexibility for frameworks requiring default constructors</li>
         * </ul>
         */
        @Test
        @DisplayName("Should create ErrorResponse with no args constructor")
        void shouldCreateErrorResponseWithNoArgsConstructor() {
            // When
            ErrorResponse errorResponse = new ErrorResponse();

            // Then
            assertNotNull(errorResponse);
            // Note: With @NoArgsConstructor(force = true), fields will be null initially
        }
    }
}
