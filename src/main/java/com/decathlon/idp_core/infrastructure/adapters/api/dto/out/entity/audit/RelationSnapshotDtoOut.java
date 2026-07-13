package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.audit;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/// Nested DTO for relation snapshot within entity audit history.
///
/// **Business purpose:** Captures the immutable state of a single entity relation
/// (relationship to other entities) as it existed at a specific audit revision,
/// enabling clients to reconstruct the exact relationship targets as they were
/// at any point in the entity's history.
@Data
@Builder
@JsonNaming(SnakeCaseStrategy.class)
@Schema(description = "Snapshot of a relation at a specific audit revision")
public class RelationSnapshotDtoOut {

  @Schema(description = "Name of the relation matching a RelationDefinition", example = "deployed-on")
  private String name;

  @Schema(description = "Identifier of the target entity template", example = "infrastructure")
  private String targetTemplateIdentifier;

  @Schema(description = "Business identifiers of target entities", example = "[\"prod-cluster\", \"staging-cluster\"]")
  private List<String> targetEntityIdentifiers;
}
