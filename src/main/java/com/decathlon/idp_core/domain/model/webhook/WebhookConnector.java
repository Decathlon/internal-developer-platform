package com.decathlon.idp_core.domain.model.webhook;

import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.TEMPLATE_NAME_MAX_SIZE;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.WEBHOOK_CONNECTOR_IDENTIFIER_MANDATORY;

public record WebhookConnector(
        UUID id,
        @NotBlank(message = WEBHOOK_CONNECTOR_IDENTIFIER_MANDATORY)
        String identifier,
        @NotBlank(message = "Webhook title is mandatory")
        @Size(max = 255, message = TEMPLATE_NAME_MAX_SIZE)
        String title,

        String description,
        boolean enabled,

        @NotEmpty
        List<@Valid EntityDynamicMapping> mappings,
        @NotNull
        @Valid
        WebhookSecurity security
) {
}
