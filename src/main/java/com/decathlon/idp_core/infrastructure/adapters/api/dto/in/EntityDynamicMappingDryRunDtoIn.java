package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.ENTITY_DYNAMIC_MAPPING_DRY_RUN_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PAYLOAD_DRY_RUN_MANDATORY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.WEBHOOK_DRY_RUN_PAYLOAD_DESCRIPTION;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for a single entity dynamic mapping dry-run validation. The
 * `payload` field accepts both a raw JSON string and a JSON object. JSON
 * normalization to String is handled in the API adapter before invoking the
 * domain service to keep the domain free of transport JSON types.
 */
@Schema(description = "Request payload for a single entity dynamic mapping dry-run validation")
@JsonNaming(SnakeCaseStrategy.class)
public record EntityDynamicMappingDryRunDtoIn(

    @NotNull(message = ENTITY_DYNAMIC_MAPPING_DRY_RUN_MANDATORY) @Valid EntityDynamicMappingCreateDtoIn mapping,

    @NotNull(message = PAYLOAD_DRY_RUN_MANDATORY) @Schema(description = WEBHOOK_DRY_RUN_PAYLOAD_DESCRIPTION) Object payload) {

  /**
   * Keeps syntactic validation equivalent to previous String-based contract. Null
   * is handled by {@link NotNull}; this guard prevents blank String payloads.
   */
  @SuppressWarnings("unused")
  @AssertTrue(message = PAYLOAD_DRY_RUN_MANDATORY)
  public boolean isPayloadNotBlankWhenString() {
    return !(payload instanceof String payloadString) || !payloadString.isBlank();
  }
}
