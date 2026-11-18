package com.decathlon.idp_core.infrastructure.adapters.ingestion.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookSecurityValidationPolicy;

/// Enables binding and mapping of webhook security configuration properties.
@Configuration
@EnableConfigurationProperties(WebhookSecurityProperties.class)
public class WebhookSecurityConfiguration {

  @Bean
  public WebhookSecurityValidationPolicy webhookSecurityValidationPolicy(
      WebhookSecurityProperties webhookSecurityProperties) {
    return new WebhookSecurityValidationPolicy(webhookSecurityProperties.allowNone());
  }
}
