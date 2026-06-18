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
import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorTitleAlreadyExistsException;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookSecurity;
import com.decathlon.idp_core.domain.port.WebhookConnectorRepositoryPort;
import com.decathlon.idp_core.domain.port.WebhookTemplateMappingPort;
import com.decathlon.idp_core.domain.service.webhook.security.WebhookSecurityValidationService;

@DisplayName("WebhookConnectorValidationService Tests")
@ExtendWith(MockitoExtension.class)
class WebhookConnectorValidationServiceTest {

  @Mock
  private WebhookConnectorRepositoryPort webhookConnectorRepositoryPort;

  @Mock
  private WebhookTemplateMappingPort webhookTemplateMappingPort;

  @Mock
  private WebhookSecurityValidationService webhookSecurityValidationService;

  @Mock
  private EntityDynamicMappingValidationService webhookConnectorMappingValidationService;

  private WebhookConnectorValidationService service;

  @BeforeEach
  void setUp() {
    service = new WebhookConnectorValidationService(webhookConnectorRepositoryPort,
        webhookTemplateMappingPort, webhookConnectorMappingValidationService,
        webhookSecurityValidationService);
  }

  @Nested
  @DisplayName("validateWebhookConnectorForCreation")
  class ValidateWebhookConnectorForCreationTests {

    @Test
    @DisplayName("Should validate uniqueness and security for creation")
    void shouldValidateAllChecksForCreation() {
      WebhookConnector connector = buildWebhookConnector("github-dora", "GitHub DORA");
      when(webhookConnectorRepositoryPort.existsByIdentifier("github-dora")).thenReturn(false);
      when(webhookConnectorRepositoryPort.existsByTitle("GitHub DORA")).thenReturn(false);

      service.validateWebhookConnectorForCreation(connector);

      var order = inOrder(webhookConnectorRepositoryPort, webhookSecurityValidationService);
      order.verify(webhookConnectorRepositoryPort).existsByIdentifier("github-dora");
      order.verify(webhookConnectorRepositoryPort).existsByTitle("GitHub DORA");
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

      verify(webhookConnectorRepositoryPort, never()).existsByTitle(any());
      verify(webhookSecurityValidationService, never()).validateForCreation(any());
    }

    @Test
    @DisplayName("Should throw when title already exists and skip security validation")
    void shouldThrowWhenTitleAlreadyExists() {
      WebhookConnector connector = buildWebhookConnector("github-dora", "GitHub DORA");
      when(webhookConnectorRepositoryPort.existsByIdentifier("github-dora")).thenReturn(false);
      when(webhookConnectorRepositoryPort.existsByTitle("GitHub DORA")).thenReturn(true);

      assertThatThrownBy(() -> service.validateWebhookConnectorForCreation(connector))
          .isInstanceOf(WebhookConnectorTitleAlreadyExistsException.class)
          .hasMessageContaining("GitHub DORA");

      verify(webhookSecurityValidationService, never()).validateForCreation(any());
    }
  }

  @Nested
  @DisplayName("validateWebhookConnectorForUpdate")
  class ValidateWebhookConnectorForUpdateTests {

    @Test
    @DisplayName("Should validate title uniqueness when title changes")
    void shouldValidateTitleUniquenessWhenTitleChanges() {
      WebhookConnector existingConnector = buildWebhookConnector("github-dora", "Old title");
      WebhookConnector connectorToUpdate = buildWebhookConnector("github-dora", "New title");
      when(webhookConnectorRepositoryPort.existsByTitle("New title")).thenReturn(false);

      service.validateWebhookConnectorForUpdate(existingConnector, connectorToUpdate);

      verify(webhookConnectorRepositoryPort).existsByTitle("New title");
      verify(webhookSecurityValidationService).validateForCreation(connectorToUpdate.security());
    }

    @Test
    @DisplayName("Should skip title uniqueness check when title is unchanged")
    void shouldSkipTitleUniquenessWhenTitleIsUnchanged() {
      WebhookConnector existingConnector = buildWebhookConnector("github-dora", "Same title");
      WebhookConnector connectorToUpdate = buildWebhookConnector("github-dora", "Same title");

      service.validateWebhookConnectorForUpdate(existingConnector, connectorToUpdate);

      verify(webhookConnectorRepositoryPort, never()).existsByTitle(any());
      verify(webhookSecurityValidationService).validateForCreation(connectorToUpdate.security());
    }

    @Test
    @DisplayName("Should throw when changed title already exists and stop validation chain")
    void shouldThrowWhenChangedTitleAlreadyExists() {
      WebhookConnector existingConnector = buildWebhookConnector("github-dora", "Old title");
      WebhookConnector connectorToUpdate = buildWebhookConnector("github-dora", "Taken title");
      when(webhookConnectorRepositoryPort.existsByTitle("Taken title")).thenReturn(true);

      assertThatThrownBy(
          () -> service.validateWebhookConnectorForUpdate(existingConnector, connectorToUpdate))
              .isInstanceOf(WebhookConnectorTitleAlreadyExistsException.class)
              .hasMessageContaining("Taken title");

      verify(webhookSecurityValidationService, never()).validateForCreation(any());
    }
  }

  @Nested
  @DisplayName("validateTitleUniqueness")
  class ValidateTitleUniquenessTests {

    @Test
    @DisplayName("Should pass when title is unique")
    void shouldPassWhenTitleIsUnique() {
      when(webhookConnectorRepositoryPort.existsByTitle("GitHub DORA")).thenReturn(false);

      assertThatCode(() -> service.validateTitleUniqueness("GitHub DORA"))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw when title already exists")
    void shouldThrowWhenTitleAlreadyExists() {
      when(webhookConnectorRepositoryPort.existsByTitle("GitHub DORA")).thenReturn(true);

      assertThatThrownBy(() -> service.validateTitleUniqueness("GitHub DORA"))
          .isInstanceOf(WebhookConnectorTitleAlreadyExistsException.class)
          .hasMessageContaining("GitHub DORA");
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
}
