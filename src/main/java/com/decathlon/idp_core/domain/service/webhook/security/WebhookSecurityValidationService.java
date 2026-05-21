package com.decathlon.idp_core.domain.service.webhook.security;

import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.webhook.WebhookSecurity;
import com.decathlon.idp_core.domain.port.WebhookSecurityStrategy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Domain service for validating webhook security configuration at creation/update time.
 *
 * This service ensures that the security configuration provided when creating or updating
 * a webhook connector is valid before storing it in the database.
 */
@Service
public class WebhookSecurityValidationService {

    private final List<WebhookSecurityStrategy> strategies;

    public WebhookSecurityValidationService(List<WebhookSecurityStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
    }

    /**
     * Validates webhook security configuration for creation or update.
     *
     * @param security the security configuration to validate
     * @throws WebhookSecurityConfigurationException if the configuration is invalid
     */
    public void validateForCreation(WebhookSecurity security) {
        if (security == null) {
            throw new WebhookSecurityConfigurationException("Webhook security section is mandatory");
        }

        Map<String, String> config = security.config();

        if (security.type() == WebhookSecurityType.NONE) {
            validateNoSecurityConfig(config);
            return;
        }

        strategies.stream()
                .filter(strategy -> strategy.supports(security.type().name()))
                .findFirst()
                .ifPresentOrElse(
                        strategy -> strategy.validateConfiguration(config),
                        () -> {
                            throw new WebhookSecurityConfigurationException(
                                    "No validator registered for security type: " + security.type());
                        }
                );
    }

    private void validateNoSecurityConfig(Map<String, String> config) {
        if (!config.isEmpty()) {
            throw new WebhookSecurityConfigurationException("Webhook security config must be empty when type is NONE");
        }
    }
}
