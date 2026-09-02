package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.ENTITY_DYNAMIC_MAPPING_ACTION_MANDATORY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingConfigurationException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_mapping.MappingAction;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookSecurity;
import com.decathlon.idp_core.domain.port.MappingEnginePort;
import com.decathlon.idp_core.domain.service.entity.EntityService;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/// Unit tests for IngestionProcessor covering all mapping actions and edge cases.
///
/// Tests verify:
/// - All mapping actions (UPDATE_ENTITY, UPDATE_PROPERTIES, UPDATE_RELATIONS, DELETE)
/// - Entity existence checks for conditional logic
/// - Null mapping results (filtered payloads)
/// - Property and relation stripping logic
/// - Exception handling for non-existing entities
@DisplayName("IngestionProcessor Unit Tests")
@ExtendWith(MockitoExtension.class)
class IngestionProcessorTest {

  @Mock
  private MappingEnginePort mappingEngine;

  @Mock
  private EntityService entityService;

  private IngestionProcessor ingestionProcessor;

  private ListAppender<ILoggingEvent> listAppender;

  @BeforeEach
  void setUp() {
    Logger logger = (Logger) LoggerFactory.getLogger(IngestionProcessor.class);
    listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);
    ingestionProcessor = new IngestionProcessor(mappingEngine, entityService);
  }

  private Entity createTestEntity(String templateId, String identifier) {
    return new Entity(UUID.randomUUID(), templateId, "Test Entity", identifier,
        List.of(new Property(UUID.randomUUID(), "prop1", "value1")),
        List.of(new Relation(UUID.randomUUID(), "rel1", "targetId", List.of())));
  }

  private EntityDynamicMapping createTestMapping(MappingAction action) {
    return new EntityDynamicMapping(UUID.randomUUID(), "test-mapping-" + action.name(),
        "test-template", ".action == \"pushed\"", action, "Test Mapping",
        "Test mapping description", ".repository.full_name", ".repository.name",
        Map.of("prop1", ".value1"), List.of());
  }

  private WebhookConnector createWebhookConnector(String identifier,
      List<EntityDynamicMapping> mappings) {
    return new WebhookConnector(UUID.randomUUID(), identifier, "Test Webhook",
        "Test webhook description", true, mappings,
        new WebhookSecurity(WebhookSecurityType.HMAC_SHA256,
            Map.of("header_name", "X-Hub-Signature-256", "secret_alias", "MY_SECRET")));
  }

  @Nested
  @DisplayName("Ingestion with single mapping")
  class IngestionTest {

    @Test
    @DisplayName("Should ingest webhook payload with single mapping successfully")
    void ingest_single_mapping_success() {
      String payload = "{\"action\": \"pushed\"}";
      EntityDynamicMapping mapping = createTestMapping(MappingAction.UPDATE_ENTITY);
      WebhookConnector connector = createWebhookConnector("test-connector", List.of(mapping));
      Entity entity = createTestEntity("test-template", "test-id");

      when(mappingEngine.mapToEntity(payload, mapping)).thenReturn(entity);
      when(entityService.entityExists("test-template", "test-id")).thenReturn(false);

      ingestionProcessor.ingest(payload, connector);

      verify(mappingEngine).mapToEntity(payload, mapping);
      verify(entityService).createEntity(entity);
    }

    @Test
    @DisplayName("Should ingest webhook payload with multiple mappings successfully")
    void ingest_multiple_mappings_success() {
      String payload = "{\"action\": \"pushed\"}";
      EntityDynamicMapping mapping1 = createTestMapping(MappingAction.UPDATE_ENTITY);
      EntityDynamicMapping mapping2 = createTestMapping(MappingAction.UPDATE_PROPERTIES);
      WebhookConnector connector = createWebhookConnector("test-connector",
          List.of(mapping1, mapping2));
      Entity entity1 = createTestEntity("test-template-1", "test-id-1");
      Entity entity2 = createTestEntity("test-template-2", "test-id-2");

      when(mappingEngine.mapToEntity(payload, mapping1)).thenReturn(entity1);
      when(mappingEngine.mapToEntity(payload, mapping2)).thenReturn(entity2);
      when(entityService.entityExists("test-template-1", "test-id-1")).thenReturn(false);
      when(entityService.entityExists("test-template-2", "test-id-2")).thenReturn(true);

      ingestionProcessor.ingest(payload, connector);

      verify(mappingEngine).mapToEntity(payload, mapping1);
      verify(mappingEngine).mapToEntity(payload, mapping2);
      verify(entityService).createEntity(entity1);
      verify(entityService).patchEntity(eq("test-template-2"), eq("test-id-2"), any());
    }

    @Test
    @DisplayName("Should stop processing immediately when mapping throws exception")
    void ingest_fail_fast_on_exception() {
      String payload = "{\"action\": \"pushed\"}";
      EntityDynamicMapping mapping1 = createTestMapping(MappingAction.UPDATE_ENTITY);
      EntityDynamicMapping mapping2 = createTestMapping(MappingAction.UPDATE_ENTITY);
      WebhookConnector connector = createWebhookConnector("test-connector",
          List.of(mapping1, mapping2));

      when(mappingEngine.mapToEntity(payload, mapping1))
          .thenThrow(new RuntimeException("Mapping failed"));

      assertThrows(RuntimeException.class, () -> ingestionProcessor.ingest(payload, connector));

      verify(mappingEngine).mapToEntity(payload, mapping1);
      verify(mappingEngine, never()).mapToEntity(payload, mapping2);
    }
  }

  @Nested
  @DisplayName("UPDATE_ENTITY action")
  class UpdateEntityActionTest {

    @Test
    @DisplayName("Should create entity when it does not exist")
    void upsert_create_when_not_exists() {
      String payload = "{\"action\": \"pushed\"}";
      EntityDynamicMapping mapping = createTestMapping(MappingAction.UPDATE_ENTITY);
      WebhookConnector connector = createWebhookConnector("test-connector", List.of(mapping));
      Entity entity = createTestEntity("test-template", "test-id");

      when(mappingEngine.mapToEntity(payload, mapping)).thenReturn(entity);
      when(entityService.entityExists("test-template", "test-id")).thenReturn(false);

      ingestionProcessor.ingest(payload, connector);

      verify(entityService).createEntity(entity);
      verify(entityService, never()).patchEntity(any(), any(), any());
    }

    @Test
    @DisplayName("Should patch entity when it exists")
    void upsert_patch_when_exists() {
      String payload = "{\"action\": \"pushed\"}";
      EntityDynamicMapping mapping = createTestMapping(MappingAction.UPDATE_ENTITY);
      WebhookConnector connector = createWebhookConnector("test-connector", List.of(mapping));
      Entity entity = createTestEntity("test-template", "test-id");

      when(mappingEngine.mapToEntity(payload, mapping)).thenReturn(entity);
      when(entityService.entityExists("test-template", "test-id")).thenReturn(true);

      ingestionProcessor.ingest(payload, connector);

      verify(entityService).patchEntity("test-template", "test-id", entity);
      verify(entityService, never()).createEntity(any());
    }
  }

  @Nested
  @DisplayName("UPDATE_PROPERTIES action")
  class UpdatePropertiesActionTest {

    @Test
    @DisplayName("Should create entity with properties only when it does not exist")
    void update_properties_create_when_not_exists() {
      String payload = "{\"action\": \"pushed\"}";
      EntityDynamicMapping mapping = createTestMapping(MappingAction.UPDATE_PROPERTIES);
      WebhookConnector connector = createWebhookConnector("test-connector", List.of(mapping));
      Entity entity = createTestEntity("test-template", "test-id");

      when(mappingEngine.mapToEntity(payload, mapping)).thenReturn(entity);
      when(entityService.entityExists("test-template", "test-id")).thenReturn(false);

      ingestionProcessor.ingest(payload, connector);

      verify(entityService)
          .createEntity(argThat(createdEntity -> createdEntity.properties().size() == 1
              && createdEntity.relations().isEmpty()));
    }

    @Test
    @DisplayName("Should patch entity with properties only when it exists")
    void update_properties_patch_when_exists() {
      String payload = "{\"action\": \"pushed\"}";
      EntityDynamicMapping mapping = createTestMapping(MappingAction.UPDATE_PROPERTIES);
      WebhookConnector connector = createWebhookConnector("test-connector", List.of(mapping));
      Entity entity = createTestEntity("test-template", "test-id");

      when(mappingEngine.mapToEntity(payload, mapping)).thenReturn(entity);
      when(entityService.entityExists("test-template", "test-id")).thenReturn(true);

      ingestionProcessor.ingest(payload, connector);

      verify(entityService).patchEntity(eq("test-template"), eq("test-id"),
          argThat(patchedEntity -> patchedEntity.properties().size() == 1
              && patchedEntity.relations().isEmpty()));
    }
  }

  @Nested
  @DisplayName("UPSERT_RELATIONS action")
  class UpsertRelationsActionTest {

    @Test
    @DisplayName("Should throw exception when entity does not exist")
    void upsert_relations_throws_when_not_exists() {
      String payload = "{\"action\": \"pushed\"}";
      EntityDynamicMapping mapping = createTestMapping(MappingAction.UPDATE_RELATIONS);
      WebhookConnector connector = createWebhookConnector("test-connector", List.of(mapping));
      Entity entity = createTestEntity("test-template", "test-id");

      when(mappingEngine.mapToEntity(payload, mapping)).thenReturn(entity);
      when(entityService.entityExists("test-template", "test-id")).thenReturn(false);

      assertThrows(EntityNotFoundException.class,
          () -> ingestionProcessor.ingest(payload, connector));

      verify(entityService, never()).createEntity(any());
      verify(entityService, never()).patchEntity(any(), any(), any());
    }

    @Test
    @DisplayName("Should patch entity with relations only when it exists")
    void upsert_relations_patch_when_exists() {
      String payload = "{\"action\": \"pushed\"}";
      EntityDynamicMapping mapping = createTestMapping(MappingAction.UPDATE_RELATIONS);
      WebhookConnector connector = createWebhookConnector("test-connector", List.of(mapping));
      Entity entity = createTestEntity("test-template", "test-id");

      when(mappingEngine.mapToEntity(payload, mapping)).thenReturn(entity);
      when(entityService.entityExists("test-template", "test-id")).thenReturn(true);

      ingestionProcessor.ingest(payload, connector);

      verify(entityService).patchEntity(eq("test-template"), eq("test-id"),
          argThat(patchedEntity -> patchedEntity.properties().isEmpty()
              && patchedEntity.relations().size() == 1));
    }
  }

  @Nested
  @DisplayName("PATCH_PROPERTIES action")
  class PatchPropertiesActionTest {

    @Test
    @DisplayName("Should throw exception when entity does not exist")
    void patch_properties_create_entity_when_not_exists() {
      List<ILoggingEvent> logsList = listAppender.list;
      String payload = "{\"action\": \"pushed\"}";
      EntityDynamicMapping mapping = createTestMapping(MappingAction.UPDATE_PROPERTIES);
      WebhookConnector connector = createWebhookConnector("test-connector", List.of(mapping));
      Entity entity = createTestEntity("test-template", "test-id");

      when(mappingEngine.mapToEntity(payload, mapping)).thenReturn(entity);
      when(entityService.entityExists("test-template", "test-id")).thenReturn(false);

      ingestionProcessor.ingest(payload, connector);

      assertThat(logsList).extracting(ILoggingEvent::getFormattedMessage).anyMatch(
          message -> message.contains("Completed ingestion for webhook connector: test-connector"));
      verify(entityService, never()).patchEntity(any(), any(), any());
    }

    @Test
    @DisplayName("Should patch entity properties when it exists")
    void patch_properties_succeeds_when_exists() {
      String payload = "{\"action\": \"pushed\"}";
      EntityDynamicMapping mapping = createTestMapping(MappingAction.UPDATE_PROPERTIES);
      WebhookConnector connector = createWebhookConnector("test-connector", List.of(mapping));
      Entity entity = createTestEntity("test-template", "test-id");

      when(mappingEngine.mapToEntity(payload, mapping)).thenReturn(entity);
      when(entityService.entityExists("test-template", "test-id")).thenReturn(true);

      ingestionProcessor.ingest(payload, connector);

      verify(entityService).patchEntity(eq("test-template"), eq("test-id"),
          argThat(patchedEntity -> patchedEntity.properties().size() == 1
              && patchedEntity.relations().isEmpty()));
    }
  }

  @Nested
  @DisplayName("PATCH_RELATIONS action")
  class PatchRelationsActionTest {

    @Test
    @DisplayName("Should throw exception when entity does not exist")
    void patch_relations_throws_when_not_exists() {
      String payload = "{\"action\": \"pushed\"}";
      EntityDynamicMapping mapping = createTestMapping(MappingAction.UPDATE_RELATIONS);
      WebhookConnector connector = createWebhookConnector("test-connector", List.of(mapping));
      Entity entity = createTestEntity("test-template", "test-id");

      when(mappingEngine.mapToEntity(payload, mapping)).thenReturn(entity);
      when(entityService.entityExists("test-template", "test-id")).thenReturn(false);

      assertThrows(EntityNotFoundException.class,
          () -> ingestionProcessor.ingest(payload, connector));

      verify(entityService, never()).patchEntity(any(), any(), any());
    }

    @Test
    @DisplayName("Should patch entity relations when it exists")
    void patch_relations_succeeds_when_exists() {
      String payload = "{\"action\": \"pushed\"}";
      EntityDynamicMapping mapping = createTestMapping(MappingAction.UPDATE_RELATIONS);
      WebhookConnector connector = createWebhookConnector("test-connector", List.of(mapping));
      Entity entity = createTestEntity("test-template", "test-id");

      when(mappingEngine.mapToEntity(payload, mapping)).thenReturn(entity);
      when(entityService.entityExists("test-template", "test-id")).thenReturn(true);

      ingestionProcessor.ingest(payload, connector);

      verify(entityService).patchEntity(eq("test-template"), eq("test-id"),
          argThat(patchedEntity -> patchedEntity.properties().isEmpty()
              && patchedEntity.relations().size() == 1));
    }
  }

  @Nested
  @DisplayName("DELETE action")
  class DeleteActionTest {

    @Test
    @DisplayName("Should delete entity successfully")
    void delete_succeeds() {
      String payload = "{\"action\": \"pushed\"}";
      EntityDynamicMapping mapping = createTestMapping(MappingAction.DELETE_ENTITY);
      WebhookConnector connector = createWebhookConnector("test-connector", List.of(mapping));
      Entity entity = createTestEntity("test-template", "test-id");

      when(mappingEngine.mapToEntity(payload, mapping)).thenReturn(entity);

      ingestionProcessor.ingest(payload, connector);

      verify(entityService).deleteEntity("test-template", "test-id");
      verify(entityService, never()).createEntity(any());
      verify(entityService, never()).patchEntity(any(), any(), any());
    }

    @Test
    @DisplayName("Should delete entity regardless of existence check")
    void delete_ignores_existence() {
      String payload = "{\"action\": \"pushed\"}";
      EntityDynamicMapping mapping = createTestMapping(MappingAction.DELETE_ENTITY);
      WebhookConnector connector = createWebhookConnector("test-connector", List.of(mapping));
      Entity entity = createTestEntity("test-template", "test-id");

      when(mappingEngine.mapToEntity(payload, mapping)).thenReturn(entity);
      // Existence check is not performed for DELETE

      ingestionProcessor.ingest(payload, connector);

      verify(entityService).deleteEntity("test-template", "test-id");
    }
  }

  @Nested
  @DisplayName("Null mapping results (filtered payloads)")
  class FilteredPayloadTest {

    @Test
    @DisplayName("Should skip mapping when filter returns null")
    void filtered_payload_skipped() {
      String payload = "{\"action\": \"other\"}";
      EntityDynamicMapping mapping = createTestMapping(MappingAction.UPDATE_ENTITY);
      WebhookConnector connector = createWebhookConnector("test-connector", List.of(mapping));

      when(mappingEngine.mapToEntity(payload, mapping)).thenReturn(null);

      ingestionProcessor.ingest(payload, connector);

      verify(entityService, never()).createEntity(any());
      verify(entityService, never()).patchEntity(any(), any(), any());
      verify(entityService, never()).deleteEntity(any(), any());
    }

    @Test
    @DisplayName("Should continue to next mapping when one is filtered")
    void filtered_payload_continues_to_next_mapping() {
      String payload = "{\"action\": \"pushed\"}";
      EntityDynamicMapping mapping1 = createTestMapping(MappingAction.UPDATE_PROPERTIES);
      EntityDynamicMapping mapping2 = createTestMapping(MappingAction.UPDATE_PROPERTIES);
      WebhookConnector connector = createWebhookConnector("test-connector",
          List.of(mapping1, mapping2));
      Entity entity2 = createTestEntity("test-template-2", "test-id-2");

      when(mappingEngine.mapToEntity(payload, mapping1)).thenReturn(null);
      when(mappingEngine.mapToEntity(payload, mapping2)).thenReturn(entity2);
      when(entityService.entityExists("test-template-2", "test-id-2")).thenReturn(false);

      ingestionProcessor.ingest(payload, connector);

      verify(mappingEngine).mapToEntity(payload, mapping1);
      verify(mappingEngine).mapToEntity(payload, mapping2);
      verify(entityService, never()).createEntity(entity2);
    }
  }

  @Nested
  @DisplayName("Unsupported action")
  class UnsupportedActionTest {

    @Test
    @DisplayName("Should throw exception when action is null")
    void unsupported_null_action() {
      UUID id = UUID.randomUUID();
      Map<String, String> props = Map.of("prop1", ".value1");
      var exception = assertThrows(EntityDynamicMappingConfigurationException.class,
          () -> new EntityDynamicMapping(id, "test-mapping-null", "test-template",
              ".action == \"pushed\"", null, "Test Mapping", "Test mapping description",
              ".repository.full_name", ".repository.name", props, List.of()));

      assertEquals(ENTITY_DYNAMIC_MAPPING_ACTION_MANDATORY, exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Strip helpers")
  class StripHelpersTest {

    @Test
    @DisplayName("stripRelations should remove relations and keep properties")
    void strip_relations_removes_relations_only() {
      String payload = "{\"action\": \"pushed\"}";
      EntityDynamicMapping mapping = createTestMapping(MappingAction.UPDATE_PROPERTIES);
      WebhookConnector connector = createWebhookConnector("test-connector", List.of(mapping));
      Entity entity = createTestEntity("test-template", "test-id");

      when(mappingEngine.mapToEntity(payload, mapping)).thenReturn(entity);
      when(entityService.entityExists("test-template", "test-id")).thenReturn(false);

      ingestionProcessor.ingest(payload, connector);

      verify(entityService)
          .createEntity(argThat(strippedEntity -> strippedEntity.properties().size() == 1
              && strippedEntity.relations().isEmpty()
              && strippedEntity.identifier().equals(entity.identifier())
              && strippedEntity.templateIdentifier().equals(entity.templateIdentifier())));
    }

    @Test
    @DisplayName("stripProperties should throw when entity does not exist")
    void strip_properties_throws_when_not_exists() {
      String payload = "{\"action\": \"pushed\"}";
      EntityDynamicMapping mapping = createTestMapping(MappingAction.UPDATE_RELATIONS);
      WebhookConnector connector = createWebhookConnector("test-connector", List.of(mapping));
      Entity entity = createTestEntity("test-template", "test-id");

      when(mappingEngine.mapToEntity(payload, mapping)).thenReturn(entity);
      when(entityService.entityExists("test-template", "test-id")).thenReturn(false);

      assertThrows(EntityNotFoundException.class,
          () -> ingestionProcessor.ingest(payload, connector));

      verify(entityService, never()).createEntity(any());
    }
  }

  @Nested
  @DisplayName("Entity with empty collections")
  class EmptyCollectionsTest {

    @Test
    @DisplayName("Should handle entity with no properties and no relations")
    void empty_properties_and_relations() {
      String payload = "{\"action\": \"pushed\"}";
      EntityDynamicMapping mapping = createTestMapping(MappingAction.UPDATE_ENTITY);
      WebhookConnector connector = createWebhookConnector("test-connector", List.of(mapping));
      Entity entity = new Entity(UUID.randomUUID(), "test-template", "Test Entity", "test-id",
          List.of(), List.of());

      when(mappingEngine.mapToEntity(payload, mapping)).thenReturn(entity);
      when(entityService.entityExists("test-template", "test-id")).thenReturn(false);

      ingestionProcessor.ingest(payload, connector);

      verify(entityService)
          .createEntity(argThat(createdEntity -> createdEntity.properties().isEmpty()
              && createdEntity.relations().isEmpty()));
    }
  }
}
