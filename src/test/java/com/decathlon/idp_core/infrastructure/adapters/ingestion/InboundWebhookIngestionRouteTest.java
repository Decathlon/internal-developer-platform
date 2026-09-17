package com.decathlon.idp_core.infrastructure.adapters.ingestion;

import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.HTTP_BAD_REQUEST;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.HTTP_CREATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.jdbc.Sql;

import com.decathlon.idp_core.AbstractIntegrationTest;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookSecurity;
import com.decathlon.idp_core.domain.port.WebhookSecurityStrategy;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookJwksHostForbiddenException;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.security.WebhookJwtDecoderProvider;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Integration tests for Camel webhook ingestion entrypoint.
 */
@DisplayName("Inbound Webhook Ingestion Route Integration Tests")
@Import(InboundWebhookIngestionRouteTest.JwtDecoderProviderTestConfiguration.class)
@Sql(scripts = {"/db/test/R__1_Insert_test_data.sql", "/db/test/R__4_insert_webhook_test_data.sql",
    "/db/test/R__5_insert_disabled_webhook_connector.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class InboundWebhookIngestionRouteTest extends AbstractIntegrationTest {

  private static final String HMAC_PREFIX = "sha256=";
  private static final String BASIC_AUTH_USERNAME_ENV_KEY = "BASIC_AUTH_USERNAME";
  private static final String BASIC_AUTH_USERNAME = "admin";
  private static final AtomicBoolean UNSUPPORTED_STATIC_TOKEN_STRATEGY_ENABLED = new AtomicBoolean(
      false);

  @Value("${app.ingestion.webhook.test-security.token-env-key}")
  private String webhookTokenEnvKey;

  @Value("${app.ingestion.webhook.test-security.token-env-value}")
  private String webhookTokenEnvValue;

  @Value("${app.ingestion.webhook.test-security.hmac-env-key}")
  private String githubSecretEnvKey;

  @Value("${app.ingestion.webhook.test-security.hmac-env-value}")
  private String githubSecretEnvValue;

  @Value("${app.ingestion.webhook.test-security.basic-auth-env-key}")
  private String basicAuthEnvKey;

  @Value("${app.ingestion.webhook.test-security.basic-auth-env-value}")
  private String basicAuthEnvValue;

  @Value("${app.ingestion.webhook.test-security.jwks-env-key}")
  private String jwksEnvKey;

  @Value("${app.ingestion.webhook.test-security.jwks-env-value}")
  private String jwksEnvValue;

  @Autowired
  private ProducerTemplate producerTemplate;

  @Autowired
  private WebhookJwtDecoderProvider jwtDecoderProvider;

  @Autowired
  private JwtDecoder jwtDecoder;

  @TestConfiguration
  static class JwtDecoderProviderTestConfiguration {

    @Bean
    @Primary
    WebhookJwtDecoderProvider jwtDecoderProvider() {
      return mock(WebhookJwtDecoderProvider.class);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    WebhookSecurityStrategy unsupportedStaticTokenStrategy() {
      return new WebhookSecurityStrategy() {
        @Override
        public boolean supports(WebhookSecurityType securityType) {
          return UNSUPPORTED_STATIC_TOKEN_STRATEGY_ENABLED.get()
              && securityType == WebhookSecurityType.STATIC_TOKEN;
        }

        @Override
        public void validateConfiguration(Map<String, String> config) {
          // No-op on purpose: this bean only exists to exercise the runtime guard.
        }
      };
    }
  }

  @BeforeEach
  void configureRuntimeSecretsForWebhookSecurity() {
    System.setProperty(webhookTokenEnvKey, webhookTokenEnvValue);
    System.setProperty(githubSecretEnvKey, githubSecretEnvValue);
    System.setProperty(basicAuthEnvKey, basicAuthEnvValue);
    System.setProperty(BASIC_AUTH_USERNAME_ENV_KEY, BASIC_AUTH_USERNAME);
    System.setProperty(jwksEnvKey, jwksEnvValue);
  }

  @AfterEach
  void clearRuntimeSecretsForWebhookSecurity() {
    System.clearProperty(webhookTokenEnvKey);
    System.clearProperty(githubSecretEnvKey);
    System.clearProperty(basicAuthEnvKey);
    System.clearProperty(BASIC_AUTH_USERNAME_ENV_KEY);
    System.clearProperty(jwksEnvKey);
  }

  private Exchange invokeValidateSecurityRoute(WebhookConnector webhookConnector,
      Map<String, Object> headers) {
    return producerTemplate.request("direct:validate-security", exchange -> {
      exchange.setProperty("connectorIdentifier", webhookConnector.identifier());
      exchange.setProperty("webhookConfig", webhookConnector);
      exchange.setProperty("rawPayloadBody", "{}");
      headers.forEach((name, value) -> exchange.getIn().setHeader(name, value));
    });
  }

  private WebhookConnector webhookConnectorWithSecurity(String identifier,
      WebhookSecurity security) {
    return new WebhookConnector(UUID.randomUUID(), identifier, "Test Connector", "test", true,
        List.of(), security);
  }

  private Exchange invokeIngestionRoute(String connectorIdentifier) {
    return invokeIngestionRoute(connectorIdentifier, "{\"event\":\"ping\"}", null);
  }

  private Exchange invokeIngestionRoute(String connectorIdentifier, Object payload,
      String contentEncoding) {
    return invokeIngestionRoute(connectorIdentifier, payload, contentEncoding, Map.of());
  }

  private Exchange invokeIngestionRoute(String connectorIdentifier, Object payload,
      String contentEncoding, Map<String, Object> additionalHeaders) {
    return producerTemplate.request("direct:process-event", exchange -> {
      exchange.setProperty("connectorIdentifier", connectorIdentifier);
      exchange.getIn().setBody(payload);
      if (contentEncoding != null) {
        exchange.getIn().setHeader("Content-Encoding", contentEncoding);
      }
      additionalHeaders.forEach((name, value) -> exchange.getIn().setHeader(name, value));
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

  private String computeHmacSha256Signature(String payload) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(
        new SecretKeySpec(githubSecretEnvValue.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    return HMAC_PREFIX + toHex(digest);
  }

  private static String toHex(byte[] bytes) {
    StringBuilder builder = new StringBuilder(bytes.length * 2);
    for (byte value : bytes) {
      builder.append(String.format("%02x", value));
    }
    return builder.toString();
  }

  private Exchange invokeValidationWithoutWebhookConfig() {
    return producerTemplate.request("direct:validate-enabled",
        exchange -> exchange.setProperty("connectorIdentifier", "missing-config-connector"));
  }

  private void assertJsonSuccessResponse(Exchange exchange) throws Exception {
    JsonNode response = objectMapper.readTree(exchange.getMessage().getBody(String.class));
    assertEquals("SUCCESS", response.get("status").asText());
    assertEquals("Inbound Webhook event processed successfully.", response.get("message").asText());
  }

  private void assertJsonErrorResponse(Exchange exchange, String expectedError,
      String expectedDescription) throws Exception {
    JsonNode response = objectMapper.readTree(exchange.getMessage().getBody(String.class));
    assertEquals(expectedError, response.get("error").asText());
    assertEquals(expectedDescription, response.get("error_description").asText());
  }

  private void assertSecurityValidationSucceeded(Exchange exchange) {
    assertNull(exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertNull(exchange.getMessage().getBody());
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
    assertJsonErrorResponse(exchange, "webhook_connector_not_found",
        "Webhook configuration not found");
  }

  @Test
  @DisplayName("Route returns 403 when webhook exists but is disabled")
  void postWebhookRoute_403_whenWebhookIsDisabled() throws Exception {
    Exchange exchange = invokeIngestionRoute("disabled-connector");

    assertEquals(403, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    assertJsonErrorResponse(exchange, "webhook_connector_disabled",
        "Webhook connector is disabled");
  }

  @Test
  @DisplayName("Route returns 401 when webhook authentication is missing")
  void postWebhookRoute_401_whenWebhookAuthenticationIsMissing() throws Exception {
    Exchange exchange = invokeIngestionRoute("token-connector");

    assertEquals(401, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    assertJsonErrorResponse(exchange, "webhook_authentication_failed",
        "Webhook authentication failed");
  }

  @Test
  @DisplayName("Route returns 403 when webhook authentication credentials are rejected")
  void postWebhookRoute_403_whenWebhookAuthenticationFails() throws Exception {
    Exchange exchange = invokeIngestionRoute("token-connector", "{\"event\":\"ping\"}", null,
        Map.of("X-Auth-Token", "invalid-token"));

    assertEquals(403, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    assertJsonErrorResponse(exchange, "webhook_forbidden", "Invalid credentials");
  }

  @Test
  @DisplayName("Route returns 201 when STATIC_TOKEN header matches runtime secret")
  void postWebhookRoute_201_whenStaticTokenAuthenticationSucceeds() throws Exception {
    Exchange exchange = invokeIngestionRoute("token-connector", "{\"event\":\"ping\"}", null,
        Map.of("X-Auth-Token", webhookTokenEnvValue));

    assertEquals(HTTP_CREATED, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    assertJsonSuccessResponse(exchange);
  }

  @Test
  @DisplayName("Route returns 201 when HMAC signature matches runtime secret")
  void postWebhookRoute_201_whenHmacAuthenticationSucceeds() throws Exception {
    String payload = """
        {
          "action": "pushed",
          "repository": {
            "full_name": "acme/payment-service",
            "name": "payment-service",
            "language": "JAVA"
          },
          "sender": {
            "email": "owner@acme.test"
          },
          "ref": "1.0.0"
        }
        """;
    String signature = computeHmacSha256Signature(payload);
    Exchange exchange = invokeIngestionRoute("github-dora-connector-success", payload, null,
        Map.of("X-Hub-Signature-256", signature));

    assertEquals(HTTP_CREATED, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    assertJsonSuccessResponse(exchange);
  }

  @Test
  @DisplayName("Route returns 401 when HMAC signature prefix format is invalid")
  void postWebhookRoute_401_whenHmacSignaturePrefixIsInvalid() throws Exception {
    Exchange exchange = invokeIngestionRoute("github-dora-connector", "{\"action\":\"pushed\"}",
        null, Map.of("X-Hub-Signature-256", "sha1=abc123"));

    assertEquals(401, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    assertJsonErrorResponse(exchange, "webhook_authentication_failed",
        "Webhook authentication failed");
  }

  @Test
  @DisplayName("Validate-security route accepts NONE mode")
  void validateSecurityRoute_acceptsNoneMode() {
    WebhookConnector connector = webhookConnectorWithSecurity("none-connector",
        new WebhookSecurity(WebhookSecurityType.NONE, Map.of()));

    Exchange exchange = invokeValidateSecurityRoute(connector, Map.of());

    assertNull(exchange.getException());
  }

  @Test
  @DisplayName("Validate-security route accepts BASIC_AUTH mode")
  void validateSecurityRoute_acceptsBasicAuthMode() {
    String credentials = Base64.getEncoder().encodeToString(
        (BASIC_AUTH_USERNAME + ":" + basicAuthEnvValue).getBytes(StandardCharsets.UTF_8));
    WebhookConnector connector = webhookConnectorWithSecurity("basic-connector",
        new WebhookSecurity(WebhookSecurityType.BASIC_AUTH, Map.of("username",
            "env:" + BASIC_AUTH_USERNAME_ENV_KEY, "secret_alias", basicAuthEnvKey)));

    Exchange exchange = invokeValidateSecurityRoute(connector,
        Map.of("Authorization", "Basic " + credentials));

    assertSecurityValidationSucceeded(exchange);
  }

  @Test
  @DisplayName("Validate-security route returns 401 when Basic auth scheme is invalid")
  void validateSecurityRoute_401_whenBasicSchemeIsInvalid() throws Exception {
    WebhookConnector connector = webhookConnectorWithSecurity("basic-connector",
        new WebhookSecurity(WebhookSecurityType.BASIC_AUTH,
            Map.of("username", BASIC_AUTH_USERNAME_ENV_KEY, "secret_alias", basicAuthEnvKey)));

    Exchange exchange = invokeValidateSecurityRoute(connector,
        Map.of("Authorization", "Bearer not-basic"));

    assertEquals(401, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    assertJsonErrorResponse(exchange, "webhook_authentication_failed",
        "Webhook authentication failed");
  }

  @Test
  @DisplayName("Validate-security route returns 403 when Basic username is rejected")
  void validateSecurityRoute_403_whenBasicUsernameIsRejected() throws Exception {
    String credentials = Base64.getEncoder()
        .encodeToString(("wrong-user:" + basicAuthEnvValue).getBytes(StandardCharsets.UTF_8));
    WebhookConnector connector = webhookConnectorWithSecurity("basic-connector",
        new WebhookSecurity(WebhookSecurityType.BASIC_AUTH,
            Map.of("username", BASIC_AUTH_USERNAME_ENV_KEY, "secret_alias", basicAuthEnvKey)));

    Exchange exchange = invokeValidateSecurityRoute(connector,
        Map.of("Authorization", "Basic " + credentials));

    assertEquals(403, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    assertJsonErrorResponse(exchange, "webhook_forbidden", "Invalid credentials");
  }

  @Test
  @DisplayName("Validate-security route returns 403 when Basic password is rejected")
  void validateSecurityRoute_403_whenBasicPasswordIsRejected() throws Exception {
    String credentials = Base64.getEncoder()
        .encodeToString((BASIC_AUTH_USERNAME + ":wrong-password").getBytes(StandardCharsets.UTF_8));
    WebhookConnector connector = webhookConnectorWithSecurity("basic-connector",
        new WebhookSecurity(WebhookSecurityType.BASIC_AUTH,
            Map.of("username", BASIC_AUTH_USERNAME_ENV_KEY, "secret_alias", basicAuthEnvKey)));

    Exchange exchange = invokeValidateSecurityRoute(connector,
        Map.of("Authorization", "Basic " + credentials));

    assertEquals(403, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    assertJsonErrorResponse(exchange, "webhook_forbidden", "Invalid credentials");
  }

  @Test
  @DisplayName("Validate-security route accepts JWT_BEARER mode")
  void validateSecurityRoute_acceptsJwtBearerMode() {
    String expectedClientEmail = "ps-fb25-product-events-produ@cpe-idp-stg-337o.iam.gserviceaccount.com";
    String jwtPayload = "{\"email\":\"" + expectedClientEmail + "\"}";
    String encodedHeader = Base64.getUrlEncoder().withoutPadding()
        .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
    String encodedPayload = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(jwtPayload.getBytes(StandardCharsets.UTF_8));
    String token = encodedHeader + "." + encodedPayload + ".signature";
    String jwksUri = "https://www.googleapis.com/oauth2/v3/certs";

    Jwt jwt = Jwt.withTokenValue(token).header("alg", "RS256").claim("email", expectedClientEmail)
        .build();
    when(jwtDecoderProvider.get(jwksUri)).thenReturn(jwtDecoder);
    when(jwtDecoder.decode(token)).thenReturn(jwt);

    WebhookConnector connector = webhookConnectorWithSecurity("jwt-connector",
        new WebhookSecurity(WebhookSecurityType.JWT_BEARER, Map.of("jwks_uri", jwksUri,
            "client_id_field", "email", "client_id_values", expectedClientEmail)));

    Exchange exchange = invokeValidateSecurityRoute(connector,
        Map.of("Authorization", "Bearer " + token));

    assertSecurityValidationSucceeded(exchange);
  }

  @Test
  @DisplayName("Validate-security route returns 403 when the JWKS host is not allow-listed")
  void validateSecurityRoute_403_whenJwksHostIsNotAllowListed() {
    String jwksUri = "https://github.com/oauth2/certs";
    when(jwtDecoderProvider.get(jwksUri))
        .thenThrow(new WebhookJwksHostForbiddenException("github.com"));
    WebhookConnector connector = webhookConnectorWithSecurity("jwt-connector",
        new WebhookSecurity(WebhookSecurityType.JWT_BEARER, Map.of("jwks_uri", jwksUri,
            "client_id_field", "email", "client_id_values", "expected@example.com")));

    Exchange exchange = invokeValidateSecurityRoute(connector,
        Map.of("Authorization", "Bearer signed-token"));

    assertEquals(403, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    try {
      JsonNode response = objectMapper.readTree(exchange.getMessage().getBody(String.class));
      assertEquals("webhook_forbidden", response.get("error").asText());
      assertEquals("Invalid credentials", response.get("error_description").asText());
    } catch (Exception exception) {
      throw new AssertionError("Unable to read webhook error response", exception);
    }
  }

  @Test
  @DisplayName("Route returns 500 when internal webhook configuration invariant is broken")
  void postWebhookRoute_500_whenWebhookConfigurationIsMissing() throws Exception {
    Exchange exchange = invokeValidationWithoutWebhookConfig();

    assertEquals(500, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    assertJsonErrorResponse(exchange, "webhook_configuration_missing",
        "Webhook configuration unavailable");
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
    assertJsonErrorResponse(exchange, "invalid_compressed_payload",
        "Invalid, unsupported, or oversized compressed payload");
  }

  @Test
  @DisplayName("Route returns 400 when gzip header is declared with null payload")
  void postWebhookRoute_400_whenGzipHeaderPayloadIsNull() throws Exception {
    Exchange exchange = invokeIngestionRoute("public-connector", null, "gzip");

    assertEquals(HTTP_BAD_REQUEST, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
    assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
    assertJsonErrorResponse(exchange, "invalid_compressed_payload",
        "Invalid, unsupported, or oversized compressed payload");
  }

  @Test
  @DisplayName("Validate-security route returns 401 when a matching strategy does not implement the runtime authenticator contract")
  void validateSecurityRoute_401_whenMatchingStrategyIsNotAuthenticator() throws Exception {
    UNSUPPORTED_STATIC_TOKEN_STRATEGY_ENABLED.set(true);
    try {
      WebhookConnector connector = webhookConnectorWithSecurity("static-token-connector",
          new WebhookSecurity(WebhookSecurityType.STATIC_TOKEN,
              Map.of("header_name", "X-Auth-Token", "secret_alias", webhookTokenEnvKey)));

      Exchange exchange = invokeValidateSecurityRoute(connector,
          Map.of("X-Auth-Token", "any-token"));

      assertEquals(401, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
      assertEquals("application/json", exchange.getMessage().getHeader(Exchange.CONTENT_TYPE));
      assertJsonErrorResponse(exchange, "webhook_authentication_failed",
          "Webhook authentication failed");
    } finally {
      UNSUPPORTED_STATIC_TOKEN_STRATEGY_ENABLED.set(false);
    }
  }

  private static Stream<Arguments> successDecodingCases() throws Exception {
    return Stream.of(Arguments.of("{\"event\":\"identity\"}", "identity"),
        Arguments.of(gzipSamplePayload(), "gzip"), Arguments.of(null, null));
  }
}
