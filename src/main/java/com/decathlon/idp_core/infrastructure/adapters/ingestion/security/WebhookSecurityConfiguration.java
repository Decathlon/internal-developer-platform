package com.decathlon.idp_core.infrastructure.adapters.ingestion.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/// Enables binding of webhook security configuration properties.
@Configuration
@EnableConfigurationProperties(WebhookSecurityProperties.class)
public class WebhookSecurityConfiguration {
}
