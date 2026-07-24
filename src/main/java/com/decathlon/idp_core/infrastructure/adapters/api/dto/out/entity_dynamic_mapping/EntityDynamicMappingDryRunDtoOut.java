package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_dynamic_mapping;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
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
      @Schema(description = "Extracted properties") Map<String, String> properties,
      @Schema(description = "Extracted relations") List<DryRunRelationDto> relations) {
  }

  @JsonNaming(SnakeCaseStrategy.class)
  public record DryRunRelationDto(@Schema(description = "Relation name") String name,
      @Schema(description = "Target entity identifiers extracted from payload") @JsonProperty("target_entity_identifiers") List<String> targetEntityIdentifiers) {
  }

  @JsonNaming(SnakeCaseStrategy.class)
  public record DryRunErrorDto(@Schema(description = "Error type") String type,
      @Schema(description = "Error message") String message) {
  }
}
