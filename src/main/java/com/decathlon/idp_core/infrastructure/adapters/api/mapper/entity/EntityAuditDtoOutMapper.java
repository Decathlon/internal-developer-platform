package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity.EntityAuditInfo;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.audit.EntityAuditDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.audit.EntitySnapshotDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.audit.PropertySnapshotDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.audit.RelationSnapshotDtoOut;

/// Mapper converting domain entity audit information to API response DTOs.
///
/// **Business purpose:** Translates immutable domain audit records into mutable
/// DTO structures suitable for serialization to JSON in REST API responses.
/// Handles null safety and defensive mapping of nested snapshot collections.
@Component
public class EntityAuditDtoOutMapper {

  public EntityAuditDtoOut fromEntityAuditInfo(EntityAuditInfo auditInfo) {
    if (auditInfo == null) {
      return null;
    }

    EntitySnapshotDtoOut snapshotDto = null;
    if (auditInfo.snapshot() != null) {
      snapshotDto = EntitySnapshotDtoOut.builder().id(auditInfo.snapshot().id())
          .templateIdentifier(auditInfo.snapshot().templateIdentifier())
          .name(auditInfo.snapshot().name()).identifier(auditInfo.snapshot().identifier())
          .properties(mapPropertySnapshots(auditInfo.snapshot().properties()))
          .relations(mapRelationSnapshots(auditInfo.snapshot().relations()))
          .modifiedFlags(auditInfo.snapshot().modifiedFlags() != null
              ? Map.copyOf(auditInfo.snapshot().modifiedFlags())
              : Map.of())
          .build();
    }

    return EntityAuditDtoOut.builder().revisionNumber(auditInfo.revisionNumber())
        .revisionDate(auditInfo.revisionDate()).revisionType(auditInfo.revisionType())
        .modifiedBy(auditInfo.modifiedBy()).snapshot(snapshotDto).build();
  }

  public List<EntityAuditDtoOut> fromEntityAuditInfoList(List<EntityAuditInfo> auditInfoList) {
    if (auditInfoList == null) {
      return List.of();
    }
    return auditInfoList.stream().map(this::fromEntityAuditInfo).toList();
  }

  /// Maps domain property snapshots to DTO property snapshots.
  /// Ensures null safety and empty collection handling.
  private List<PropertySnapshotDtoOut> mapPropertySnapshots(
      List<EntityAuditInfo.PropertySnapshot> properties) {
    if (properties == null || properties.isEmpty()) {
      return List.of();
    }
    return properties.stream()
        .map(prop -> PropertySnapshotDtoOut.builder().id(prop.id()).name(prop.name())
            .value(prop.value())
            .modifiedFlags(
                prop.modifiedFlags() != null ? Map.copyOf(prop.modifiedFlags()) : Map.of())
            .build())
        .toList();
  }

  /// Maps domain relation snapshots to DTO relation snapshots.
  /// Ensures null safety and empty collection handling of target identifiers.
  private List<RelationSnapshotDtoOut> mapRelationSnapshots(
      List<EntityAuditInfo.RelationSnapshot> relations) {
    if (relations == null || relations.isEmpty()) {
      return List.of();
    }
    return relations.stream()
        .map(rel -> RelationSnapshotDtoOut.builder().id(rel.id()).name(rel.name())
            .targetTemplateIdentifier(rel.targetTemplateIdentifier())
            .targetEntityIdentifiers(rel.targetEntityIdentifiers() != null
                ? List.copyOf(rel.targetEntityIdentifiers())
                : List.of())
            .modifiedFlags(rel.modifiedFlags() != null ? Map.copyOf(rel.modifiedFlags()) : Map.of())
            .build())
        .toList();
  }
}
