package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;
import com.decathlon.idp_core.domain.model.webhook.WebhookSecurity;
import com.decathlon.idp_core.domain.port.WebhookSecurityStrategy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * HMAC SHA256 security strategy for webhooks.
 *
 * Validates HMAC SHA256 signature configuration at creation time and authenticates
 * incoming webhook requests by verifying the signature against a stored secret.
 */
@Component
public class HmacSha256SecurityValidator implements WebhookSecurityStrategy {

    private final HmacSignatureValidator signatureValidator;

    public HmacSha256SecurityValidator(HmacSignatureValidator signatureValidator) {
        this.signatureValidator = signatureValidator;
    }

    @Override
    public boolean supports(String securityType) {
        return "HMAC_SHA256".equalsIgnoreCase(securityType);
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
        String prefix = WebhookSecurityConfigurationUtils.optional(security.config(), "prefix", "");

        String provided = headers.get(headerName);
        if (provided == null || provided.isBlank()) {
            throw new WebhookAuthenticationException("Missing signature header: " + headerName);
        }

        String secret = WebhookSecurityConfigurationUtils.getSecretFromEnvironment(alias);

        String expected = prefix + signatureValidator.computeHexSha256(rawBody, secret);
        if (!expected.equals(provided)) {
            throw new WebhookAuthenticationException("Invalid HMAC signature");
        }
    }
}
