package com.decathlon.idp_core.domain.service.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingAlreadyInUseException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingNotFoundException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_mapping.RelationMapping;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookSecurity;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookTemplateMapping;
import com.decathlon.idp_core.domain.port.EntityDynamicMappingPort;
import com.decathlon.idp_core.domain.port.WebhookMappingLinkPort;
import com.decathlon.idp_core.domain.service.entity_dynamic_mapping.EntityDynamicMappingService;
import com.decathlon.idp_core.domain.service.entity_dynamic_mapping.EntityDynamicMappingValidationService;

@DisplayName("DynamicMappingService Tests")
@ExtendWith(MockitoExtension.class)
class DynamicMappingServiceTest {

  @Mock
  private EntityDynamicMappingPort entityDynamicMappingPort;

  @Mock
  private WebhookMappingLinkPort webhookTemplateMappingPort;

  @Mock
  private EntityDynamicMappingValidationService entityDynamicMappingValidationService;

  private EntityDynamicMappingService service;

  private static final String MAPPING_IDENTIFIER = "github_deployment_status mapping";

  @BeforeEach
  void setUp() {
    service = new EntityDynamicMappingService(entityDynamicMappingPort, webhookTemplateMappingPort,
        entityDynamicMappingValidationService);
  }

  @Nested
  @DisplayName("createEntityDynamicMapping")
  class CreateEntityDynamicMappingTests {

    @Test
    @DisplayName("Should validate uniqueness, validate mapping then save")
    void shouldValidateThenSave() {
      EntityDynamicMapping mapping = buildMapping();
      when(entityDynamicMappingPort.existsByIdentifier(MAPPING_IDENTIFIER)).thenReturn(false);
      when(entityDynamicMappingPort.save(mapping)).thenReturn(mapping);

      EntityDynamicMapping result = service.createEntityDynamicMapping(mapping);

      assertThat(result).isEqualTo(mapping);
      verify(entityDynamicMappingValidationService).validateMapping(mapping);
      verify(entityDynamicMappingPort).save(mapping);
    }

    @Test
    @DisplayName("Should throw conflict and not save when identifier already exists")
    void shouldThrowWhenIdentifierAlreadyExists() {
      EntityDynamicMapping mapping = buildMapping();
      when(entityDynamicMappingPort.existsByIdentifier(MAPPING_IDENTIFIER)).thenReturn(true);

      assertThatThrownBy(() -> service.createEntityDynamicMapping(mapping))
          .isInstanceOf(EntityDynamicMappingAlreadyExistsException.class)
          .hasMessageContaining(MAPPING_IDENTIFIER);

      verify(entityDynamicMappingValidationService, never()).validateMapping(any());
      verify(entityDynamicMappingPort, never()).save(any());
    }
  }

  @Nested
  @DisplayName("getEntityDynamicMapping")
  class GetEntityDynamicMappingTests {

    @Test
    @DisplayName("Should return mapping when it exists")
    void shouldReturnMappingWhenExists() {
      EntityDynamicMapping mapping = buildMapping();
      when(entityDynamicMappingPort.findByIdentifier(MAPPING_IDENTIFIER))
          .thenReturn(Optional.of(mapping));

      EntityDynamicMapping result = service.getEntityDynamicMapping(MAPPING_IDENTIFIER);

      assertThat(result).isEqualTo(mapping);
    }

    @Test
    @DisplayName("Should throw EntityDynamicMappingNotFoundException when not found")
    void shouldThrowWhenMappingNotFound() {
      when(entityDynamicMappingPort.findByIdentifier("unknown")).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getEntityDynamicMapping("unknown"))
          .isInstanceOf(EntityDynamicMappingNotFoundException.class)
          .hasMessageContaining("unknown");
    }
  }

  @Nested
  @DisplayName("getAllEntityDynamicMapping")
  class GetAllEntityDynamicMappingTests {

    @Test
    @DisplayName("Should return paginated mappings from repository")
    void shouldReturnPaginatedMappings() {
      var pageable = PageRequest.of(0, 10);
      var page = new PageImpl<>(List.of(buildMapping()), pageable, 1);
      when(entityDynamicMappingPort.findAll(pageable)).thenReturn(page);

      var result = service.getAllEntityDynamicMapping(pageable);

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return empty page when no mappings exist")
    void shouldReturnEmptyPage() {
      var pageable = PageRequest.of(0, 10);
      when(entityDynamicMappingPort.findAll(pageable))
          .thenReturn(new PageImpl<>(List.of(), pageable, 0));

      var result = service.getAllEntityDynamicMapping(pageable);

      assertThat(result.getContent()).isEmpty();
    }
  }

  @Nested
  @DisplayName("updateEntityDynamicMapping")
  class UpdateEntityDynamicMappingTests {

    @Test
    @DisplayName("Should preserve id and identifier from existing mapping")
    void shouldPreserveIdAndIdentifier() {
      EntityDynamicMapping existing = buildMapping();
      EntityDynamicMapping incoming = new EntityDynamicMapping(null, "ignored-id",
          "new-entityTemplateIdentifier", ".newFilter", "New Name", "New Desc", ".newId",
          ".newTitle", Map.of("prop", ".val"), List.of());

      when(entityDynamicMappingPort.findByIdentifier(MAPPING_IDENTIFIER))
          .thenReturn(Optional.of(existing));
      when(entityDynamicMappingPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

      EntityDynamicMapping result = service.updateEntityDynamicMapping(MAPPING_IDENTIFIER,
          incoming);

      assertThat(result.id()).isEqualTo(existing.id());
      assertThat(result.identifier()).isEqualTo(existing.identifier());
    }

    @Test
    @DisplayName("Should apply updated fields from incoming mapping")
    void shouldApplyIncomingFields() {
      EntityDynamicMapping existing = buildMapping();
      EntityDynamicMapping incoming = new EntityDynamicMapping(null, "ignored",
          "new-entityTemplateIdentifier", ".newFilter", "New Name", "New Desc", ".newId",
          ".newTitle", Map.of("k", ".v"), List.of(new RelationMapping("rel", List.of(".rel"))));

      when(entityDynamicMappingPort.findByIdentifier(MAPPING_IDENTIFIER))
          .thenReturn(Optional.of(existing));
      when(entityDynamicMappingPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

      EntityDynamicMapping result = service.updateEntityDynamicMapping(MAPPING_IDENTIFIER,
          incoming);

      assertThat(result.entityTemplateIdentifier()).isEqualTo("new-entityTemplateIdentifier");
      assertThat(result.filter()).isEqualTo(".newFilter");
      assertThat(result.name()).isEqualTo("New Name");
      assertThat(result.description()).isEqualTo("New Desc");
      assertThat(result.properties()).containsKey("k");
    }

    @Test
    @DisplayName("Should validate mapping before saving")
    void shouldValidateMappingBeforeSaving() {
      EntityDynamicMapping existing = buildMapping();
      EntityDynamicMapping incoming = buildMapping();

      when(entityDynamicMappingPort.findByIdentifier(MAPPING_IDENTIFIER))
          .thenReturn(Optional.of(existing));
      when(entityDynamicMappingPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

      service.updateEntityDynamicMapping(MAPPING_IDENTIFIER, incoming);

      verify(entityDynamicMappingValidationService).validateMapping(incoming);
    }

    @Test
    @DisplayName("Should throw when mapping to update does not exist")
    void shouldThrowWhenMappingNotFound() {
      EntityDynamicMapping incoming = buildMapping();
      when(entityDynamicMappingPort.findByIdentifier("unknown")).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.updateEntityDynamicMapping("unknown", incoming))
          .isInstanceOf(EntityDynamicMappingNotFoundException.class)
          .hasMessageContaining("unknown");

      verify(entityDynamicMappingPort, never()).save(any());
    }

    @Test
    @DisplayName("Should save the merged mapping with correct fields")
    void shouldSaveMergedMapping() {
      EntityDynamicMapping existing = buildMapping();
      EntityDynamicMapping incoming = new EntityDynamicMapping(null, "ignored",
          "updated-entityTemplateIdentifier", ".updated", "Updated Name", "Updated Desc", ".uid",
          ".utitle", Map.of(), List.of());

      when(entityDynamicMappingPort.findByIdentifier(MAPPING_IDENTIFIER))
          .thenReturn(Optional.of(existing));
      when(entityDynamicMappingPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

      service.updateEntityDynamicMapping(MAPPING_IDENTIFIER, incoming);

      var captor = ArgumentCaptor.forClass(EntityDynamicMapping.class);
      verify(entityDynamicMappingPort).save(captor.capture());
      var saved = captor.getValue();

      assertThat(saved.id()).isEqualTo(existing.id());
      assertThat(saved.identifier()).isEqualTo(existing.identifier());
      assertThat(saved.entityTemplateIdentifier()).isEqualTo("updated-entityTemplateIdentifier");
      assertThat(saved.filter()).isEqualTo(".updated");
    }
  }

  @Nested
  @DisplayName("deleteEntityDynamicMapping")
  class DeleteEntityDynamicMappingTests {

    @Test
    @DisplayName("Should delete when mapping exists and is not in use")
    void shouldDeleteWhenMappingExistsAndNotInUse() {
      EntityDynamicMapping mapping = buildMapping();
      when(entityDynamicMappingPort.existsByIdentifier(MAPPING_IDENTIFIER)).thenReturn(true);
      when(entityDynamicMappingPort.findByIdentifier(MAPPING_IDENTIFIER))
          .thenReturn(Optional.of(mapping));
      when(webhookTemplateMappingPort.existsByEntityMappingId(mapping.id())).thenReturn(false);

      service.deleteEntityDynamicMapping(MAPPING_IDENTIFIER);

      verify(entityDynamicMappingPort).deleteByIdentifier(MAPPING_IDENTIFIER);
    }

    @Test
    @DisplayName("Should throw when mapping does not exist")
    void shouldThrowWhenMappingNotFound() {
      when(entityDynamicMappingPort.existsByIdentifier("unknown")).thenReturn(false);

      assertThatThrownBy(() -> service.deleteEntityDynamicMapping("unknown"))
          .isInstanceOf(EntityDynamicMappingNotFoundException.class)
          .hasMessageContaining("unknown");

      verify(entityDynamicMappingPort, never()).deleteByIdentifier(any());
    }

    @Test
    @DisplayName("Should throw EntityDynamicMappingAlreadyInUseException when mapping is used by a webhook")
    void shouldThrowWhenMappingIsInUse() {
      EntityDynamicMapping mapping = buildMapping();
      WebhookConnector webhook = buildWebhookConnector("my-webhook");
      WebhookTemplateMapping templateMapping = new WebhookTemplateMapping(UUID.randomUUID(),
          webhook, null, null, null);

      when(entityDynamicMappingPort.existsByIdentifier(MAPPING_IDENTIFIER)).thenReturn(true);
      when(entityDynamicMappingPort.findByIdentifier(MAPPING_IDENTIFIER))
          .thenReturn(Optional.of(mapping));
      when(webhookTemplateMappingPort.existsByEntityMappingId(mapping.id())).thenReturn(true);
      when(webhookTemplateMappingPort.findByEntityMappingId(mapping.id()))
          .thenReturn(List.of(templateMapping));

      assertThatThrownBy(() -> service.deleteEntityDynamicMapping(MAPPING_IDENTIFIER))
          .isInstanceOf(EntityDynamicMappingAlreadyInUseException.class)
          .hasMessageContaining("my-webhook");

      verify(entityDynamicMappingPort, never()).deleteByIdentifier(any());
    }

    @Test
    @DisplayName("Should include all referencing webhook identifiers in exception")
    void shouldIncludeAllReferencingWebhooksInException() {
      EntityDynamicMapping mapping = buildMapping();
      WebhookTemplateMapping ref1 = new WebhookTemplateMapping(UUID.randomUUID(),
          buildWebhookConnector("webhook-a"), null, null, null);
      WebhookTemplateMapping ref2 = new WebhookTemplateMapping(UUID.randomUUID(),
          buildWebhookConnector("webhook-b"), null, null, null);

      when(entityDynamicMappingPort.existsByIdentifier(MAPPING_IDENTIFIER)).thenReturn(true);
      when(entityDynamicMappingPort.findByIdentifier(MAPPING_IDENTIFIER))
          .thenReturn(Optional.of(mapping));
      when(webhookTemplateMappingPort.existsByEntityMappingId(mapping.id())).thenReturn(true);
      when(webhookTemplateMappingPort.findByEntityMappingId(mapping.id()))
          .thenReturn(List.of(ref1, ref2));

      assertThatThrownBy(() -> service.deleteEntityDynamicMapping(MAPPING_IDENTIFIER))
          .isInstanceOf(EntityDynamicMappingAlreadyInUseException.class)
          .hasMessageContaining("webhook-a").hasMessageContaining("webhook-b");
    }
  }

  private EntityDynamicMapping buildMapping() {
    return new EntityDynamicMapping(UUID.randomUUID(), MAPPING_IDENTIFIER,
        "github_deployment_status", ".deployment_status != null", "github deployment status name",
        "github deployment status description", ".id", ".name", Map.of(), List.of());
  }

  private WebhookConnector buildWebhookConnector(String identifier) {
    return new WebhookConnector(UUID.randomUUID(), identifier, identifier + " name", "desc", false,
        List.of(), new WebhookSecurity(WebhookSecurityType.NONE, Map.of()));
  }
}
