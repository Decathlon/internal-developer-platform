package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;
import com.decathlon.idp_core.domain.model.webhook.WebhookSecurity;
import com.decathlon.idp_core.domain.port.WebhookSecurityStrategy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Static Token security strategy for webhooks.
 *
 * Validates static token configuration at creation time and authenticates incoming
 * webhook requests by comparing the provided token against the stored secret.
 */
@Component
public class StaticTokenSecurityValidator implements WebhookSecurityStrategy {

    @Override
    public boolean supports(String securityType) {
        return "STATIC_TOKEN".equalsIgnoreCase(securityType);
    }

    @Override
    public void validateConfiguration(Map<String, String> config) {
        WebhookSecurityConfigurationUtils.required(config, "header_name", "headerName");
        String alias = WebhookSecurityConfigurationUtils.required(config, "secret_alias", "secretAlias");
        WebhookSecurityConfigurationUtils.validateSecretAliasFormat(alias);
    }

    @Override
    public void validateRequest(WebhookSecurity security, Map<String, String> headers, byte[] rawBody) {
        String headerName = WebhookSecurityConfigurationUtils.requiredAtRuntime(security.config(), "header_name", "headerName");
        String alias = WebhookSecurityConfigurationUtils.requiredAtRuntime(security.config(), "secret_alias", "secretAlias");

        String provided = headers.get(headerName);
        if (provided == null || provided.isBlank()) {
            throw new WebhookAuthenticationException("Missing token header: " + headerName);
        }

        String expected = WebhookSecurityConfigurationUtils.getSecretFromEnvironment(alias);

        if (!expected.equals(provided)) {
            throw new WebhookAuthenticationException("Invalid static token");
        }
    }
}
