package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;

@DisplayName("HmacSha256SecurityValidator Tests")
class HmacSha256SecurityValidatorTest {

  private HmacSha256SecurityValidator validator;

  @BeforeEach
  void setUp() {
    validator = new HmacSha256SecurityValidator(mock(HmacSignatureValidator.class));
  }

  @Test
  @DisplayName("Should support HMAC_SHA256 only")
  void shouldSupportHmacSha256() {
    assertThat(validator.supports(WebhookSecurityType.HMAC_SHA256)).isTrue();
    assertThat(validator.supports(WebhookSecurityType.STATIC_TOKEN)).isFalse();
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
