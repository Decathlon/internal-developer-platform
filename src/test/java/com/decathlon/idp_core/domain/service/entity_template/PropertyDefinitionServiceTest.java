package com.decathlon.idp_core.domain.service.entity_template;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.exception.PropertyNameAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.UnsafeTypeConversionException;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.enums.PropertyType;

@DisplayName("PropertyDefinitionService Tests")
class PropertyDefinitionServiceTest {

    private PropertyDefinitionService propertyDefinitionService;

    @BeforeEach
    void setUp() {
        propertyDefinitionService = new PropertyDefinitionService();
    }
    @Nested
    @DisplayName("validateUniquePropertyNames")
    class ValidateUniquePropertyNamesTests {

        @Test
        @DisplayName("Happy path: all property names are unique")
        void testUniquePropertyNames() {
            List<PropertyDefinition> properties = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "name", "Name", PropertyType.STRING, true, null),
                    new PropertyDefinition(UUID.randomUUID(), "age", "Age", PropertyType.NUMBER, false, null),
                    new PropertyDefinition(UUID.randomUUID(), "active", "Active", PropertyType.BOOLEAN, true, null)
            );

            assertDoesNotThrow(() -> propertyDefinitionService.validateUniquePropertyNames(properties));
        }

        @Test
        @DisplayName("Happy path: null property list")
        void testNullPropertyList() {
            assertDoesNotThrow(() -> propertyDefinitionService.validateUniquePropertyNames(null));
        }

        @Test
        @DisplayName("Happy path: empty property list")
        void testEmptyPropertyList() {
            assertDoesNotThrow(() -> propertyDefinitionService.validateUniquePropertyNames(new ArrayList<>()));
        }

        @Test
        @DisplayName("Error: duplicate property names")
        void testDuplicatePropertyNames() {
            List<PropertyDefinition> properties = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "email", "Email", PropertyType.STRING, true, null),
                    new PropertyDefinition(UUID.randomUUID(), "email", "Alternative Email", PropertyType.STRING, false, null)
            );

            PropertyNameAlreadyExistsException ex = assertThrows(
                    PropertyNameAlreadyExistsException.class,
                    () -> propertyDefinitionService.validateUniquePropertyNames(properties)
            );
            assertTrue(ex.getMessage().contains("email"));
        }

        @Test
        @DisplayName("Error: multiple duplicates detected")
        void testMultipleDuplicates() {
            List<PropertyDefinition> properties = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "name", "Name", PropertyType.STRING, true, null),
                    new PropertyDefinition(UUID.randomUUID(), "name", "Duplicate 1", PropertyType.STRING, false, null),
                    new PropertyDefinition(UUID.randomUUID(), "email", "Email", PropertyType.STRING, true, null),
                    new PropertyDefinition(UUID.randomUUID(), "email", "Duplicate Email", PropertyType.STRING, false, null)
            );

            PropertyNameAlreadyExistsException ex = assertThrows(
                    PropertyNameAlreadyExistsException.class,
                    () -> propertyDefinitionService.validateUniquePropertyNames(properties)
            );
            // Should fail on first duplicate found
            assertTrue(ex.getMessage().contains("name"));
        }
    }

    @Nested
    @DisplayName("validateTypeChanges")
    class ValidateTypeChangesTests {

        @Test
        @DisplayName("Happy path: no existing properties")
        void testNoExistingProperties() {
            List<PropertyDefinition> updated = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "name", "Name", PropertyType.STRING, true, null)
            );

            assertDoesNotThrow(() -> propertyDefinitionService.validateTypeChanges(null, updated, "template-1"));
        }

        @Test
        @DisplayName("Happy path: no type changes")
        void testNoTypeChanges() {
            UUID propertyId = UUID.randomUUID();
            List<PropertyDefinition> existing = List.of(
                    new PropertyDefinition(propertyId, "name", "Name", PropertyType.STRING, true, null)
            );
            List<PropertyDefinition> updated = List.of(
                    new PropertyDefinition(propertyId, "name", "Updated Name", PropertyType.STRING, false, null)
            );

            assertDoesNotThrow(() -> propertyDefinitionService.validateTypeChanges(existing, updated, "template-1"));
        }

        @Test
        @DisplayName("Error: conversion NUMBER to STRING is forbidden")
        void testConversionNumberToStringForbidden() {
            List<PropertyDefinition> existing = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "age", "Age", PropertyType.NUMBER, true, null)
            );
            List<PropertyDefinition> updated = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "age", "Age", PropertyType.STRING, true, null)
            );

            UnsafeTypeConversionException ex = assertThrows(
                    UnsafeTypeConversionException.class,
                    () -> propertyDefinitionService.validateTypeChanges(existing, updated, "template-1")
            );
            assertTrue(ex.getMessage().contains("age"));
            assertTrue(ex.getMessage().contains("NUMBER"));
            assertTrue(ex.getMessage().contains("STRING"));
        }

        @Test
        @DisplayName("Error: conversion BOOLEAN to STRING is forbidden")
        void testConversionBooleanToStringForbidden() {
            List<PropertyDefinition> existing = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "active", "Active", PropertyType.BOOLEAN, true, null)
            );
            List<PropertyDefinition> updated = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "active", "Active", PropertyType.STRING, true, null)
            );

            UnsafeTypeConversionException ex = assertThrows(
                    UnsafeTypeConversionException.class,
                    () -> propertyDefinitionService.validateTypeChanges(existing, updated, "template-1")
            );
            assertTrue(ex.getMessage().contains("active"));
            assertTrue(ex.getMessage().contains("BOOLEAN"));
            assertTrue(ex.getMessage().contains("STRING"));
        }

        @Test
        @DisplayName("Error: conversion NUMBER to STRING is forbidden with entities")
        void testConversionNumberToStringForbiddenWithEntities() {
            List<PropertyDefinition> existing = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "score", "Score", PropertyType.NUMBER, true, null)
            );
            List<PropertyDefinition> updated = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "score", "Score", PropertyType.STRING, true, null)
            );

            UnsafeTypeConversionException ex = assertThrows(
                    UnsafeTypeConversionException.class,
                    () -> propertyDefinitionService.validateTypeChanges(existing, updated, "template-1")
            );
            assertTrue(ex.getMessage().contains("score"));
            assertTrue(ex.getMessage().contains("NUMBER"));
            assertTrue(ex.getMessage().contains("STRING"));
        }

        @Test
        @DisplayName("Error: any type conversion STRING to NUMBER is forbidden")
        void testConversionStringToNumberForbidden() {
            List<PropertyDefinition> existing = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "code", "Code", PropertyType.STRING, true, null)
            );
            List<PropertyDefinition> updated = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "code", "Code", PropertyType.NUMBER, true, null)
            );

            UnsafeTypeConversionException ex = assertThrows(
                    UnsafeTypeConversionException.class,
                    () -> propertyDefinitionService.validateTypeChanges(existing, updated, "template-1")
            );
            assertTrue(ex.getMessage().contains("code"));
            assertTrue(ex.getMessage().contains("STRING"));
            assertTrue(ex.getMessage().contains("NUMBER"));
        }

        @Test
        @DisplayName("Error: any type conversion NUMBER to BOOLEAN is forbidden")
        void testConversionNumberToBooleanForbidden() {
            List<PropertyDefinition> existing = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "count", "Count", PropertyType.NUMBER, true, null)
            );
            List<PropertyDefinition> updated = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "count", "Count", PropertyType.BOOLEAN, true, null)
            );

            UnsafeTypeConversionException ex = assertThrows(
                    UnsafeTypeConversionException.class,
                    () -> propertyDefinitionService.validateTypeChanges(existing, updated, "template-1")
            );
            assertTrue(ex.getMessage().contains("count"));
            assertTrue(ex.getMessage().contains("NUMBER"));
            assertTrue(ex.getMessage().contains("BOOLEAN"));
        }

        @Test
        @DisplayName("Error: any type conversion BOOLEAN to NUMBER is forbidden")
        void testConversionBooleanToNumberForbidden() {
            List<PropertyDefinition> existing = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "active", "Active", PropertyType.BOOLEAN, true, null)
            );
            List<PropertyDefinition> updated = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "active", "Active", PropertyType.NUMBER, true, null)
            );

            UnsafeTypeConversionException ex = assertThrows(
                    UnsafeTypeConversionException.class,
                    () -> propertyDefinitionService.validateTypeChanges(existing, updated, "template-1")
            );
            assertTrue(ex.getMessage().contains("active"));
            assertTrue(ex.getMessage().contains("BOOLEAN"));
            assertTrue(ex.getMessage().contains("NUMBER"));
        }

        @Test
        @DisplayName("Error: any type conversion is forbidden even without existing entities")
        void testAnyTypeConversionForbiddenNoEntities() {
            List<PropertyDefinition> existing = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "data", "Data", PropertyType.STRING, true, null)
            );
            List<PropertyDefinition> updated = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "data", "Data", PropertyType.BOOLEAN, true, null)
            );

            UnsafeTypeConversionException ex = assertThrows(
                    UnsafeTypeConversionException.class,
                    () -> propertyDefinitionService.validateTypeChanges(existing, updated, "template-1")
            );
            assertTrue(ex.getMessage().contains("data"));
            assertTrue(ex.getMessage().contains("STRING"));
            assertTrue(ex.getMessage().contains("BOOLEAN"));
        }

        @Test
        @DisplayName("Happy path: property removed from updated list")
        void testPropertyRemoved() {
            List<PropertyDefinition> existing = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "name", "Name", PropertyType.STRING, true, null),
                    new PropertyDefinition(UUID.randomUUID(), "age", "Age", PropertyType.NUMBER, false, null)
            );
            List<PropertyDefinition> updated = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "name", "Name", PropertyType.STRING, true, null)
            );

            assertDoesNotThrow(() -> propertyDefinitionService.validateTypeChanges(existing, updated, "template-1"));
        }

        @Test
        @DisplayName("Happy path: new property added to updated list")
        void testPropertyAdded() {
            List<PropertyDefinition> existing = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "name", "Name", PropertyType.STRING, true, null)
            );
            List<PropertyDefinition> updated = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "name", "Name", PropertyType.STRING, true, null),
                    new PropertyDefinition(UUID.randomUUID(), "email", "Email", PropertyType.STRING, false, null)
            );

            assertDoesNotThrow(() -> propertyDefinitionService.validateTypeChanges(existing, updated, "template-1"));
        }

        @Test
        @DisplayName("Error: multiple type conversions forbidden, fails on first")
        void testMultipleTypeConversionsForbidden() {
            List<PropertyDefinition> existing = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "field1", "Field 1", PropertyType.STRING, true, null),
                    new PropertyDefinition(UUID.randomUUID(), "field2", "Field 2", PropertyType.NUMBER, true, null)
            );
            List<PropertyDefinition> updated = List.of(
                    new PropertyDefinition(UUID.randomUUID(), "field1", "Field 1", PropertyType.NUMBER, true, null),
                    new PropertyDefinition(UUID.randomUUID(), "field2", "Field 2", PropertyType.BOOLEAN, true, null)
            );

            UnsafeTypeConversionException ex = assertThrows(
                    UnsafeTypeConversionException.class,
                    () -> propertyDefinitionService.validateTypeChanges(existing, updated, "template-1")
            );
            assertTrue(ex.getMessage().contains("field1"));
            assertTrue(ex.getMessage().contains("STRING"));
            assertTrue(ex.getMessage().contains("NUMBER"));
            assertFalse(ex.getMessage().contains("BOOLEAN"));
        }
    }
}
