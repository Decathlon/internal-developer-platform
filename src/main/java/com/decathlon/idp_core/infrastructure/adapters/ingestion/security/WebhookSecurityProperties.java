package com.decathlon.idp_core.infrastructure.adapters.ingestion.security;

import java.util.Locale;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// Externalized JWKS security configuration for webhook authentication.
///
/// The `allowedJwksHosts` allow-list is intentionally small and explicit so teams can permit
/// trusted issuers without hardcoding hostnames in the validator.
@ConfigurationProperties(prefix = "idp.security.webhook")
public record WebhookSecurityProperties(Set<String> allowedJwksHosts) {

  public WebhookSecurityProperties {
    allowedJwksHosts = allowedJwksHosts == null
        ? Set.of()
        : allowedJwksHosts.stream().filter(value -> value != null && !value.isBlank())
            .map(String::trim).map(value -> value.toLowerCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
  }
}
