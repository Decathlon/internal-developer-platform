package com.decathlon.idp_core.infrastructure.adapters.persistence.repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.springframework.stereotype.Repository;

import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.audit.CustomRevisionEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.EntityJpaEntity;

/**
 * Executes Envers queries used to retrieve entity audit history.
 */
@Repository
public class JpaAuditRepository {

  /**
   * Finds all entity identifiers matching the supplied composite business key in
   * audit history.
   *
   * @param auditReader
   *          Envers reader used to execute the query
   * @param templateIdentifier
   *          entity template identifier
   * @param entityIdentifier
   *          entity identifier
   * @return identifiers found in audit history
   * @throws EntityNotFoundException
   *           when no matching audit entry exists
   */
  public Set<UUID> findEntityIdsInAuditHistory(AuditReader auditReader, String templateIdentifier,
      String entityIdentifier) {
    AuditQuery query = auditReader.createQuery()
        .forRevisionsOfEntity(EntityJpaEntity.class, false, true)
        .add(AuditEntity.property("templateIdentifier").eq(templateIdentifier))
        .add(AuditEntity.property("identifier").eq(entityIdentifier))
        .addOrder(AuditEntity.revisionNumber().desc());

    Set<UUID> entityIds = new HashSet<>();
    for (EnversRevision<EntityJpaEntity> revision : executeAuditQuery(query,
        EntityJpaEntity.class)) {
      if (revision.entity() != null && revision.entity().getId() != null) {
        entityIds.add(revision.entity().getId());
      }
    }

    if (entityIds.isEmpty()) {
      throw new EntityNotFoundException(templateIdentifier, entityIdentifier);
    }
    return entityIds;
  }

  /**
   * Finds all revisions for the supplied entity identifiers, newest first.
   *
   * @param auditReader
   *          Envers reader used to execute the query
   * @param entityIds
   *          entity identifiers to query
   * @return typed audit revisions
   */
  public List<EnversRevision<EntityJpaEntity>> findEntityRevisions(AuditReader auditReader,
      Set<UUID> entityIds) {
    AuditQuery query = auditReader.createQuery()
        .forRevisionsOfEntity(EntityJpaEntity.class, false, true)
        .add(AuditEntity.id().in(entityIds)).addOrder(AuditEntity.revisionNumber().desc());
    return executeAuditQuery(query, EntityJpaEntity.class);
  }

  /**
   * Represents the three values returned by an Envers revision query.
   *
   * @param <T>
   *          audited entity type
   * @param entity
   *          audited entity snapshot
   * @param revisionEntity
   *          revision metadata
   * @param revisionType
   *          revision operation
   */
  public record EnversRevision<T> (T entity, CustomRevisionEntity revisionEntity,
      RevisionType revisionType) {
  }

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
}
