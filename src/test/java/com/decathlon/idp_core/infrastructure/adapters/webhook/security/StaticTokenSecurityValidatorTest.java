package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("StaticTokenSecurityValidator Tests")
class StaticTokenSecurityValidatorTest {

  private StaticTokenSecurityValidator validator;

  @BeforeEach
  void setUp() {
    validator = new StaticTokenSecurityValidator();
  }

  @Test
  @DisplayName("Should support STATIC_TOKEN (case-insensitive)")
  void shouldSupportStaticToken() {
    assertThat(validator.supports("STATIC_TOKEN")).isTrue();
    assertThat(validator.supports("static_token")).isTrue();
    assertThat(validator.supports("HMAC_SHA256")).isFalse();
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
}
