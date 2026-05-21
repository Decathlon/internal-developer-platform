package com.decathlon.idp_core.domain.service.webhook.security;

import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.webhook.WebhookSecurity;
import com.decathlon.idp_core.domain.port.WebhookSecurityStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@DisplayName("WebhookSecurityValidationService Tests")
@ExtendWith(MockitoExtension.class)
class WebhookSecurityValidationServiceTest {

    @Mock
    private WebhookSecurityStrategy hmacCreationValidator;

    private WebhookSecurityValidationService service;

    @BeforeEach
    void setUp() {
        lenient().when(hmacCreationValidator.supports("HMAC_SHA256")).thenReturn(true);
        service = new WebhookSecurityValidationService(List.of(hmacCreationValidator));
    }

    @Nested
    @DisplayName("validateForCreation — null/blank guards")
    class NullBlankGuards {

        @Test
        @DisplayName("Should throw when security is null")
        void shouldThrowWhenSecurityIsNull() {
            assertThatThrownBy(() -> service.validateForCreation(null))
                    .isInstanceOf(WebhookSecurityConfigurationException.class)
                    .hasMessageContaining("mandatory");
        }

        @Test
        @DisplayName("Should throw when security type is null")
        void shouldThrowWhenTypeIsNull() {
            assertThatThrownBy(() -> new WebhookSecurity(null, Map.of("k", "v")))
                    .isInstanceOf(WebhookSecurityConfigurationException.class)
                    .hasMessageContaining("type is mandatory");
        }

        @Test
        @DisplayName("Should throw when config is null")
        void shouldThrowWhenConfigIsNull() {
            assertThatThrownBy(() -> new WebhookSecurity(WebhookSecurityType.HMAC_SHA256, null))
                    .isInstanceOf(WebhookSecurityConfigurationException.class)
                    .hasMessageContaining("config section is mandatory");
        }
    }

    @Nested
    @DisplayName("validateForCreation — known type delegation")
    class KnownTypeDelegation {

        @Test
        @DisplayName("Should delegate to the matching creation validator for HMAC_SHA256")
        void shouldDelegateToHmacValidator() {
            var config = Map.of("header_name", "X-Hub-Signature-256", "secret_alias", "MY_SECRET");
            var security = new WebhookSecurity(WebhookSecurityType.HMAC_SHA256, config);

            assertThatCode(() -> service.validateForCreation(security)).doesNotThrowAnyException();

            verify(hmacCreationValidator).validateConfiguration(config);
        }
    }

    @Nested
    @DisplayName("validateForCreation — NONE type")
    class NoneTypeValidation {

        @Test
        @DisplayName("Should pass for NONE type with empty config")
        void shouldPassForNoneTypeWithEmptyConfig() {
            var security = new WebhookSecurity(WebhookSecurityType.NONE, Map.of());

            assertThatCode(() -> service.validateForCreation(security)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw for NONE type with non-empty config")
        void shouldThrowForNoneTypeWithNonEmptyConfig() {
            var security = new WebhookSecurity(WebhookSecurityType.NONE, Map.of("header_name", "X-Test"));

            assertThatThrownBy(() -> service.validateForCreation(security))
                    .isInstanceOf(WebhookSecurityConfigurationException.class)
                    .hasMessageContaining("must be empty when type is NONE");
        }
    }

    @Nested
    @DisplayName("validateForCreation — unregistered type")
    class UnregisteredType {

        @Test
        @DisplayName("Should throw when no validator is registered for the given type")
        void shouldThrowForUnregisteredType() {
            lenient().when(hmacCreationValidator.supports("JWT_BEARER")).thenReturn(false);
            var security = new WebhookSecurity(WebhookSecurityType.JWT_BEARER, Map.of("jwks_uri", "https://example.com/.well-known/jwks"));

            assertThatThrownBy(() -> service.validateForCreation(security))
                    .isInstanceOf(WebhookSecurityConfigurationException.class)
                    .hasMessageContaining("No validator registered");
        }
    }
}
