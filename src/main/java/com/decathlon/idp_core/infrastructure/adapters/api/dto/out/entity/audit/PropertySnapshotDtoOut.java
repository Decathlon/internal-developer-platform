package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.audit;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/// Nested DTO for property snapshot within entity audit history.
///
/// **Business purpose:** Captures the immutable state of a single entity property
/// as it existed at a specific audit revision, enabling clients to reconstruct
/// the exact property values as they were at any point in the entity's history.
@Data
@Builder
@JsonNaming(SnakeCaseStrategy.class)
@Schema(description = "Snapshot of a property at a specific audit revision")
public class PropertySnapshotDtoOut {

  @Schema(description = "Name of the property matching a PropertyDefinition", example = "description")
  private String name;

  @Schema(description = "Value of the property at this revision", example = "My service description")
  private String value;
}
