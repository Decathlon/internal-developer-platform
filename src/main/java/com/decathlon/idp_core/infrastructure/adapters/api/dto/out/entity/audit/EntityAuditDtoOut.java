package com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.audit;

import java.time.Instant;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/// Output DTO for entity audit information exposed via REST API.
///
/// **Infrastructure responsibility:** Serializes audit data for HTTP responses
/// using JSON with snake_case naming convention.
@Data
@Builder
@JsonNaming(SnakeCaseStrategy.class)
@Schema(description = "Audit information for an entity revision")
public class EntityAuditDtoOut {

  @Schema(description = "Unique revision number in the audit log", example = "42")
  private Number revisionNumber;

  @Schema(description = "Timestamp when the revision was created", example = "2026-06-08T14:37:27.743Z")
  private Instant revisionDate;

  @Schema(description = "Type of operation performed (CREATED, UPDATED, DELETED)", example = "UPDATED")
  private String revisionType;

  @Schema(description = "Identifier of the user who performed the modification", example = "user@example.com")
  private String modifiedBy;

  @Schema(description = "Snapshot of the entity state at this revision")
  private EntitySnapshotDtoOut snapshot;

}
