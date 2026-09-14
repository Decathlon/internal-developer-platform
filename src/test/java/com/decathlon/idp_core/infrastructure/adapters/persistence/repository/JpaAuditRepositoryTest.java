package com.decathlon.idp_core.infrastructure.adapters.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.AuditQueryCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.audit.CustomRevisionEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.EntityJpaEntity;

class JpaAuditRepositoryTest {

  @Mock
  private AuditReader auditReader;

  @Mock
  private AuditQueryCreator auditQueryCreator;

  @Mock
  private AuditQuery auditQuery;

  private JpaAuditRepository repository;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    repository = new JpaAuditRepository();
    when(auditReader.createQuery()).thenReturn(auditQueryCreator);
    when(auditQueryCreator.forRevisionsOfEntity(EntityJpaEntity.class, false, true))
        .thenReturn(auditQuery);
    when(auditQuery.add(any())).thenReturn(auditQuery);
    when(auditQuery.addOrder(any())).thenReturn(auditQuery);
  }

  @Test
  void shouldFindDistinctEntityIdsInAuditHistory() {
    UUID firstId = UUID.randomUUID();
    UUID secondId = UUID.randomUUID();
    EntityJpaEntity firstEntity = mockEntity(firstId);
    EntityJpaEntity secondEntity = mockEntity(secondId);
    CustomRevisionEntity revisionEntity = mock(CustomRevisionEntity.class);
    when(auditQuery.getResultList())
        .thenReturn(List.<Object[]>of(new Object[]{firstEntity, revisionEntity, RevisionType.MOD},
            new Object[]{firstEntity, revisionEntity, RevisionType.ADD},
            new Object[]{secondEntity, revisionEntity, RevisionType.ADD}));

    Set<UUID> result = repository.findEntityIdsInAuditHistory(auditReader, "template", "entity");

    assertThat(result).containsExactlyInAnyOrder(firstId, secondId);
  }

  @Test
  void shouldRejectAuditHistoryWithoutValidEntitySnapshots() {
    when(auditQuery.getResultList()).thenReturn(List
        .<Object[]>of(new Object[]{"invalid", mock(CustomRevisionEntity.class), RevisionType.ADD}));

    assertThatThrownBy(
        () -> repository.findEntityIdsInAuditHistory(auditReader, "template", "entity"))
            .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void shouldReturnOnlyValidTypedRevisions() {
    UUID entityId = UUID.randomUUID();
    EntityJpaEntity entity = mockEntity(entityId);
    CustomRevisionEntity revisionEntity = mock(CustomRevisionEntity.class);
    when(auditQuery.getResultList())
        .thenReturn(List.<Object[]>of(new Object[]{entity, revisionEntity, RevisionType.ADD},
            new Object[]{entity, revisionEntity}));

    List<JpaAuditRepository.EnversRevision<EntityJpaEntity>> result = repository
        .findEntityRevisions(auditReader, Set.of(entityId));

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().entity()).isSameAs(entity);
  }

  private EntityJpaEntity mockEntity(UUID id) {
    EntityJpaEntity entity = mock(EntityJpaEntity.class);
    when(entity.getId()).thenReturn(id);
    return entity;
  }
}
