package com.decathlon.idp_core.infrastructure.adapters.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import com.decathlon.idp_core.AbstractIntegrationTest;

/**
 * Integration tests for Camel webhook ingestion entrypoint.
 */
@DisplayName("Inbound Webhook Ingestion Route Integration Tests")
@Sql(scripts = {"/db/test/R__1_Insert_test_data.sql", "/db/test/R__4_insert_webhook_test_data.sql",
    "/db/test/R__5_insert_disabled_webhook_connector.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class InboundWebhookIngestionRouteTest extends AbstractIntegrationTest {

  @Autowired
  private ProducerTemplate producerTemplate;

  private Exchange invokeIngestionRoute(String connectorIdentifier) {
    return producerTemplate.request("direct:process-event", exchange -> {
      exchange.setProperty("connectorIdentifier", connectorIdentifier);
      exchange.getIn().setBody("{\"event\":\"ping\"}");
    });
  }

  @Test
  @DisplayName("Route returns 200 when webhook exists and is enabled")
  void postWebhookRoute_200_whenWebhookExistsAndEnabled() throws Exception {
    Exchange exchange = invokeIngestionRoute("public-connector");

    assertEquals(200, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    assertEquals(
        "{\"status\": \"SUCCESS\", \"message\": \"Webhook configuration loaded and enabled.\"}",
        exchange.getMessage().getBody(String.class));
  }

  @Test
  @DisplayName("Route returns 404 when webhook does not exist")
  void postWebhookRoute_404_whenWebhookDoesNotExist() throws Exception {
    Exchange exchange = invokeIngestionRoute("does-not-exist");

    assertEquals(404, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    assertEquals("{\"error\": \"Webhook configuration not found\"}",
        exchange.getMessage().getBody(String.class));
  }

  @Test
  @DisplayName("Route returns 403 when webhook exists but is disabled")
  void postWebhookRoute_403_whenWebhookIsDisabled() throws Exception {
    Exchange exchange = invokeIngestionRoute("disabled-connector");

    assertEquals(403, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    assertEquals("{\"error\": \"Webhook connector is disabled\"}",
        exchange.getMessage().getBody(String.class));
  }
}
