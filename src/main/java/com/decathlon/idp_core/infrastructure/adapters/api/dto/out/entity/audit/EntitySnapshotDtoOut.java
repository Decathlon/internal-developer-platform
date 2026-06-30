package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.audit;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/// Nested DTO for entity snapshot within audit history.
///
/// **Business purpose:** Captures the complete state of an entity (core data, all
/// properties, and all relations) as it existed at a specific audit revision,
/// enabling clients to reconstruct the exact entity state as it was at any point
/// in the entity's history.
@Data
@Builder
@JsonNaming(SnakeCaseStrategy.class)
@Schema(description = "Snapshot of entity state at a specific audit revision")
public class EntitySnapshotDtoOut {

  @Schema(description = "Unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
  private UUID id;

  @Schema(description = "Template identifier", example = "web-service")
  private String templateIdentifier;

  @Schema(description = "Entity name", example = "My Service")
  private String name;

  @Schema(description = "Entity identifier", example = "my-service-api")
  private String identifier;

  @Schema(description = "Properties of the entity at this revision")
  private List<PropertySnapshotDtoOut> properties;

  @Schema(description = "Relations of the entity at this revision")
  private List<RelationSnapshotDtoOut> relations;

  @Schema(description = "Map of modified flags", example = "{'name_mod': true, 'identifier_mod': false}")
  private Map<String, Boolean> modifiedFlags;
}
