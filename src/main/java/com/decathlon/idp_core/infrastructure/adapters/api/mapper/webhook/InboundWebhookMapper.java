package com.decathlon.idp_core.infrastructure.adapters.api.mapper.webhook;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookSecurity;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.InboundWebhookCreateDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.InboundWebhookMappingDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.InboundWebhookSecurityContractDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.webhook.InboundWebhookDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.webhook.InboundWebhookEntityMappingDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.webhook.InboundWebhookMappingDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.webhook.InboundWebhookSecurityDtoOut;

/// Maps inbound webhook API DTOs to domain models and back.
@Component
public class InboundWebhookMapper {

  /// Converts API input payload to the domain aggregate.
  ///
  /// @param dto inbound webhook creation request
  /// @return domain webhook connector
  public WebhookConnector toDomain(InboundWebhookCreateDtoIn dto) {
    return new WebhookConnector(null, dto.identifier(), dto.title(), dto.description(),
        dto.enabled(), safeMappings(dto.mappings()), toDomain(dto.security()));
  }

  /// Converts API update payload to domain aggregate using the path identifier as
  /// source of truth.
  ///
  /// @param identifier webhook connector identifier from URL path
  /// @param dto inbound webhook update request body
  /// @return domain webhook connector prepared for update
  public WebhookConnector toDomainForUpdate(String identifier, InboundWebhookCreateDtoIn dto) {
    var mappings = safeMappings(dto.mappings());
    var security = toDomain(dto.security());
    return new WebhookConnector(null, identifier, dto.title(), dto.description(), dto.enabled(),
        mappings, security);
  }

  /// Converts domain aggregate to API response payload.
  ///
  /// @param domain created webhook connector
  /// @return response DTO
  public InboundWebhookDtoOut fromWebhookConnectorToDto(WebhookConnector domain) {
    var mappings = domain.mappings().stream().map(this::fromEntityMappingToDto).toList();
    var security = new InboundWebhookSecurityDtoOut(domain.security().type().name(),
        domain.security().config());
    return new InboundWebhookDtoOut(domain.identifier(), domain.title(), domain.description(),
        domain.enabled(), mappings, security);
  }

  private InboundWebhookMappingDtoOut fromEntityMappingToDto(EntityDynamicMapping mapping) {
    return new InboundWebhookMappingDtoOut(mapping.templateIdentifier(), mapping.filter(),
        new InboundWebhookEntityMappingDtoOut(mapping.entityIdentifier(), mapping.entityTitle(),
            Map.copyOf(mapping.properties()), Map.copyOf(mapping.relations())));
  }

  private EntityDynamicMapping toDomain(InboundWebhookMappingDtoIn mapping) {
    return new EntityDynamicMapping(null, mapping.template(), mapping.filter(),
        mapping.entity().identifier(), mapping.entity().title(),
        safeMap(mapping.entity().properties()), safeMap(mapping.entity().relations()));
  }

  private List<EntityDynamicMapping> safeMappings(
      java.util.List<InboundWebhookMappingDtoIn> mappings) {
    if (mappings == null || mappings.isEmpty()) {
      return java.util.List.of();
    }
    return mappings.stream().map(this::toDomain).toList();
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
    } catch (IllegalArgumentException _) {
      throw new WebhookSecurityConfigurationException("Unsupported security type: " + typeString);
    }
  }

  private Map<String, String> safeMap(Map<String, String> input) {
    return input == null ? Map.of() : Map.copyOf(input);
  }
}
