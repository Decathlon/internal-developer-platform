package com.decathlon.idp_core.infrastructure.adapters.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.model.entity.EntityAuditInfo;
import com.decathlon.idp_core.domain.port.audit.EntityAuditPort;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.audit.CustomRevisionEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.EntityJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.PropertyJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.RelationJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.RelationTargetJpaEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostgresEntityAuditAdapter implements EntityAuditPort {

  private final EntityManager entityManager;

  @Override
  public List<EntityAuditInfo> getEntityAuditHistory(String templateIdentifier,
      String entityIdentifier) {
    AuditReader auditReader = AuditReaderFactory.get(entityManager);
    Set<UUID> entityIds = findEntityIdsInAuditHistory(auditReader, templateIdentifier,
        entityIdentifier);

    // Fetch all revisions for all matching IDs in a single query (fixes N+1 issue)
    AuditQuery query = auditReader.createQuery()
        .forRevisionsOfEntity(EntityJpaEntity.class, false, true)
        .add(AuditEntity.id().in(entityIds)).addOrder(AuditEntity.revisionNumber().desc());

    List<EnversRevision<EntityJpaEntity>> revisions = executeAuditQuery(query,
        EntityJpaEntity.class);

    List<EntityAuditInfo> auditInfoList = new ArrayList<>(revisions.size());
    for (int i = 0; i < revisions.size(); i++) {
      EnversRevision<EntityJpaEntity> currentRevision = revisions.get(i);

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

  private Number findPreviousRevisionNumber(List<EnversRevision<EntityJpaEntity>> revisions,
      int currentIndex, UUID entityId) {
    for (int i = currentIndex + 1; i < revisions.size(); i++) {
      EnversRevision<EntityJpaEntity> previousRevision = revisions.get(i);
      if (previousRevision.entity().getId().equals(entityId)) {
        return previousRevision.revisionEntity().getRev();
      }
    }
    return null;
  }

  private Set<UUID> findEntityIdsInAuditHistory(AuditReader auditReader, String templateIdentifier,
      String entityIdentifier) {
    AuditQuery query = auditReader.createQuery()
        .forRevisionsOfEntity(EntityJpaEntity.class, false, true)
        .add(AuditEntity.property("templateIdentifier").eq(templateIdentifier))
        .add(AuditEntity.property("identifier").eq(entityIdentifier))
        .addOrder(AuditEntity.revisionNumber().desc());

    List<EnversRevision<EntityJpaEntity>> revisions = executeAuditQuery(query,
        EntityJpaEntity.class);

    Set<UUID> entityIds = new HashSet<>();
    for (EnversRevision<EntityJpaEntity> revision : revisions) {
      if (revision.entity() != null && revision.entity().getId() != null) {
        entityIds.add(revision.entity().getId());
      }
    }

    if (!entityIds.isEmpty()) {
      return entityIds;
    }

    throw new EntityNotFoundException(templateIdentifier, entityIdentifier);
  }

  /**
   * Centralized utility method to execute the query and safely map the raw
   * Object[] array. This is the ONLY place where the unchecked cast warning is
   * suppressed.
   */
  @SuppressWarnings("unchecked")
  private <T> List<EnversRevision<T>> executeAuditQuery(AuditQuery query, Class<T> entityType) {
    List<Object[]> results = query.getResultList();
    return results.stream()
        .filter(row -> row.length >= 3 && entityType.isInstance(row[0])
            && row[1] instanceof CustomRevisionEntity && row[2] instanceof RevisionType)
        .map(row -> new EnversRevision<>(entityType.cast(row[0]), (CustomRevisionEntity) row[1],
            (RevisionType) row[2]))
        .toList();
  }

  private EntityAuditInfo mapToEntityAuditInfo(EnversRevision<EntityJpaEntity> revision,
      AuditReader auditReader, Number snapshotRevisionNumber) {
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

  /**
   * strongly-typed record to replace the ambiguous Object[] array from Envers.
   */
  private record EnversRevision<T> (T entity, CustomRevisionEntity revisionEntity,
      RevisionType revisionType) {
  }
}
