package com.decathlon.idp_core.domain.model.entity_mapping;

import java.util.Map;

import com.decathlon.idp_core.domain.model.webhook.WebhookConnector;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record
EntityDynamicMapping(
        @NotBlank
        String templateIdentifier,
        @NotBlank
        String filter,
        @NotBlank
        String entityIdentifier,
        @NotBlank
        String entityTitle,
        @NotNull
        Map<String, String> properties,
        @NotNull
        Map<String, String> relations
) {
}
