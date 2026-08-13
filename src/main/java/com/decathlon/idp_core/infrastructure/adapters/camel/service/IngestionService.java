package com.decathlon.idp_core.infrastructure.adapters.camel.service;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IngestionService {

  public void ingest(WebhookConnector webhookConnectorConfiguration, String payload) {

    webhookConnectorConfiguration.mappings().forEach(mapping -> {
      log.info("Ingesting entity for webhook: {} with mapping: {}", webhookConnectorConfiguration,
          mapping);
    });

  }

}
