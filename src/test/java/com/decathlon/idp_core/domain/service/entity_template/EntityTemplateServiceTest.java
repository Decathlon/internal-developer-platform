package com.decathlon.idp_core.domain.service.entity_template;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.domain.model.enums.PropertyType;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.domain.port.EntityTemplateRepositoryPort;

@DisplayName("EntityTemplateService Tests")
@ExtendWith(MockitoExtension.class)
class EntityTemplateServiceTest {

  @Mock
  private EntityTemplateRepositoryPort entityTemplateRepositoryPort;
  @Mock
  private EntityTemplateValidationService entityTemplateValidationService;
  @Mock
  private EntityRepositoryPort entityRepositoryPort;

  private EntityTemplateService entityTemplateService;

  @BeforeEach
  void setUp() {
    entityTemplateService = new EntityTemplateService(entityTemplateRepositoryPort,
        entityTemplateValidationService, entityRepositoryPort);
  }

  @Nested
  @DisplayName("updateEntityTemplate - relation purge on definition removal")
  class RelationPurgeTests {

    private static final UUID TEMPLATE_ID = UUID.randomUUID();
    private static final String TEMPLATE_IDENTIFIER = "web-service";

    private EntityTemplate buildTemplate(List<RelationDefinition> relations) {
      return new EntityTemplate(TEMPLATE_ID, TEMPLATE_IDENTIFIER, "Web Service", "desc", List.of(),
          relations);
    }

    @Test
    @DisplayName("Should purge entity relations when a RelationDefinition is removed")
    void shouldPurgeWhenRelationDefinitionRemoved() {
      var existingRelation = new RelationDefinition(UUID.randomUUID(), "owns", "microservice", true,
          false);
      var existingTemplate = buildTemplate(List.of(existingRelation));
      var updatedTemplate = buildTemplate(List.of()); // "owns" removed

      when(entityTemplateRepositoryPort.findByIdentifier(TEMPLATE_IDENTIFIER))
          .thenReturn(Optional.of(existingTemplate));
      when(entityTemplateRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

      entityTemplateService.updateEntityTemplate(TEMPLATE_IDENTIFIER, updatedTemplate);

      verify(entityRepositoryPort).deleteRelationsByTemplateIdentifierAndRelationName(
          eq(TEMPLATE_IDENTIFIER),
          argThat((Collection<String> c) -> c.size() == 1 && c.contains("owns")));
    }

    @Test
    @DisplayName("Should purge all removed relation names when multiple are removed")
    void shouldPurgeAllRemovedRelations() {
      var rel1 = new RelationDefinition(UUID.randomUUID(), "owns", "microservice", true, false);
      var rel2 = new RelationDefinition(UUID.randomUUID(), "uses", "database-service", false, true);
      var rel3 = new RelationDefinition(UUID.randomUUID(), "belongsTo", "team", false, false);
      var existingTemplate = buildTemplate(List.of(rel1, rel2, rel3));
      // Only "belongsTo" is kept
      var updatedTemplate = buildTemplate(List.of(rel3));

      when(entityTemplateRepositoryPort.findByIdentifier(TEMPLATE_IDENTIFIER))
          .thenReturn(Optional.of(existingTemplate));
      when(entityTemplateRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

      entityTemplateService.updateEntityTemplate(TEMPLATE_IDENTIFIER, updatedTemplate);

      verify(entityRepositoryPort)
          .deleteRelationsByTemplateIdentifierAndRelationName(eq(TEMPLATE_IDENTIFIER), argThat(
              (Collection<String> c) -> c.size() == 2 && c.contains("owns") && c.contains("uses")));
    }

    @Test
    @DisplayName("Should NOT call purge when no RelationDefinitions are removed")
    void shouldNotPurgeWhenNoRelationsRemoved() {
      var rel = new RelationDefinition(UUID.randomUUID(), "owns", "microservice", true, false);
      var existingTemplate = buildTemplate(List.of(rel));
      var updatedTemplate = buildTemplate(List.of(rel));

      when(entityTemplateRepositoryPort.findByIdentifier(TEMPLATE_IDENTIFIER))
          .thenReturn(Optional.of(existingTemplate));
      when(entityTemplateRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

      entityTemplateService.updateEntityTemplate(TEMPLATE_IDENTIFIER, updatedTemplate);

      verify(entityRepositoryPort, never())
          .deleteRelationsByTemplateIdentifierAndRelationName(anyString(), any());
    }

    @Test
    @DisplayName("Should NOT call purge when template had no relations")
    void shouldNotPurgeWhenTemplateHadNoRelations() {
      var existingTemplate = buildTemplate(List.of());
      var newRel = new RelationDefinition(UUID.randomUUID(), "owns", "microservice", true, false);
      var updatedTemplate = buildTemplate(List.of(newRel));

      when(entityTemplateRepositoryPort.findByIdentifier(TEMPLATE_IDENTIFIER))
          .thenReturn(Optional.of(existingTemplate));
      when(entityTemplateRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

      entityTemplateService.updateEntityTemplate(TEMPLATE_IDENTIFIER, updatedTemplate);

      verify(entityRepositoryPort, never())
          .deleteRelationsByTemplateIdentifierAndRelationName(anyString(), any());
    }

    @Test
    @DisplayName("Should match removed relations case-insensitively")
    void shouldMatchRemovedRelationsCaseInsensitively() {
      var rel = new RelationDefinition(UUID.randomUUID(), "Owns", "microservice", true, false);
      var existingTemplate = buildTemplate(List.of(rel));
      // Incoming uses different casing but same logical name
      var updatedRelation = new RelationDefinition(null, "owns", "microservice", true, false);
      var updatedTemplate = buildTemplate(List.of(updatedRelation));

      when(entityTemplateRepositoryPort.findByIdentifier(TEMPLATE_IDENTIFIER))
          .thenReturn(Optional.of(existingTemplate));
      when(entityTemplateRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

      entityTemplateService.updateEntityTemplate(TEMPLATE_IDENTIFIER, updatedTemplate);

      // "Owns" and "owns" are the same — no removal, no purge call
      verify(entityRepositoryPort, never())
          .deleteRelationsByTemplateIdentifierAndRelationName(anyString(), any());
    }
  }

  @Nested
  @DisplayName("updateEntityTemplate - property purge on definition removal")
  class PropertyPurgeTests {

    private static final UUID TEMPLATE_ID = UUID.randomUUID();
    private static final String TEMPLATE_IDENTIFIER = "web-service";

    private EntityTemplate buildTemplate(List<PropertyDefinition> properties) {
      return new EntityTemplate(TEMPLATE_ID, TEMPLATE_IDENTIFIER, "Web Service", "desc", properties,
          List.of());
    }

    private PropertyDefinition prop(String name) {
      return new PropertyDefinition(UUID.randomUUID(), name, "desc", PropertyType.STRING, false,
          null);
    }

    @Test
    @DisplayName("Should purge entity properties when a PropertyDefinition is removed")
    void shouldPurgeWhenPropertyDefinitionRemoved() {
      var existingTemplate = buildTemplate(List.of(prop("color")));
      var updatedTemplate = buildTemplate(List.of()); // "color" removed

      when(entityTemplateRepositoryPort.findByIdentifier(TEMPLATE_IDENTIFIER))
          .thenReturn(Optional.of(existingTemplate));
      when(entityTemplateRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

      entityTemplateService.updateEntityTemplate(TEMPLATE_IDENTIFIER, updatedTemplate);

      verify(entityRepositoryPort).deletePropertiesByTemplateIdentifierAndPropertyName(
          eq(TEMPLATE_IDENTIFIER),
          argThat((Collection<String> c) -> c.size() == 1 && c.contains("color")));
    }

    @Test
    @DisplayName("Should purge all removed property names when multiple are removed")
    void shouldPurgeAllRemovedProperties() {
      var p1 = prop("color");
      var p2 = prop("port");
      var p3 = prop("env");
      var existingTemplate = buildTemplate(List.of(p1, p2, p3));
      var updatedTemplate = buildTemplate(List.of(p3)); // keep only "env"

      when(entityTemplateRepositoryPort.findByIdentifier(TEMPLATE_IDENTIFIER))
          .thenReturn(Optional.of(existingTemplate));
      when(entityTemplateRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

      entityTemplateService.updateEntityTemplate(TEMPLATE_IDENTIFIER, updatedTemplate);

      verify(entityRepositoryPort).deletePropertiesByTemplateIdentifierAndPropertyName(
          eq(TEMPLATE_IDENTIFIER), argThat((Collection<String> c) -> c.size() == 2
              && c.contains("color") && c.contains("port")));
    }

    @Test
    @DisplayName("Should NOT call purge when no PropertyDefinitions are removed")
    void shouldNotPurgeWhenNoPropertiesRemoved() {
      var p = prop("color");
      var existingTemplate = buildTemplate(List.of(p));
      var updatedTemplate = buildTemplate(List.of(p));

      when(entityTemplateRepositoryPort.findByIdentifier(TEMPLATE_IDENTIFIER))
          .thenReturn(Optional.of(existingTemplate));
      when(entityTemplateRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

      entityTemplateService.updateEntityTemplate(TEMPLATE_IDENTIFIER, updatedTemplate);

      verify(entityRepositoryPort, never())
          .deletePropertiesByTemplateIdentifierAndPropertyName(anyString(), any());
    }

    @Test
    @DisplayName("Should NOT call purge when template had no properties")
    void shouldNotPurgeWhenTemplateHadNoProperties() {
      var existingTemplate = buildTemplate(List.of());
      var updatedTemplate = buildTemplate(List.of(prop("color")));

      when(entityTemplateRepositoryPort.findByIdentifier(TEMPLATE_IDENTIFIER))
          .thenReturn(Optional.of(existingTemplate));
      when(entityTemplateRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

      entityTemplateService.updateEntityTemplate(TEMPLATE_IDENTIFIER, updatedTemplate);

      verify(entityRepositoryPort, never())
          .deletePropertiesByTemplateIdentifierAndPropertyName(anyString(), any());
    }

    @Test
    @DisplayName("Should match removed properties case-insensitively")
    void shouldMatchRemovedPropertiesCaseInsensitively() {
      var existingTemplate = buildTemplate(List.of(new PropertyDefinition(UUID.randomUUID(),
          "Color", "desc", PropertyType.STRING, false, null)));
      // Incoming uses lowercase — same logical property
      var updatedTemplate = buildTemplate(
          List.of(new PropertyDefinition(null, "color", "desc", PropertyType.STRING, false, null)));

      when(entityTemplateRepositoryPort.findByIdentifier(TEMPLATE_IDENTIFIER))
          .thenReturn(Optional.of(existingTemplate));
      when(entityTemplateRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

      entityTemplateService.updateEntityTemplate(TEMPLATE_IDENTIFIER, updatedTemplate);

      // "Color" and "color" are the same — no removal, no purge call
      verify(entityRepositoryPort, never())
          .deletePropertiesByTemplateIdentifierAndPropertyName(anyString(), any());
    }
  }
}
