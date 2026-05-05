package com.decathlon.idp_core.domain.service.entity_template;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import com.decathlon.idp_core.domain.exception.entity_template.PropertyDefinitionRulesConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.PropertyRules;
import com.decathlon.idp_core.domain.model.enums.PropertyFormat;
import com.decathlon.idp_core.domain.model.enums.PropertyType;

@DisplayName("PropertyDefinitionValidationService Tests")
class PropertyDefinitionValidationServiceTest {

    private PropertyDefinitionValidationService propertyDefinitionValidationService;

    @BeforeEach
    void setUp() {
        propertyDefinitionValidationService = new PropertyDefinitionValidationService();
    }

    @Nested
    @DisplayName("STRING Property Type")
    class StringPropertyTypeTests {

        @Test
        @DisplayName("Happy path: STRING with format and max_length rules")
        void testStringWithValidRules() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    PropertyFormat.EMAIL,
                    null,
                    null,
                    255,
                    1,
                    null,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "email",
                    "Email address",
                    PropertyType.STRING,
                    true,
                    rules
            );

            assertDoesNotThrow(() -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property));
        }

        @Test
        @DisplayName("Happy path: STRING with min_length and max_length")
        void testStringWithLengthConstraints() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    null,
                    null,
                    100,
                    10,
                    null,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "description",
                    "A description",
                    PropertyType.STRING,
                    false,
                    rules
            );

            assertDoesNotThrow(() -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property));
        }

        @Test
        @DisplayName("Happy path: STRING with enum_values")
        void testStringWithEnumValues() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    List.of("ACTIVE", "INACTIVE"),
                    null,
                    null,
                    null,
                    null,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "status",
                    "Status",
                    PropertyType.STRING,
                    true,
                    rules
            );

            assertDoesNotThrow(() -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property));
        }

        @Test
        @DisplayName("Happy path: STRING with regex pattern")
        void testStringWithRegex() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    null,
                    "^[a-zA-Z0-9]+$",
                    null,
                    null,
                    null,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "username",
                    "Username",
                    PropertyType.STRING,
                    true,
                    rules
            );

            assertDoesNotThrow(() -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property));
        }

        @Test
        @DisplayName("Error: STRING with numeric max_value rule")
        void testStringRejectsMaxValue() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    100,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "name",
                    "Name",
                    PropertyType.STRING,
                    true,
                    rules
            );

            PropertyDefinitionRulesConflictException ex = assertThrows(
                    PropertyDefinitionRulesConflictException.class,
                    () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
            );
            assertTrue(ex.getMessage().contains("name"));
            assertTrue(ex.getMessage().contains("STRING"));
        }

        @Test
        @DisplayName("Error: STRING with numeric min_value rule")
        void testStringRejectsMinValue() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    0
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "counter",
                    "Counter",
                    PropertyType.STRING,
                    false,
                    rules
            );

            PropertyDefinitionRulesConflictException ex = assertThrows(
                    PropertyDefinitionRulesConflictException.class,
                    () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
            );
            assertTrue(ex.getMessage().contains("counter"));
            assertTrue(ex.getMessage().contains("STRING"));
        }

        @Test
        @DisplayName("Error: STRING with min_length > max_length")
        void testStringWithInvalidLengthConstraints() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    null,
                    null,
                    50,
                    100,
                    null,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "field",
                    "A field",
                    PropertyType.STRING,
                    true,
                    rules
            );

            PropertyDefinitionRulesConflictException ex = assertThrows(
                    PropertyDefinitionRulesConflictException.class,
                    () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
            );
            assertTrue(ex.getMessage().contains("min_length"));
            assertTrue(ex.getMessage().contains("max_length"));
        }

        @Test
        @DisplayName("Error: STRING with negative min_length")
        void testStringWithNegativeMinLength() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    null,
                    null,
                    255,
                    -1,
                    null,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "field",
                    "A field",
                    PropertyType.STRING,
                    true,
                    rules
            );

            PropertyDefinitionRulesConflictException ex = assertThrows(
                    PropertyDefinitionRulesConflictException.class,
                    () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
            );
            assertTrue(ex.getMessage().contains("min_length"));
            assertTrue(ex.getMessage().contains("0"));
        }

        @Test
        @DisplayName("Error: STRING with invalid regex pattern")
        void testStringWithInvalidRegexPattern() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    null,
                    "[invalid-regex",
                    255,
                    null,
                    null,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "field",
                    "A field",
                    PropertyType.STRING,
                    true,
                    rules
            );

            PropertyDefinitionRulesConflictException ex = assertThrows(
                    PropertyDefinitionRulesConflictException.class,
                    () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
            );
            assertTrue(ex.getMessage().contains("regex"));
            assertTrue(ex.getMessage().contains("[invalid-regex"));
        }

        @Test
        @DisplayName("Happy path: STRING with null rules")
        void testStringWithNullRules() {
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "field",
                    "A field",
                    PropertyType.STRING,
                    true,
                    null
            );

            assertDoesNotThrow(() -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property));
        }

        @Test
        @DisplayName("Happy path: STRING with min_length = 0 and max_length > 0")
        void testStringWithZeroMinLength() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    null,
                    null,
                    100,
                    0,
                    null,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "optional_field",
                    "An optional field",
                    PropertyType.STRING,
                    false,
                    rules
            );

            assertDoesNotThrow(() -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property));
        }

        @Test
        @DisplayName("Error: STRING with max_length <= 0")
        void testStringWithNonPositiveMaxLength() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    null,
                    null,
                    0,
                    null,
                    null,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "field",
                    "A field",
                    PropertyType.STRING,
                    true,
                    rules
            );

            PropertyDefinitionRulesConflictException ex = assertThrows(
                    PropertyDefinitionRulesConflictException.class,
                    () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
            );
            assertTrue(ex.getMessage().contains("max_length"));
            assertTrue(ex.getMessage().contains("greater than 0"));
        }

        @Test
        @DisplayName("Error: STRING with format and enum_values combined")
        void testStringRejectsFormatWithEnumValues() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    PropertyFormat.EMAIL,
                    List.of("EMAIL", "POSTAL_CODE"),
                    null,
                    null,
                    null,
                    null,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "contact",
                    "Contact field",
                    PropertyType.STRING,
                    true,
                    rules
            );

            PropertyDefinitionRulesConflictException ex = assertThrows(
                    PropertyDefinitionRulesConflictException.class,
                    () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
            );
            assertTrue(ex.getMessage().contains("format"));
            assertTrue(ex.getMessage().contains("enum_values"));
        }

        @Test
        @DisplayName("Error: STRING with format and regex combined")
        void testStringRejectsFormatWithRegex() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    PropertyFormat.EMAIL,
                    null,
                    "^[a-zA-Z]+$",
                    null,
                    null,
                    null,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "contact",
                    "Contact field",
                    PropertyType.STRING,
                    true,
                    rules
            );

            PropertyDefinitionRulesConflictException ex = assertThrows(
                    PropertyDefinitionRulesConflictException.class,
                    () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
            );
            assertTrue(ex.getMessage().contains("format"));
            assertTrue(ex.getMessage().contains("regex"));
        }

        @Test
        @DisplayName("Error: STRING with regex and enum_values combined")
        void testStringRejectsRegexWithEnumValues() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    List.of("ACTIVE", "INACTIVE"),
                    "^[A-Z]+$",
                    null,
                    null,
                    null,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "status",
                    "Status field",
                    PropertyType.STRING,
                    true,
                    rules
            );

            PropertyDefinitionRulesConflictException ex = assertThrows(
                    PropertyDefinitionRulesConflictException.class,
                    () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
            );
            assertTrue(ex.getMessage().contains("regex"));
            assertTrue(ex.getMessage().contains("enum_values"));
        }

        @Test
        @DisplayName("Error: STRING with enum_values and max_length combined")
        void testStringRejectsEnumValuesWithMaxLength() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    List.of("EMAIL", "POSTAL_CODE"),
                    null,
                    12,
                    null,
                    null,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "contact_type",
                    "Contact type field",
                    PropertyType.STRING,
                    true,
                    rules
            );

            PropertyDefinitionRulesConflictException ex = assertThrows(
                    PropertyDefinitionRulesConflictException.class,
                    () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
            );
            assertTrue(ex.getMessage().contains("enum_values"));
            assertTrue(ex.getMessage().contains("max_length"));
        }

        @Test
        @DisplayName("Error: STRING with enum_values and min_length combined")
        void testStringRejectsEnumValuesWithMinLength() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    List.of("EMAIL", "POSTAL_CODE"),
                    null,
                    null,
                    3,
                    null,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "contact_type",
                    "Contact type field",
                    PropertyType.STRING,
                    true,
                    rules
            );

            PropertyDefinitionRulesConflictException ex = assertThrows(
                    PropertyDefinitionRulesConflictException.class,
                    () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
            );
            assertTrue(ex.getMessage().contains("enum_values"));
            assertTrue(ex.getMessage().contains("min_length"));
        }

        @Nested
        @DisplayName("Regex Validation Security Tests")
        class RegexSecurityTests {

            @Test
            @DisplayName("Happy path: STRING with simple valid regex")
            void testRegexWithSimpleValidPattern() {
                PropertyRules rules = new PropertyRules(
                        UUID.randomUUID(),
                        null,
                        null,
                        "^[a-z0-9]+@[a-z0-9]+\\.[a-z]{2,}$",
                        null,
                        null,
                        null,
                        null
                );
                PropertyDefinition property = new PropertyDefinition(
                        UUID.randomUUID(),
                        "email",
                        "Email field",
                        PropertyType.STRING,
                        true,
                        rules
                );

                assertDoesNotThrow(() -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property));
            }

            @Test
            @DisplayName("Error: Regex pattern exceeds maximum length (1000 chars)")
            void testRegexPatternTooLong() {
                String longPattern = "a".repeat(1001);
                PropertyRules rules = new PropertyRules(
                        UUID.randomUUID(),
                        null,
                        null,
                        longPattern,
                        null,
                        null,
                        null,
                        null
                );
                PropertyDefinition property = new PropertyDefinition(
                        UUID.randomUUID(),
                        "field",
                        "A field",
                        PropertyType.STRING,
                        true,
                        rules
                );

                PropertyDefinitionRulesConflictException ex = assertThrows(
                        PropertyDefinitionRulesConflictException.class,
                        () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
                );
                assertTrue(ex.getMessage().contains("too long"));
                assertTrue(ex.getMessage().contains("1,000"));
            }

            @ParameterizedTest
            @ValueSource(strings = {
                    "(a+)+",           // nested quantifiers with +
                    "(a*)*",           // nested quantifiers with *
                    "(a+)*",           // mixed nested quantifiers
                    "(a|b)+",          // quantified alternation with +
                    "(foo|bar)*",      // quantified alternation with *
                    "a{1,5000}"        // excessive repetition bound
            })
            @DisplayName("Error: Regex patterns with ReDoS vulnerabilities")
            void testRegexWithDangerousPatterns(String dangerousPattern) {
                PropertyRules rules = new PropertyRules(
                        UUID.randomUUID(),
                        null,
                        null,
                        dangerousPattern,
                        null,
                        null,
                        null,
                        null
                );
                PropertyDefinition property = new PropertyDefinition(
                        UUID.randomUUID(),
                        "field",
                        "A field",
                        PropertyType.STRING,
                        true,
                        rules
                );

                PropertyDefinitionRulesConflictException ex = assertThrows(
                        PropertyDefinitionRulesConflictException.class,
                        () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
                );
                assertTrue(ex.getMessage().contains("unsafe"),
                        "Expected 'unsafe' in error message for pattern: " + dangerousPattern);
            }

            @Test
            @DisplayName("Happy path: Regex with safe alternation (not quantified)")
            void testRegexWithSafeAlternation() {
                PropertyRules rules = new PropertyRules(
                        UUID.randomUUID(),
                        null,
                        null,
                        "^(foo|bar)$",
                        null,
                        null,
                        null,
                        null
                );
                PropertyDefinition property = new PropertyDefinition(
                        UUID.randomUUID(),
                        "field",
                        "A field",
                        PropertyType.STRING,
                        true,
                        rules
                );

                assertDoesNotThrow(() -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property));
            }

            @Test
            @DisplayName("Happy path: Regex with safe repetition bounds")
            void testRegexWithSafeRepetitionBounds() {
                PropertyRules rules = new PropertyRules(
                        UUID.randomUUID(),
                        null,
                        null,
                        "a{1,999}",
                        null,
                        null,
                        null,
                        null
                );
                PropertyDefinition property = new PropertyDefinition(
                        UUID.randomUUID(),
                        "field",
                        "A field",
                        PropertyType.STRING,
                        true,
                        rules
                );

                assertDoesNotThrow(() -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property));
            }

            @Test
            @DisplayName("Error: Regex with invalid syntax")
            void testRegexWithInvalidSyntax() {
                PropertyRules rules = new PropertyRules(
                        UUID.randomUUID(),
                        null,
                        null,
                        "[unclosed-bracket",
                        null,
                        null,
                        null,
                        null
                );
                PropertyDefinition property = new PropertyDefinition(
                        UUID.randomUUID(),
                        "field",
                        "A field",
                        PropertyType.STRING,
                        true,
                        rules
                );

                PropertyDefinitionRulesConflictException ex = assertThrows(
                        PropertyDefinitionRulesConflictException.class,
                        () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
                );
                assertTrue(ex.getMessage().contains("Invalid regex"));
            }
        }
    }

    @Nested
    @DisplayName("NUMBER Property Type")
    class NumberPropertyTypeTests {

        @Test
        @DisplayName("Happy path: NUMBER with min_value and max_value")
        void testNumberWithValidRules() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    1000,
                    0
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "score",
                    "Numeric score",
                    PropertyType.NUMBER,
                    true,
                    rules
            );

            assertDoesNotThrow(() -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property));
        }

        @Test
        @DisplayName("Happy path: NUMBER with only max_value")
        void testNumberWithOnlyMaxValue() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    100,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "percentage",
                    "Percentage value",
                    PropertyType.NUMBER,
                    false,
                    rules
            );

            assertDoesNotThrow(() -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property));
        }

        @Test
        @DisplayName("Error: NUMBER with format rule")
        void testNumberRejectsFormat() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    PropertyFormat.EMAIL,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "value",
                    "Numeric value",
                    PropertyType.NUMBER,
                    true,
                    rules
            );

            PropertyDefinitionRulesConflictException ex = assertThrows(
                    PropertyDefinitionRulesConflictException.class,
                    () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
            );
            assertTrue(ex.getMessage().contains("value"));
            assertTrue(ex.getMessage().contains("NUMBER"));
            assertTrue(ex.getMessage().contains("format"));
        }

        @Test
        @DisplayName("Error: NUMBER with enum_values rule")
        void testNumberRejectsEnumValues() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    List.of("1", "2", "3"),
                    null,
                    null,
                    null,
                    null,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "category",
                    "Category",
                    PropertyType.NUMBER,
                    true,
                    rules
            );

            PropertyDefinitionRulesConflictException ex = assertThrows(
                    PropertyDefinitionRulesConflictException.class,
                    () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
            );
            assertTrue(ex.getMessage().contains("enum_values"));
        }

        @Test
        @DisplayName("Error: NUMBER with regex rule")
        void testNumberRejectsRegex() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    null,
                    "^[0-9]+$",
                    null,
                    null,
                    null,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "id",
                    "ID",
                    PropertyType.NUMBER,
                    true,
                    rules
            );

            PropertyDefinitionRulesConflictException ex = assertThrows(
                    PropertyDefinitionRulesConflictException.class,
                    () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
            );
            assertTrue(ex.getMessage().contains("regex"));
        }

        @Test
        @DisplayName("Error: NUMBER with min_length rule")
        void testNumberRejectsMinLength() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    null,
                    null,
                    null,
                    5,
                    null,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "field",
                    "A field",
                    PropertyType.NUMBER,
                    true,
                    rules
            );

            PropertyDefinitionRulesConflictException ex = assertThrows(
                    PropertyDefinitionRulesConflictException.class,
                    () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
            );
            assertTrue(ex.getMessage().contains("min_length"));
        }

        @Test
        @DisplayName("Error: NUMBER with max_length rule")
        void testNumberRejectsMaxLength() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    null,
                    null,
                    50,
                    null,
                    null,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "field",
                    "A field",
                    PropertyType.NUMBER,
                    true,
                    rules
            );

            PropertyDefinitionRulesConflictException ex = assertThrows(
                    PropertyDefinitionRulesConflictException.class,
                    () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
            );
            assertTrue(ex.getMessage().contains("max_length"));
        }

        @Test
        @DisplayName("Error: NUMBER with min_value > max_value")
        void testNumberWithInvalidValueConstraints() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    0,
                    100
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "range",
                    "A range",
                    PropertyType.NUMBER,
                    true,
                    rules
            );

            PropertyDefinitionRulesConflictException ex = assertThrows(
                    PropertyDefinitionRulesConflictException.class,
                    () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
            );
            assertTrue(ex.getMessage().contains("min_value"));
            assertTrue(ex.getMessage().contains("max_value"));
        }

        @Test
        @DisplayName("Happy path: NUMBER with only min_value")
        void testNumberWithOnlyMinValue() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    10
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "minimum_age",
                    "Minimum age",
                    PropertyType.NUMBER,
                    false,
                    rules
            );

            assertDoesNotThrow(() -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property));
        }

        @Test
        @DisplayName("Happy path: NUMBER with negative min_value and max_value")
        void testNumberWithNegativeValues() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    100,
                    -100
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "temperature",
                    "Temperature",
                    PropertyType.NUMBER,
                    false,
                    rules
            );

            assertDoesNotThrow(() -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property));
        }

        @Test
        @DisplayName("Happy path: NUMBER with null rules")
        void testNumberWithNullRules() {
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "count",
                    "A count",
                    PropertyType.NUMBER,
                    true,
                    null
            );

            assertDoesNotThrow(() -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property));
        }
    }

    @Nested
    @DisplayName("BOOLEAN Property Type")
    class BooleanPropertyTypeTests {

        @Test
        @DisplayName("Happy path: BOOLEAN with no rules")
        void testBooleanWithNullRules() {
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "active",
                    "Is active",
                    PropertyType.BOOLEAN,
                    true,
                    null
            );

            assertDoesNotThrow(() -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property));
        }

        @Test
        @DisplayName("Error: BOOLEAN with format rule")
        void testBooleanRejectsFormat() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    PropertyFormat.EMAIL,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "enabled",
                    "Enabled",
                    PropertyType.BOOLEAN,
                    true,
                    rules
            );

            PropertyDefinitionRulesConflictException ex = assertThrows(
                    PropertyDefinitionRulesConflictException.class,
                    () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
            );
            assertTrue(ex.getMessage().contains("BOOLEAN"));
            assertTrue(ex.getMessage().contains("rules"));
        }

        @Test
        @DisplayName("Error: BOOLEAN with enum_values rule")
        void testBooleanRejectsEnumValues() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    List.of("true", "false"),
                    null,
                    null,
                    null,
                    null,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "flag",
                    "A flag",
                    PropertyType.BOOLEAN,
                    true,
                    rules
            );

            PropertyDefinitionRulesConflictException ex = assertThrows(
                    PropertyDefinitionRulesConflictException.class,
                    () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
            );
            assertTrue(ex.getMessage().contains("BOOLEAN"));
        }

        @Test
        @DisplayName("Error: BOOLEAN with regex rule")
        void testBooleanRejectsRegex() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    null,
                    ".*",
                    null,
                    null,
                    null,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "test",
                    "Test",
                    PropertyType.BOOLEAN,
                    true,
                    rules
            );

            PropertyDefinitionRulesConflictException ex = assertThrows(
                    PropertyDefinitionRulesConflictException.class,
                    () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
            );
            assertTrue(ex.getMessage().contains("BOOLEAN"));
        }

        @Test
        @DisplayName("Error: BOOLEAN with min_value rule")
        void testBooleanRejectsMinValue() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    0
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "valid",
                    "Valid",
                    PropertyType.BOOLEAN,
                    true,
                    rules
            );

            PropertyDefinitionRulesConflictException ex = assertThrows(
                    PropertyDefinitionRulesConflictException.class,
                    () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
            );
            assertTrue(ex.getMessage().contains("BOOLEAN"));
        }

        @Test
        @DisplayName("Error: BOOLEAN with max_value rule")
        void testBooleanRejectsMaxValue() {
            PropertyRules rules = new PropertyRules(
                    UUID.randomUUID(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    1,
                    null
            );
            PropertyDefinition property = new PropertyDefinition(
                    UUID.randomUUID(),
                    "valid",
                    "Valid",
                    PropertyType.BOOLEAN,
                    true,
                    rules
            );

            PropertyDefinitionRulesConflictException ex = assertThrows(
                    PropertyDefinitionRulesConflictException.class,
                    () -> propertyDefinitionValidationService.validatePropertyDefinitionRules(property)
            );
            assertTrue(ex.getMessage().contains("BOOLEAN"));
        }
    }
}
