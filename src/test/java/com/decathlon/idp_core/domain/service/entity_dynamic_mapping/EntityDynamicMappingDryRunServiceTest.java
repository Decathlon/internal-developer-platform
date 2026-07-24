package com.decathlon.idp_core.domain.service.entity_dynamic_mapping;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingConfigurationException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingJsltErrorException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity_mapping.DryRunResult;
import com.decathlon.idp_core.domain.model.entity_mapping.DryRunResult.DryRunEntityResult;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.enums.ErrorType;
import com.decathlon.idp_core.domain.port.MappingEnginePort;
import com.decathlon.idp_core.domain.service.entity.EntityValidationService;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateService;

/// Unit tests for EntityDynamicMappingDryRunService.
///
/// Tests cover:
/// - Successful mapping execution with entity result (supports lists)
/// - Skipped mapping when filter returns false/null (empty list)
/// - JSLT error handling during mapping
/// - Unexpected error handling
/// - Validation of referential integrity
@DisplayName("EntityDynamicMappingDryRunService Unit Tests")
@ExtendWith(MockitoExtension.class)
class EntityDynamicMappingDryRunServiceTest {

  @Mock
  private MappingEnginePort mappingEnginePort;

  @Mock
  private EntityTemplateService entityTemplateService;

  @Mock
  private EntityValidationService entityValidationService;

  @Mock
  private EntityDynamicMappingValidationService entityDynamicMappingValidationService;

  @InjectMocks
  private EntityDynamicMappingDryRunService dryRunService;

  private EntityDynamicMapping createValidMapping() {
    return new EntityDynamicMapping(null, "test-mapping", "microservice", ".action == \"pushed\"",
        "Test Mapping", "Test Description", ".repository.full_name", ".repository.name",
        Map.of("applicationName", ".repository.name", "language", ".repository.language"),
        List.of());
  }

  @Nested
  @DisplayName("executeSingleMappingDryRun Tests")
  class ExecuteSingleMappingDryRunTests {

    @Test
    @DisplayName("Should execute successful dry-run and return mapped entity")
    void executeSingleMappingDryRun_success() {
      EntityDynamicMapping mapping = createValidMapping();
      String payload = "{\"repository\": {\"full_name\": \"my-org/my-repo\", \"name\": \"my-repo\", \"language\": \"Java\"}, \"action\": \"pushed\"}";
      Entity mappedEntity = new Entity(null, "microservice", "my-repo", "my-org/my-repo",
          List.of(new Property(null, "applicationName", "my-repo"),
              new Property(null, "language", "Java")),
          List.of());
      EntityTemplate dummyTemplate = new EntityTemplate(null, "microservice", "Microservice",
          "Desc", List.of(), List.of());

      doNothing().when(entityDynamicMappingValidationService).validateMapping(mapping);
      doReturn(mappedEntity).when(mappingEnginePort).mapToEntity(payload, mapping);
      doReturn(dummyTemplate).when(entityTemplateService)
          .getEntityTemplateByIdentifier("microservice");
      doNothing().when(entityValidationService).validateForDryRun(mappedEntity, dummyTemplate);

      DryRunResult result = dryRunService.executeSingleMappingDryRun(mapping, payload);

      assertNotNull(result);
      assertFalse(result.entityResults().isEmpty());
      DryRunEntityResult entityResult = result.entityResults().get(0);
      assertTrue(entityResult.success());
      assertEquals("microservice", entityResult.mappingTemplateIdentifier());
      assertNotNull(entityResult.entity());
      assertEquals("my-org/my-repo", entityResult.entity().identifier());
      assertEquals("my-repo", entityResult.entity().name());
      assertNull(entityResult.error());

      verify(entityDynamicMappingValidationService).validateMapping(mapping);
      verify(mappingEnginePort).mapToEntity(payload, mapping);
      verify(entityTemplateService).getEntityTemplateByIdentifier("microservice");
      verify(entityValidationService).validateForDryRun(mappedEntity, dummyTemplate);
    }

    @Test
    @DisplayName("Should return skipped result when mapping filter returns empty list or null")
    void executeSingleMappingDryRun_skipped() {
      EntityDynamicMapping mapping = createValidMapping();
      String payload = "{\"repository\": {\"full_name\": \"my-org/my-repo\"}, \"action\": \"released\"}";

      doNothing().when(entityDynamicMappingValidationService).validateMapping(mapping);
      doReturn(null).when(mappingEnginePort).mapToEntity(payload, mapping);

      DryRunResult result = dryRunService.executeSingleMappingDryRun(mapping, payload);

      assertNotNull(result);
      assertFalse(result.entityResults().isEmpty());
      DryRunEntityResult entityResult = result.entityResults().getFirst();
      assertTrue(entityResult.success());
      assertEquals("microservice", entityResult.mappingTemplateIdentifier());
      assertNull(entityResult.entity());
      assertNotNull(entityResult.error());
      assertEquals(ErrorType.SKIPPED, entityResult.error().type());
      assertTrue(entityResult.error().message().contains("Filter"));

      verify(entityDynamicMappingValidationService).validateMapping(mapping);
      verify(mappingEnginePort).mapToEntity(payload, mapping);
    }

    @Test
    @DisplayName("Should throw configuration exception when payload parsing or mapping fails")
    void executeSingleMappingDryRun_jslt_error() {
      EntityDynamicMapping mapping = createValidMapping();
      String payload = "{\"invalid\": \"payload\"}";
      EntityDynamicMappingConfigurationException jsltException = new EntityDynamicMappingConfigurationException(
          "Invalid JSLT expression");

      doNothing().when(entityDynamicMappingValidationService).validateMapping(mapping);
      doThrow(jsltException).when(mappingEnginePort).mapToEntity(payload, mapping);

      assertThrows(EntityDynamicMappingConfigurationException.class,
          () -> dryRunService.executeSingleMappingDryRun(mapping, payload));

      verify(entityDynamicMappingValidationService).validateMapping(mapping);
      verify(mappingEnginePort).mapToEntity(payload, mapping);
    }

    @Test
    @DisplayName("Should catch unexpected exception and return failure result")
    void executeSingleMappingDryRun_unexpected_error() {
      EntityDynamicMapping mapping = createValidMapping();
      String payload = "{\"test\": \"data\"}";
      RuntimeException unexpectedException = new RuntimeException("Unexpected error occurred");

      doNothing().when(entityDynamicMappingValidationService).validateMapping(mapping);
      doThrow(unexpectedException).when(mappingEnginePort).mapToEntity(payload, mapping);

      DryRunResult result = dryRunService.executeSingleMappingDryRun(mapping, payload);

      assertNotNull(result);
      assertFalse(result.entityResults().isEmpty());
      DryRunEntityResult entityResult = result.entityResults().getFirst();
      assertFalse(entityResult.success());
      assertEquals("microservice", entityResult.mappingTemplateIdentifier());
      assertNull(entityResult.entity());
      assertNotNull(entityResult.error());
      assertEquals(ErrorType.JSLT_ERROR, entityResult.error().type());
      assertTrue(entityResult.error().message().contains("Unexpected transformation error"));

      verify(entityDynamicMappingValidationService).validateMapping(mapping);
      verify(mappingEnginePort).mapToEntity(payload, mapping);
    }
  }

  @Nested
  @DisplayName("processMapping Tests")
  class ProcessMappingTests {

    @Test
    @DisplayName("Should process mapping successfully for multiple entities")
    void processMapping_success() {
      EntityDynamicMapping mapping = createValidMapping();
      String payload = "{\"repository\": {\"full_name\": \"test/repo\", \"name\": \"repo\"}, \"action\": \"pushed\"}";
      Entity mappedEntity = new Entity(null, "microservice", "repo", "test/repo",
          List.of(new Property(null, "applicationName", "repo")), List.of());
      EntityTemplate dummyTemplate = new EntityTemplate(null, "microservice", "Microservice",
          "Desc", List.of(), List.of());

      doReturn(mappedEntity).when(mappingEnginePort).mapToEntity(payload, mapping);
      doReturn(dummyTemplate).when(entityTemplateService)
          .getEntityTemplateByIdentifier("microservice");
      doNothing().when(entityValidationService).validateForDryRun(mappedEntity, dummyTemplate);

      List<DryRunEntityResult> results = dryRunService.processMapping(mapping, payload);

      assertNotNull(results);
      assertEquals(1, results.size());
      DryRunEntityResult result = results.getFirst();
      assertTrue(result.success());
      assertEquals("microservice", result.mappingTemplateIdentifier());
      assertNotNull(result.entity());
      assertEquals("test/repo", result.entity().identifier());
      assertNull(result.error());

      verify(mappingEnginePort).mapToEntity(payload, mapping);
      verify(entityTemplateService).getEntityTemplateByIdentifier("microservice");
      verify(entityValidationService).validateForDryRun(mappedEntity, dummyTemplate);
    }

    @Test
    @DisplayName("Should handle empty list return as skipped")
    void processMapping_skipped() {
      EntityDynamicMapping mapping = createValidMapping();
      String payload = "{\"action\": \"released\"}";

      doReturn(null).when(mappingEnginePort).mapToEntity(payload, mapping);

      List<DryRunEntityResult> results = dryRunService.processMapping(mapping, payload);

      assertNotNull(results);
      assertEquals(1, results.size());
      DryRunEntityResult result = results.getFirst();
      assertTrue(result.success());
      assertEquals("microservice", result.mappingTemplateIdentifier());
      assertNull(result.entity());
      assertNotNull(result.error());
      assertEquals(ErrorType.SKIPPED, result.error().type());

      verify(mappingEnginePort).mapToEntity(payload, mapping);
    }

    @Test
    @DisplayName("Should throw configuration exception when mapping engine fails")
    void processMapping_jslt_error() {
      EntityDynamicMapping mapping = createValidMapping();
      String payload = "{}";
      EntityDynamicMappingConfigurationException exception = new EntityDynamicMappingConfigurationException(
          "JSLT syntax error");

      doThrow(exception).when(mappingEnginePort).mapToEntity(payload, mapping);

      assertThrows(EntityDynamicMappingConfigurationException.class,
          () -> dryRunService.processMapping(mapping, payload));

      verify(mappingEnginePort).mapToEntity(payload, mapping);
    }

    @Test
    @DisplayName("Should throw dedicated mapping exception when entity validation fails")
    void processMapping_entity_validation_error() {
      EntityDynamicMapping mapping = createValidMapping();
      String payload = "{\"repository\": {\"full_name\": \"test/repo\", \"name\": \"repo\"}, \"action\": \"pushed\"}";
      Entity mappedEntity = new Entity(null, "microservice", "repo", "test/repo",
          List.of(new Property(null, "applicationName", "repo")), List.of());
      EntityTemplate dummyTemplate = new EntityTemplate(null, "microservice", "Microservice",
          "Desc", List.of(), List.of());

      doReturn(mappedEntity).when(mappingEnginePort).mapToEntity(payload, mapping);
      doReturn(dummyTemplate).when(entityTemplateService)
          .getEntityTemplateByIdentifier("microservice");
      doThrow(new EntityValidationException(List.of("invalid relation")))
          .when(entityValidationService).validateForDryRun(any(Entity.class), eq(dummyTemplate));

      assertThrows(EntityDynamicMappingJsltErrorException.class,
          () -> dryRunService.processMapping(mapping, payload));

      verify(mappingEnginePort).mapToEntity(payload, mapping);
      verify(entityTemplateService).getEntityTemplateByIdentifier("microservice");
      verify(entityValidationService).validateForDryRun(any(Entity.class), eq(dummyTemplate));
    }

    @Test
    @DisplayName("Should handle unexpected exception")
    void processMapping_unexpected_error() {
      EntityDynamicMapping mapping = createValidMapping();
      String payload = "{}";
      IllegalStateException exception = new IllegalStateException("Unexpected state");

      doThrow(exception).when(mappingEnginePort).mapToEntity(payload, mapping);

      List<DryRunEntityResult> results = dryRunService.processMapping(mapping, payload);

      assertNotNull(results);
      assertEquals(1, results.size());
      DryRunEntityResult result = results.getFirst();
      assertFalse(result.success());
      assertEquals("microservice", result.mappingTemplateIdentifier());
      assertNull(result.entity());
      assertNotNull(result.error());
      assertEquals(ErrorType.JSLT_ERROR, result.error().type());
      assertTrue(result.error().message().contains("Unexpected transformation error"));

      verify(mappingEnginePort).mapToEntity(payload, mapping);
    }
  }
}
