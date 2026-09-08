package com.decathlon.idp_core.infrastructure.adapters.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.decathlon.idp_core.domain.model.entity.EntityAuditInfo;
import com.decathlon.idp_core.domain.port.audit.EntityAuditPort;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.EntityJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.PropertyJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.RelationJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.RelationTargetJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaAuditRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostgresEntityAuditAdapter implements EntityAuditPort {

  private final EntityManager entityManager;
  private final JpaAuditRepository jpaAuditRepository;

  @Override
  public List<EntityAuditInfo> getEntityAuditHistory(String templateIdentifier,
      String entityIdentifier) {
    AuditReader auditReader = AuditReaderFactory.get(entityManager);
    var entityIds = jpaAuditRepository.findEntityIdsInAuditHistory(auditReader, templateIdentifier,
        entityIdentifier);

    List<JpaAuditRepository.EnversRevision<EntityJpaEntity>> revisions = jpaAuditRepository
        .findEntityRevisions(auditReader, entityIds);

    List<EntityAuditInfo> auditInfoList = new ArrayList<>(revisions.size());
    for (int i = 0; i < revisions.size(); i++) {
      JpaAuditRepository.EnversRevision<EntityJpaEntity> currentRevision = revisions.get(i);

      Number snapshotRevisionNumber;
      if (currentRevision.revisionType() != RevisionType.DEL) {
        snapshotRevisionNumber = currentRevision.revisionEntity().getRev();
      } else {
        snapshotRevisionNumber = findPreviousRevisionNumber(revisions, i,
            currentRevision.entity().getId());
      }

      auditInfoList.add(mapToEntityAuditInfo(currentRevision, auditReader, snapshotRevisionNumber));
    }

    return auditInfoList;
  }

  private Number findPreviousRevisionNumber(
      List<JpaAuditRepository.EnversRevision<EntityJpaEntity>> revisions, int currentIndex,
      UUID entityId) {
    for (int i = currentIndex + 1; i < revisions.size(); i++) {
      JpaAuditRepository.EnversRevision<EntityJpaEntity> previousRevision = revisions.get(i);
      if (previousRevision.entity().getId().equals(entityId)) {
        return previousRevision.revisionEntity().getRev();
      }
    }
    return null;
  }

  private EntityAuditInfo mapToEntityAuditInfo(
      JpaAuditRepository.EnversRevision<EntityJpaEntity> revision, AuditReader auditReader,
      Number snapshotRevisionNumber) {
    Number revisionNumber = revision.revisionEntity().getRev();
    Instant revisionDate = Instant.ofEpochMilli(revision.revisionEntity().getRevisionTimestamp());
    String revisionTypeStr = mapRevisionType(revision.revisionType());
    String modifiedBy = revision.revisionEntity().getAuthId() != null
        ? revision.revisionEntity().getAuthId()
        : "system";

    EntityAuditInfo.EntitySnapshot snapshot = null;
    UUID entityId = revision.entity().getId();

    // Only attempt to read snapshot if a valid historical revision was resolved
    if (snapshotRevisionNumber != null) {
      EntityJpaEntity historicalEntity = auditReader.find(EntityJpaEntity.class, entityId,
          snapshotRevisionNumber);

      if (historicalEntity != null) {
        List<EntityAuditInfo.PropertySnapshot> propertySnapshots = mapPropertySnapshots(
            historicalEntity.getProperties());
        List<EntityAuditInfo.RelationSnapshot> relationSnapshots = mapRelationSnapshots(
            historicalEntity.getRelations());

        snapshot = new EntityAuditInfo.EntitySnapshot(historicalEntity.getId(),
            historicalEntity.getTemplateIdentifier(), historicalEntity.getName(),
            historicalEntity.getIdentifier(), propertySnapshots, relationSnapshots);
      }
    }

    return new EntityAuditInfo(revisionNumber, revisionDate, revisionTypeStr, modifiedBy, snapshot);
  }

  private List<EntityAuditInfo.PropertySnapshot> mapPropertySnapshots(
      Set<PropertyJpaEntity> properties) {
    if (properties == null || properties.isEmpty()) {
      return List.of();
    }
    return properties.stream().map(
        prop -> new EntityAuditInfo.PropertySnapshot(prop.getId(), prop.getName(), prop.getValue()))
        .toList();
  }

  private List<EntityAuditInfo.RelationSnapshot> mapRelationSnapshots(
      Set<RelationJpaEntity> relations) {
    if (relations == null || relations.isEmpty()) {
      return List.of();
    }
    return relations.stream()
        .map(rel -> new EntityAuditInfo.RelationSnapshot(rel.getId(), rel.getName(),
            rel.getTargetTemplateIdentifier(),
            rel.getTargetEntities() != null
                ? rel.getTargetEntities().stream()
                    .map(RelationTargetJpaEntity::getTargetEntityIdentifier).toList()
                : List.of()))
        .toList();
  }

  private String mapRevisionType(RevisionType revisionType) {
    return switch (revisionType) {
      case ADD -> "CREATED";
      case MOD -> "UPDATED";
      case DEL -> "DELETED";
    };
  }

}
