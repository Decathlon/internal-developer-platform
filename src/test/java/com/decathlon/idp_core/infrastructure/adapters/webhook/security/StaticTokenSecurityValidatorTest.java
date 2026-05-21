package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.webhook.WebhookSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @DisplayName("validateRequest missing header")
    class MissingHeader {

        @Test
        @DisplayName("Should throw when the token header is absent")
        void shouldThrowWhenTokenHeaderMissing() {
            WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.STATIC_TOKEN,
                    Map.of("header_name", "X-Webhook-Token", "secret_alias", "TEST_MISSING_STATIC_TOKEN_ALIAS"));

            assertThatThrownBy(() -> validator.validateRequest(security, Map.of(), new byte[0]))
                    .isInstanceOf(WebhookAuthenticationException.class)
                    .hasMessageContaining("Missing token header");
        }

        @Test
        @DisplayName("Should throw when the token header value is blank")
        void shouldThrowWhenTokenHeaderBlank() {
            WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.STATIC_TOKEN,
                    Map.of("header_name", "X-Webhook-Token", "secret_alias", "TEST_MISSING_STATIC_TOKEN_ALIAS"));
            Map<String,String> headers = Map.of("X-Webhook-Token", "  ");

            assertThatThrownBy(() -> validator.validateRequest(security, headers, new byte[0]))
                    .isInstanceOf(WebhookAuthenticationException.class)
                    .hasMessageContaining("Missing token header");
        }
    }

    @Nested
    @DisplayName("validateRequest — missing env secret")
    class MissingEnvSecret {

        @Test
        @DisplayName("Should throw when the environment secret is not set")
        void shouldThrowWhenEnvSecretMissing() {
            WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.STATIC_TOKEN,
                    Map.of("header_name", "X-Webhook-Token", "secret_alias", "TEST_MISSING_STATIC_TOKEN_ALIAS"));
            Map<String,String> headers = Map.of("X-Webhook-Token", "test-token-placeholder");

            assertThatThrownBy(() -> validator.validateRequest(security, headers, new byte[0]))
                    .isInstanceOf(WebhookAuthenticationException.class)
                    .hasMessageContaining("Missing environment secret for alias");
        }
    }

    @Nested
    @DisplayName("validateRequest — missing config keys")
    class MissingConfigKeys {

        @Test
        @DisplayName("Should throw when header_name is missing from config")
        void shouldThrowWhenHeaderNameMissing() {
            WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.STATIC_TOKEN, Map.of("secret_alias", "MY_ALIAS"));

            assertThatThrownBy(() -> validator.validateRequest(security, Map.of(), new byte[0]))
                    .isInstanceOf(WebhookAuthenticationException.class)
                    .hasMessageContaining("header_name");
        }

        @Test
        @DisplayName("Should throw when secret_alias is missing from config")
        void shouldThrowWhenSecretAliasMissing() {
            WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.STATIC_TOKEN, Map.of("header_name", "X-Token"));

            assertThatThrownBy(() -> validator.validateRequest(security, Map.of(), new byte[0]))
                    .isInstanceOf(WebhookAuthenticationException.class)
                    .hasMessageContaining("secret_alias");
        }
    }
}
