package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/// Mapping rule request for inbound webhook transformation.
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record EntityDynamicMappingCreateDtoIn(
    @NotBlank(message = "Entity dynamic mapping identifier is mandatory") String identifier,
    @NotBlank(message = "Webhook mapping template is mandatory") String template,
    @NotBlank(message = "Webhook mapping filter is mandatory") String filter,
    @NotNull(message = "Webhook mapping entity section is mandatory") @Valid InboundWebhookEntityMappingDtoIn entity) {

  /// Returns a CommonFields view for compatibility with the mapper.
  public EntityDynamicMappingDtoInCommonFields commonFields() {
    return new EntityDynamicMappingDtoInCommonFields(template, filter, entity);
  }
}
