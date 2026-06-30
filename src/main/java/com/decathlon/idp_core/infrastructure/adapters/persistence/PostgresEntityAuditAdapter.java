package com.decathlon.idp_core.infrastructure.adapters.persistence;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private static final Logger logger = LoggerFactory.getLogger(PostgresEntityAuditAdapter.class);

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
      // Retrieve modification flags for entity using Envers API
      Map<String, Boolean> entityModFlags = getModificationFlags(EntityJpaEntity.class, entityId,
          snapshotRevisionNumber);

      List<EntityAuditInfo.PropertySnapshot> propertySnapshots = mapPropertySnapshots(
          historicalEntity.getProperties(), snapshotRevisionNumber);
      List<EntityAuditInfo.RelationSnapshot> relationSnapshots = mapRelationSnapshots(
          historicalEntity.getRelations(), snapshotRevisionNumber);

      snapshot = new EntityAuditInfo.EntitySnapshot(historicalEntity.getId(),
          historicalEntity.getTemplateIdentifier(), historicalEntity.getName(),
          historicalEntity.getIdentifier(), entityModFlags, propertySnapshots, relationSnapshots);
    }

    return new EntityAuditInfo(revisionNumber, revisionDate, revisionTypeStr, modifiedBy, snapshot);
  }

  /// Retrieves all modification flags using Hibernate Envers API natively.
  /// Queries the modified property names for a specific revision and entity type,
  /// then maps them to the expected format (e.g. "fieldName_mod" -> true).
  private Map<String, Boolean> getModificationFlags(Class<?> clazz, UUID id,
      Number revisionNumber) {
    Map<String, Boolean> modifiedFlags = new HashMap<>();
    try {
      AuditReader auditReader = AuditReaderFactory.get(entityManager);

      @SuppressWarnings("unchecked")
      List<Object[]> revisions = auditReader.createQuery()
          .forRevisionsOfEntityWithChanges(clazz, true).add(AuditEntity.id().eq(id))
          .add(AuditEntity.revisionNumber().eq(revisionNumber)).getResultList();

      if (!revisions.isEmpty()) {
        Object changesObj = revisions.getFirst()[3];
        if (changesObj instanceof Set<?> changedProperties) {
          for (Object prop : changedProperties) {
            if (prop instanceof String propName) {
              modifiedFlags.put(propName + "_mod", true);
            }
          }
        }
      }
    } catch (Exception e) {
      logger.warn(
          "Failed to retrieve modification flags for entity '{}' of type '{}' at revision '{}'. "
              + "Modification flag tracking is optional and will not be included in this audit entry.",
          id, clazz.getSimpleName(), revisionNumber.longValue(), e);
    }
    return modifiedFlags;
  }

  private List<EntityAuditInfo.PropertySnapshot> mapPropertySnapshots(
      List<PropertyJpaEntity> properties, Number revisionNumber) {
    if (properties == null || properties.isEmpty()) {
      return List.of();
    }
    return properties.stream().map(prop -> {
      Map<String, Boolean> modFlags = getModificationFlags(PropertyJpaEntity.class, prop.getId(),
          revisionNumber);
      return new EntityAuditInfo.PropertySnapshot(prop.getId(), prop.getName(), prop.getValue(),
          modFlags);
    }).toList();
  }

  private List<EntityAuditInfo.RelationSnapshot> mapRelationSnapshots(
      List<RelationJpaEntity> relations, Number revisionNumber) {
    if (relations == null || relations.isEmpty()) {
      return List.of();
    }
    return relations.stream().map(rel -> {
      Map<String, Boolean> modFlags = getModificationFlags(RelationJpaEntity.class, rel.getId(),
          revisionNumber);
      return new EntityAuditInfo.RelationSnapshot(rel.getId(), rel.getName(),
          rel.getTargetTemplateIdentifier(),
          rel.getTargetEntities() != null
              ? rel.getTargetEntities().stream()
                  .map(RelationTargetJpaEntity::getTargetEntityIdentifier).toList()
              : List.of(),
          modFlags);
    }).toList();
  }

  private String mapRevisionType(RevisionType revisionType) {
    return switch (revisionType) {
      case ADD -> "CREATED";
      case MOD -> "UPDATED";
      case DEL -> "DELETED";
    };
  }
}
