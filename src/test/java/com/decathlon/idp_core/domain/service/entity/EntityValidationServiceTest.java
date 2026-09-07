package com.decathlon.idp_core.domain.service.entity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.decathlon.idp_core.domain.exception.entity.EntityAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.domain.service.property.PropertyValidationService;
import com.decathlon.idp_core.domain.service.relation.RelationValidationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("EntityValidationService Tests")
class EntityValidationServiceTest {

  @Mock
  private EntityRepositoryPort entityRepository;

  @Mock
  private RelationValidationService relationValidationService;

  @Mock
  private PropertyValidationService propertyValidationService;

  private EntityValidationService entityValidationService;

  @BeforeEach
  void setUp() {
    entityValidationService = new EntityValidationService(entityRepository,
        propertyValidationService, relationValidationService);
  }

  @Nested
  @DisplayName("validateForCreation - uniqueness checks")
  class ValidateForCreationUniquenessTests {

    @Test
    @DisplayName("Should throw EntityAlreadyExistsException when entity with same identifier already exists")
    void shouldThrowWhenEntityAlreadyExists() {
      var template = buildTemplate();
      var entity = buildEntity("web-service", "catalog-api", "Catalog API");
      when(entityRepository.findByTemplateIdentifierAndIdentifier("web-service", "catalog-api"))
          .thenReturn(Optional.of(entity));

      assertThrows(EntityAlreadyExistsException.class,
          () -> entityValidationService.validateForCreation(entity, template));

      verify(entityRepository).findByTemplateIdentifierAndIdentifier("web-service", "catalog-api");
    }

    @Test
    @DisplayName("Should not query repository when entity identifier is null")
    void shouldNotQueryRepositoryWhenIdentifierIsNull() {
      var template = buildTemplate();
      var entity = buildEntity("web-service", null, "Catalog API");

      assertDoesNotThrow(() -> entityValidationService.validateForCreation(entity, template));

      verify(entityRepository, never()).findByTemplateIdentifierAndIdentifier(any(), any());
    }

    @Test
    @DisplayName("Should succeed when entity identifier does not exist")
    void shouldSucceedWhenIdentifierDoesNotExist() {
      var template = buildTemplate();
      var entity = buildEntity("web-service", "new-catalog-api", "New Catalog API");
      when(entityRepository.findByTemplateIdentifierAndIdentifier("web-service", "new-catalog-api"))
          .thenReturn(Optional.empty());

      assertDoesNotThrow(() -> entityValidationService.validateForCreation(entity, template));

      verify(entityRepository).findByTemplateIdentifierAndIdentifier("web-service",
          "new-catalog-api");
    }
  }

  @Nested
  @DisplayName("validateForCreation - template conformance")
  class ValidateForCreationTemplateConformanceTests {

    @Test
    @DisplayName("Should validate entity successfully by delegating to property and relation validation services")
    void shouldValidateSuccessfullyWhenNoViolations() {
      var template = buildTemplate();
      var property = new Property(UUID.randomUUID(), "version", "1.0.0");
      var relation = new Relation(UUID.randomUUID(), "owned-by", "team", List.of("team-a"));
      var entity = buildEntity("web-service", "catalog-api", "Catalog API", List.of(property),
          List.of(relation));

      when(entityRepository.findByTemplateIdentifierAndIdentifier(any(), any()))
          .thenReturn(Optional.empty());

      assertDoesNotThrow(() -> entityValidationService.validateForCreation(entity, template));

      verify(propertyValidationService).validatePropertiesAgainstTemplate(eq(template),
          eq(template.propertiesDefinitions()), eq(Map.of("version", property)),
          any(Violations.class));

      verify(relationValidationService).validateRelationsAgainstTemplate(eq(template),
          eq(entity.relations()), any(Violations.class));
    }

    @Test
    @DisplayName("Should throw EntityValidationException when delegated validations populate the Violations aggregate")
    void shouldThrowEntityValidationExceptionWhenViolationsExist() {
      var template = buildTemplate();
      var entity = buildEntity("web-service", "catalog-api", "Catalog API");

      when(entityRepository.findByTemplateIdentifierAndIdentifier(any(), any()))
          .thenReturn(Optional.empty());

      try (var _ = mockConstruction(Violations.class, (mock, context) -> {
        when(mock.isEmpty()).thenReturn(false);
        when(mock.asList())
            .thenReturn(List.of("Delegated property error", "Delegated relation error"));
      })) {

        var exception = assertThrows(EntityValidationException.class,
            () -> entityValidationService.validateForCreation(entity, template));

        assertEquals(2, exception.getViolations().size());
        assertEquals("Delegated property error", exception.getViolations().get(0));

        verify(propertyValidationService).validatePropertiesAgainstTemplate(eq(template), any(),
            any(), any());
        verify(relationValidationService).validateRelationsAgainstTemplate(eq(template), any(),
            any());
      }
    }

    @Test
    @DisplayName("Should handle entity with null properties list")
    void shouldHandleEntityWithNullProperties() {
      var template = buildTemplate();
      var entity = new Entity(UUID.randomUUID(), "web-service", "Catalog API", "catalog-api", null,
          List.of());

      when(entityRepository.findByTemplateIdentifierAndIdentifier(any(), any()))
          .thenReturn(Optional.empty());

      assertDoesNotThrow(() -> entityValidationService.validateForCreation(entity, template));

      verify(propertyValidationService).validatePropertiesAgainstTemplate(eq(template),
          eq(template.propertiesDefinitions()), eq(Map.of()), any(Violations.class));
    }

    @Test
    @DisplayName("Should handle entity with null relations list")
    void shouldHandleEntityWithNullRelations() {
      var template = buildTemplate();
      // Entity's compact constructor converts null relations to empty list
      var entity = new Entity(UUID.randomUUID(), "web-service", "Catalog API", "catalog-api",
          List.of(), null);

      when(entityRepository.findByTemplateIdentifierAndIdentifier(any(), any()))
          .thenReturn(Optional.empty());

      assertDoesNotThrow(() -> entityValidationService.validateForCreation(entity, template));

      // Entity's compact constructor converts null to empty list
      verify(relationValidationService).validateRelationsAgainstTemplate(eq(template),
          eq(List.of()), any(Violations.class));
    }

    @Test
    @DisplayName("Should handle template with null property definitions list")
    void shouldHandleTemplateWithNullPropertyDefinitions() {
      var template = new EntityTemplate(UUID.randomUUID(), "web-service", "Web Service", "desc",
          null, List.of());
      var entity = buildEntity("web-service", "catalog-api", "Catalog API");

      when(entityRepository.findByTemplateIdentifierAndIdentifier(any(), any()))
          .thenReturn(Optional.empty());

      assertDoesNotThrow(() -> entityValidationService.validateForCreation(entity, template));

      verify(propertyValidationService).validatePropertiesAgainstTemplate(eq(template),
          eq(List.of()), any(), any(Violations.class));
    }

    @Test
    @DisplayName("Should filter out properties with null names before validation")
    void shouldFilterOutPropertiesWithNullNames() {
      var template = buildTemplate();
      var validProperty = new Property(UUID.randomUUID(), "version", "1.0.0");
      var nullNameProperty = new Property(UUID.randomUUID(), null, "value");
      var entity = buildEntity("web-service", "catalog-api", "Catalog API",
          List.of(validProperty, nullNameProperty), List.of());

      when(entityRepository.findByTemplateIdentifierAndIdentifier(any(), any()))
          .thenReturn(Optional.empty());

      assertDoesNotThrow(() -> entityValidationService.validateForCreation(entity, template));

      verify(propertyValidationService).validatePropertiesAgainstTemplate(eq(template),
          eq(template.propertiesDefinitions()), eq(Map.of("version", validProperty)),
          any(Violations.class));
    }

    @Test
    @DisplayName("Should keep first property when duplicate property names exist")
    void shouldKeepFirstPropertyWhenDuplicateNamesExist() {
      var template = buildTemplate();
      var firstProperty = new Property(UUID.randomUUID(), "version", "1.0.0");
      var duplicateProperty = new Property(UUID.randomUUID(), "version", "2.0.0");
      var entity = buildEntity("web-service", "catalog-api", "Catalog API",
          List.of(firstProperty, duplicateProperty), List.of());

      when(entityRepository.findByTemplateIdentifierAndIdentifier(any(), any()))
          .thenReturn(Optional.empty());

      assertDoesNotThrow(() -> entityValidationService.validateForCreation(entity, template));

      verify(propertyValidationService).validatePropertiesAgainstTemplate(eq(template),
          eq(template.propertiesDefinitions()), eq(Map.of("version", firstProperty)),
          any(Violations.class));
    }
  }

  @Nested
  @DisplayName("validateForUpdate - skips uniqueness check")
  class ValidateForUpdateTests {

    @Test
    @DisplayName("Should validate without checking uniqueness for update")
    void shouldValidateWithoutUniquenessCheck() {
      var template = buildTemplate();
      var property = new Property(UUID.randomUUID(), "version", "2.0.0");
      var entity = buildEntity("web-service", "catalog-api", "Catalog API", List.of(property),
          List.of());

      assertDoesNotThrow(() -> entityValidationService.validateForUpdate(entity, template));

      verify(entityRepository, never()).findByTemplateIdentifierAndIdentifier(any(), any());
      verify(propertyValidationService).validatePropertiesAgainstTemplate(eq(template),
          eq(template.propertiesDefinitions()), eq(Map.of("version", property)),
          any(Violations.class));
      verify(relationValidationService).validateRelationsAgainstTemplate(eq(template),
          eq(entity.relations()), any(Violations.class));
    }

    @Test
    @DisplayName("Should throw EntityValidationException when violations exist during update")
    void shouldThrowWhenViolationsExistDuringUpdate() {
      var template = buildTemplate();
      var entity = buildEntity("web-service", "catalog-api", "Catalog API");

      try (var _ = mockConstruction(Violations.class, (mock, context) -> {
        when(mock.isEmpty()).thenReturn(false);
        when(mock.asList()).thenReturn(List.of("Update validation error"));
      })) {

        var exception = assertThrows(EntityValidationException.class,
            () -> entityValidationService.validateForUpdate(entity, template));

        assertEquals(1, exception.getViolations().size());
        assertEquals("Update validation error", exception.getViolations().get(0));
      }
    }

    @Test
    @DisplayName("Should handle update with null properties and relations")
    void shouldHandleUpdateWithNullPropertiesAndRelations() {
      var template = buildTemplate();
      // Entity's compact constructor converts null to empty lists
      var entity = new Entity(UUID.randomUUID(), "web-service", "Catalog API", "catalog-api", null,
          null);

      assertDoesNotThrow(() -> entityValidationService.validateForUpdate(entity, template));

      // Entity's compact constructor converts null to empty lists
      verify(propertyValidationService).validatePropertiesAgainstTemplate(eq(template),
          eq(template.propertiesDefinitions()), eq(Map.of()), any(Violations.class));
      verify(relationValidationService).validateRelationsAgainstTemplate(eq(template),
          eq(List.of()), any(Violations.class));
    }
  }

  // Helper methods to build test data

  private EntityTemplate buildTemplate() {
    return new EntityTemplate(UUID.randomUUID(), "web-service", "Web Service", "desc", List.of(),
        List.of());
  }

  private Entity buildEntity(String templateIdentifier, String identifier, String name) {
    return buildEntity(templateIdentifier, identifier, name, List.of(), List.of());
  }

  private Entity buildEntity(String templateIdentifier, String identifier, String name,
      List<Property> properties, List<Relation> relations) {
    return new Entity(UUID.randomUUID(), templateIdentifier, name, identifier, properties,
        relations);
  }
}
