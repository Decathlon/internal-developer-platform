package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_dynamic_mapping;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;

/// Result of an entity dynamic mapping dry-run validation.
@Schema(description = "Result of an entity dynamic mapping dry-run validation")
@JsonNaming(SnakeCaseStrategy.class)
public record EntityDynamicMappingDryRunDtoOut(
    @Schema(description = "List of entity mapping results") List<DryRunEntityResultDto> results) {

  @JsonNaming(SnakeCaseStrategy.class)
  public record DryRunEntityResultDto(
      @Schema(description = "Template identifier for this mapping") String mappingTemplateIdentifier,
      @Schema(description = "Whether the mapping was successful") boolean success,
      @Schema(description = "Mapped entity data") DryRunEntityDto entity,
      @Schema(description = "Error details") DryRunErrorDto error) {
  }

  @JsonNaming(SnakeCaseStrategy.class)
  public record DryRunEntityDto(
      @Schema(description = "Target template identifier") String templateIdentifier,
      @Schema(description = "Entity name") String name,
      @Schema(description = "Entity identifier") String identifier,
      @Schema(description = "Extracted properties") Map<String, String> properties) {
  }

  @JsonNaming(SnakeCaseStrategy.class)
  public record DryRunErrorDto(@Schema(description = "Error type") String type,
      @Schema(description = "Error message") String message) {
  }
}
