package com.decathlon.idp_core.infrastructure.adapters.api.mapper.connector.webhook;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookSecurity;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.InboundWebhookCreateDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.InboundWebhookSecurityContractDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.InboundWebhookUpdateDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_dynamic_mapping.EntityDynamicMappingDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.webhook.InboundWebhookDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.webhook.InboundWebhookSecurityDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity_dynamic_mapping.EntityDynamicMappingMapper;

import lombok.AllArgsConstructor;

/// Maps inbound webhook API DTOs to domain models and back.
@Component
@AllArgsConstructor
public class InboundWebhookMapper {

  private final EntityDynamicMappingMapper dynamicMappingMapper;

  /// Converts API input payload to the domain aggregate.
  ///
  /// @param dto inbound webhook creation request
  /// @param resolvedMappings the existing dynamic mappings referenced by the
  /// request, already resolved and validated by the domain layer
  /// @return domain webhook connector
  public WebhookConnector toDomain(InboundWebhookCreateDtoIn dto,
      List<EntityDynamicMapping> resolvedMappings) {
    return new WebhookConnector(null, dto.identifier(), dto.name(), dto.description(),
        dto.enabled(), safeMappings(resolvedMappings), toDomain(dto.security()));
  }

  /// Converts API update payload to domain aggregate using the path identifier as
  /// source of truth.
  ///
  /// @param identifier webhook connector identifier from URL path
  /// @param dto inbound webhook update request body
  /// @param resolvedMappings the existing dynamic mappings referenced by the
  /// request, already resolved and validated by the domain layer
  /// @return domain webhook connector prepared for update
  public WebhookConnector toDomainForUpdate(String identifier, InboundWebhookUpdateDtoIn dto,
      List<EntityDynamicMapping> resolvedMappings) {
    return new WebhookConnector(null, identifier, dto.name(), dto.description(), dto.enabled(),
        safeMappings(resolvedMappings), toDomain(dto.security()));
  }

  /// Converts domain aggregate to API response payload.
  ///
  /// @param domain created webhook connector
  /// @return response DTO
  public InboundWebhookDtoOut fromWebhookConnectorToDto(WebhookConnector domain) {
    List<EntityDynamicMappingDtoOut> mappings = domain.mappings().stream()
        .map(dynamicMappingMapper::fromEntityMappingToDto).toList();
    InboundWebhookSecurityDtoOut security = new InboundWebhookSecurityDtoOut(
        domain.security().type().name(), domain.security().config());
    return new InboundWebhookDtoOut(domain.identifier(), domain.name(), domain.description(),
        domain.enabled(), mappings, security);
  }

  private List<EntityDynamicMapping> safeMappings(List<EntityDynamicMapping> mappings) {
    return mappings == null ? List.of() : List.copyOf(mappings);
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
