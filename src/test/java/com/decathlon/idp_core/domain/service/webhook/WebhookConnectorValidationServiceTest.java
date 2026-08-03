package com.decathlon.idp_core.domain.service.webhook;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

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

import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorAlreadyExistException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorNotFoundException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookSecurity;
import com.decathlon.idp_core.domain.port.WebhookConnectorRepositoryPort;
import com.decathlon.idp_core.domain.service.entity_dynamic_mapping.EntityDynamicMappingValidationService;
import com.decathlon.idp_core.domain.service.webhook.security.WebhookSecurityValidationService;

@DisplayName("WebhookConnectorValidationService Tests")
@ExtendWith(MockitoExtension.class)
class WebhookConnectorValidationServiceTest {

  @Mock
  private WebhookConnectorRepositoryPort webhookConnectorRepositoryPort;

  @Mock
  private WebhookSecurityValidationService webhookSecurityValidationService;

  @Mock
  private EntityDynamicMappingValidationService webhookConnectorMappingValidationService;

  private WebhookConnectorValidationService service;

  @BeforeEach
  void setUp() {
    service = new WebhookConnectorValidationService(webhookConnectorRepositoryPort,
        webhookConnectorMappingValidationService, webhookSecurityValidationService);
  }

  @Nested
  @DisplayName("validateWebhookConnectorForCreation")
  class ValidateWebhookConnectorForCreationTests {

    @Test
    @DisplayName("Should validate identifier uniqueness and security for creation")
    void shouldValidateAllChecksForCreation() {
      WebhookConnector connector = buildWebhookConnector("github-dora", "GitHub DORA");
      when(webhookConnectorRepositoryPort.existsByIdentifier("github-dora")).thenReturn(false);

      service.validateWebhookConnectorForCreation(connector);

      var order = inOrder(webhookConnectorRepositoryPort, webhookSecurityValidationService);
      order.verify(webhookConnectorRepositoryPort).existsByIdentifier("github-dora");
      order.verify(webhookSecurityValidationService).validateForCreation(connector.security());
    }

    @Test
    @DisplayName("Should throw when identifier already exists and stop validation chain")
    void shouldThrowWhenIdentifierAlreadyExists() {
      WebhookConnector connector = buildWebhookConnector("github-dora", "GitHub DORA");
      when(webhookConnectorRepositoryPort.existsByIdentifier("github-dora")).thenReturn(true);

      assertThatThrownBy(() -> service.validateWebhookConnectorForCreation(connector))
          .isInstanceOf(WebhookConnectorAlreadyExistException.class)
          .hasMessageContaining("github-dora");

      verify(webhookSecurityValidationService, never()).validateForCreation(any());
    }
  }

  @Nested
  @DisplayName("validateWebhookConnectorForUpdate")
  class ValidateWebhookConnectorForUpdateTests {

    @Test
    @DisplayName("Should validate mappings when connector has mappings")
    void shouldValidateMappingsWhenPresent() {
      WebhookConnector connectorToUpdate = buildWebhookConnectorWithMappings("github-dora",
          "Title");

      service.validateWebhookConnectorForUpdate(connectorToUpdate);

      verify(webhookConnectorMappingValidationService)
          .validateMappings(connectorToUpdate.mappings());
      verify(webhookSecurityValidationService).validateForCreation(connectorToUpdate.security());
    }

    @Test
    @DisplayName("Should skip mapping validation when connector has no mappings")
    void shouldSkipMappingValidationWhenNoMappings() {
      WebhookConnector connectorToUpdate = buildWebhookConnector("github-dora", "Title");

      service.validateWebhookConnectorForUpdate(connectorToUpdate);

      verify(webhookConnectorMappingValidationService, never()).validateMappings(any());
      verify(webhookSecurityValidationService).validateForCreation(connectorToUpdate.security());
    }

    @Test
    @DisplayName("Should allow duplicate names (no uniqueness constraint)")
    void shouldAllowDuplicateNames() {
      WebhookConnector connectorToUpdate = buildWebhookConnector("github-dora", "Duplicate Name");

      service.validateWebhookConnectorForUpdate(connectorToUpdate);

      verify(webhookConnectorRepositoryPort, never()).existsByTitle(any());
    }
  }

  @Nested
  @DisplayName("validateIdentifierExists")
  class ValidateIdentifierExistsTests {

    @Test
    @DisplayName("Should pass when identifier exists")
    void shouldPassWhenIdentifierExists() {
      when(webhookConnectorRepositoryPort.existsByIdentifier("github-dora")).thenReturn(true);

      assertThatCode(() -> service.validateIdentifierExists("github-dora"))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw when identifier does not exist")
    void shouldThrowWhenIdentifierDoesNotExist() {
      when(webhookConnectorRepositoryPort.existsByIdentifier("github-dora")).thenReturn(false);

      assertThatThrownBy(() -> service.validateIdentifierExists("github-dora"))
          .isInstanceOf(WebhookConnectorNotFoundException.class)
          .hasMessageContaining("github-dora");
    }
  }

  private WebhookConnector buildWebhookConnector(String identifier, String title) {
    WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.HMAC_SHA256,
        Map.of("header_name", "X-Hub-Signature-256", "secret_alias", "MY_SECRET"));
    return new WebhookConnector(UUID.randomUUID(), identifier, title, "desc", true, List.of(),
        security);
  }

  private WebhookConnector buildWebhookConnectorWithMappings(String identifier, String title) {
    WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.HMAC_SHA256,
        Map.of("header_name", "X-Hub-Signature-256", "secret_alias", "MY_SECRET"));
    EntityDynamicMapping mapping = new EntityDynamicMapping(UUID.randomUUID(), "my-mapping",
        "web-service", ".filter", "name", "desc", ".id", ".name", Map.of(), Map.of());
    return new WebhookConnector(UUID.randomUUID(), identifier, title, "desc", true,
        List.of(mapping), security);
  }
}
