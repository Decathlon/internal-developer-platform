package com.decathlon.idp_core.domain.service.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doThrow;

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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingNotFoundException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorNotFoundException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookSecurity;
import com.decathlon.idp_core.domain.port.EntityDynamicMappingPort;
import com.decathlon.idp_core.domain.port.WebhookConnectorRepositoryPort;

@DisplayName("WebhookConnectorService Tests")
@ExtendWith(MockitoExtension.class)
class WebhookConnectorServiceTest {

  @Mock
  private WebhookConnectorRepositoryPort webhookConnectorRepositoryPort;

  @Mock
  private WebhookConnectorValidationService webhookConnectorValidationService;

  @Mock
  private EntityDynamicMappingPort entityDynamicMappingPort;

  private WebhookConnectorService service;

  @BeforeEach
  void setUp() {
    service = new WebhookConnectorService(webhookConnectorRepositoryPort,
        webhookConnectorValidationService, entityDynamicMappingPort);
  }

  @Nested
  @DisplayName("getWebhookConnector")
  class GetWebhookConnectorTests {

    @Test
    @DisplayName("Should return connector when it exists")
    void shouldReturnConnectorWhenExists() {
      WebhookConnector existing = buildWebhookConnector(UUID.randomUUID(), "github-dora",
          "GitHub DORA", "desc", true);
      when(webhookConnectorRepositoryPort.findByIdentifier("github-dora"))
          .thenReturn(Optional.of(existing));

      WebhookConnector result = service.getWebhookConnector("github-dora");

      assertThat(result).isEqualTo(existing);
    }

    @Test
    @DisplayName("Should throw WebhookConnectorNotFoundException when not found")
    void shouldThrowWhenConnectorNotFound() {
      when(webhookConnectorRepositoryPort.findByIdentifier("unknown")).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getWebhookConnector("unknown"))
          .isInstanceOf(WebhookConnectorNotFoundException.class).hasMessageContaining("unknown");
    }
  }

  @Nested
  @DisplayName("createWebhookConnector")
  class CreateWebhookConnectorTests {

    @Test
    @DisplayName("Should validate then save and return the connector")
    void shouldValidateAndSave() {
      WebhookConnector toCreate = buildWebhookConnector(null, "github-dora", "GitHub DORA", "desc",
          false);
      WebhookConnector saved = buildWebhookConnector(UUID.randomUUID(), "github-dora",
          "GitHub DORA", "desc", false);
      when(webhookConnectorRepositoryPort.save(any())).thenReturn(saved);

      WebhookConnector result = service.createWebhookConnector(toCreate);

      verify(webhookConnectorValidationService).validateWebhookConnectorForCreation(toCreate);
      verify(webhookConnectorRepositoryPort).save(toCreate);
      assertThat(result.id()).isNotNull();
      assertThat(result.identifier()).isEqualTo("github-dora");
    }

    @Test
    @DisplayName("Should NOT save when validation throws")
    void shouldNotSaveWhenValidationFails() {
      WebhookConnector toCreate = buildWebhookConnector(null, "github-dora", "GitHub DORA", "desc",
          true);
      doThrow(new RuntimeException("validation error")).when(webhookConnectorValidationService)
          .validateWebhookConnectorForCreation(toCreate);

      assertThatThrownBy(() -> service.createWebhookConnector(toCreate))
          .hasMessageContaining("validation error");

      verify(webhookConnectorRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Should force enabled to false when mappings are empty")
    void shouldDisableConnectorWhenMappingsAreEmpty() {
      // User tries to create an enabled connector with no mappings
      WebhookConnector toCreate = buildWebhookConnector(null, "github-dora", "GitHub DORA", "desc",
          true);
      when(webhookConnectorRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

      service.createWebhookConnector(toCreate);

      var captor = ArgumentCaptor.forClass(WebhookConnector.class);
      verify(webhookConnectorRepositoryPort).save(captor.capture());
      var saved = captor.getValue();

      // Even though toCreate had enabled=true, it should be saved with enabled=false
      assertThat(saved.enabled()).isFalse();
      assertThat(saved.mappings()).isEmpty();
    }

    @Test
    @DisplayName("Should keep enabled as true when mappings are present")
    void shouldKeepEnabledWhenMappingsPresent() {
      EntityDynamicMapping mapping = new EntityDynamicMapping(UUID.randomUUID(),
          "deployment-mapping", "deployment", "true", "deployment name", "deployment description",
          ".id", ".name", Map.of(), List.of());
      WebhookConnector toCreate = buildWebhookConnectorWithMappings(null, "github-dora",
          "GitHub DORA", "desc", true, List.of(mapping));
      when(webhookConnectorRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

      service.createWebhookConnector(toCreate);

      var captor = ArgumentCaptor.forClass(WebhookConnector.class);
      verify(webhookConnectorRepositoryPort).save(captor.capture());
      var saved = captor.getValue();

      // With mappings present, enabled should remain true
      assertThat(saved.enabled()).isTrue();
      assertThat(saved.mappings()).hasSize(1);
    }
  }

  @Nested
  @DisplayName("updateWebhookConnector")
  class UpdateWebhookConnectorTests {

    private static final UUID EXISTING_ID = UUID.randomUUID();
    private static final String IDENTIFIER = "github-dora";

    @Test
    @DisplayName("Should preserve id and identifier from the stored connector")
    void shouldPreserveIdAndIdentifier() {
      WebhookConnector existing = buildWebhookConnector(EXISTING_ID, IDENTIFIER, "Old name",
          "Old desc", true);
      WebhookConnector incoming = buildWebhookConnector(null, "ignored-from-body", "New name",
          "New desc", false);

      when(webhookConnectorRepositoryPort.findByIdentifier(IDENTIFIER))
          .thenReturn(Optional.of(existing));
      when(webhookConnectorRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

      WebhookConnector result = service.updateWebhookConnector(IDENTIFIER, incoming);

      assertThat(result.id()).isEqualTo(EXISTING_ID);
      assertThat(result.identifier()).isEqualTo(IDENTIFIER);
    }

    @Test
    @DisplayName("Should apply updated fields from the incoming connector")
    void shouldApplyIncomingFields() {
      WebhookConnector existing = buildWebhookConnector(EXISTING_ID, IDENTIFIER, "Old name",
          "Old desc", true);
      WebhookConnector incoming = buildWebhookConnector(null, IDENTIFIER, "New name", "New desc",
          false);

      when(webhookConnectorRepositoryPort.findByIdentifier(IDENTIFIER))
          .thenReturn(Optional.of(existing));
      when(webhookConnectorRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

      WebhookConnector result = service.updateWebhookConnector(IDENTIFIER, incoming);

      assertThat(result.name()).isEqualTo("New name");
      assertThat(result.description()).isEqualTo("New desc");
      assertThat(result.enabled()).isFalse();
    }

    @Test
    @DisplayName("Should delegate validation before saving")
    void shouldDelegateValidationBeforeSave() {
      WebhookConnector existing = buildWebhookConnector(EXISTING_ID, IDENTIFIER, "Old name",
          "Old desc", true);
      WebhookConnector incoming = buildWebhookConnector(null, IDENTIFIER, "New name", "New desc",
          false);

      when(webhookConnectorRepositoryPort.findByIdentifier(IDENTIFIER))
          .thenReturn(Optional.of(existing));
      when(webhookConnectorRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

      service.updateWebhookConnector(IDENTIFIER, incoming);

      InOrder order = org.mockito.Mockito.inOrder(webhookConnectorValidationService,
          webhookConnectorRepositoryPort);
      order.verify(webhookConnectorValidationService).validateWebhookConnectorForUpdate(incoming);
      order.verify(webhookConnectorRepositoryPort).save(any());
    }

    @Test
    @DisplayName("Should save the merged connector with correct fields")
    void shouldSaveMergedConnector() {
      WebhookConnector existing = buildWebhookConnector(EXISTING_ID, IDENTIFIER, "Old name",
          "Old desc", true);
      WebhookConnector incoming = buildWebhookConnector(null, IDENTIFIER, "New name", "New desc",
          false);

      when(webhookConnectorRepositoryPort.findByIdentifier(IDENTIFIER))
          .thenReturn(Optional.of(existing));
      when(webhookConnectorRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

      service.updateWebhookConnector(IDENTIFIER, incoming);

      var captor = ArgumentCaptor.forClass(WebhookConnector.class);
      verify(webhookConnectorRepositoryPort).save(captor.capture());
      var saved = captor.getValue();

      assertThat(saved.id()).isEqualTo(EXISTING_ID);
      assertThat(saved.identifier()).isEqualTo(IDENTIFIER);
      assertThat(saved.name()).isEqualTo("New name");
      assertThat(saved.description()).isEqualTo("New desc");
      assertThat(saved.enabled()).isFalse();
    }

    @Test
    @DisplayName("Should throw WebhookConnectorNotFoundException when connector is missing")
    void shouldThrowWhenConnectorMissing() {
      var incoming = buildWebhookConnector(null, IDENTIFIER, "New name", "New desc", true);
      when(webhookConnectorRepositoryPort.findByIdentifier(IDENTIFIER))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.updateWebhookConnector(IDENTIFIER, incoming))
          .isInstanceOf(WebhookConnectorNotFoundException.class).hasMessageContaining(IDENTIFIER);

      verify(webhookConnectorValidationService, never()).validateWebhookConnectorForUpdate(any());
      verify(webhookConnectorRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Should force enabled to false when update removes all mappings")
    void shouldDisableConnectorWhenUpdatingWithEmptyMappings() {
      WebhookConnector existing = buildWebhookConnector(EXISTING_ID, IDENTIFIER, "Old name",
          "Old desc", true);
      // User tries to update with enabled=true but empty mappings
      WebhookConnector incoming = buildWebhookConnector(null, IDENTIFIER, "New name", "New desc",
          true);

      when(webhookConnectorRepositoryPort.findByIdentifier(IDENTIFIER))
          .thenReturn(Optional.of(existing));
      when(webhookConnectorRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

      service.updateWebhookConnector(IDENTIFIER, incoming);

      var captor = ArgumentCaptor.forClass(WebhookConnector.class);
      verify(webhookConnectorRepositoryPort).save(captor.capture());
      var saved = captor.getValue();

      // Should be forced to false because mappings are empty
      assertThat(saved.enabled()).isFalse();
      assertThat(saved.mappings()).isEmpty();
    }

    @Test
    @DisplayName("Should keep enabled value when update has mappings")
    void shouldKeepEnabledWhenUpdateHasMappings() {
      EntityDynamicMapping mapping = new EntityDynamicMapping(UUID.randomUUID(),
          "deployment-mapping", "deployment", "true", "deployment name", "deployment description",
          ".id", ".name", Map.of(), List.of());
      WebhookConnector existing = buildWebhookConnector(EXISTING_ID, IDENTIFIER, "Old name",
          "Old desc", false);
      WebhookConnector incoming = buildWebhookConnectorWithMappings(null, IDENTIFIER, "New name",
          "New desc", true, List.of(mapping));

      when(webhookConnectorRepositoryPort.findByIdentifier(IDENTIFIER))
          .thenReturn(Optional.of(existing));
      when(webhookConnectorRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

      service.updateWebhookConnector(IDENTIFIER, incoming);

      var captor = ArgumentCaptor.forClass(WebhookConnector.class);
      verify(webhookConnectorRepositoryPort).save(captor.capture());
      var saved = captor.getValue();

      // Should remain true because mappings are present
      assertThat(saved.enabled()).isTrue();
      assertThat(saved.mappings()).hasSize(1);
    }
  }

  @Nested
  @DisplayName("deleteWebhookConnector")
  class DeleteWebhookConnectorTests {

    @Test
    @DisplayName("Should validate existence then delete")
    void shouldValidateAndDelete() {
      service.deleteWebhookConnector("github-dora");

      var order = org.mockito.Mockito.inOrder(webhookConnectorValidationService,
          webhookConnectorRepositoryPort);
      order.verify(webhookConnectorValidationService).validateIdentifierExists("github-dora");
      order.verify(webhookConnectorRepositoryPort).deleteByIdentifier("github-dora");
    }

    @Test
    @DisplayName("Should NOT delete when validation throws")
    void shouldNotDeleteWhenValidationFails() {
      doThrow(new WebhookConnectorNotFoundException("github-dora not found"))
          .when(webhookConnectorValidationService).validateIdentifierExists("github-dora");

      assertThatThrownBy(() -> service.deleteWebhookConnector("github-dora"))
          .isInstanceOf(WebhookConnectorNotFoundException.class);

      verify(webhookConnectorRepositoryPort, never()).deleteByIdentifier(any());
    }
  }

  @Nested
  @DisplayName("getAllWebhookConnector")
  class GetAllWebhookConnectorTests {

    @Test
    @DisplayName("Should return paginated connectors from repository")
    void shouldReturnPaginatedConnectors() {
      PageRequest pageable = PageRequest.of(0, 10);
      WebhookConnector c1 = buildWebhookConnector(UUID.randomUUID(), "connector-a", "A", "desc",
          true);
      WebhookConnector c2 = buildWebhookConnector(UUID.randomUUID(), "connector-b", "B", "desc",
          false);
      var page = new PageImpl<>(List.of(c1, c2), pageable, 2);
      when(webhookConnectorRepositoryPort.findAll(pageable)).thenReturn(page);

      Page<WebhookConnector> result = service.getAllWebhookConnector(pageable);

      assertThat(result.getContent()).hasSize(2);
      assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return empty page when no connectors exist")
    void shouldReturnEmptyPage() {
      Pageable pageable = PageRequest.of(0, 10);
      when(webhookConnectorRepositoryPort.findAll(pageable))
          .thenReturn(new PageImpl<>(List.of(), pageable, 0));

      Page<WebhookConnector> result = service.getAllWebhookConnector(pageable);

      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();
    }
  }

  private WebhookConnector buildWebhookConnector(UUID id, String identifier, String title,
      String description, boolean enabled) {
    WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.HMAC_SHA256,
        Map.of("header_name", "X-Hub-Signature-256", "secret_alias", "MY_SECRET"));
    return new WebhookConnector(id, identifier, title, description, enabled, List.of(), security);
  }

  private WebhookConnector buildWebhookConnectorWithMappings(UUID id, String identifier,
      String title, String description, boolean enabled, List<EntityDynamicMapping> mappings) {
    WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.HMAC_SHA256,
        Map.of("header_name", "X-Hub-Signature-256", "secret_alias", "MY_SECRET"));
    return new WebhookConnector(id, identifier, title, description, enabled, mappings, security);
  }

  @Nested
  @DisplayName("resolveAndValidateMappings")
  class ResolveAndValidateMappingsTests {

    @Test
    @DisplayName("Should return empty list when identifiers are null or empty")
    void shouldReturnEmptyWhenNoIdentifiers() {
      assertThat(service.resolveAndValidateMappings(null)).isEmpty();
      assertThat(service.resolveAndValidateMappings(List.of())).isEmpty();
      verifyNoInteractions(entityDynamicMappingPort);
    }

    @Test
    @DisplayName("Should resolve each identifier to its existing mapping")
    void shouldResolveExistingMappings() {
      EntityDynamicMapping mapping = buildMapping("deployment-mapping");
      when(entityDynamicMappingPort.findByIdentifier("deployment-mapping"))
          .thenReturn(Optional.of(mapping));

      List<EntityDynamicMapping> result = service
          .resolveAndValidateMappings(List.of("deployment-mapping"));

      assertThat(result).containsExactly(mapping);
    }

    @Test
    @DisplayName("Should throw EntityDynamicMappingNotFoundException when a mapping is missing")
    void shouldThrowWhenMappingMissing() {
      when(entityDynamicMappingPort.findByIdentifier("missing-mapping"))
          .thenReturn(Optional.empty());

      List<String> mappings = List.of("missing-mapping");

      assertThatThrownBy(() -> service.resolveAndValidateMappings(mappings))
          .isInstanceOf(EntityDynamicMappingNotFoundException.class)
          .hasMessageContaining("missing-mapping");
    }

    private EntityDynamicMapping buildMapping(String identifier) {
      return new EntityDynamicMapping(UUID.randomUUID(), identifier, "deployment", "true",
          "deployment name", "deployment description", ".id", ".name", Map.of(), List.of());
    }
  }
}
