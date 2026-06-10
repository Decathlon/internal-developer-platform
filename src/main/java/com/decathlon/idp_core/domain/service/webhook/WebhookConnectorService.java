package com.decathlon.idp_core.domain.service.webhook;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorNotFoundException;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.port.WebhookConnectorRepositoryPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class WebhookConnectorService {

  private final WebhookConnectorRepositoryPort webhookConnectorRepositoryPort;
  private final WebhookConnectorValidationService webhookConnectorValidationService;

  public WebhookConnector getWebhookConnector(String identifier) {
    return webhookConnectorRepositoryPort.findByIdentifier(identifier)
        .orElseThrow(() -> new WebhookConnectorNotFoundException(identifier));
  }

  @Transactional
  public WebhookConnector createWebhookConnector(@Valid WebhookConnector connector) {
    webhookConnectorValidationService.validateWebhookConnectorForCreation(connector);
    return webhookConnectorRepositoryPort.save(connector);
  }

  @Transactional
  public WebhookConnector updateWebhookConnector(String identifier,
      @Valid WebhookConnector connectorToUpdate) {
    WebhookConnector webhookConnectorInDb = getWebhookConnector(identifier);
    webhookConnectorValidationService.validateWebhookConnectorForUpdate(webhookConnectorInDb,
        connectorToUpdate);

    WebhookConnector mergedConnector = new WebhookConnector(webhookConnectorInDb.id(),
        webhookConnectorInDb.identifier(), connectorToUpdate.title(),
        connectorToUpdate.description(), connectorToUpdate.enabled(), connectorToUpdate.mappings(),
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
