package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;
import com.decathlon.idp_core.domain.model.webhook.WebhookSecurity;
import com.decathlon.idp_core.domain.port.WebhookSecurityStrategy;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * JWT Bearer security strategy for webhooks.
 *
 * Validates JWT Bearer configuration at creation time and authenticates incoming
 * webhook requests by verifying the JWT token against a JWKS endpoint.
 */
@Component
public class JwtBearerSecurityValidator implements WebhookSecurityStrategy {

    private final WebhookJwtDecoderProvider jwtDecoderProvider;

    public JwtBearerSecurityValidator(WebhookJwtDecoderProvider jwtDecoderProvider) {
        this.jwtDecoderProvider = jwtDecoderProvider;
    }

    @Override
    public boolean supports(String securityType) {
        return "JWT_BEARER".equalsIgnoreCase(securityType);
    }

    @Override
    public void validateConfiguration(Map<String, String> config) {
        String jwksUri = WebhookSecurityConfigurationUtils.required(config, "jwks_uri", "jwksUri");
        if (jwksUri.isBlank()) {
            throw new WebhookSecurityConfigurationException("Invalid jwks_uri for JWT_BEARER security");
        }
    }

    @Override
    public void validateRequest(WebhookSecurity security, Map<String, String> headers, byte[] rawBody) {
        String jwksUri = WebhookSecurityConfigurationUtils.requiredAtRuntime(security.config(), "jwks_uri", "jwksUri");
        if (jwksUri.isBlank()) {
            throw new WebhookAuthenticationException("Invalid jwks_uri for JWT_BEARER security");
        }

        String authorization = headers.get("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new WebhookAuthenticationException("Missing Authorization Bearer header");
        }

        String token = authorization.substring("Bearer ".length()).trim();
        if (token.isBlank()) {
            throw new WebhookAuthenticationException("Missing bearer token");
        }

        try {
            jwtDecoderProvider.get(jwksUri).decode(token);
        } catch (JwtException exception) {
            throw new WebhookAuthenticationException("Invalid JWT bearer token", exception);
        }
    }
}
