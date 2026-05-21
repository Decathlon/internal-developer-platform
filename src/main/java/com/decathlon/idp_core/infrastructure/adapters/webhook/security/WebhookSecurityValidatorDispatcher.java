package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.webhook.WebhookSecurity;
import com.decathlon.idp_core.domain.port.WebhookSecurityStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Dispatcher for webhook runtime authentication.
 *
 * Routes incoming webhook validation requests to the appropriate security strategy
 * based on the configured security type.
 */
@Component
public class WebhookSecurityValidatorDispatcher {

    private final List<WebhookSecurityStrategy> strategies;

    public WebhookSecurityValidatorDispatcher(List<WebhookSecurityStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
    }

    /**
     * Dispatches webhook request validation to the appropriate security strategy.
     *
     * @param security the webhook security configuration
     * @param headers HTTP request headers
     * @param rawBody raw request body
     * @throws WebhookAuthenticationException if validation fails or no strategy is found
     */
    public void dispatch(WebhookSecurity security, Map<String, String> headers, byte[] rawBody) {
        if (security.type() == WebhookSecurityType.NONE) {
            return;
        }

        strategies.stream()
                .filter(strategy -> strategy.supports(security.type().name()))
                .findFirst()
                .orElseThrow(() -> new WebhookAuthenticationException("Unsupported webhook security strategy: " + security.type()))
                .validateRequest(security, headers, rawBody);
    }
}
