package com.decathlon.idp_core.domain.service.webhook.security;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookSecurity;
import com.decathlon.idp_core.domain.port.WebhookSecurityStrategy;

/// Domain service for validating webhook security configuration at creation/update time.
///
/// This service ensures that the security configuration provided when creating or updating
/// a webhook connector is valid before storing it in the database. The `NONE` security type
/// (unauthenticated webhooks) is rejected unless `IDP_WEBHOOK_SECURITY_ALLOW_NONE` is enabled.
@Service
public class WebhookSecurityValidationService {

  private final List<WebhookSecurityStrategy> strategies;
  private final boolean allowNone;

  public WebhookSecurityValidationService(List<WebhookSecurityStrategy> strategies,
      @Value("${idp.security.webhook.allow-none:false}") boolean allowNone) {
    this.strategies = List.copyOf(strategies);
    this.allowNone = allowNone;
  }

  /// Validates webhook security configuration for creation or update.
  ///
  /// @param security the security configuration to validate
  /// @throws WebhookSecurityConfigurationException if the configuration is
  /// invalid
  public void validateForCreation(WebhookSecurity security) {
    if (security == null) {
      throw new WebhookSecurityConfigurationException("Webhook security section is mandatory");
    }

    Map<String, String> config = security.config();

    if (security.type() == WebhookSecurityType.NONE) {
      if (!allowNone) {
        throw new WebhookSecurityConfigurationException(
            "Webhook security type NONE is disabled. Set IDP_WEBHOOK_SECURITY_ALLOW_NONE=true to allow it");
      }
      validateNoSecurityConfig(config);
      return;
    }

    strategies.stream().filter(strategy -> strategy.supports(security.type())).findFirst()
        .ifPresentOrElse(strategy -> strategy.validateConfiguration(config), () -> {
          throw new WebhookSecurityConfigurationException(
              "No validator registered for security type: " + security.type());
        });
  }

  private void validateNoSecurityConfig(Map<String, String> config) {
    if (config != null && !config.isEmpty()) {
      throw new WebhookSecurityConfigurationException(
          "Webhook security config must be empty when type is NONE");
    }
  }
}
