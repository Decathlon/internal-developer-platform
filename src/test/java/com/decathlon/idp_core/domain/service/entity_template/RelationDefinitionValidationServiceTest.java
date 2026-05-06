package com.decathlon.idp_core.domain.service.entity_template;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import com.decathlon.idp_core.domain.exception.RelationNameAlreadyExistsException;
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

            assertDoesNotThrow(() -> relationDefinitionValidationService.validateUniqueRelationNames(relations));
        }

        @Test
        @DisplayName("Happy path: single relation")
        void testSingleRelation() {
            List<RelationDefinition> relations = List.of(
                    new RelationDefinition(UUID.randomUUID(), "owner", "owner-template", true, false)
            );

            assertDoesNotThrow(() -> relationDefinitionValidationService.validateUniqueRelationNames(relations));
        }

        @Test
        @DisplayName("Happy path: empty relation list")
        void testEmptyRelationList() {
            assertDoesNotThrow(() -> relationDefinitionValidationService.validateUniqueRelationNames(new ArrayList<>()));
        }

        @Test
        @DisplayName("Happy path: relations with null names are ignored")
        void testRelationsWithNullNames() {
            List<RelationDefinition> relations = List.of(
                    new RelationDefinition(UUID.randomUUID(), null, "template-1", true, false),
                    new RelationDefinition(UUID.randomUUID(), "valid", "template-2", false, true)
            );

            assertDoesNotThrow(() -> relationDefinitionValidationService.validateUniqueRelationNames(relations));
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
                    () -> relationDefinitionValidationService.validateUniqueRelationNames(relations)
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
                    () -> relationDefinitionValidationService.validateUniqueRelationNames(relations)
            );
            // Should fail on first duplicate found
            assertTrue(ex.getMessage().contains("parent"));
        }

        @Test
        @DisplayName("Error: three relations with same name")
        void testTripleDuplicateRelationNames() {
            List<RelationDefinition> relations = List.of(
                    new RelationDefinition(UUID.randomUUID(), "member", "member-template-1", true, false),
                    new RelationDefinition(UUID.randomUUID(), "member", "member-template-2", false, false),
                    new RelationDefinition(UUID.randomUUID(), "member", "member-template-3", true, true)
            );

            RelationNameAlreadyExistsException ex = assertThrows(
                    RelationNameAlreadyExistsException.class,
                    () -> relationDefinitionValidationService.validateUniqueRelationNames(relations)
            );
            assertTrue(ex.getMessage().contains("member"));
        }

        @Test
        @DisplayName("Happy path: null and non-null names with duplicates in non-null")
        void testMixedNullAndDuplicateNames() {
            List<RelationDefinition> relations = List.of(
                    new RelationDefinition(UUID.randomUUID(), null, "template-1", true, false),
                    new RelationDefinition(UUID.randomUUID(), "org", "org-template-1", true, false),
                    new RelationDefinition(UUID.randomUUID(), null, "template-2", false, true),
                    new RelationDefinition(UUID.randomUUID(), "org", "org-template-2", true, false)
            );

            RelationNameAlreadyExistsException ex = assertThrows(
                    RelationNameAlreadyExistsException.class,
                    () -> relationDefinitionValidationService.validateUniqueRelationNames(relations)
            );
            assertTrue(ex.getMessage().contains("org"));
        }

        @Test
        @DisplayName("Happy path: case-sensitive name comparison")
        void testCaseSensitiveNames() {
            List<RelationDefinition> relations = List.of(
                    new RelationDefinition(UUID.randomUUID(), "Parent", "parent-template", true, false),
                    new RelationDefinition(UUID.randomUUID(), "parent", "alternative-parent", false, false)
            );

            // "Parent" and "parent" are different names, so this should pass
            assertDoesNotThrow(() -> relationDefinitionValidationService.validateUniqueRelationNames(relations));
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
                    () -> relationDefinitionValidationService.validateUniqueRelationNames(relations)
            );
            assertTrue(ex.getMessage().contains("items"));
        }
    }
}
