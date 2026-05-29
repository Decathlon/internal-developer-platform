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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

  @InjectMocks
  private EntityValidationService entityValidationService;

  @Test
  @DisplayName("Should throw when entity with same identifier already exists")
  void shouldThrowWhenEntityAlreadyExists() {
    var template = new EntityTemplate(UUID.randomUUID(), "web-service", "Web Service", "desc",
        Collections.emptyList(), List.of());
    var entity = entity("web-service", "catalog-api", "Catalog API", List.of(), List.of());
    when(entityRepository.findByTemplateIdentifierAndIdentifier("web-service", "catalog-api"))
        .thenReturn(Optional.of(entity));

    assertThrows(EntityAlreadyExistsException.class,
        () -> entityValidationService.validateForCreation(entity, template));
  }

  @Test
  @DisplayName("Should not query repository when identifier is null")
  void shouldNotQueryRepositoryWhenIdentifierIsNull() {
    var template = new EntityTemplate(UUID.randomUUID(), "web-service", "Web Service", "desc",
        Collections.emptyList(), List.of());

    var entity = entity("web-service", null, "Catalog API", List.of(), List.of());

    assertDoesNotThrow(() -> entityValidationService.validateForCreation(entity, template));

    verify(entityRepository, never()).findByTemplateIdentifierAndIdentifier(any(), any());
  }

  @Test
  @DisplayName("Should validate entity successfully by delegating to property and relation validation services")
  void shouldValidateForCreationSuccessfullyWhenNoViolations() {
    var template = new EntityTemplate(UUID.randomUUID(), "web-service", "Web Service", "desc",
        List.of(), List.of());

    var property = new Property(UUID.randomUUID(), "version", "1.0.0");
    var relation = new Relation(UUID.randomUUID(), "owned-by", "team", List.of("team-a"));
    var entity = entity("web-service", "catalog-api", "Catalog API", List.of(property),
        List.of(relation));

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
    var template = new EntityTemplate(UUID.randomUUID(), "web-service", "Web Service", "desc",
        List.of(), List.of());

    var entity = entity("web-service", "catalog-api", "Catalog API", List.of(), List.of());

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

  private Entity entity(String templateIdentifier, String identifier, String name,
      List<Property> properties, List<Relation> relations) {
    return new Entity(UUID.randomUUID(), templateIdentifier, name, identifier, properties,
        relations);
  }
}
