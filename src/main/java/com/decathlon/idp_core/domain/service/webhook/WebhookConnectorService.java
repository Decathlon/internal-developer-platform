package com.decathlon.idp_core.domain.service.webhook;

import java.util.List;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.decathlon.idp_core.domain.exception.entity_mapping.EntityDynamicMappingNotFoundException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorNotFoundException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.port.EntityDynamicMappingPort;
import com.decathlon.idp_core.domain.port.WebhookConnectorRepositoryPort;

import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class WebhookConnectorService {

  private final WebhookConnectorRepositoryPort webhookConnectorRepositoryPort;
  private final WebhookConnectorValidationService webhookConnectorValidationService;
  private final EntityDynamicMappingPort entityDynamicMappingPort;

  /// Resolves a list of entity dynamic mapping identifiers into their existing
  /// domain models.
  ///
  /// Each identifier is validated against the persisted dynamic mappings. This
  /// guarantees a webhook connector can only reference mappings that were
  /// previously created through the `/api/v1/entity-dynamic-mappings` endpoint.
  ///
  /// @param mappingIdentifiers the referenced mapping identifiers (may be null or
  /// empty)
  /// @return the resolved mappings, in the same order as the provided identifiers
  /// @throws EntityDynamicMappingNotFoundException when an identifier does not
  /// match any existing mapping
  public List<EntityDynamicMapping> resolveAndValidateMappings(List<String> mappingIdentifiers) {
    if (mappingIdentifiers == null || mappingIdentifiers.isEmpty()) {
      return List.of();
    }
    return mappingIdentifiers.stream().map(this::resolveMappingOrThrow).toList();
  }

  private EntityDynamicMapping resolveMappingOrThrow(String identifier) {
    return entityDynamicMappingPort.findByIdentifier(identifier)
        .orElseThrow(() -> new EntityDynamicMappingNotFoundException(identifier));
  }

  public WebhookConnector getWebhookConnector(String identifier) {
    return webhookConnectorRepositoryPort.findByIdentifier(identifier)
        .orElseThrow(() -> new WebhookConnectorNotFoundException(identifier));
  }

  @Transactional
  public WebhookConnector createWebhookConnector(WebhookConnector connector) {
    webhookConnectorValidationService.validateWebhookConnectorForCreation(connector);

    WebhookConnector connectorToSave = connector.mappings().isEmpty() && connector.enabled()
        ? connector.withEnabled(false)
        : connector;

    return webhookConnectorRepositoryPort.save(connectorToSave);
  }

  @Transactional
  public WebhookConnector updateWebhookConnector(String identifier,
      @Valid WebhookConnector connectorToUpdate) {
    WebhookConnector webhookConnectorInDb = getWebhookConnector(identifier);
    webhookConnectorValidationService.validateWebhookConnectorForUpdate(webhookConnectorInDb,
        connectorToUpdate);

    boolean enabledValue = !connectorToUpdate.mappings().isEmpty() && connectorToUpdate.enabled();
    WebhookConnector mergedConnector = new WebhookConnector(webhookConnectorInDb.id(),
        webhookConnectorInDb.identifier(), connectorToUpdate.title(),
        connectorToUpdate.description(), enabledValue, connectorToUpdate.mappings(),
        connectorToUpdate.security());

    return webhookConnectorRepositoryPort.save(mergedConnector);
  }

  @Transactional
  public void deleteWebhookConnector(String webhookConnectorIdentifier) {
    webhookConnectorValidationService.validateIdentifierExists(webhookConnectorIdentifier);
    webhookConnectorRepositoryPort.deleteByIdentifier(webhookConnectorIdentifier);
  }

  public Page<WebhookConnector> getAllWebhookConnector(Pageable pageable) {
    return webhookConnectorRepositoryPort.findAll(pageable);
  }
}
