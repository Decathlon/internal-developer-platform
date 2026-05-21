package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("WebhookSecurityValidatorDispatcher Tests")
@ExtendWith(MockitoExtension.class)
class WebhookSecurityValidatorDispatcherTest {

    @Mock
    private WebhookSecurityStrategy hmacValidator;

    @Mock
    private WebhookSecurityStrategy staticTokenValidator;

    private WebhookSecurityValidatorDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        lenient().when(hmacValidator.supports("HMAC_SHA256")).thenReturn(true);
        lenient().when(hmacValidator.supports("STATIC_TOKEN")).thenReturn(false);
        lenient().when(staticTokenValidator.supports("HMAC_SHA256")).thenReturn(false);
        lenient().when(staticTokenValidator.supports("STATIC_TOKEN")).thenReturn(true);
        dispatcher = new WebhookSecurityValidatorDispatcher(List.of(hmacValidator, staticTokenValidator));
    }

    @Nested
    @DisplayName("dispatch — happy paths")
    class DispatchHappyPaths {

        @Test
        @DisplayName("Should delegate to the matching validator for HMAC_SHA256")
        void shouldDispatchToHmacValidator() {
            WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.HMAC_SHA256, Map.of("header_name", "X-Hub-Signature-256", "secret_alias", "MY_SECRET"));
            Map<String,String> headers = Map.of("X-Hub-Signature-256", "sha256=abc");
            byte[] body = new byte[0];

            assertThatCode(() -> dispatcher.dispatch(security, headers, body)).doesNotThrowAnyException();

            verify(hmacValidator).validateRequest(security, headers, body);
            verify(staticTokenValidator, never()).validateRequest(security, headers, body);
        }

        @Test
        @DisplayName("Should delegate to the matching validator for STATIC_TOKEN")
        void shouldDispatchToStaticTokenValidator() {
            WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.STATIC_TOKEN, Map.of("header_name", "X-Token", "secret_alias", "TOKEN_SECRET"));
            Map<String,String> headers = Map.of("X-Token", "my-token");
            byte[] body = new byte[0];

            assertThatCode(() -> dispatcher.dispatch(security, headers, body)).doesNotThrowAnyException();

            verify(staticTokenValidator).validateRequest(security, headers, body);
            verify(hmacValidator, never()).validateRequest(security, headers, body);
        }

        @Test
        @DisplayName("Should bypass validators for NONE security type")
        void shouldBypassValidatorsForNoneType() {
            WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.NONE, Map.of());
            Map<String,String> headers = Map.of("Authorization", "anything");
            byte[] body = new byte[0];

            assertThatCode(() -> dispatcher.dispatch(security, headers, body)).doesNotThrowAnyException();

            verify(hmacValidator, never()).validateRequest(security, headers, body);
            verify(staticTokenValidator, never()).validateRequest(security, headers, body);
        }
    }

    @Nested
    @DisplayName("dispatch — unsupported type")
    class DispatchUnsupportedType {

        @Test
        @DisplayName("Should throw WebhookAuthenticationException when no validator is registered for a type")
        void shouldThrowForUnsupportedSecurityType() {
            lenient().when(hmacValidator.supports("JWT_BEARER")).thenReturn(false);
            lenient().when(staticTokenValidator.supports("JWT_BEARER")).thenReturn(false);

            WebhookSecurity security = new WebhookSecurity(WebhookSecurityType.JWT_BEARER, Map.of("jwks_uri", "https://example.com/.well-known/jwks"));

            assertThatThrownBy(() -> dispatcher.dispatch(security, Map.of(), new byte[0]))
                    .isInstanceOf(WebhookAuthenticationException.class)
                    .hasMessageContaining("Unsupported webhook security strategy");
        }
    }
}
