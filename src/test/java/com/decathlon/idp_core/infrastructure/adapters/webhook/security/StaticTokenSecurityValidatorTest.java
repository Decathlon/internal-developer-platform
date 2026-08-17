package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthForbiddenException;

@DisplayName("StaticTokenSecurityValidator Tests")
class StaticTokenSecurityValidatorTest {

  private StaticTokenSecurityValidator validator;

  @BeforeEach
  void setUp() {
    validator = new StaticTokenSecurityValidator();
  }

  @Test
  @DisplayName("Should support STATIC_TOKEN only")
  void shouldSupportStaticToken() {
    assertThat(validator.supports(WebhookSecurityType.STATIC_TOKEN)).isTrue();
    assertThat(validator.supports(WebhookSecurityType.HMAC_SHA256)).isFalse();
    assertThat(validator.supports(WebhookSecurityType.BASIC_AUTH)).isFalse();
    assertThat(validator.supports(WebhookSecurityType.JWT_BEARER)).isFalse();
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
      Map<String, String> config = Map.of("header_name", "X-Token");
      assertThatThrownBy(() -> validator.validateConfiguration(config)).isInstanceOf(
          com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException.class)
          .hasMessageContaining("secret_alias");
    }
  }

  @Test
  @DisplayName("Should fail when header case does not match configured header_name")
  void shouldFailWhenHeaderCaseDoesNotMatch() {
    String tokenEnv = "STATIC_TOKEN_TEST";
    System.setProperty(tokenEnv, "expected-token");

    try {
      Map<String, String> config = Map.of("header_name", "X-Auth-Token", "secret_alias", tokenEnv);
      Map<String, Object> headers = Map.of("x-auth-token", "expected-token");

      assertThatThrownBy(() -> validator.validateRequest(headers, new byte[0], config))
          .isInstanceOf(
              com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException.class)
          .hasMessageContaining("X-Auth-Token");
    } finally {
      System.clearProperty(tokenEnv);
    }
  }

  @Test
  @DisplayName("Should reject invalid static token with 403 exception")
  void shouldRejectInvalidStaticToken() {
    String tokenEnv = "STATIC_TOKEN_TEST_2";
    System.setProperty(tokenEnv, "expected-token");

    try {
      Map<String, String> config = Map.of("header_name", "X-Auth-Token", "secret_alias", tokenEnv);
      Map<String, Object> headers = Map.of("X-Auth-Token", "invalid-token");

      assertThatThrownBy(() -> validator.validateRequest(headers, new byte[0], config))
          .isInstanceOf(WebhookAuthForbiddenException.class);
    } finally {
      System.clearProperty(tokenEnv);
    }
  }
}
