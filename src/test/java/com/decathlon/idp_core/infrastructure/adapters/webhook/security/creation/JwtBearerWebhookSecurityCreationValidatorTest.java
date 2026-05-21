package com.decathlon.idp_core.infrastructure.adapters.webhook.security.creation;

import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;
import com.decathlon.idp_core.infrastructure.adapters.webhook.security.JwtBearerSecurityValidator;
import com.decathlon.idp_core.infrastructure.adapters.webhook.security.WebhookJwtDecoderProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("Security Validator: JWT Bearer Creation")
class JwtBearerWebhookSecurityCreationValidatorTest {

    private JwtBearerSecurityValidator validator;

    @Mock
    private WebhookJwtDecoderProvider jwtDecoderProvider;

    @BeforeEach
    void setUp() {
        validator = new JwtBearerSecurityValidator(jwtDecoderProvider);
    }

    @Test
    @DisplayName("supports() -> True for 'JWT_BEARER' (case-insensitive), False for others")
    void shouldReturnTrueWhenTypeIsJwtBearer() {
        assertThat(validator.supports("JWT_BEARER")).isTrue();
        assertThat(validator.supports("jwt_bearer")).isTrue();
        assertThat(validator.supports("HMAC_SHA256")).isFalse();
        assertThat(validator.supports("BASIC_AUTH")).isFalse();
    }

    @Nested
    @DisplayName("Valid Configurations")
    class ValidConfigurations {

        @Test
        @DisplayName("validateConfiguration() -> Passes when 'jwks_uri' is provided in snake_case")
        void shouldPassWhenJwksUriIsValidWithSnakeCase() {
            Map<String, String> config = Map.of("jwks_uri", "https://example.com/.well-known/jwks.json");
            assertThatCode(() -> validator.validateConfiguration(config)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("validateConfiguration() -> Passes when 'jwksUri' is provided as camelCase")
        void shouldPassWhenJwksUriIsValidWithCamelCase() {
            Map<String, String> config = Map.of("jwksUri", "https://example.com/.well-known/jwks.json");
            assertThatCode(() -> validator.validateConfiguration(config)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Invalid Configurations (Missing or Malformed Fields)")
    class InvalidConfigurations {

        @Test
        @DisplayName("validateConfiguration() -> Throws exception when 'jwks_uri' is completely missing")
        void shouldThrowExceptionWhenJwksUriIsMissing() {
            Map<String, String> config = Map.of("other_key", "some-value");
            assertThatThrownBy(() -> validator.validateConfiguration(config))
                    .isInstanceOf(WebhookSecurityConfigurationException.class)
                    .hasMessageContaining("jwks_uri");
        }

        @Test
        @DisplayName("validateConfiguration() -> Throws exception when the configuration map is empty")
        void shouldThrowExceptionWhenConfigIsEmpty() {
            Map<String, String> config = Map.<String, String>of();
            assertThatThrownBy(() -> validator.validateConfiguration(config))
                    .isInstanceOf(WebhookSecurityConfigurationException.class);
        }
    }
}
