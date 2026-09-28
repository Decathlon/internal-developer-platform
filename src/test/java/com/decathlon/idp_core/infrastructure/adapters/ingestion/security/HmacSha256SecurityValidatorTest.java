package com.decathlon.idp_core.infrastructure.adapters.ingestion.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;

@DisplayName("HmacSha256SecurityValidator Tests")
class HmacSha256SecurityValidatorTest {

  private static final String HEADER_NAME = "X-Hub-Signature-256";
  private static final String SECRET_ALIAS = "TEST_HMAC_SECRET_VALID";

  private HmacSha256SecurityValidator validator;

  @BeforeEach
  void setUp() {
    validator = new HmacSha256SecurityValidator(new HmacSignatureValidator());
  }

  @Test
  @DisplayName("Should support HMAC_SHA256 only")
  void shouldSupportHmacSha256() {
    assertThat(validator.supports(WebhookSecurityType.HMAC_SHA256)).isTrue();
    assertThat(validator.supports(WebhookSecurityType.STATIC_TOKEN)).isFalse();
    assertThat(validator.supports(WebhookSecurityType.BASIC_AUTH)).isFalse();
    assertThat(validator.supports(WebhookSecurityType.JWT_BEARER)).isFalse();
  }

  @Test
  @DisplayName("Should validate a valid HMAC request")
  void shouldValidateValidHmacRequest() {
    System.setProperty(SECRET_ALIAS, "super-secret");
    try {
      byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
      String digest = "sha256="
          + new HmacSignatureValidator().computeHexSha256(payload, "super-secret");
      Map<String, Object> headers = Map.of(HEADER_NAME, digest, "Content-Type", "application/json");
      Map<String, String> config = Map.of("header_name", HEADER_NAME, "secret_alias", SECRET_ALIAS);

      assertThatCode(() -> validator.validateRequest(headers, payload, config))
          .doesNotThrowAnyException();
    } finally {
      System.clearProperty(SECRET_ALIAS);
    }
  }

  @Test
  @DisplayName("Should reject malformed HMAC prefix")
  void shouldRejectMalformedHmacPrefix() {
    System.setProperty(SECRET_ALIAS, "super-secret");
    try {
      byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);
      Map<String, Object> headers = Map.of(HEADER_NAME, "abc123", "Content-Type",
          "application/json");
      Map<String, String> config = Map.of("header_name", HEADER_NAME, "secret_alias", SECRET_ALIAS);

      assertThatThrownBy(() -> validator.validateRequest(headers, payload, config)).isInstanceOf(
          com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthUnauthorizedException.class)
          .hasMessageContaining("lacks required 'sha256=' prefix");
    } finally {
      System.clearProperty(SECRET_ALIAS);
    }
  }

  @Test
  @DisplayName("Should reject a signature mismatch")
  void shouldRejectSignatureMismatch() {
    System.setProperty(SECRET_ALIAS, "super-secret");
    try {
      byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
      Map<String, Object> headers = Map.of(HEADER_NAME, "sha256=deadbeef", "Content-Type",
          "application/json");
      Map<String, String> config = Map.of("header_name", HEADER_NAME, "secret_alias", SECRET_ALIAS);

      assertThatThrownBy(() -> validator.validateRequest(headers, payload, config)).isInstanceOf(
          com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthForbiddenException.class)
          .hasMessageContaining("signature does not match computed HMAC");
    } finally {
      System.clearProperty(SECRET_ALIAS);
    }
  }

  @Test
  @DisplayName("Should reject transformed form payloads before signature validation")
  void shouldRejectTransformedFormPayloads() {
    System.setProperty(SECRET_ALIAS, "super-secret");
    try {
      byte[] payload = "action=opened&repo=demo".getBytes(StandardCharsets.UTF_8);
      Map<String, Object> headers = Map.of(HEADER_NAME, "sha256=deadbeef", "Content-Type",
          "application/x-www-form-urlencoded");
      Map<String, String> config = Map.of("header_name", HEADER_NAME, "secret_alias", SECRET_ALIAS);

      assertThatThrownBy(() -> validator.validateRequest(headers, payload, config)).isInstanceOf(
          com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthForbiddenException.class)
          .hasMessageContaining("transformed before validation");
    } finally {
      System.clearProperty(SECRET_ALIAS);
    }
  }

  @Nested
  @DisplayName("validateConfiguration — missing config keys")
  class MissingConfigKeys {

    @Test
    @DisplayName("Should throw when header_name is missing from config")
    void shouldThrowWhenHeaderNameMissing() {
      Map<String, String> config = Map.of("secret_alias", "MY_ALIAS");

      assertThatThrownBy(() -> validator.validateConfiguration(config)).isInstanceOf(
          com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException.class)
          .hasMessageContaining("header_name");
    }

    @Test
    @DisplayName("Should throw when secret_alias is missing from config")
    void shouldThrowWhenSecretAliasMissing() {
      Map<String, String> config = Map.of("header_name", "X-Hub-Signature-256");

      assertThatThrownBy(() -> validator.validateConfiguration(config)).isInstanceOf(
          com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException.class)
          .hasMessageContaining("secret_alias");
    }

    @Test
    @DisplayName("Should throw when secret_alias env variable does not exist")
    void shouldThrowWhenSecretAliasEnvVarMissing() {
      Map<String, String> config = Map.of("header_name", "X-Hub-Signature-256", "secret_alias",
          "UNSET_HMAC_VAR_THAT_DOES_NOT_EXIST");

      assertThatThrownBy(() -> validator.validateConfiguration(config)).isInstanceOf(
          com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException.class)
          .hasMessageContaining("UNSET_HMAC_VAR_THAT_DOES_NOT_EXIST");
    }

    @Test
    @DisplayName("Should accept configuration when secret_alias env variable exists")
    void shouldAcceptWhenSecretAliasEnvVarExists() {
      String secretEnv = "HMAC_CONFIG_TEST";
      System.setProperty(secretEnv, "super-secret");
      try {
        Map<String, String> config = Map.of("header_name", "X-Hub-Signature-256", "secret_alias",
            secretEnv);

        assertThatCode(() -> validator.validateConfiguration(config)).doesNotThrowAnyException();
      } finally {
        System.clearProperty(secretEnv);
      }
    }
  }
}
