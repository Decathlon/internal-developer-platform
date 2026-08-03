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
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.model.entity.EntityAuditInfo;
import com.decathlon.idp_core.domain.port.audit.EntityAuditPort;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.audit.CustomRevisionEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.EntityJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.PropertyJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.RelationJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.RelationTargetJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaEntityRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostgresEntityAuditAdapter implements EntityAuditPort {

  private final EntityManager entityManager;
  private final JpaEntityRepository jpaEntityRepository;

  @Override
  public List<EntityAuditInfo> getEntityAuditHistory(String templateIdentifier,
      String entityIdentifier) {
    UUID entityId = getEntityId(templateIdentifier, entityIdentifier);

    AuditReader auditReader = AuditReaderFactory.get(entityManager);

    // 1. Fetch all revisions for this specific entity ordered from newest to oldest
    @SuppressWarnings("unchecked")
    List<Object[]> revisions = auditReader.createQuery()
        .forRevisionsOfEntity(EntityJpaEntity.class, false, true).add(AuditEntity.id().eq(entityId))
        .addOrder(AuditEntity.revisionNumber().desc()).getResultList();

    // 2. Iterate using indices to safely find the pre-deletion state from history
    List<EntityAuditInfo> auditInfoList = new ArrayList<>(revisions.size());
    for (int i = 0; i < revisions.size(); i++) {
      Object[] revision = revisions.get(i);
      CustomRevisionEntity revisionEntity = (CustomRevisionEntity) revision[1];
      RevisionType revisionType = (RevisionType) revision[2];

      Number snapshotRevisionNumber = null;
      if (revisionType != RevisionType.DEL) {
        // For CREATED/UPDATED, the state matches the current revision number
        snapshotRevisionNumber = revisionEntity.getRev();
      } else if (i + 1 < revisions.size()) {
        // For DELETED, the previous state is exactly the next item in our descending
        // list
        CustomRevisionEntity previousRevisionEntity = (CustomRevisionEntity) revisions
            .get(i + 1)[1];
        snapshotRevisionNumber = previousRevisionEntity.getRev();
      }

      auditInfoList.add(mapToEntityAuditInfo(revision, entityId, snapshotRevisionNumber));
    }

    return auditInfoList;
  }

  private UUID getEntityId(String templateIdentifier, String entityIdentifier) {
    return jpaEntityRepository
        .findByTemplateIdentifierAndIdentifier(templateIdentifier, entityIdentifier)
        .map(EntityJpaEntity::getId)
        .orElseGet(() -> findEntityIdInAuditHistory(templateIdentifier, entityIdentifier));
  }

  private UUID findEntityIdInAuditHistory(String templateIdentifier, String entityIdentifier) {
    AuditReader auditReader = AuditReaderFactory.get(entityManager);

    @SuppressWarnings("unchecked")
    List<Object[]> revisions = auditReader.createQuery()
        .forRevisionsOfEntity(EntityJpaEntity.class, false, true)
        .add(AuditEntity.property("templateIdentifier").eq(templateIdentifier))
        .add(AuditEntity.property("identifier").eq(entityIdentifier))
        .addOrder(AuditEntity.revisionNumber().desc()).getResultList();

    if (!revisions.isEmpty() && revisions.getFirst()[0]instanceof EntityJpaEntity auditedEntity) {
      return auditedEntity.getId();
    }
    throw new EntityNotFoundException(templateIdentifier, entityIdentifier);
  }

  private EntityAuditInfo mapToEntityAuditInfo(Object[] revision, UUID entityId,
      Number snapshotRevisionNumber) {
    CustomRevisionEntity revisionEntity = (CustomRevisionEntity) revision[1];
    RevisionType revisionType = (RevisionType) revision[2];

    Number revisionNumber = revisionEntity.getRev();
    Instant revisionDate = Instant.ofEpochMilli(revisionEntity.getRevisionTimestamp());
    String revisionTypeStr = mapRevisionType(revisionType);
    String modifiedBy = revisionEntity.getAuthId() != null ? revisionEntity.getAuthId() : "system";

    EntityAuditInfo.EntitySnapshot snapshot = null;

    // Only attempt to read snapshot if a valid historical revision was resolved
    if (snapshotRevisionNumber != null) {
      AuditReader auditReader = AuditReaderFactory.get(entityManager);
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
