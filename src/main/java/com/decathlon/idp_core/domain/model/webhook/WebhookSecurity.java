package com.decathlon.idp_core.domain.model.webhook;

import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookSecurity(
        @NotNull WebhookSecurityType type,
        @NotNull Map<String, String> config
) {

    public WebhookSecurity {
        if (type == null) {
            throw new WebhookSecurityConfigurationException("Webhook security type is mandatory");
        }
        if (config == null) {
            throw new WebhookSecurityConfigurationException("Webhook security config section is mandatory");
        }
        config = Map.copyOf(config);
    }
}
