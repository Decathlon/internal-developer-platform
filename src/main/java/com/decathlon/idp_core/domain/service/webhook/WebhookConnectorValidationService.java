package com.decathlon.idp_core.domain.service.webhook;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorAlreadyExistException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorNotFoundException;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.port.WebhookConnectorRepositoryPort;
import com.decathlon.idp_core.domain.service.entity_dynamic_mapping.EntityDynamicMappingValidationService;
import com.decathlon.idp_core.domain.service.webhook.security.WebhookSecurityValidationService;

import lombok.RequiredArgsConstructor;

/// Domain validation service for webhook connector lifecycle operations.
/// It validates connector uniqueness rules and delegates mapping and security
/// validation to dedicated domain services.
@Service
@Validated
@RequiredArgsConstructor
public class WebhookConnectorValidationService {

  private final WebhookConnectorRepositoryPort webhookConnectorRepositoryPort;
  private final EntityDynamicMappingValidationService entityDynamicMappingValidationService;
  private final WebhookSecurityValidationService webhookSecurityValidationService;

  public void validateWebhookConnectorForCreation(WebhookConnector webhookConnector) {
    validateIdentifierUniqueness(webhookConnector.identifier());
    webhookSecurityValidationService.validateForCreation(webhookConnector.security());

  }

  public void validateWebhookConnectorForUpdate(WebhookConnector webhookConnectorToUpdate) {
    validateMappingsIfPresent(webhookConnectorToUpdate);
    webhookSecurityValidationService.validateForCreation(webhookConnectorToUpdate.security());
  }

  private void validateMappingsIfPresent(WebhookConnector webhookConnector) {
    if (!webhookConnector.mappings().isEmpty()) {
      entityDynamicMappingValidationService.validateMappings(webhookConnector.mappings());
    }
  }

  /// Checks that no other [WebhookConnector] exists with the same identifier
  /// before allowing creation.
  ///
  /// @param webhookConnectorIdentifier the webhook connector identifier to check
  /// for uniqueness
  /// @throws WebhookConnectorAlreadyExistException if a connector with the same
  /// identifier already exists
  private void validateIdentifierUniqueness(String webhookConnectorIdentifier) {
    if (webhookConnectorRepositoryPort.existsByIdentifier(webhookConnectorIdentifier)) {
      throw new WebhookConnectorAlreadyExistException(webhookConnectorIdentifier);
    }
  }

  public void validateIdentifierExists(String webhookConnectorIdentifier) {
    if (!webhookConnectorRepositoryPort.existsByIdentifier(webhookConnectorIdentifier)) {
      throw new WebhookConnectorNotFoundException(webhookConnectorIdentifier);
    }
  }

}
