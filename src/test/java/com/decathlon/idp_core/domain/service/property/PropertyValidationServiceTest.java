package com.decathlon.idp_core.domain.service.property;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.decathlon.idp_core.domain.constant.ValidationMessages;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.PropertyRules;
import com.decathlon.idp_core.domain.model.enums.PropertyFormat;
import com.decathlon.idp_core.domain.model.enums.PropertyType;

@DisplayName("PropertyValidationService Tests")
class PropertyValidationServiceTest {

    private final PropertyValidationService service = new PropertyValidationService();

    @Nested
    @DisplayName("STRING validation")
    class StringValidationTests {

        @Test
        @DisplayName("Should report type mismatch when STRING value is null")
        void shouldReportTypeMismatchWhenStringValueIsNull() {
            var definition = propertyDefinition("label", PropertyType.STRING, null);

            var violations = service.validatePropertyValue(definition, null, null);

            assertEquals(List.of(ValidationMessages.PROPERTY_TYPE_MISMATCH.formatted("label", PropertyType.STRING)), violations);
        }

        @Test
        @DisplayName("Should return no violations when STRING has no rules")
        void shouldReturnNoViolationsWhenStringHasNoRules() {
            var definition = propertyDefinition("label", PropertyType.STRING, null);

            var violations = service.validatePropertyValue(definition, "hello", "hello");

            assertEquals(List.of(), violations);
        }

        @Test
        @DisplayName("Should return no violations when STRING value satisfies all rules")
        void shouldReturnNoViolationsWhenStringPassesAllRules() {
            var rules = new PropertyRules(null, null, List.of("dev", "prod"), "^[a-z]+$", 10, 2, null, null);
            var definition = propertyDefinition("env", PropertyType.STRING, rules);

            var violations = service.validatePropertyValue(definition, "dev", "dev");

            assertEquals(List.of(), violations);
        }

        @Test
        @DisplayName("Should report minLength violation")
        void shouldReportMinLengthViolation() {
            var rules = new PropertyRules(null, null, null, null, null, 5, null, null);
            var definition = propertyDefinition("name", PropertyType.STRING, rules);

            var violations = service.validatePropertyValue(definition, "ab", "ab");

            assertEquals(List.of(ValidationMessages.PROPERTY_MIN_LENGTH_VIOLATION.formatted("name", 5)), violations);
        }

        @Test
        @DisplayName("Should report maxLength violation")
        void shouldReportMaxLengthViolation() {
            var rules = new PropertyRules(null, null, null, null, 5, null, null, null);
            var definition = propertyDefinition("name", PropertyType.STRING, rules);

            var violations = service.validatePropertyValue(definition, "too-long-value", "too-long-value");

            assertEquals(List.of(ValidationMessages.PROPERTY_MAX_LENGTH_VIOLATION.formatted("name", 5)), violations);
        }

        @Test
        @DisplayName("Should report regex violation")
        void shouldReportRegexViolation() {
            var rules = new PropertyRules(null, null, null, "^[0-9]+$", null, null, null, null);
            var definition = propertyDefinition("code", PropertyType.STRING, rules);

            var violations = service.validatePropertyValue(definition, "abc", "abc");

            assertEquals(List.of(ValidationMessages.PROPERTY_REGEX_VIOLATION.formatted("code")), violations);
        }

        @Test
        @DisplayName("Should accept value matching regex")
        void shouldAcceptValueMatchingRegex() {
            var rules = new PropertyRules(null, null, null, "^[0-9]+$", null, null, null, null);
            var definition = propertyDefinition("code", PropertyType.STRING, rules);

            var violations = service.validatePropertyValue(definition, "12345", "12345");

            assertEquals(List.of(), violations);
        }

        @Test
        @DisplayName("Should report enum violation when value not in allowed list")
        void shouldReportEnumViolation() {
            var rules = new PropertyRules(null, null, List.of("ACTIVE", "INACTIVE"), null, null, null, null, null);
            var definition = propertyDefinition("status", PropertyType.STRING, rules);

            var violations = service.validatePropertyValue(definition, "UNKNOWN", "UNKNOWN");

            assertEquals(List.of(ValidationMessages.PROPERTY_ENUM_VIOLATION.formatted("status", List.of("ACTIVE", "INACTIVE"))), violations);
        }

        @Test
        @DisplayName("Should accept enum value with case-insensitive match")
        void shouldAcceptEnumValueCaseInsensitive() {
            var rules = new PropertyRules(null, null, List.of("ACTIVE", "INACTIVE"), null, null, null, null, null);
            var definition = propertyDefinition("status", PropertyType.STRING, rules);

            var violations = service.validatePropertyValue(definition, "active", "active");

            assertEquals(List.of(), violations);
        }

        @Test
        @DisplayName("Should skip enum check when enumValues is empty")
        void shouldSkipEnumCheckWhenEnumValuesIsEmpty() {
            var rules = new PropertyRules(null, null, List.of(), null, null, null, null, null);
            var definition = propertyDefinition("status", PropertyType.STRING, rules);

            var violations = service.validatePropertyValue(definition, "anything", "anything");

            assertEquals(List.of(), violations);
        }

        @Test
        @DisplayName("Should report format violation for invalid EMAIL")
        void shouldReportFormatViolationForInvalidEmail() {
            var rules = new PropertyRules(null, PropertyFormat.EMAIL, null, null, null, null, null, null);
            var definition = propertyDefinition("email", PropertyType.STRING, rules);

            var violations = service.validatePropertyValue(definition, "not-an-email", "not-an-email");

            assertEquals(List.of(ValidationMessages.PROPERTY_FORMAT_VIOLATION.formatted("email", PropertyFormat.EMAIL)), violations);
        }

        @Test
        @DisplayName("Should accept valid EMAIL format")
        void shouldAcceptValidEmailFormat() {
            var rules = new PropertyRules(null, PropertyFormat.EMAIL, null, null, null, null, null, null);
            var definition = propertyDefinition("email", PropertyType.STRING, rules);

            var violations = service.validatePropertyValue(definition, "user@example.com", "user@example.com");

            assertEquals(List.of(), violations);
        }

        @Test
        @DisplayName("Should report format violation for invalid URL")
        void shouldReportFormatViolationForInvalidUrl() {
            var rules = new PropertyRules(null, PropertyFormat.URL, null, null, null, null, null, null);
            var definition = propertyDefinition("url", PropertyType.STRING, rules);

            var violations = service.validatePropertyValue(definition, "not-a-url", "not-a-url");

            assertEquals(List.of(ValidationMessages.PROPERTY_FORMAT_VIOLATION.formatted("url", PropertyFormat.URL)), violations);
        }

        @Test
        @DisplayName("Should accept valid URL format")
        void shouldAcceptValidUrlFormat() {
            var rules = new PropertyRules(null, PropertyFormat.URL, null, null, null, null, null, null);
            var definition = propertyDefinition("url", PropertyType.STRING, rules);

            var violations = service.validatePropertyValue(definition, "https://github.com/org/repo", "https://github.com/org/repo");

            assertEquals(List.of(), violations);
        }

        @Test
        @DisplayName("Should report multiple violations at once")
        void shouldReportMultipleStringViolations() {
            var rules = new PropertyRules(null, PropertyFormat.EMAIL, List.of("prod", "dev"), "^[a-z]+$", 5, 3, null, null);
            var definition = propertyDefinition("name", PropertyType.STRING, rules);

            var violations = service.validatePropertyValue(definition, "AA", "AA");

            assertEquals(4, violations.size());
        }

        @Test
        @DisplayName("Should use cached pattern for repeated regex validations")
        void shouldUseCachedPatternForRepeatedRegex() {
            var rules = new PropertyRules(null, null, null, "^[a-z]+$", null, null, null, null);
            var definition = propertyDefinition("code", PropertyType.STRING, rules);

            // Validate twice with the same regex to exercise the cache
            var violations1 = service.validatePropertyValue(definition, "abc", "abc");
            var violations2 = service.validatePropertyValue(definition, "def", "def");

            assertEquals(List.of(), violations1);
            assertEquals(List.of(), violations2);
        }

        @Test
        @DisplayName("Should report type mismatch when a number is sent for a STRING property")
        void shouldReportTypeMismatchWhenNumberSentForString() {
            var rules = new PropertyRules(null, null, null, null, null, 5, null, null);
            var definition = propertyDefinition("label", PropertyType.STRING, rules);

            var violations = service.validatePropertyValue(definition, "12", 12);

            assertEquals(List.of(ValidationMessages.PROPERTY_TYPE_MISMATCH.formatted("label", PropertyType.STRING)), violations);
        }

        @Test
        @DisplayName("Should report type mismatch when a boolean is sent for a STRING property")
        void shouldReportTypeMismatchWhenBooleanSentForString() {
            var definition = propertyDefinition("label", PropertyType.STRING, null);

            var violations = service.validatePropertyValue(definition, "true", true);

            assertEquals(List.of(ValidationMessages.PROPERTY_TYPE_MISMATCH.formatted("label", PropertyType.STRING)), violations);
        }
    }

    @Nested
    @DisplayName("NUMBER validation")
    class NumberValidationTests {

        @Test
        @DisplayName("Should report type mismatch for non-numeric NUMBER value")
        void shouldReportTypeMismatchWhenNumberValueIsInvalid() {
            var definition = propertyDefinition("score", PropertyType.NUMBER, null);

            var violations = service.validatePropertyValue(definition, "not-a-number", "not-a-number");

            assertEquals(List.of(ValidationMessages.PROPERTY_TYPE_MISMATCH.formatted("score", PropertyType.NUMBER)), violations);
        }

        @Test
        @DisplayName("Should return no violations when NUMBER has no rules")
        void shouldReturnNoViolationsWhenNumberHasNoRules() {
            var definition = propertyDefinition("count", PropertyType.NUMBER, null);

            var violations = service.validatePropertyValue(definition, "42", 42);

            assertEquals(List.of(), violations);
        }

        @Test
        @DisplayName("Should return no violations when NUMBER is within bounds")
        void shouldReturnNoViolationsWhenNumberIsWithinBounds() {
            var rules = new PropertyRules(null, null, null, null, null, null, 100, 0);
            var definition = propertyDefinition("score", PropertyType.NUMBER, rules);

            var violations = service.validatePropertyValue(definition, "50", 50);

            assertEquals(List.of(), violations);
        }

        @Test
        @DisplayName("Should report minValue violation")
        void shouldReportMinValueViolation() {
            var rules = new PropertyRules(null, null, null, null, null, null, 10, 5);
            var definition = propertyDefinition("size", PropertyType.NUMBER, rules);

            var violations = service.validatePropertyValue(definition, "3", 3);

            assertEquals(List.of(ValidationMessages.PROPERTY_MIN_VALUE_VIOLATION.formatted("size", 5)), violations);
        }

        @Test
        @DisplayName("Should report maxValue violation")
        void shouldReportMaxValueViolation() {
            var rules = new PropertyRules(null, null, null, null, null, null, 10, 0);
            var definition = propertyDefinition("size", PropertyType.NUMBER, rules);

            var violations = service.validatePropertyValue(definition, "15", 15);

            assertEquals(List.of(ValidationMessages.PROPERTY_MAX_VALUE_VIOLATION.formatted("size", 10)), violations);
        }

        @Test
        @DisplayName("Should report both minValue and maxValue violations")
        void shouldReportBothMinAndMaxViolations() {
            // minValue > maxValue edge case — value below min triggers min violation
            var rules = new PropertyRules(null, null, null, null, null, null, 5, 10);
            var definition = propertyDefinition("range", PropertyType.NUMBER, rules);

            var violations = service.validatePropertyValue(definition, "7", 7);

            // 7 < 10 (minValue) → min violation; 7 > 5 (maxValue) → max violation
            assertEquals(2, violations.size());
        }

        @Test
        @DisplayName("Should accept decimal number values")
        void shouldAcceptDecimalNumberValues() {
            var rules = new PropertyRules(null, null, null, null, null, null, 100, 0);
            var definition = propertyDefinition("rate", PropertyType.NUMBER, rules);

            var violations = service.validatePropertyValue(definition, "99.5", 99.5);

            assertEquals(List.of(), violations);
        }

        @Test
        @DisplayName("Should report type mismatch when a boolean is sent for a NUMBER property")
        void shouldReportTypeMismatchWhenBooleanSentForNumber() {
            var definition = propertyDefinition("count", PropertyType.NUMBER, null);

            var violations = service.validatePropertyValue(definition, "true", true);

            assertEquals(List.of(ValidationMessages.PROPERTY_TYPE_MISMATCH.formatted("count", PropertyType.NUMBER)), violations);
        }
    }

    @Nested
    @DisplayName("BOOLEAN validation")
    class BooleanValidationTests {

        @ParameterizedTest(name = "Should accept valid boolean value: ''{0}''")
        @ValueSource(strings = {"true", "false", "TRUE", "FALSE"})
        void shouldAcceptValidBooleanValues(String value) {
            var definition = propertyDefinition("flag", PropertyType.BOOLEAN, null);
            Object originalValue = "true".equalsIgnoreCase(value) ? Boolean.TRUE : Boolean.FALSE;

            var violations = service.validatePropertyValue(definition, value, originalValue);

            assertEquals(List.of(), violations);
        }

        @Test
        @DisplayName("Should report type mismatch for invalid boolean value")
        void shouldReportTypeMismatchForInvalidBoolean() {
            var definition = propertyDefinition("flag", PropertyType.BOOLEAN, null);

            var violations = service.validatePropertyValue(definition, "yes", "yes");

            assertEquals(List.of(ValidationMessages.PROPERTY_TYPE_MISMATCH.formatted("flag", PropertyType.BOOLEAN)), violations);
        }

        @Test
        @DisplayName("Should report type mismatch when a number is sent for a BOOLEAN property")
        void shouldReportTypeMismatchWhenNumberSentForBoolean() {
            var definition = propertyDefinition("flag", PropertyType.BOOLEAN, null);

            var violations = service.validatePropertyValue(definition, "42", 42);

            assertEquals(List.of(ValidationMessages.PROPERTY_TYPE_MISMATCH.formatted("flag", PropertyType.BOOLEAN)), violations);
        }
    }

    private PropertyDefinition propertyDefinition(String name, PropertyType type, PropertyRules rules) {
        return new PropertyDefinition(null, name, "description", type, true, rules);
    }
}
