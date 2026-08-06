package com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration;

import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.CONNECTOR_IDENTIFIER_PROPERTY;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.DIRECT_PROCESS_EVENT;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.DIRECT_WEBHOOK_INGESTION;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.ROUTE_ID_GENERIC_WEBHOOK_ENTRYPOINT;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.TRACE_METHOD_LOG_INBOUND_REQUEST;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.WEBHOOK_BASE_PATH;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.WEBHOOK_IDENTIFIER_HEADER;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.WEBHOOK_IDENTIFIER_PATH;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.infrastructure.adapters.ingestion.processor.WebhookIngestionTracingProcessor;

import lombok.RequiredArgsConstructor;

/**
 * Centralizes REST endpoint configuration for the webhook ingestion pipeline.
 *
 * Separates HTTP binding concerns (REST DSL, path parameters, descriptions)
 * from route logic (transformations, service invocations, error handling). This
 * follows the Single Responsibility Principle and makes it easier to modify API
 * contracts without touching domain route wiring.
 */
@Component
@RequiredArgsConstructor
public class CamelRestConfiguration extends RouteBuilder {

  private final WebhookIngestionTracingProcessor tracingProcessor;

  @Override
  public void configure() throws Exception {

    restConfiguration()
        // Use platform-http instead of servlet for better Spring Boot integration
        .component("platform-http").bindingMode(org.apache.camel.model.rest.RestBindingMode.off);

    rest(WEBHOOK_BASE_PATH).post(WEBHOOK_IDENTIFIER_PATH)
        .description("Generic webhook ingestion endpoint").to(DIRECT_WEBHOOK_INGESTION);

    from(DIRECT_WEBHOOK_INGESTION).routeId(ROUTE_ID_GENERIC_WEBHOOK_ENTRYPOINT)
        .log(LoggingLevel.INFO,
            "Received generic request for identifier: ${header.webhookIdentifier}")
        .setProperty(CONNECTOR_IDENTIFIER_PROPERTY, header(WEBHOOK_IDENTIFIER_HEADER))
        .bean(tracingProcessor, TRACE_METHOD_LOG_INBOUND_REQUEST).to(DIRECT_PROCESS_EVENT);

  }
}
