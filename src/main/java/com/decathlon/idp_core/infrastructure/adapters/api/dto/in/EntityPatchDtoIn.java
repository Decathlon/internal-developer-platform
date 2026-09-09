package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_ENTITY_NAME;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_ENTITY_PROPERTIES;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FIELD_ENTITY_RELATIONS;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.SCHEMA_ENTITY_PATCH_IN;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/// Input DTO for partially updating an entity.
///
/// Omitted fields remain unchanged. An explicitly supplied empty properties map
/// or relations list is passed through as an empty collection.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(SnakeCaseStrategy.class)
@Schema(description = SCHEMA_ENTITY_PATCH_IN)
public class EntityPatchDtoIn {

  @Schema(description = FIELD_ENTITY_NAME, example = "my-web-service-updated")
  private String name;

  @Schema(description = FIELD_ENTITY_PROPERTIES, example = "{\"port\": \"8080\"}")
  private Map<String, String> properties;

  @Valid
  @Schema(description = FIELD_ENTITY_RELATIONS)
  private List<EntityDtoInCommonFields.RelationDtoIn> relations;
}
