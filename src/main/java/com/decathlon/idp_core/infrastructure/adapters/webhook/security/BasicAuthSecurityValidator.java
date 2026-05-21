package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;
import com.decathlon.idp_core.domain.model.webhook.WebhookSecurity;
import com.decathlon.idp_core.domain.port.WebhookSecurityStrategy;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Basic Authentication security strategy for webhooks.
 *
 * Validates Basic Auth credentials at both creation time (configuration validation)
 * and runtime (request authentication).
 */
@Component
public class BasicAuthSecurityValidator implements WebhookSecurityStrategy {

    @Override
    public boolean supports(String securityType) {
        return "BASIC_AUTH".equalsIgnoreCase(securityType);
    }

    @Override
    public void validateConfiguration(Map<String, String> config) {
        WebhookSecurityConfigurationUtils.required(config, "username");
        String alias = WebhookSecurityConfigurationUtils.required(config, "secret_alias", "secretAlias");
        WebhookSecurityConfigurationUtils.validateSecretAliasFormat(alias);
    }

    @Override
    public void validateRequest(WebhookSecurity security, Map<String, String> headers, byte[] rawBody) {
        String username = WebhookSecurityConfigurationUtils.requiredAtRuntime(security.config(), "username");
        String alias = WebhookSecurityConfigurationUtils.requiredAtRuntime(security.config(), "secret_alias", "secretAlias");

        String password = WebhookSecurityConfigurationUtils.getSecretFromEnvironment(alias);

        String authorization = headers.get("Authorization");
        if (authorization == null || !authorization.startsWith("Basic ")) {
            throw new WebhookAuthenticationException("Missing Authorization Basic header");
        }

        String expectedRaw = username + ":" + password;
        String expected = "Basic " + Base64.getEncoder().encodeToString(expectedRaw.getBytes(StandardCharsets.UTF_8));
        if (!expected.equals(authorization)) {
            throw new WebhookAuthenticationException("Invalid basic authentication credentials");
        }
    }
}
