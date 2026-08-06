package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_dynamic_mapping;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.*;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;

/// Result of an entity dynamic mapping dry-run validation.
@Schema(description = SCHEMA_DRY_RUN_RESULT)
@JsonNaming(SnakeCaseStrategy.class)
public record EntityDynamicMappingDryRunDtoOut(
    @Schema(description = SCHEMA_DRY_RUN_RESULTS) List<DryRunEntityResultDto> results) {

  @JsonNaming(SnakeCaseStrategy.class)
  public record DryRunEntityResultDto(
      @Schema(description = SCHEMA_DRY_RUN_MAPPING_TEMPLATE_IDENTIFIER) String mappingTemplateIdentifier,
      @Schema(description = SCHEMA_DRY_RUN_SUCCESS) boolean success,
      @Schema(description = SCHEMA_DRY_RUN_ENTITY) DryRunEntityDto entity,
      @Schema(description = SCHEMA_DRY_RUN_ERROR) DryRunErrorDto error) {
  }

  @JsonNaming(SnakeCaseStrategy.class)
  public record DryRunEntityDto(
      @Schema(description = SCHEMA_DRY_RUN_TEMPLATE_IDENTIFIER) String templateIdentifier,
      @Schema(description = SCHEMA_DRY_RUN_ENTITY_NAME) String name,
      @Schema(description = SCHEMA_DRY_RUN_ENTITY_IDENTIFIER) String identifier,
      @Schema(description = SCHEMA_DRY_RUN_PROPERTIES) Map<String, String> properties,
      @Schema(description = SCHEMA_DRY_RUN_RELATIONS) List<DryRunRelationDto> relations) {
  }

  @JsonNaming(SnakeCaseStrategy.class)
  public record DryRunRelationDto(@Schema(description = SCHEMA_DRY_RUN_RELATION_NAME) String name,
      @Schema(description = SCHEMA_DRY_RUN_RELATION_TARGET_IDENTIFIERS) @JsonProperty("target_entity_identifiers") List<String> targetEntityIdentifiers) {
  }

  @JsonNaming(SnakeCaseStrategy.class)
  public record DryRunErrorDto(@Schema(description = SCHEMA_DRY_RUN_ERROR_TYPE) String type,
      @Schema(description = SCHEMA_DRY_RUN_ERROR_MESSAGE) String message) {
  }
}
