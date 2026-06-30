package com.decathlon.idp_core.infrastructure.adapters.persistence;

import java.sql.ResultSetMetaData;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
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
  private final JdbcTemplate jdbcTemplate;

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
      // Retrieve modification flags for entity from audit table
      Map<String, Boolean> entityModFlags = getModificationFlags("entity_aud", entityId,
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

  /// Retrieves all modification flags from an audit table dynamically.
  /// Selects all columns ending with '_mod' and returns them as a map.
  /// Only flags that are true are included in the response (missing flags are
  /// assumed false).
  ///
  /// **Note:** Modification tracking is an optional feature. If retrieval fails,
  /// the audit
  /// history remains available without granular field-level change tracking.
  ///
  /// @param tableName name of the audit table (for example, "entity_aud",
  /// "property_aud")
  /// @param id UUID of the audited entity
  /// @param revisionNumber revision number to query
  /// @return map of field names to modification status (for example, "name_mod"
  /// ->
  /// true);
  /// only true flags are included in the map
  private Map<String, Boolean> getModificationFlags(String tableName, UUID id,
      Number revisionNumber) {
    Map<String, Boolean> modifiedFlags = new HashMap<>();
    try {
      String sql = "SELECT * FROM " + tableName + " WHERE id = ? AND rev = ?";

      jdbcTemplate.query(sql, rs -> {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
          String columnName = metaData.getColumnName(i);
          if (columnName.endsWith("_mod")) {
            Boolean value = rs.getBoolean(i);
            if (!rs.wasNull() && value) {
              modifiedFlags.put(columnName, true);
            }
          }
        }
      }, id, revisionNumber.longValue());
    } catch (Exception e) {
      // Modification tracking is optional. Log warning but don't fail audit history
      // retrieval.
      logger.warn("Failed to retrieve modification flags for entity '{}' at revision '{}'. "
          + "Modification flag tracking is optional and will not be included in this audit entry.",
          id, revisionNumber.longValue(), e);
    }
    return modifiedFlags;
  }

  /// Converts a list of JPA property entities to domain property snapshot
  /// records.
  /// Ensures null safety and defensive copying to preserve immutability.
  private List<EntityAuditInfo.PropertySnapshot> mapPropertySnapshots(
      List<PropertyJpaEntity> properties, Number revisionNumber) {
    if (properties == null || properties.isEmpty()) {
      return List.of();
    }
    return properties.stream().map(prop -> {
      Map<String, Boolean> modFlags = getModificationFlags("property_aud", prop.getId(),
          revisionNumber);
      return new EntityAuditInfo.PropertySnapshot(prop.getId(), prop.getName(), prop.getValue(),
          modFlags);
    }).toList();
  }

  /// Converts a list of JPA relation entities to domain relation snapshot
  /// records.
  /// Ensures null safety and defensive copying to preserve immutability of target
  /// identifiers.
  private List<EntityAuditInfo.RelationSnapshot> mapRelationSnapshots(
      List<RelationJpaEntity> relations, Number revisionNumber) {
    if (relations == null || relations.isEmpty()) {
      return List.of();
    }
    return relations.stream().map(rel -> {
      Map<String, Boolean> modFlags = getModificationFlags("relation_aud", rel.getId(),
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
