package com.decathlon.idp_core.infrastructure.adapters.webhook.security.creation;

import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;
import com.decathlon.idp_core.infrastructure.adapters.webhook.security.HmacSha256SecurityValidator;
import com.decathlon.idp_core.infrastructure.adapters.webhook.security.HmacSignatureValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Security Validator: HMAC SHA-256 Creation")
class HmacSha256WebhookSecurityCreationValidatorTest {

    private HmacSha256SecurityValidator validator;

    @BeforeEach
    void setUp() {
        HmacSignatureValidator signatureValidator = new HmacSignatureValidator();
        validator = new HmacSha256SecurityValidator(signatureValidator);
    }

    @Test
    @DisplayName("supports() -> True for 'HMAC_SHA256' (case-insensitive), False for others")
    void shouldReturnTrueWhenTypeIsHmacSha256() {
        assertThat(validator.supports("HMAC_SHA256")).isTrue();
        assertThat(validator.supports("hmac_sha256")).isTrue();
        assertThat(validator.supports("STATIC_TOKEN")).isFalse();
    }

    @Nested
    @DisplayName("Valid Configurations")
    class ValidConfigurations {

        @Test
        @DisplayName("validateConfiguration() -> Passes when header_name and UPPER_SNAKE_CASE secret_alias are present")
        void shouldPassWhenAllRequiredFieldsAreValidWithSnakeCase() {
            Map<String, String> config = Map.of("header_name", "X-Hub-Signature-256", "secret_alias", "MY_GITHUB_SECRET");
            assertThatCode(() -> validator.validateConfiguration(config)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("validateConfiguration() -> Passes when keys are provided as camelCase (headerName, secretAlias)")
        void shouldPassWhenRequiredFieldsAreCamelCase() {
            Map<String, String> config = Map.of("headerName", "X-Hub-Signature-256", "secretAlias", "MY_GITHUB_SECRET");
            assertThatCode(() -> validator.validateConfiguration(config)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Invalid Configurations (Missing or Malformed Fields)")
    class InvalidConfigurations {

        @Test
        @DisplayName("validateConfiguration() -> Throws exception when 'header_name' is completely missing")
        void shouldThrowExceptionWhenHeaderNameIsMissing() {
            Map<String, String> config = Map.of("secret_alias", "MY_SECRET");
            assertThatThrownBy(() -> validator.validateConfiguration(config))
                    .isInstanceOf(WebhookSecurityConfigurationException.class)
                    .hasMessageContaining("header_name");
        }

        @Test
        @DisplayName("validateConfiguration() -> Throws exception when 'secret_alias' is completely missing")
        void shouldThrowExceptionWhenSecretAliasIsMissing() {
            Map<String, String> config = Map.of("header_name", "X-Hub-Signature-256");
            assertThatThrownBy(() -> validator.validateConfiguration(config))
                    .isInstanceOf(WebhookSecurityConfigurationException.class)
                    .hasMessageContaining("secret_alias");
        }

        @Test
        @DisplayName("validateConfiguration() -> Throws exception when 'secret_alias' contains lowercase letters or invalid characters")
        void shouldThrowExceptionWhenSecretAliasIsNotUpperSnakeCase() {
            Map<String, String> config = Map.of("header_name", "X-Hub-Signature-256", "secret_alias", "my-raw-secret-value");
            assertThatThrownBy(() -> validator.validateConfiguration(config))
                    .isInstanceOf(WebhookSecurityConfigurationException.class)
                    .hasMessageContaining("UPPER_SNAKE_CASE");
        }
    }
}
