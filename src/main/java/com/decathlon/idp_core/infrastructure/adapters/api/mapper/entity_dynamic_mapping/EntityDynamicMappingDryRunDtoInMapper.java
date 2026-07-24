package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity_dynamic_mapping;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingConfigurationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Transport-level mapper for dry-run requests. Normalizes inbound JSON payload
 * structures (ObjectNode or Raw String) into a JSON string for domain port
 * processing.
 */
@Component
@RequiredArgsConstructor
public class EntityDynamicMappingDryRunDtoInMapper {

  private final ObjectMapper objectMapper = new ObjectMapper();

  public String toRawPayload(Object payload) {
    if (payload instanceof String payloadString) {
      return payloadString;
    }

    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException exception) {
      throw new EntityDynamicMappingConfigurationException("Invalid dry-run payload format",
          exception);
    }
  }
}
