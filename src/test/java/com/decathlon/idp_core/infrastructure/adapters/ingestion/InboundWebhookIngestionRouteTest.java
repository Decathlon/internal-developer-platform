package com.decathlon.idp_core.infrastructure.adapters.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import com.decathlon.idp_core.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;

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
    return invokeIngestionRoute(connectorIdentifier, "{\"event\":\"ping\"}", null);
  }

  private Exchange invokeIngestionRoute(String connectorIdentifier, Object payload,
      String contentEncoding) {
    return producerTemplate.request("direct:process-event", exchange -> {
      exchange.setProperty("connectorIdentifier", connectorIdentifier);
      exchange.getIn().setBody(payload);
      if (contentEncoding != null) {
        exchange.getIn().setHeader("Content-Encoding", contentEncoding);
      }
    });
  }

  private byte[] gzip(String payload) throws Exception {
    try (ByteArrayOutputStream output = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutput = new GZIPOutputStream(output)) {
      gzipOutput.write(payload.getBytes(StandardCharsets.UTF_8));
      gzipOutput.finish();
      return output.toByteArray();
    }
  }

  private Exchange invokeValidationWithoutWebhookConfig() {
    return producerTemplate.request("direct:validate-enabled",
        exchange -> exchange.setProperty("connectorIdentifier", "missing-config-connector"));
  }

  private void assertJsonSuccessResponse(Exchange exchange) throws Exception {
    JsonNode response = objectMapper.readTree(exchange.getMessage().getBody(String.class));
    assertEquals("SUCCESS", response.get("status").asText());
    assertEquals("Webhook configuration loaded and enabled.", response.get("message").asText());
  }

  private void assertJsonErrorResponse(Exchange exchange, String expectedError,
      String expectedDescription) throws Exception {
    JsonNode response = objectMapper.readTree(exchange.getMessage().getBody(String.class));
    assertEquals(expectedError, response.get("error").asText());
    assertEquals(expectedDescription, response.get("errorDescription").asText());
  }

  @Test
  @DisplayName("Route returns 200 when webhook exists and is enabled")
  void postWebhookRoute_200_whenWebhookExistsAndEnabled() throws Exception {
    Exchange exchange = invokeIngestionRoute("public-connector");

    assertEquals(200, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    assertJsonSuccessResponse(exchange);
  }

  @Test
  @DisplayName("Route returns 404 when webhook does not exist")
  void postWebhookRoute_404_whenWebhookDoesNotExist() throws Exception {
    Exchange exchange = invokeIngestionRoute("does-not-exist");

    assertEquals(404, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    assertJsonErrorResponse(exchange, "NOT_FOUND", "Webhook configuration not found");
  }

  @Test
  @DisplayName("Route returns 403 when webhook exists but is disabled")
  void postWebhookRoute_403_whenWebhookIsDisabled() throws Exception {
    Exchange exchange = invokeIngestionRoute("disabled-connector");

    assertEquals(403, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    assertJsonErrorResponse(exchange, "FORBIDDEN", "Webhook connector is disabled");
  }

  @Test
  @DisplayName("Route returns 500 when internal webhook configuration invariant is broken")
  void postWebhookRoute_500_whenWebhookConfigurationIsMissing() throws Exception {
    Exchange exchange = invokeValidationWithoutWebhookConfig();

    assertEquals(500, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    assertJsonErrorResponse(exchange, "INTERNAL_SERVER_ERROR", "Webhook configuration unavailable");
  }

  @Test
  @DisplayName("Route accepts payload without encoding and returns 200")
  void postWebhookRoute_200_withIdentityPayload() throws Exception {
    Exchange exchange = invokeIngestionRoute("public-connector", "{\"event\":\"identity\"}",
        "identity");

    assertEquals(200, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    assertJsonSuccessResponse(exchange);
  }

  @Test
  @DisplayName("Route accepts gzip payload and returns 200")
  void postWebhookRoute_200_withGzipPayload() throws Exception {
    byte[] compressedPayload = gzip("{\"event\":\"gzip\"}");
    Exchange exchange = invokeIngestionRoute("public-connector", compressedPayload, "gzip");

    assertEquals(200, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    assertJsonSuccessResponse(exchange);
  }
}
