package com.decathlon.idp_core.infrastructure.adapters.api.auth.http_message_signature;

import static com.decathlon.idp_core.infrastructure.adapters.api.auth.http_message_signature.HttpMessageSignatureVerifierTest.ID;
import static com.decathlon.idp_core.infrastructure.adapters.api.auth.http_message_signature.HttpMessageSignatureVerifierTest.signed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.decathlon.idp_core.AbstractIntegrationTest;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.HttpMessageSignatureAlgorithm;
import com.fasterxml.jackson.annotation.JsonProperty;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "app.security.http-message-signature.enabled=true",
    "app.security.http-message-signature.secret=http-message-test-secret",
    "app.ingestion.webhook.base-path=/signature-webhooks", "spring.threads.virtual.enabled=true"})
@Sql(statements = {"""
    INSERT INTO webhook_connector (id, identifier, name, description, enabled, security)
    VALUES ('80e8400e-e29b-41d4-a716-446655440001', 'signature-test-hmac', 'HTTP message test HMAC',
            'HTTP message signature HTTP fixture', true,
            '{"type":"HMAC_SHA256","config":{"header_name":"X-Hub-Signature-256",
              "secret_alias":"HTTP_MESSAGE_TEST_WEBHOOK_SECRET","prefix":"sha256="}}'::jsonb);
    """,
    """
        INSERT INTO webhook_connector (id, identifier, name, description, enabled, security)
        VALUES ('80e8400e-e29b-41d4-a716-446655440002', 'signature-test-token', 'HTTP message test token',
                'HTTP message signature HTTP fixture', true,
                '{"type":"STATIC_TOKEN","config":{"header_name":"X-Auth-Token",
                  "secret_alias":"HTTP_MESSAGE_TEST_WEBHOOK_SECRET"}}'::jsonb);
        """}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(statements = """
    DELETE FROM webhook_connector WHERE identifier IN ('signature-test-hmac', 'signature-test-token')
    """, executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
class HttpMessageSignatureIntegrationTest extends AbstractIntegrationTest {

  @LocalServerPort
  private int port;

  @Autowired
  private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

  private RestTestClient client;

  @BeforeEach
  void setUpClient() {
    client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    System.setProperty("HTTP_MESSAGE_TEST_WEBHOOK_SECRET", "http-message-webhook-test-secret");
    org.mockito.Mockito.clearInvocations(jwtDecoder);
  }

  @AfterEach
  void clearWebhookSecret() {
    System.clearProperty("HTTP_MESSAGE_TEST_WEBHOOK_SECRET");
  }

  @ParameterizedTest
  @ValueSource(strings = {"/api/v1/entities", "/", "/login",
      "/signature-webhooks/public-connector"})
  void unsignedRequestsStopBeforeDownstreamAuthentication(String path) {
    client.post().uri(path).header("Authorization", "Bearer invalid")
        .contentType(MediaType.APPLICATION_JSON).body("{}").exchange().expectStatus()
        .isUnauthorized().expectHeader()
        .valueEquals("WWW-Authenticate", "Signature realm=\"idp-core\"").expectHeader()
        .valueEquals("Cache-Control", "no-store").expectBody(SignatureError.class).value(error -> {
          assertThat(error.error()).isEqualTo("UNAUTHORIZED");
          assertThat(error.errorDescription()).isEqualTo("The HTTP message signature is missing");
        });
    verifyNoInteractions(jwtDecoder);
  }

  @Test
  void preflightAlsoRequiresHttpMessageSignature() {
    client.options().uri("/api/v1/entities").header("Origin", "https://example.com")
        .header("Access-Control-Request-Method", "POST").exchange().expectStatus().isUnauthorized()
        .expectBody(SignatureError.class).value(error -> assertThat(error.errorDescription())
            .isEqualTo("The HTTP message signature is missing"));
  }

  @Test
  void validSignatureStillRequiresJwtAuthentication() throws Exception {
    client.get().uri("/api/v1/entities").header("x-request-id", ID)
        .header("Signature", currentSignature()).exchange().expectStatus().isUnauthorized()
        .expectHeader().value("WWW-Authenticate", value -> assertThat(value).startsWith("Bearer"));
  }

  @Test
  void validSignatureDoesNotBypassConnectorAuthentication() throws Exception {
    client.post().uri("/signature-webhooks/signature-test-token").header("x-request-id", ID)
        .header("Signature", currentSignature()).contentType(MediaType.APPLICATION_JSON)
        .body("{\"event\":\"ping\"}").exchange().expectStatus().isUnauthorized()
        .expectBody(SignatureError.class)
        .value(error -> assertThat(error.error()).isEqualTo("webhook_authentication_failed"));
  }

  @Test
  void directClientAndWebhookSignaturesPreserveExactPayloadBytes() throws Exception {
    String payload = "{ \"event\" : \"ping\", \"unicode\" : \"\\u00e9\" }\n";
    var mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec("http-message-webhook-test-secret".getBytes(StandardCharsets.UTF_8),
        "HmacSHA256"));
    String webhookSignature = "sha256="
        + HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    client.post().uri("/signature-webhooks/signature-test-hmac").header("x-request-id", ID)
        .header("Signature", currentSignature()).header("X-Hub-Signature-256", webhookSignature)
        .contentType(MediaType.APPLICATION_JSON).body(payload).exchange().expectStatus()
        .isCreated();
  }

  @Test
  void healthExclusionPreservesPublicAccess() {
    client.get().uri("/actuator/health").exchange().expectStatus().isOk();
  }

  @Test
  void expiredAndTamperedSignaturesRejectBeforeWebhookProcessing() throws Exception {
    long now = Instant.now().getEpochSecond();
    String expired = signed(HttpMessageSignatureAlgorithm.HMAC_SHA256, "x-request-id", ID,
        Long.toString(now - 60), Long.toString(now - 30));
    client.post().uri("/signature-webhooks/public-connector").header("x-request-id", ID)
        .header("Signature", expired).contentType(MediaType.APPLICATION_JSON).body("{}").exchange()
        .expectStatus().isUnauthorized().expectBody(SignatureError.class)
        .value(error -> assertThat(error.errorDescription())
            .isEqualTo("The HTTP message signature is expired"));
    client.post().uri("/signature-webhooks/public-connector").header("x-request-id", "tampered")
        .header("Signature", currentSignature()).contentType(MediaType.APPLICATION_JSON).body("{}")
        .exchange().expectStatus().isUnauthorized().expectBody(SignatureError.class)
        .value(error -> assertThat(error.errorDescription())
            .isEqualTo("The HTTP message signature is invalid"));
  }

  private String currentSignature() throws Exception {
    long now = Instant.now().getEpochSecond();
    return signed(HttpMessageSignatureAlgorithm.HMAC_SHA256, "x-request-id", ID, Long.toString(now),
        Long.toString(now + 30));
  }

  private record SignatureError(String error,
      @JsonProperty("error_description") String errorDescription) {
  }
}
