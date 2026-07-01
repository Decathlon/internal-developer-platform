package com.decathlon.idp_core.infrastructure.adapters.webhook.security.creation;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;
import com.decathlon.idp_core.infrastructure.adapters.webhook.security.StaticTokenSecurityValidator;

@DisplayName("Security Validator: Static Token Creation")
class StaticTokenWebhookSecurityCreationValidatorTest {

  private StaticTokenSecurityValidator validator;

  @BeforeEach
  void setUp() {
    validator = new StaticTokenSecurityValidator();
  }

  @Test
  @DisplayName("supports() -> True for 'STATIC_TOKEN' (case-insensitive), False for others")
  void shouldReturnTrueWhenTypeIsStaticToken() {
    assertThat(validator.supports("STATIC_TOKEN")).isTrue();
    assertThat(validator.supports("static_token")).isTrue();
    assertThat(validator.supports("HMAC_SHA256")).isFalse();
  }

  @Nested
  @DisplayName("Valid Configurations")
  class ValidConfigurations {

    @Test
    @DisplayName("validateConfiguration() -> Passes when header_name and UPPER_SNAKE_CASE secret_alias are present")
    void shouldPassWhenAllRequiredFieldsAreValidWithSnakeCase() {
      Map<String, String> config = Map.of("header_name", "X-Webhook-Token", "secret_alias",
          "WEBHOOK_TOKEN_SECRET");
      assertThatCode(() -> validator.validateConfiguration(config)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateConfiguration() -> Passes when keys are provided as camelCase (headerName, secretAlias)")
    void shouldPassWhenRequiredFieldsAreCamelCase() {
      Map<String, String> config = Map.of("headerName", "X-Webhook-Token", "secretAlias",
          "WEBHOOK_TOKEN_SECRET");
      assertThatCode(() -> validator.validateConfiguration(config)).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Invalid Configurations (Missing or Malformed Fields)")
  class InvalidConfigurations {

    @Test
    @DisplayName("validateConfiguration() -> Throws exception when 'header_name' is completely missing")
    void shouldThrowExceptionWhenHeaderNameIsMissing() {
      Map<String, String> config = Map.of("secret_alias", "TOKEN_SECRET");
      assertThatThrownBy(() -> validator.validateConfiguration(config))
          .isInstanceOf(WebhookSecurityConfigurationException.class)
          .hasMessageContaining("header_name");
    }

    @Test
    @DisplayName("validateConfiguration() -> Throws exception when 'secret_alias' is completely missing")
    void shouldThrowExceptionWhenSecretAliasIsMissing() {
      Map<String, String> config = Map.of("header_name", "X-Webhook-Token");
      assertThatThrownBy(() -> validator.validateConfiguration(config))
          .isInstanceOf(WebhookSecurityConfigurationException.class)
          .hasMessageContaining("secret_alias");
    }

    @Test
    @DisplayName("validateConfiguration() -> Throws exception when 'secret_alias' contains lowercase letters or invalid characters")
    void shouldThrowExceptionWhenSecretAliasIsNotUpperSnakeCase() {
      Map<String, String> config = Map.of("header_name", "X-Webhook-Token", "secret_alias",
          "plainTextSecret");
      assertThatThrownBy(() -> validator.validateConfiguration(config))
          .isInstanceOf(WebhookSecurityConfigurationException.class)
          .hasMessageContaining("UPPER_SNAKE_CASE");
    }
  }
}
