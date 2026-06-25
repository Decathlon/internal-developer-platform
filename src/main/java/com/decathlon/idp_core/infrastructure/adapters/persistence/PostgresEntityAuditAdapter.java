package com.decathlon.idp_core.infrastructure.adapters.persistence;

import java.time.Instant;
import java.util.List;
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

    @SuppressWarnings("unchecked")
    List<Object[]> revisions = auditReader.createQuery()
        .forRevisionsOfEntity(EntityJpaEntity.class, false, true).add(AuditEntity.id().eq(entityId))
        .addOrder(AuditEntity.revisionNumber().desc()).getResultList();

    return revisions.stream().map(revision -> mapToEntityAuditInfo(revision, entityId)).toList();
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

  private EntityAuditInfo mapToEntityAuditInfo(Object[] revision, UUID entityId) {
    CustomRevisionEntity revisionEntity = (CustomRevisionEntity) revision[1];
    RevisionType revisionType = (RevisionType) revision[2];

    Number revisionNumber = revisionEntity.getRev();
    Instant revisionDate = Instant.ofEpochMilli(revisionEntity.getRevisionTimestamp());
    String revisionTypeStr = mapRevisionType(revisionType);
    String modifiedBy = revisionEntity.getAuthId() != null ? revisionEntity.getAuthId() : "system";

    EntityAuditInfo.EntitySnapshot snapshot = null;

    Number snapshotRevisionNumber = revisionType == RevisionType.DEL
        ? revisionNumber.longValue() - 1
        : revisionNumber;

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

    return new EntityAuditInfo(revisionNumber, revisionDate, revisionTypeStr, modifiedBy, snapshot);
  }

  /// Converts a list of JPA property entities to domain property snapshot
  /// records.
  /// Ensures null safety and defensive copying to preserve immutability.
  private List<EntityAuditInfo.PropertySnapshot> mapPropertySnapshots(
      List<PropertyJpaEntity> properties) {
    if (properties == null || properties.isEmpty()) {
      return List.of();
    }
    return properties.stream().map(
        prop -> new EntityAuditInfo.PropertySnapshot(prop.getId(), prop.getName(), prop.getValue()))
        .toList();
  }

  /// Converts a list of JPA relation entities to domain relation snapshot
  /// records.
  /// Ensures null safety and defensive copying to preserve immutability of target
  /// identifiers.
  private List<EntityAuditInfo.RelationSnapshot> mapRelationSnapshots(
      List<RelationJpaEntity> relations) {
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
