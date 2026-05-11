package com.decathlon.idp_core.domain.service.entity_template;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.decathlon.idp_core.domain.exception.entity_template.RelationNameAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity_template.TargetTemplateNotFoundException;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.domain.port.EntityTemplateRepositoryPort;

@DisplayName("RelationDefinitionValidationService Tests")
@ExtendWith(MockitoExtension.class)
class RelationDefinitionValidationServiceTest {

    @Mock
    private EntityTemplateRepositoryPort entityTemplateRepositoryPort;

    private RelationDefinitionValidationService relationDefinitionValidationService;

    @BeforeEach
    void setUp() {
        relationDefinitionValidationService = new RelationDefinitionValidationService(entityTemplateRepositoryPort);
    }

    @Nested
    @DisplayName("validateUniqueRelationNames")
    class ValidateUniqueRelationNamesTests {

        @Test
        @DisplayName("Happy path: all relation names are unique")
        void testUniqueRelationNames() {
            List<RelationDefinition> relations = List.of(
                    new RelationDefinition(UUID.randomUUID(), "parent", "parent-template", true, false),
                    new RelationDefinition(UUID.randomUUID(), "children", "child-template", false, true),
                    new RelationDefinition(UUID.randomUUID(), "owner", "owner-template", true, false)
            );

            assertDoesNotThrow(() -> relationDefinitionValidationService.validateRelationNamesUniqueness(relations));
        }

        @Test
        @DisplayName("Happy path: single relation")
        void testSingleRelation() {
            List<RelationDefinition> relations = List.of(
                    new RelationDefinition(UUID.randomUUID(), "owner", "owner-template", true, false)
            );

            assertDoesNotThrow(() -> relationDefinitionValidationService.validateRelationNamesUniqueness(relations));
        }

        @Test
        @DisplayName("Happy path: empty relation list")
        void testEmptyRelationList() {
            assertDoesNotThrow(() -> relationDefinitionValidationService.validateRelationNamesUniqueness(new ArrayList<>()));
        }

        @Test
        @DisplayName("Error: duplicate relation names")
        void testDuplicateRelationNames() {
            List<RelationDefinition> relations = List.of(
                    new RelationDefinition(UUID.randomUUID(), "parent", "parent-template", true, false),
                    new RelationDefinition(UUID.randomUUID(), "parent", "alternative-parent-template", false, false)
            );

            RelationNameAlreadyExistsException ex = assertThrows(
                    RelationNameAlreadyExistsException.class,
                    () -> relationDefinitionValidationService.validateRelationNamesUniqueness(relations)
            );
            assertTrue(ex.getMessage().contains("parent"));
        }

        @Test
        @DisplayName("Error: multiple duplicates detected on first occurrence")
        void testMultipleDuplicates() {
            List<RelationDefinition> relations = List.of(
                    new RelationDefinition(UUID.randomUUID(), "parent", "parent-template", true, false),
                    new RelationDefinition(UUID.randomUUID(), "parent", "duplicate-parent", true, false),
                    new RelationDefinition(UUID.randomUUID(), "children", "child-template", false, true),
                    new RelationDefinition(UUID.randomUUID(), "children", "duplicate-children", false, true)
            );

            RelationNameAlreadyExistsException ex = assertThrows(
                    RelationNameAlreadyExistsException.class,
                    () -> relationDefinitionValidationService.validateRelationNamesUniqueness(relations)
            );
            // Should fail on first duplicate found
            assertTrue(ex.getMessage().contains("parent"));
        }

        @Test
        @DisplayName("Error: case-insensitive name comparison (Parent vs parent)")
        void testCaseInsensitiveNames() {
            List<RelationDefinition> relations = List.of(
                    new RelationDefinition(UUID.randomUUID(), "Parent", "parent-template", true, false),
                    new RelationDefinition(UUID.randomUUID(), "parent", "alternative-parent", false, false)
            );

            // "Parent" and "parent" should now be treated as duplicates (case-insensitive)
            RelationNameAlreadyExistsException ex = assertThrows(
                    RelationNameAlreadyExistsException.class,
                    () -> relationDefinitionValidationService.validateRelationNamesUniqueness(relations)
            );
            assertTrue(ex.getMessage().contains("parent"));
        }

        @Test
        @DisplayName("Error: duplicate relation names with different cardinalities")
        void testDuplicateNamesWithDifferentCardinalities() {
            List<RelationDefinition> relations = List.of(
                    new RelationDefinition(UUID.randomUUID(), "items", "item-template", true, false),
                    new RelationDefinition(UUID.randomUUID(), "items", "item-template", false, true)
            );

            RelationNameAlreadyExistsException ex = assertThrows(
                    RelationNameAlreadyExistsException.class,
                    () -> relationDefinitionValidationService.validateRelationNamesUniqueness(relations)
            );
            assertTrue(ex.getMessage().contains("items"));
        }
    }

    @Nested
    @DisplayName("validateTargetTemplatesExist")
    class ValidateTargetTemplatesExistTests {

        @Test
        @DisplayName("Happy path: all target templates exist")
        void testAllTargetTemplatesExist() {
            List<RelationDefinition> relations = List.of(
                    new RelationDefinition(UUID.randomUUID(), "parent", "parent-template", true, false),
                    new RelationDefinition(UUID.randomUUID(), "children", "child-template", false, true),
                    new RelationDefinition(UUID.randomUUID(), "owner", "owner-template", true, false)
            );

            when(entityTemplateRepositoryPort.existsByIdentifier("parent-template")).thenReturn(true);
            when(entityTemplateRepositoryPort.existsByIdentifier("child-template")).thenReturn(true);
            when(entityTemplateRepositoryPort.existsByIdentifier("owner-template")).thenReturn(true);

            assertDoesNotThrow(() -> relationDefinitionValidationService.validateTargetTemplatesExist(relations));
        }

        @Test
        @DisplayName("Happy path: empty relation list")
        void testEmptyRelationList() {
            assertDoesNotThrow(() -> relationDefinitionValidationService.validateTargetTemplatesExist(new ArrayList<>()));
        }

        @Test
        @DisplayName("Error: single relation with non-existent target")
        void testSingleRelationWithNonExistentTarget() {
            List<RelationDefinition> relations = List.of(
                    new RelationDefinition(UUID.randomUUID(), "owner", "non-existent-template", true, false)
            );

            when(entityTemplateRepositoryPort.existsByIdentifier("non-existent-template")).thenReturn(false);

            TargetTemplateNotFoundException ex = assertThrows(
                    TargetTemplateNotFoundException.class,
                    () -> relationDefinitionValidationService.validateTargetTemplatesExist(relations)
            );
            assertTrue(ex.getMessage().contains("non-existent-template"));
        }

        @Test
        @DisplayName("Error: multiple relations with multiple targets missing")
        void testMultipleTargetsNotFound() {
            List<RelationDefinition> relations = List.of(
                    new RelationDefinition(UUID.randomUUID(), "parent", "missing-parent-template", true, false),
                    new RelationDefinition(UUID.randomUUID(), "children", "child-template", false, true),
                    new RelationDefinition(UUID.randomUUID(), "owner", "missing-owner-template", true, false)
            );

            when(entityTemplateRepositoryPort.existsByIdentifier("missing-parent-template")).thenReturn(false);

            TargetTemplateNotFoundException ex = assertThrows(
                    TargetTemplateNotFoundException.class,
                    () -> relationDefinitionValidationService.validateTargetTemplatesExist(relations)
            );
            // Should fail on first missing target
            assertTrue(ex.getMessage().contains("missing-parent-template"));
        }

        @Test
        @DisplayName("Error: relation with null target identifier")
        void testRelationWithNullTargetIdentifier() {
            List<RelationDefinition> relations = List.of(
                    new RelationDefinition(UUID.randomUUID(), "optional-parent", null, false, false)
            );

            TargetTemplateNotFoundException ex = assertThrows(
                    TargetTemplateNotFoundException.class,
                    () -> relationDefinitionValidationService.validateTargetTemplatesExist(relations)
            );
            assertTrue(ex.getMessage().contains("null") || ex.getMessage().contains("target"));
        }
    }
}
