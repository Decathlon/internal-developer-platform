package com.decathlon.idp_core.domain.service.entity_template;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateIdentifierCannotChangeException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateIsRelationTargetException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNameAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateUsedByDynamicMappingException;
import com.decathlon.idp_core.domain.exception.entity_template.PropertyDefinitionRulesConflictException;
import com.decathlon.idp_core.domain.exception.entity_template.PropertyNameAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity_template.RelationCannotTargetItselfException;
import com.decathlon.idp_core.domain.exception.entity_template.RelationNameAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity_template.RelationTargetTemplateChangeException;
import com.decathlon.idp_core.domain.exception.entity_template.TargetTemplateNotFoundException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_mapping.MappingAction;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.domain.model.enums.PropertyType;
import com.decathlon.idp_core.domain.port.EntityDynamicMappingPort;
import com.decathlon.idp_core.domain.port.EntityTemplateRepositoryPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("EntityTemplateValidationService Tests")
class EntityTemplateValidationServiceTest {

  @Mock
  private EntityTemplateRepositoryPort entityTemplateRepository;

  @Mock
  private PropertyDefinitionValidationService propertyDefinitionValidationService;

  @Mock
  private RelationDefinitionValidationService relationDefinitionValidationService;

  @Mock
  private EntityDynamicMappingPort entityDynamicMappingPort;

  private EntityTemplateValidationService validationService;

  @BeforeEach
  void setUp() {
    validationService = new EntityTemplateValidationService(entityTemplateRepository,
        propertyDefinitionValidationService, relationDefinitionValidationService,
        entityDynamicMappingPort);
  }

  @Nested
  @DisplayName("validateTemplateExists")
  class ValidateTemplateExistsTest {

    @Test
    @DisplayName("Should throw EntityTemplateNotFoundException when identifier is null")
    void shouldThrowWhenIdentifierIsNull() {
      assertThrows(EntityTemplateNotFoundException.class,
          () -> validationService.validateTemplateExists(null));

      verify(entityTemplateRepository, never()).existsByIdentifier(any());
    }

    @Test
    @DisplayName("Should throw EntityTemplateNotFoundException when identifier is blank")
    void shouldThrowWhenIdentifierIsBlank() {
      assertThrows(EntityTemplateNotFoundException.class,
          () -> validationService.validateTemplateExists("   "));

      verify(entityTemplateRepository, never()).existsByIdentifier(any());
    }

    @Test
    @DisplayName("Should throw EntityTemplateNotFoundException when template does not exist")
    void shouldThrowWhenTemplateDoesNotExist() {
      when(entityTemplateRepository.existsByIdentifier("web-service")).thenReturn(false);

      assertThrows(EntityTemplateNotFoundException.class,
          () -> validationService.validateTemplateExists("web-service"));
    }

    @Test
    @DisplayName("Should not throw when template exists")
    void shouldNotThrowWhenTemplateExists() {
      when(entityTemplateRepository.existsByIdentifier("web-service")).thenReturn(true);

      assertDoesNotThrow(() -> validationService.validateTemplateExists("web-service"));
    }
  }

  @Nested
  @DisplayName("validateIdentifierUniqueness")
  class ValidateIdentifierUniquenessTest {

    @Test
    @DisplayName("Should skip validation when identifier is null")
    void shouldSkipValidationWhenIdentifierIsNull() {
      assertDoesNotThrow(() -> validationService.validateIdentifierUniqueness(null));

      verify(entityTemplateRepository, never()).existsByIdentifier(any());
    }

    @Test
    @DisplayName("Should skip validation when identifier is blank")
    void shouldSkipValidationWhenIdentifierIsBlank() {
      assertDoesNotThrow(() -> validationService.validateIdentifierUniqueness("   "));

      verify(entityTemplateRepository, never()).existsByIdentifier(any());
    }

    @Test
    @DisplayName("Should throw EntityTemplateAlreadyExistsException when identifier already exists")
    void shouldThrowWhenIdentifierAlreadyExists() {
      when(entityTemplateRepository.existsByIdentifier("web-service")).thenReturn(true);

      assertThrows(EntityTemplateAlreadyExistsException.class,
          () -> validationService.validateIdentifierUniqueness("web-service"));
    }

    @Test
    @DisplayName("Should not throw when identifier is unique")
    void shouldNotThrowWhenIdentifierIsUnique() {
      when(entityTemplateRepository.existsByIdentifier("web-service")).thenReturn(false);

      assertDoesNotThrow(() -> validationService.validateIdentifierUniqueness("web-service"));
    }
  }

  @Nested
  @DisplayName("validateNameUniqueness")
  class ValidateNameUniquenessTest {

    @Test
    @DisplayName("Should skip validation when name is null")
    void shouldSkipValidationWhenNameIsNull() {
      assertDoesNotThrow(() -> validationService.validateNameUniqueness(null));

      verify(entityTemplateRepository, never()).existsByName(any());
    }

    @Test
    @DisplayName("Should skip validation when name is blank")
    void shouldSkipValidationWhenNameIsBlank() {
      assertDoesNotThrow(() -> validationService.validateNameUniqueness("   "));

      verify(entityTemplateRepository, never()).existsByName(any());
    }

    @Test
    @DisplayName("Should throw EntityTemplateNameAlreadyExistsException when name already exists")
    void shouldThrowWhenNameAlreadyExists() {
      when(entityTemplateRepository.existsByName("Web Service")).thenReturn(true);

      assertThrows(EntityTemplateNameAlreadyExistsException.class,
          () -> validationService.validateNameUniqueness("Web Service"));
    }

    @Test
    @DisplayName("Should not throw when name is unique")
    void shouldNotThrowWhenNameIsUnique() {
      when(entityTemplateRepository.existsByName("Web Service")).thenReturn(false);

      assertDoesNotThrow(() -> validationService.validateNameUniqueness("Web Service"));
    }
  }

  @Nested
  @DisplayName("validateForCreation")
  class ValidateForCreationTest {

    @Test
    @DisplayName("Should validate successfully for minimal valid template")
    void shouldValidateSuccessfullyForMinimalTemplate() {
      var template = buildTemplate("web-service", "Web Service");

      when(entityTemplateRepository.existsByIdentifier("web-service")).thenReturn(false);
      when(entityTemplateRepository.existsByName("Web Service")).thenReturn(false);

      assertDoesNotThrow(() -> validationService.validateForCreation(template));

      // Implementation calls validatePropertyNamesUniqueness and
      // validateRelationNamesUniqueness
      // even for empty lists
      verify(propertyDefinitionValidationService)
          .validatePropertyNamesUniqueness(Collections.emptyList());
      verify(relationDefinitionValidationService)
          .validateRelationNamesUniqueness(Collections.emptyList());
    }

    @Test
    @DisplayName("Should validate properties when template has property definitions")
    void shouldValidatePropertiesWhenPresent() {
      var property = new PropertyDefinition(UUID.randomUUID(), "version", "Version",
          PropertyType.STRING, false, null);
      var template = buildTemplateWithProperties("web-service", "Web Service", List.of(property));

      when(entityTemplateRepository.existsByIdentifier("web-service")).thenReturn(false);
      when(entityTemplateRepository.existsByName("Web Service")).thenReturn(false);

      assertDoesNotThrow(() -> validationService.validateForCreation(template));

      verify(propertyDefinitionValidationService)
          .validatePropertyNamesUniqueness(List.of(property));
      verify(propertyDefinitionValidationService).validatePropertyDefinitionRules(property);
    }

    @Test
    @DisplayName("Should validate relations when template has relation definitions")
    void shouldValidateRelationsWhenPresent() {
      var relation = new RelationDefinition(UUID.randomUUID(), "owned-by", "team", false, false);
      var template = buildTemplateWithRelations("web-service", "Web Service", List.of(relation));

      when(entityTemplateRepository.existsByIdentifier("web-service")).thenReturn(false);
      when(entityTemplateRepository.existsByName("Web Service")).thenReturn(false);

      assertDoesNotThrow(() -> validationService.validateForCreation(template));

      verify(relationDefinitionValidationService)
          .validateRelationNamesUniqueness(List.of(relation));
      verify(relationDefinitionValidationService).validateRelationNoSelfReference("web-service",
          List.of(relation));
      verify(relationDefinitionValidationService).validateTargetTemplatesExist(List.of(relation));
    }

    @Test
    @DisplayName("Should throw EntityTemplateAlreadyExistsException when identifier already exists")
    void shouldThrowWhenIdentifierAlreadyExists() {
      var template = buildTemplate("web-service", "Web Service");

      when(entityTemplateRepository.existsByIdentifier("web-service")).thenReturn(true);

      assertThrows(EntityTemplateAlreadyExistsException.class,
          () -> validationService.validateForCreation(template));
    }

    @Test
    @DisplayName("Should throw EntityTemplateNameAlreadyExistsException when name already exists")
    void shouldThrowWhenNameAlreadyExists() {
      var template = buildTemplate("web-service", "Web Service");

      when(entityTemplateRepository.existsByIdentifier("web-service")).thenReturn(false);
      when(entityTemplateRepository.existsByName("Web Service")).thenReturn(true);

      assertThrows(EntityTemplateNameAlreadyExistsException.class,
          () -> validationService.validateForCreation(template));
    }

    @Test
    @DisplayName("Should propagate PropertyNameAlreadyExistsException from property validation")
    void shouldPropagatePropertyNameException() {
      var property = new PropertyDefinition(UUID.randomUUID(), "version", "Version",
          PropertyType.STRING, false, null);
      var template = buildTemplateWithProperties("web-service", "Web Service", List.of(property));

      when(entityTemplateRepository.existsByIdentifier("web-service")).thenReturn(false);
      when(entityTemplateRepository.existsByName("Web Service")).thenReturn(false);
      doThrow(new PropertyNameAlreadyExistsException("version"))
          .when(propertyDefinitionValidationService).validatePropertyNamesUniqueness(anyList());

      assertThrows(PropertyNameAlreadyExistsException.class,
          () -> validationService.validateForCreation(template));
    }

    @Test
    @DisplayName("Should propagate PropertyDefinitionRulesConflictException from property validation")
    void shouldPropagatePropertyRulesException() {
      var property = new PropertyDefinition(UUID.randomUUID(), "version", "Version",
          PropertyType.STRING, false, null);
      var template = buildTemplateWithProperties("web-service", "Web Service", List.of(property));

      when(entityTemplateRepository.existsByIdentifier("web-service")).thenReturn(false);
      when(entityTemplateRepository.existsByName("Web Service")).thenReturn(false);
      doThrow(new PropertyDefinitionRulesConflictException("version", "STRING", "conflict"))
          .when(propertyDefinitionValidationService).validatePropertyDefinitionRules(any());

      assertThrows(PropertyDefinitionRulesConflictException.class,
          () -> validationService.validateForCreation(template));
    }

    @Test
    @DisplayName("Should propagate RelationNameAlreadyExistsException from relation validation")
    void shouldPropagateRelationNameException() {
      var relation = new RelationDefinition(UUID.randomUUID(), "owned-by", "team", false, false);
      var template = buildTemplateWithRelations("web-service", "Web Service", List.of(relation));

      when(entityTemplateRepository.existsByIdentifier("web-service")).thenReturn(false);
      when(entityTemplateRepository.existsByName("Web Service")).thenReturn(false);
      doThrow(new RelationNameAlreadyExistsException("owned-by"))
          .when(relationDefinitionValidationService).validateRelationNamesUniqueness(anyList());

      assertThrows(RelationNameAlreadyExistsException.class,
          () -> validationService.validateForCreation(template));
    }

    @Test
    @DisplayName("Should propagate RelationCannotTargetItselfException from relation validation")
    void shouldPropagateRelationSelfReferenceException() {
      var relation = new RelationDefinition(UUID.randomUUID(), "owned-by", "web-service", false,
          false);
      var template = buildTemplateWithRelations("web-service", "Web Service", List.of(relation));

      when(entityTemplateRepository.existsByIdentifier("web-service")).thenReturn(false);
      when(entityTemplateRepository.existsByName("Web Service")).thenReturn(false);
      doThrow(new RelationCannotTargetItselfException("web-service", "owned-by"))
          .when(relationDefinitionValidationService)
          .validateRelationNoSelfReference(eq("web-service"), anyList());

      assertThrows(RelationCannotTargetItselfException.class,
          () -> validationService.validateForCreation(template));
    }

    @Test
    @DisplayName("Should propagate TargetTemplateNotFoundException from relation validation")
    void shouldPropagateTargetTemplateNotFoundException() {
      var relation = new RelationDefinition(UUID.randomUUID(), "owned-by", "team", false, false);
      var template = buildTemplateWithRelations("web-service", "Web Service", List.of(relation));

      when(entityTemplateRepository.existsByIdentifier("web-service")).thenReturn(false);
      when(entityTemplateRepository.existsByName("Web Service")).thenReturn(false);
      doThrow(new TargetTemplateNotFoundException("team")).when(relationDefinitionValidationService)
          .validateTargetTemplatesExist(anyList());

      assertThrows(TargetTemplateNotFoundException.class,
          () -> validationService.validateForCreation(template));
    }
  }

  @Nested
  @DisplayName("validateForUpdate")
  class ValidateForUpdateTest {

    @Test
    @DisplayName("Should throw EntityTemplateIdentifierCannotChangeException when identifier changes")
    void shouldThrowWhenIdentifierChanges() {
      var existingTemplate = buildTemplate("web-service", "Web Service");
      var mergedTemplate = buildTemplate("new-identifier", "Web Service");

      assertThrows(EntityTemplateIdentifierCannotChangeException.class, () -> validationService
          .validateForUpdate("web-service", "Web Service", existingTemplate, mergedTemplate));
    }

    @Test
    @DisplayName("Should validate name uniqueness when name changes")
    void shouldValidateNameUniquenessWhenNameChanges() {
      var existingTemplate = buildTemplate("web-service", "Web Service");
      var mergedTemplate = buildTemplate("web-service", "New Name");

      when(entityTemplateRepository.existsByName("New Name")).thenReturn(false);

      assertDoesNotThrow(() -> validationService.validateForUpdate("web-service", "Web Service",
          existingTemplate, mergedTemplate));

      verify(entityTemplateRepository).existsByName("New Name");
    }

    @Test
    @DisplayName("Should not validate name uniqueness when name remains unchanged")
    void shouldNotValidateNameUniquenessWhenNameUnchanged() {
      var existingTemplate = buildTemplate("web-service", "Web Service");
      var mergedTemplate = buildTemplate("web-service", "Web Service");

      assertDoesNotThrow(() -> validationService.validateForUpdate("web-service", "Web Service",
          existingTemplate, mergedTemplate));

      verify(entityTemplateRepository, never()).existsByName(any());
    }

    @Test
    @DisplayName("Should validate property type changes when properties are present")
    void shouldValidatePropertyTypeChangesWhenPropertiesPresent() {
      var oldProperty = new PropertyDefinition(UUID.randomUUID(), "version", "Version",
          PropertyType.STRING, false, null);
      var newProperty = new PropertyDefinition(UUID.randomUUID(), "version", "Version",
          PropertyType.NUMBER, false, null);

      var existingTemplate = buildTemplateWithProperties("web-service", "Web Service",
          List.of(oldProperty));
      var mergedTemplate = buildTemplateWithProperties("web-service", "Web Service",
          List.of(newProperty));

      assertDoesNotThrow(() -> validationService.validateForUpdate("web-service", "Web Service",
          existingTemplate, mergedTemplate));

      verify(propertyDefinitionValidationService).validateTypeChanges(List.of(oldProperty),
          List.of(newProperty));
    }

    @Test
    @DisplayName("Should validate target template changes when relations are present")
    void shouldValidateTargetTemplateChangesWhenRelationsPresent() {
      var oldRelation = new RelationDefinition(UUID.randomUUID(), "owned-by", "team", false, false);
      var newRelation = new RelationDefinition(UUID.randomUUID(), "owned-by", "new-team", false,
          false);

      var existingTemplate = buildTemplateWithRelations("web-service", "Web Service",
          List.of(oldRelation));
      var mergedTemplate = buildTemplateWithRelations("web-service", "Web Service",
          List.of(newRelation));

      assertDoesNotThrow(() -> validationService.validateForUpdate("web-service", "Web Service",
          existingTemplate, mergedTemplate));

      verify(relationDefinitionValidationService)
          .validateTargetTemplateChanges(List.of(oldRelation), List.of(newRelation));
    }

    @Test
    @DisplayName("Should propagate RelationTargetTemplateChangeException when target template changes")
    void shouldPropagateRelationTargetTemplateChangeException() {
      var oldRelation = new RelationDefinition(UUID.randomUUID(), "owned-by", "team", false, false);
      var newRelation = new RelationDefinition(UUID.randomUUID(), "owned-by", "new-team", false,
          false);

      var existingTemplate = buildTemplateWithRelations("web-service", "Web Service",
          List.of(oldRelation));
      var mergedTemplate = buildTemplateWithRelations("web-service", "Web Service",
          List.of(newRelation));

      doThrow(new RelationTargetTemplateChangeException("owned-by", "team", "new-team"))
          .when(relationDefinitionValidationService)
          .validateTargetTemplateChanges(List.of(oldRelation), List.of(newRelation));

      assertThrows(RelationTargetTemplateChangeException.class, () -> validationService
          .validateForUpdate("web-service", "Web Service", existingTemplate, mergedTemplate));
    }

    @Test
    @DisplayName("Should validate property rules when properties are present")
    void shouldValidatePropertyRulesWhenPropertiesPresent() {
      var property = new PropertyDefinition(UUID.randomUUID(), "version", "Version",
          PropertyType.STRING, false, null);
      var existingTemplate = buildTemplateWithProperties("web-service", "Web Service",
          List.of(property));
      var mergedTemplate = buildTemplateWithProperties("web-service", "Web Service",
          List.of(property));

      assertDoesNotThrow(() -> validationService.validateForUpdate("web-service", "Web Service",
          existingTemplate, mergedTemplate));

      verify(propertyDefinitionValidationService).validatePropertyDefinitionRules(property);
    }

    @Test
    @DisplayName("Should validate relation referential integrity when relations are present")
    void shouldValidateRelationReferentialIntegrityWhenRelationsPresent() {
      var relation = new RelationDefinition(UUID.randomUUID(), "owned-by", "team", false, false);
      var existingTemplate = buildTemplateWithRelations("web-service", "Web Service",
          List.of(relation));
      var mergedTemplate = buildTemplateWithRelations("web-service", "Web Service",
          List.of(relation));

      assertDoesNotThrow(() -> validationService.validateForUpdate("web-service", "Web Service",
          existingTemplate, mergedTemplate));

      verify(relationDefinitionValidationService).validateRelationNoSelfReference("web-service",
          List.of(relation));
      verify(relationDefinitionValidationService).validateTargetTemplatesExist(List.of(relation));
    }
  }

  @Nested
  @DisplayName("validateForDeletion")
  class ValidateForDeletionTest {

    @Test
    @DisplayName("Should throw EntityTemplateNotFoundException when template does not exist")
    void shouldThrowWhenTemplateDoesNotExist() {
      when(entityTemplateRepository.existsByIdentifier("web-service")).thenReturn(false);

      assertThrows(EntityTemplateNotFoundException.class,
          () -> validationService.validateForDeletion("web-service"));
    }

    @Test
    @DisplayName("Should throw EntityTemplateIsRelationTargetException when template is referenced by relation")
    void shouldThrowWhenTemplateIsRelationTarget() {
      when(entityTemplateRepository.existsByIdentifier("web-service")).thenReturn(true);
      when(entityTemplateRepository.existsRelationTargetingTemplate("web-service"))
          .thenReturn(true);

      assertThrows(EntityTemplateIsRelationTargetException.class,
          () -> validationService.validateForDeletion("web-service"));
    }

    @Test
    @DisplayName("Should throw EntityTemplateUsedByDynamicMappingException when template is used by mapping")
    void shouldThrowWhenTemplateIsUsedByMapping() {
      var mapping = new EntityDynamicMapping(UUID.randomUUID(), "gitlab-project", "web-service",
          ".visibility == \"private\"", MappingAction.UPDATE_ENTITY, "GitLab Project Mapping",
          "desc", ".id | tostring", ".name", Collections.emptyMap(), Collections.emptyList());

      when(entityTemplateRepository.existsByIdentifier("web-service")).thenReturn(true);
      when(entityTemplateRepository.existsRelationTargetingTemplate("web-service"))
          .thenReturn(false);
      when(entityDynamicMappingPort.findByEntityTemplateIdentifier("web-service"))
          .thenReturn(List.of(mapping));

      var exception = assertThrows(EntityTemplateUsedByDynamicMappingException.class,
          () -> validationService.validateForDeletion("web-service"));

      assertEquals(
          "Cannot delete template because it is currently mapped to '[gitlab-project]' entity dynamic mappings. Please remove the associated entity dynamic mappings before deleting the template.",
          exception.getMessage());
    }

    @Test
    @DisplayName("Should validate successfully when template can be safely deleted")
    void shouldValidateSuccessfullyWhenTemplateCanBeDeleted() {
      when(entityTemplateRepository.existsByIdentifier("web-service")).thenReturn(true);
      when(entityTemplateRepository.existsRelationTargetingTemplate("web-service"))
          .thenReturn(false);
      when(entityDynamicMappingPort.findByEntityTemplateIdentifier("web-service"))
          .thenReturn(Collections.emptyList());

      assertDoesNotThrow(() -> validationService.validateForDeletion("web-service"));
    }
  }

  @Nested
  @DisplayName("validateTemplateIsNotUsedInEntityDynamicMapper")
  class ValidateTemplateIsNotUsedInEntityDynamicMapperTest {

    @Test
    @DisplayName("Should throw EntityTemplateUsedByDynamicMappingException when template is used by single mapping")
    void shouldThrowWhenTemplateIsUsedBySingleMapping() {
      var mapping = new EntityDynamicMapping(UUID.randomUUID(), "gitlab-project", "web-service",
          ".visibility == \"private\"", MappingAction.UPDATE_ENTITY, "GitLab Project Mapping",
          "desc", ".id | tostring", ".name", Collections.emptyMap(), Collections.emptyList());

      when(entityDynamicMappingPort.findByEntityTemplateIdentifier("web-service"))
          .thenReturn(List.of(mapping));

      var exception = assertThrows(EntityTemplateUsedByDynamicMappingException.class,
          () -> validationService.validateTemplateIsNotUsedInEntityDynamicMapper("web-service"));

      assertEquals(
          "Cannot delete template because it is currently mapped to '[gitlab-project]' entity dynamic mappings. Please remove the associated entity dynamic mappings before deleting the template.",
          exception.getMessage());
    }

    @Test
    @DisplayName("Should throw EntityTemplateUsedByDynamicMappingException when template is used by multiple mappings")
    void shouldThrowWhenTemplateIsUsedByMultipleMappings() {
      var mapping1 = new EntityDynamicMapping(UUID.randomUUID(), "gitlab-project", "web-service",
          ".visibility == \"private\"", MappingAction.UPDATE_ENTITY, "GitLab Project Mapping",
          "desc", ".id | tostring", ".name", Collections.emptyMap(), Collections.emptyList());
      var mapping2 = new EntityDynamicMapping(UUID.randomUUID(), "github-repo", "web-service",
          ".private == true", MappingAction.UPDATE_ENTITY, "GitHub Repo Mapping", "desc",
          ".id | tostring", ".name", Collections.emptyMap(), Collections.emptyList());

      when(entityDynamicMappingPort.findByEntityTemplateIdentifier("web-service"))
          .thenReturn(List.of(mapping1, mapping2));

      var exception = assertThrows(EntityTemplateUsedByDynamicMappingException.class,
          () -> validationService.validateTemplateIsNotUsedInEntityDynamicMapper("web-service"));

      assertEquals(
          "Cannot delete template because it is currently mapped to '[gitlab-project, github-repo]' entity dynamic mappings. Please remove the associated entity dynamic mappings before deleting the template.",
          exception.getMessage());
    }

    @Test
    @DisplayName("Should not throw when template is not used by any mapping")
    void shouldNotThrowWhenTemplateIsNotUsedByAnyMapping() {
      when(entityDynamicMappingPort.findByEntityTemplateIdentifier("web-service"))
          .thenReturn(Collections.emptyList());

      assertDoesNotThrow(
          () -> validationService.validateTemplateIsNotUsedInEntityDynamicMapper("web-service"));
    }
  }

  // Helper methods to build test data

  private EntityTemplate buildTemplate(String identifier, String name) {
    return new EntityTemplate(UUID.randomUUID(), identifier, name, "desc", Collections.emptyList(),
        Collections.emptyList());
  }

  private EntityTemplate buildTemplateWithProperties(String identifier, String name,
      List<PropertyDefinition> properties) {
    return new EntityTemplate(UUID.randomUUID(), identifier, name, "desc", properties,
        Collections.emptyList());
  }

  private EntityTemplate buildTemplateWithRelations(String identifier, String name,
      List<RelationDefinition> relations) {
    return new EntityTemplate(UUID.randomUUID(), identifier, name, "desc", Collections.emptyList(),
        relations);
  }
}
