package com.decathlon.idp_core.infrastructure.adapters.api.mapper.webhook;

import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.model.webhook.WebhookSecurity;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.InboundWebhookCreateDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.InboundWebhookMappingDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.InboundWebhookSecurityContractDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.webhook.InboundWebhookDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.webhook.InboundWebhookEntityMappingDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.webhook.InboundWebhookMappingDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.webhook.InboundWebhookSecurityDtoOut;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Maps inbound webhook API DTOs to domain models and back.
 */
@Component
public class InboundWebhookMapper {

    /**
     * Converts API input payload to the domain aggregate.
     *
     * @param dto inbound webhook creation request
     * @return domain webhook connector
     */
    public WebhookConnector toDomain(InboundWebhookCreateDtoIn dto) {
        return new WebhookConnector(
                null,
                dto.identifier(),
                dto.title(),
                dto.description(),
                dto.enabled(),
                dto.mappings().stream().map(this::toDomain).toList(),
                toDomain(dto.security())
        );
    }

    /**
     * Converts API update payload to domain aggregate using the path identifier as source of truth.
     *
     * @param identifier webhook connector identifier from URL path
     * @param dto        inbound webhook update request body
     * @return domain webhook connector prepared for update
     */
    public WebhookConnector toDomainForUpdate(String identifier, InboundWebhookCreateDtoIn dto) {
        var mappings = dto.mappings().stream().map(this::toDomain).toList();
        var security = toDomain(dto.security());
        return new WebhookConnector(
                null,
                identifier,
                dto.title(),
                dto.description(),
                dto.enabled(),
                mappings,
                security
        );
    }

    /**
     * Converts domain aggregate to API response payload.
     *
     * @param domain created webhook connector
     * @return response DTO
     */
    public InboundWebhookDtoOut fromWebhookConnectorToDto(WebhookConnector domain) {
        var mappings = domain.mappings().stream().map(this::fromEntityMappingToDto).toList();
        var security = new InboundWebhookSecurityDtoOut(domain.security().type().name());
        return new InboundWebhookDtoOut(
                domain.identifier(),
                domain.title(),
                domain.description(),
                domain.enabled(),
                mappings,
                security
        );
    }

    private InboundWebhookMappingDtoOut fromEntityMappingToDto(EntityDynamicMapping mapping) {
        return new InboundWebhookMappingDtoOut(
                mapping.templateIdentifier(),
                mapping.filter(),
                new InboundWebhookEntityMappingDtoOut(
                        mapping.entityIdentifier(),
                        mapping.entityTitle(),
                        Map.copyOf(mapping.properties()),
                        Map.copyOf(mapping.relations())
                )
        );
    }

    private EntityDynamicMapping toDomain(InboundWebhookMappingDtoIn mapping) {
        return new EntityDynamicMapping(
                mapping.template(),
                mapping.filter(),
                mapping.entity().identifier(),
                mapping.entity().title(),
                safeMap(mapping.entity().properties()),
                safeMap(mapping.entity().relations())
        );
    }

    private WebhookSecurity toDomain(InboundWebhookSecurityContractDtoIn security) {
        if (security == null) {
            return new WebhookSecurity(WebhookSecurityType.NONE, Map.of());
        }

        var type = parseSecurityType(security.type());
        var config = safeMap(security.config());

        return new WebhookSecurity(type, config);
    }

    private WebhookSecurityType parseSecurityType(String typeString) {
        try {
            return WebhookSecurityType.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new WebhookSecurityConfigurationException("Unsupported security type: " + typeString);
        }
    }

    private Map<String, String> safeMap(Map<String, String> input) {
        return input == null ? Map.of() : Map.copyOf(input);
    }
}
