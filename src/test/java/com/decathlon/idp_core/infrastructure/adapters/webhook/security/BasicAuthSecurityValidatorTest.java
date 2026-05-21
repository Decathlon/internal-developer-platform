package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.webhook.WebhookSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Runtime Security: Basic Auth Validator")
class BasicAuthSecurityValidatorTest {

    private BasicAuthSecurityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new BasicAuthSecurityValidator();
    }

    @Test
    @DisplayName("supports() -> True for 'BASIC_AUTH' (case-insensitive), False for others")
    void shouldReturnTrueWhenTypeIsBasicAuth() {
        assertThat(validator.supports("BASIC_AUTH")).isTrue();
        assertThat(validator.supports("basic_auth")).isTrue();
        assertThat(validator.supports("HMAC_SHA256")).isFalse();
    }

    @Nested
    @DisplayName("Execution: Header Parsing and Secret Resolution")
    class SecretResolution {

        @Test
        @DisplayName("validateRequest() -> Processes valid Basic header but throws when secret environment variable is missing")
        void shouldProcessHeaderAndThrowWhenEnvironmentSecretIsMissing() {
            String username = "webhook-user";
            String nonSecretPasswordPlaceholder = "test-password-placeholder";
            String alias = "TEST_MISSING_PASSWORD_ALIAS";

            WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.BASIC_AUTH, Map.of("username", username, "secret_alias", alias));
            Map<String,String> headers = Map.of("Authorization", "Basic " + Base64.getEncoder().encodeToString((username + ":" + nonSecretPasswordPlaceholder).getBytes(StandardCharsets.UTF_8)));

            assertThatThrownBy(() -> validator.validateRequest(security, headers, new byte[0]))
                    .isInstanceOf(WebhookAuthenticationException.class)
                    .hasMessageContaining("Missing environment secret for alias");
        }
    }

    @Nested
    @DisplayName("Execution: Invalid or Missing Authorization Headers")
    class InvalidHeaders {

        @Test
        @DisplayName("validateRequest() -> Throws exception when 'Authorization' header is completely missing")
        void shouldThrowExceptionWhenAuthorizationHeaderIsMissing() {
            WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.BASIC_AUTH,
                    Map.of("username", "user", "secret_alias", "TEST_MISSING_BASIC_AUTH_ALIAS"));

            assertThatThrownBy(() -> validator.validateRequest(security, Map.of(), new byte[0]))
                    .isInstanceOf(WebhookAuthenticationException.class);
        }

        @Test
        @DisplayName("validateRequest() -> Throws exception when 'Authorization' header does not use the 'Basic' scheme")
        void shouldThrowExceptionWhenAuthorizationHeaderIsNotBasicScheme() {
            WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.BASIC_AUTH,
                    Map.of("username", "user", "secret_alias", "TEST_MISSING_BASIC_AUTH_ALIAS"));
            Map<String,String> headers = Map.of("Authorization", "Bearer some.jwt.token");

            assertThatThrownBy(() -> validator.validateRequest(security, headers, new byte[0]))
                    .isInstanceOf(WebhookAuthenticationException.class);
        }
    }

    @Nested
    @DisplayName("Configuration Fallback: Missing Required Config Keys (Runtime Safety)")
    class MissingConfigKeys {

        @Test
        @DisplayName("validateRequest() -> Throws exception when 'username' is missing from the stored configuration")
        void shouldThrowExceptionWhenUsernameIsMissingFromConfig() {
            WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.BASIC_AUTH, Map.of("secret_alias", "MY_ALIAS"));

            assertThatThrownBy(() -> validator.validateRequest(security, Map.of(), new byte[0]))
                    .isInstanceOf(WebhookAuthenticationException.class)
                    .hasMessageContaining("username");
        }

        @Test
        @DisplayName("validateRequest() -> Throws exception when 'secret_alias' is missing from the stored configuration")
        void shouldThrowExceptionWhenSecretAliasIsMissingFromConfig() {
            WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.BASIC_AUTH, Map.of("username", "user"));

            assertThatThrownBy(() -> validator.validateRequest(security, Map.of(), new byte[0]))
                    .isInstanceOf(WebhookAuthenticationException.class)
                    .hasMessageContaining("secret_alias");
        }
    }
}
