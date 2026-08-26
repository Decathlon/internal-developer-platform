package com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration;

import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.CONNECTOR_IDENTIFIER_PROPERTY;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.DIRECT_PROCESS_EVENT;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.DIRECT_WEBHOOK_INGESTION;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.ROUTE_ID_GENERIC_WEBHOOK_ENTRYPOINT;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.WEBHOOK_IDENTIFIER_HEADER;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/// Centralizes REST endpoint configuration for the webhook ingestion pipeline.
@Component
@EnableConfigurationProperties(IngestionProperties.class)
@RequiredArgsConstructor
public class CamelRestConfiguration extends RouteBuilder {

  private final IngestionProperties ingestionProperties;

  @Override
  public void configure() throws Exception {

    restConfiguration().component("platform-http")
        .bindingMode(org.apache.camel.model.rest.RestBindingMode.off);

    rest(ingestionProperties.basePath()).post(ingestionProperties.identifierPath())
        .description("Generic webhook ingestion endpoint").to(DIRECT_WEBHOOK_INGESTION);

    from(DIRECT_WEBHOOK_INGESTION).routeId(ROUTE_ID_GENERIC_WEBHOOK_ENTRYPOINT)
        .log(LoggingLevel.INFO,
            "Received generic request for identifier: ${header.webhookIdentifier}")
        .setProperty(CONNECTOR_IDENTIFIER_PROPERTY, header(WEBHOOK_IDENTIFIER_HEADER))
        .to(DIRECT_PROCESS_EVENT);
  }
}
