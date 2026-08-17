package com.decathlon.idp_core.infrastructure.adapters.ingestion;

import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.HTTP_BAD_REQUEST;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.HTTP_CREATED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

  private static byte[] gzipSamplePayload() throws Exception {
    try (ByteArrayOutputStream output = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutput = new GZIPOutputStream(output)) {
      gzipOutput.write("{\"event\":\"gzip\"}".getBytes(StandardCharsets.UTF_8));
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
    assertEquals("Webhook entity updated.", response.get("message").asText());
  }

  private void assertJsonErrorResponse(Exchange exchange, String expectedError,
      String expectedDescription) throws Exception {
    JsonNode response = objectMapper.readTree(exchange.getMessage().getBody(String.class));
    assertEquals(expectedError, response.get("error").asText());
    assertEquals(expectedDescription, response.get("error_description").asText());
  }

  @Test
  @DisplayName("Route returns 200 when webhook exists and is enabled")
  void postWebhookRoute_201_whenWebhookExistsAndEnabled() throws Exception {
    Exchange exchange = invokeIngestionRoute("public-connector");

    assertEquals(HTTP_CREATED, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
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

  @ParameterizedTest(name = "[{index}]")
  @MethodSource("successDecodingCases")
  @DisplayName("Route returns 201 for supported decoding scenarios")
  void postWebhookRoute_201_forSupportedDecodingScenarios(Object payload, String contentEncoding)
      throws Exception {
    Exchange exchange = invokeIngestionRoute("public-connector", payload, contentEncoding);

    assertEquals(HTTP_CREATED, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    assertJsonSuccessResponse(exchange);
  }

  @Test
  @DisplayName("Route returns 400 when gzip header is declared but payload is not compressed")
  void postWebhookRoute_400_whenGzipHeaderPayloadIsCorrupted() throws Exception {
    Exchange exchange = invokeIngestionRoute("public-connector", "{\"event\":\"not-gzip\"}",
        "gzip");

    assertEquals(HTTP_BAD_REQUEST, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    assertJsonErrorResponse(exchange, "BAD_REQUEST", "Corrupted or invalid compressed gzip stream");
  }

  @Test
  @DisplayName("Route returns 400 when gzip header is declared with null payload")
  void postWebhookRoute_400_whenGzipHeaderPayloadIsNull() throws Exception {
    Exchange exchange = invokeIngestionRoute("public-connector", null, "gzip");

    assertEquals(HTTP_BAD_REQUEST, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    assertJsonErrorResponse(exchange, "BAD_REQUEST", "Empty payload cannot be decoded as gzip");
  }

  private static Stream<Arguments> successDecodingCases() throws Exception {
    return Stream.of(Arguments.of("{\"event\":\"identity\"}", "identity"),
        Arguments.of(gzipSamplePayload(), "gzip"), Arguments.of(null, null));
  }
}
