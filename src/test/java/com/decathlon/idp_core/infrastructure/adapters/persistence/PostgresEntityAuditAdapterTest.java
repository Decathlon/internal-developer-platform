package com.decathlon.idp_core.infrastructure.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.AuditQueryCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.model.entity.EntityAuditInfo;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.audit.CustomRevisionEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.EntityJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.PropertyJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.RelationJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.RelationTargetJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaEntityRepository;

/// Unit tests for PostgresEntityAuditAdapter.
/// Covers entity audit history retrieval with Hibernate Envers integration.
@DisplayName("PostgresEntityAuditAdapter Tests")
class PostgresEntityAuditAdapterTest {

  @Mock
  private EntityManager entityManager;

  @Mock
  private JpaEntityRepository jpaEntityRepository;

  @Mock
  private JdbcTemplate jdbcTemplate;

  @Mock
  private AuditReader auditReader;

  @Mock
  private AuditQueryCreator auditQueryCreator;

  @Mock
  private AuditQuery auditQuery;

  private PostgresEntityAuditAdapter adapter;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    adapter = new PostgresEntityAuditAdapter(entityManager, jpaEntityRepository, jdbcTemplate);
  }

  @Nested
  @DisplayName("Get Entity Audit History Tests")
  class GetEntityAuditHistoryTests {

    @Test
    @DisplayName("Should retrieve audit history for existing entity")
    void shouldRetrieveAuditHistoryForExistingEntity() {
      // Given
      UUID entityId = UUID.randomUUID();
      String templateIdentifier = "web-service";
      String entityIdentifier = "web-api";

      EntityJpaEntity jpaEntity = mock(EntityJpaEntity.class);
      when(jpaEntity.getId()).thenReturn(entityId);

      CustomRevisionEntity revisionEntity = mock(CustomRevisionEntity.class);
      when(revisionEntity.getRev()).thenReturn(1L);
      when(revisionEntity.getRevisionTimestamp()).thenReturn(System.currentTimeMillis());
      when(revisionEntity.getAuthId()).thenReturn("test-user");

      Object[] revision = {jpaEntity, revisionEntity, RevisionType.ADD};

      when(jpaEntityRepository.findByTemplateIdentifierAndIdentifier(templateIdentifier,
          entityIdentifier)).thenReturn(Optional.of(jpaEntity));

      try (
          MockedStatic<org.hibernate.envers.AuditReaderFactory> auditReaderFactoryMock = org.mockito.Mockito
              .mockStatic(org.hibernate.envers.AuditReaderFactory.class)) {
        auditReaderFactoryMock
            .when(() -> org.hibernate.envers.AuditReaderFactory.get(entityManager))
            .thenReturn(auditReader);

        when(auditReader.createQuery()).thenReturn(auditQueryCreator);
        when(auditQueryCreator.forRevisionsOfEntity(EntityJpaEntity.class, false, true))
            .thenReturn(auditQuery);
        when(auditQuery.add(any())).thenReturn(auditQuery);
        when(auditQuery.addOrder(any())).thenReturn(auditQuery);
        when(auditQuery.getResultList()).thenReturn(List.<Object[]>of(revision));

        EntityJpaEntity historicalEntity = mock(EntityJpaEntity.class);
        when(historicalEntity.getId()).thenReturn(entityId);
        when(historicalEntity.getTemplateIdentifier()).thenReturn(templateIdentifier);
        when(historicalEntity.getName()).thenReturn("Web API");
        when(historicalEntity.getIdentifier()).thenReturn(entityIdentifier);
        when(historicalEntity.getProperties()).thenReturn(List.of());
        when(historicalEntity.getRelations()).thenReturn(List.of());

        when(auditReader.find(EntityJpaEntity.class, entityId, 1L)).thenReturn(historicalEntity);

        // When
        List<EntityAuditInfo> result = adapter.getEntityAuditHistory(templateIdentifier,
            entityIdentifier);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().revisionNumber()).isEqualTo(1L);
        assertThat(result.getFirst().revisionType()).isEqualTo("CREATED");
        assertThat(result.getFirst().modifiedBy()).isEqualTo("test-user");
      }
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException if entity not found")
    void shouldThrowExceptionIfEntityNotFound() {
      // Given
      String templateIdentifier = "non-existent";
      String entityIdentifier = "non-existent";

      when(jpaEntityRepository.findByTemplateIdentifierAndIdentifier(templateIdentifier,
          entityIdentifier)).thenReturn(Optional.empty());

      try (
          MockedStatic<org.hibernate.envers.AuditReaderFactory> auditReaderFactoryMock = org.mockito.Mockito
              .mockStatic(org.hibernate.envers.AuditReaderFactory.class)) {
        auditReaderFactoryMock
            .when(() -> org.hibernate.envers.AuditReaderFactory.get(entityManager))
            .thenReturn(auditReader);

        when(auditReader.createQuery()).thenReturn(auditQueryCreator);
        when(auditQueryCreator.forRevisionsOfEntity(EntityJpaEntity.class, false, true))
            .thenReturn(auditQuery);
        when(auditQuery.add(any())).thenReturn(auditQuery);
        when(auditQuery.addOrder(any())).thenReturn(auditQuery);
        when(auditQuery.getResultList()).thenReturn(List.of());

        // When & Then
        assertThatThrownBy(
            () -> adapter.getEntityAuditHistory(templateIdentifier, entityIdentifier))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Entity not found with template identifier " + templateIdentifier
                    + " and entity identifier '" + entityIdentifier + "'");
      }
    }

    @Test
    @DisplayName("Should retrieve multiple revision entries sorted by revision number descending")
    void shouldRetrieveMultipleRevisionsSorted() {
      // Given
      UUID entityId = UUID.randomUUID();
      String templateIdentifier = "web-service";
      String entityIdentifier = "web-api";

      EntityJpaEntity jpaEntity = mock(EntityJpaEntity.class);
      when(jpaEntity.getId()).thenReturn(entityId);

      // Create multiple revisions
      CustomRevisionEntity rev1 = mock(CustomRevisionEntity.class);
      when(rev1.getRev()).thenReturn(3L);
      when(rev1.getRevisionTimestamp()).thenReturn(System.currentTimeMillis());
      when(rev1.getAuthId()).thenReturn("user1");

      CustomRevisionEntity rev2 = mock(CustomRevisionEntity.class);
      when(rev2.getRev()).thenReturn(2L);
      when(rev2.getRevisionTimestamp()).thenReturn(System.currentTimeMillis());
      when(rev2.getAuthId()).thenReturn("user2");

      CustomRevisionEntity rev3 = mock(CustomRevisionEntity.class);
      when(rev3.getRev()).thenReturn(1L);
      when(rev3.getRevisionTimestamp()).thenReturn(System.currentTimeMillis());
      when(rev3.getAuthId()).thenReturn("user3");

      Object[] revision1 = {jpaEntity, rev1, RevisionType.MOD};
      Object[] revision2 = {jpaEntity, rev2, RevisionType.MOD};
      Object[] revision3 = {jpaEntity, rev3, RevisionType.ADD};

      when(jpaEntityRepository.findByTemplateIdentifierAndIdentifier(templateIdentifier,
          entityIdentifier)).thenReturn(Optional.of(jpaEntity));

      try (
          MockedStatic<org.hibernate.envers.AuditReaderFactory> auditReaderFactoryMock = org.mockito.Mockito
              .mockStatic(org.hibernate.envers.AuditReaderFactory.class)) {
        auditReaderFactoryMock
            .when(() -> org.hibernate.envers.AuditReaderFactory.get(entityManager))
            .thenReturn(auditReader);

        when(auditReader.createQuery()).thenReturn(auditQueryCreator);
        when(auditQueryCreator.forRevisionsOfEntity(EntityJpaEntity.class, false, true))
            .thenReturn(auditQuery);
        when(auditQuery.add(any())).thenReturn(auditQuery);
        when(auditQuery.addOrder(any())).thenReturn(auditQuery);
        when(auditQuery.getResultList())
            .thenReturn(List.<Object[]>of(revision1, revision2, revision3));

        EntityJpaEntity historicalEntity = mock(EntityJpaEntity.class);
        when(historicalEntity.getId()).thenReturn(entityId);
        when(historicalEntity.getTemplateIdentifier()).thenReturn(templateIdentifier);
        when(historicalEntity.getName()).thenReturn("Web API");
        when(historicalEntity.getIdentifier()).thenReturn(entityIdentifier);
        when(historicalEntity.getProperties()).thenReturn(List.of());
        when(historicalEntity.getRelations()).thenReturn(List.of());

        when(auditReader.find(EntityJpaEntity.class, entityId, 3L)).thenReturn(historicalEntity);
        when(auditReader.find(EntityJpaEntity.class, entityId, 2L)).thenReturn(historicalEntity);
        when(auditReader.find(EntityJpaEntity.class, entityId, 1L)).thenReturn(historicalEntity);

        // When
        List<EntityAuditInfo> result = adapter.getEntityAuditHistory(templateIdentifier,
            entityIdentifier);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.getFirst().revisionNumber()).isEqualTo(3L);
        assertThat(result.get(1).revisionNumber()).isEqualTo(2L);
        assertThat(result.get(2).revisionNumber()).isEqualTo(1L);
        assertThat(result.get(2).revisionType()).isEqualTo("CREATED");
      }
    }
  }

  @Nested
  @DisplayName("Revision Type Mapping Tests")
  class RevisionTypeMappingTests {

    @Test
    @DisplayName("Should map RevisionType.ADD to CREATED")
    void shouldMapAddToCreated() {
      // Given
      UUID entityId = UUID.randomUUID();
      String templateIdentifier = "web-service";
      String entityIdentifier = "web-api";

      EntityJpaEntity jpaEntity = mock(EntityJpaEntity.class);
      when(jpaEntity.getId()).thenReturn(entityId);

      CustomRevisionEntity revisionEntity = mock(CustomRevisionEntity.class);
      when(revisionEntity.getRev()).thenReturn(1L);
      when(revisionEntity.getRevisionTimestamp()).thenReturn(System.currentTimeMillis());
      when(revisionEntity.getAuthId()).thenReturn("test-user");

      Object[] revision = {jpaEntity, revisionEntity, RevisionType.ADD};

      when(jpaEntityRepository.findByTemplateIdentifierAndIdentifier(templateIdentifier,
          entityIdentifier)).thenReturn(Optional.of(jpaEntity));

      try (
          MockedStatic<org.hibernate.envers.AuditReaderFactory> auditReaderFactoryMock = org.mockito.Mockito
              .mockStatic(org.hibernate.envers.AuditReaderFactory.class)) {
        auditReaderFactoryMock
            .when(() -> org.hibernate.envers.AuditReaderFactory.get(entityManager))
            .thenReturn(auditReader);

        when(auditReader.createQuery()).thenReturn(auditQueryCreator);
        when(auditQueryCreator.forRevisionsOfEntity(EntityJpaEntity.class, false, true))
            .thenReturn(auditQuery);
        when(auditQuery.add(any())).thenReturn(auditQuery);
        when(auditQuery.addOrder(any())).thenReturn(auditQuery);
        when(auditQuery.getResultList()).thenReturn(List.<Object[]>of(revision));

        EntityJpaEntity historicalEntity = mock(EntityJpaEntity.class);
        when(historicalEntity.getId()).thenReturn(entityId);
        when(historicalEntity.getTemplateIdentifier()).thenReturn(templateIdentifier);
        when(historicalEntity.getName()).thenReturn("Web API");
        when(historicalEntity.getIdentifier()).thenReturn(entityIdentifier);
        when(historicalEntity.getProperties()).thenReturn(List.of());
        when(historicalEntity.getRelations()).thenReturn(List.of());

        when(auditReader.find(EntityJpaEntity.class, entityId, 1L)).thenReturn(historicalEntity);

        // When
        List<EntityAuditInfo> result = adapter.getEntityAuditHistory(templateIdentifier,
            entityIdentifier);

        // Then
        assertThat(result.getFirst().revisionType()).isEqualTo("CREATED");
      }
    }

    @Test
    @DisplayName("Should map RevisionType.MOD to UPDATED")
    void shouldMapModToUpdated() {
      // Given
      UUID entityId = UUID.randomUUID();
      String templateIdentifier = "web-service";
      String entityIdentifier = "web-api";

      EntityJpaEntity jpaEntity = mock(EntityJpaEntity.class);
      when(jpaEntity.getId()).thenReturn(entityId);

      CustomRevisionEntity revisionEntity = mock(CustomRevisionEntity.class);
      when(revisionEntity.getRev()).thenReturn(2L);
      when(revisionEntity.getRevisionTimestamp()).thenReturn(System.currentTimeMillis());
      when(revisionEntity.getAuthId()).thenReturn("test-user");

      Object[] revision = {jpaEntity, revisionEntity, RevisionType.MOD};

      when(jpaEntityRepository.findByTemplateIdentifierAndIdentifier(templateIdentifier,
          entityIdentifier)).thenReturn(Optional.of(jpaEntity));

      try (
          MockedStatic<org.hibernate.envers.AuditReaderFactory> auditReaderFactoryMock = org.mockito.Mockito
              .mockStatic(org.hibernate.envers.AuditReaderFactory.class)) {
        auditReaderFactoryMock
            .when(() -> org.hibernate.envers.AuditReaderFactory.get(entityManager))
            .thenReturn(auditReader);

        when(auditReader.createQuery()).thenReturn(auditQueryCreator);
        when(auditQueryCreator.forRevisionsOfEntity(EntityJpaEntity.class, false, true))
            .thenReturn(auditQuery);
        when(auditQuery.add(any())).thenReturn(auditQuery);
        when(auditQuery.addOrder(any())).thenReturn(auditQuery);
        when(auditQuery.getResultList()).thenReturn(List.<Object[]>of(revision));

        EntityJpaEntity historicalEntity = mock(EntityJpaEntity.class);
        when(historicalEntity.getId()).thenReturn(entityId);
        when(historicalEntity.getTemplateIdentifier()).thenReturn(templateIdentifier);
        when(historicalEntity.getName()).thenReturn("Web API");
        when(historicalEntity.getIdentifier()).thenReturn(entityIdentifier);
        when(historicalEntity.getProperties()).thenReturn(List.of());
        when(historicalEntity.getRelations()).thenReturn(List.of());

        when(auditReader.find(EntityJpaEntity.class, entityId, 2L)).thenReturn(historicalEntity);

        // When
        List<EntityAuditInfo> result = adapter.getEntityAuditHistory(templateIdentifier,
            entityIdentifier);

        // Then
        assertThat(result.getFirst().revisionType()).isEqualTo("UPDATED");
      }
    }

    @Test
    @DisplayName("Should map RevisionType.DEL to DELETED and query previous revision for snapshot")
    void shouldMapDelToDeletedAndQueryPreviousRevision() {
      // Given
      UUID entityId = UUID.randomUUID();
      String templateIdentifier = "web-service";
      String entityIdentifier = "web-api";

      EntityJpaEntity jpaEntity = mock(EntityJpaEntity.class);
      when(jpaEntity.getId()).thenReturn(entityId);

      CustomRevisionEntity revisionEntity = mock(CustomRevisionEntity.class);
      when(revisionEntity.getRev()).thenReturn(3L);
      when(revisionEntity.getRevisionTimestamp()).thenReturn(System.currentTimeMillis());
      when(revisionEntity.getAuthId()).thenReturn("admin");

      Object[] revision = {jpaEntity, revisionEntity, RevisionType.DEL};

      when(jpaEntityRepository.findByTemplateIdentifierAndIdentifier(templateIdentifier,
          entityIdentifier)).thenReturn(Optional.of(jpaEntity));

      try (
          MockedStatic<org.hibernate.envers.AuditReaderFactory> auditReaderFactoryMock = org.mockito.Mockito
              .mockStatic(org.hibernate.envers.AuditReaderFactory.class)) {
        auditReaderFactoryMock
            .when(() -> org.hibernate.envers.AuditReaderFactory.get(entityManager))
            .thenReturn(auditReader);

        when(auditReader.createQuery()).thenReturn(auditQueryCreator);
        when(auditQueryCreator.forRevisionsOfEntity(EntityJpaEntity.class, false, true))
            .thenReturn(auditQuery);
        when(auditQuery.add(any())).thenReturn(auditQuery);
        when(auditQuery.addOrder(any())).thenReturn(auditQuery);
        when(auditQuery.getResultList()).thenReturn(List.<Object[]>of(revision));

        EntityJpaEntity historicalEntity = mock(EntityJpaEntity.class);
        when(historicalEntity.getId()).thenReturn(entityId);
        when(historicalEntity.getTemplateIdentifier()).thenReturn(templateIdentifier);
        when(historicalEntity.getName()).thenReturn("Web API");
        when(historicalEntity.getIdentifier()).thenReturn(entityIdentifier);
        when(historicalEntity.getProperties()).thenReturn(List.of());
        when(historicalEntity.getRelations()).thenReturn(List.of());

        // For DEL, should query revision 2 (3-1)
        when(auditReader.find(EntityJpaEntity.class, entityId, 2L)).thenReturn(historicalEntity);

        // When
        List<EntityAuditInfo> result = adapter.getEntityAuditHistory(templateIdentifier,
            entityIdentifier);

        // Then
        assertThat(result.getFirst().revisionType()).isEqualTo("DELETED");
        assertThat(result.getFirst().snapshot()).isNotNull();
        verify(auditReader).find(EntityJpaEntity.class, entityId, 2L);
      }
    }
  }

  @Nested
  @DisplayName("Snapshot Mapping Tests")
  class SnapshotMappingTests {

    @Test
    @DisplayName("Should include properties and relations in snapshot")
    void shouldIncludePropertiesAndRelationsInSnapshot() {
      // Given
      UUID entityId = UUID.randomUUID();
      String templateIdentifier = "web-service";
      String entityIdentifier = "web-api";

      EntityJpaEntity jpaEntity = mock(EntityJpaEntity.class);
      when(jpaEntity.getId()).thenReturn(entityId);

      CustomRevisionEntity revisionEntity = mock(CustomRevisionEntity.class);
      when(revisionEntity.getRev()).thenReturn(1L);
      when(revisionEntity.getRevisionTimestamp()).thenReturn(System.currentTimeMillis());
      when(revisionEntity.getAuthId()).thenReturn("test-user");

      Object[] revision = {jpaEntity, revisionEntity, RevisionType.ADD};

      when(jpaEntityRepository.findByTemplateIdentifierAndIdentifier(templateIdentifier,
          entityIdentifier)).thenReturn(Optional.of(jpaEntity));

      try (
          MockedStatic<org.hibernate.envers.AuditReaderFactory> auditReaderFactoryMock = org.mockito.Mockito
              .mockStatic(org.hibernate.envers.AuditReaderFactory.class)) {
        auditReaderFactoryMock
            .when(() -> org.hibernate.envers.AuditReaderFactory.get(entityManager))
            .thenReturn(auditReader);

        when(auditReader.createQuery()).thenReturn(auditQueryCreator);
        when(auditQueryCreator.forRevisionsOfEntity(EntityJpaEntity.class, false, true))
            .thenReturn(auditQuery);
        when(auditQuery.add(any())).thenReturn(auditQuery);
        when(auditQuery.addOrder(any())).thenReturn(auditQuery);
        when(auditQuery.getResultList()).thenReturn(List.<Object[]>of(revision));

        PropertyJpaEntity property = mock(PropertyJpaEntity.class);
        when(property.getId()).thenReturn(UUID.randomUUID());
        when(property.getName()).thenReturn("environment");
        when(property.getValue()).thenReturn("PROD");

        RelationTargetJpaEntity target1 = mock(RelationTargetJpaEntity.class);
        when(target1.getTargetEntityIdentifier()).thenReturn("svc-1");
        RelationTargetJpaEntity target2 = mock(RelationTargetJpaEntity.class);
        when(target2.getTargetEntityIdentifier()).thenReturn("svc-2");

        RelationJpaEntity relation = mock(RelationJpaEntity.class);
        when(relation.getId()).thenReturn(UUID.randomUUID());
        when(relation.getName()).thenReturn("dependencies");
        when(relation.getTargetTemplateIdentifier()).thenReturn("service");
        when(relation.getTargetEntities()).thenReturn(List.of(target1, target2));

        EntityJpaEntity historicalEntity = mock(EntityJpaEntity.class);
        when(historicalEntity.getId()).thenReturn(entityId);
        when(historicalEntity.getTemplateIdentifier()).thenReturn(templateIdentifier);
        when(historicalEntity.getName()).thenReturn("Web API");
        when(historicalEntity.getIdentifier()).thenReturn(entityIdentifier);
        when(historicalEntity.getProperties()).thenReturn(List.of(property));
        when(historicalEntity.getRelations()).thenReturn(List.of(relation));

        when(auditReader.find(EntityJpaEntity.class, entityId, 1L)).thenReturn(historicalEntity);

        // When
        List<EntityAuditInfo> result = adapter.getEntityAuditHistory(templateIdentifier,
            entityIdentifier);

        // Then
        assertThat(result.getFirst().snapshot()).isNotNull();
        assertThat(result.getFirst().snapshot().properties()).hasSize(1);
        assertThat(result.getFirst().snapshot().properties().getFirst().name())
            .isEqualTo("environment");
        assertThat(result.getFirst().snapshot().relations()).hasSize(1);
        assertThat(result.getFirst().snapshot().relations().getFirst().name())
            .isEqualTo("dependencies");
        assertThat(result.getFirst().snapshot().relations().getFirst().targetEntityIdentifiers())
            .containsExactly("svc-1", "svc-2");
      }
    }

    @Test
    @DisplayName("Should handle null properties and relations")
    void shouldHandleNullPropertiesAndRelations() {
      // Given
      UUID entityId = UUID.randomUUID();
      String templateIdentifier = "web-service";
      String entityIdentifier = "web-api";

      EntityJpaEntity jpaEntity = mock(EntityJpaEntity.class);
      when(jpaEntity.getId()).thenReturn(entityId);

      CustomRevisionEntity revisionEntity = mock(CustomRevisionEntity.class);
      when(revisionEntity.getRev()).thenReturn(1L);
      when(revisionEntity.getRevisionTimestamp()).thenReturn(System.currentTimeMillis());
      when(revisionEntity.getAuthId()).thenReturn("test-user");

      Object[] revision = {jpaEntity, revisionEntity, RevisionType.ADD};

      when(jpaEntityRepository.findByTemplateIdentifierAndIdentifier(templateIdentifier,
          entityIdentifier)).thenReturn(Optional.of(jpaEntity));

      try (
          MockedStatic<org.hibernate.envers.AuditReaderFactory> auditReaderFactoryMock = org.mockito.Mockito
              .mockStatic(org.hibernate.envers.AuditReaderFactory.class)) {
        auditReaderFactoryMock
            .when(() -> org.hibernate.envers.AuditReaderFactory.get(entityManager))
            .thenReturn(auditReader);

        when(auditReader.createQuery()).thenReturn(auditQueryCreator);
        when(auditQueryCreator.forRevisionsOfEntity(EntityJpaEntity.class, false, true))
            .thenReturn(auditQuery);
        when(auditQuery.add(any())).thenReturn(auditQuery);
        when(auditQuery.addOrder(any())).thenReturn(auditQuery);
        when(auditQuery.getResultList()).thenReturn(List.<Object[]>of(revision));

        EntityJpaEntity historicalEntity = mock(EntityJpaEntity.class);
        when(historicalEntity.getId()).thenReturn(entityId);
        when(historicalEntity.getTemplateIdentifier()).thenReturn(templateIdentifier);
        when(historicalEntity.getName()).thenReturn("Web API");
        when(historicalEntity.getIdentifier()).thenReturn(entityIdentifier);
        when(historicalEntity.getProperties()).thenReturn(null);
        when(historicalEntity.getRelations()).thenReturn(null);

        when(auditReader.find(EntityJpaEntity.class, entityId, 1L)).thenReturn(historicalEntity);

        // When
        List<EntityAuditInfo> result = adapter.getEntityAuditHistory(templateIdentifier,
            entityIdentifier);

        // Then
        assertThat(result.getFirst().snapshot()).isNotNull();
        assertThat(result.getFirst().snapshot().properties()).isEmpty();
        assertThat(result.getFirst().snapshot().relations()).isEmpty();
      }
    }
  }

  @Nested
  @DisplayName("Deleted Entity Audit History Tests")
  class DeletedEntityAuditHistoryTests {

    @Test
    @DisplayName("Should retrieve audit history for deleted entity from audit data")
    void shouldRetrieveAuditForDeletedEntity() {
      // Given
      UUID entityId = UUID.randomUUID();
      String templateIdentifier = "web-service";
      String entityIdentifier = "web-api-deleted";

      // Entity not in current database
      when(jpaEntityRepository.findByTemplateIdentifierAndIdentifier(templateIdentifier,
          entityIdentifier)).thenReturn(Optional.empty());

      EntityJpaEntity deletedJpaEntity = mock(EntityJpaEntity.class);
      when(deletedJpaEntity.getId()).thenReturn(entityId);
      when(deletedJpaEntity.getTemplateIdentifier()).thenReturn(templateIdentifier);
      when(deletedJpaEntity.getIdentifier()).thenReturn(entityIdentifier);

      CustomRevisionEntity revisionEntity = mock(CustomRevisionEntity.class);
      when(revisionEntity.getRev()).thenReturn(3L);
      when(revisionEntity.getRevisionTimestamp()).thenReturn(System.currentTimeMillis());
      when(revisionEntity.getAuthId()).thenReturn("admin");

      Object[] auditRevision = {deletedJpaEntity, revisionEntity, RevisionType.DEL};

      try (
          MockedStatic<org.hibernate.envers.AuditReaderFactory> auditReaderFactoryMock = org.mockito.Mockito
              .mockStatic(org.hibernate.envers.AuditReaderFactory.class)) {
        auditReaderFactoryMock
            .when(() -> org.hibernate.envers.AuditReaderFactory.get(entityManager))
            .thenReturn(auditReader);

        when(auditReader.createQuery()).thenReturn(auditQueryCreator);
        when(auditQueryCreator.forRevisionsOfEntity(EntityJpaEntity.class, false, true))
            .thenReturn(auditQuery);
        when(auditQuery.add(any())).thenReturn(auditQuery);
        when(auditQuery.addOrder(any())).thenReturn(auditQuery);
        when(auditQuery.getResultList()).thenReturn(List.<Object[]>of(auditRevision));

        EntityJpaEntity historicalEntity = mock(EntityJpaEntity.class);
        when(historicalEntity.getId()).thenReturn(entityId);
        when(historicalEntity.getTemplateIdentifier()).thenReturn(templateIdentifier);
        when(historicalEntity.getName()).thenReturn("Deleted Service");
        when(historicalEntity.getIdentifier()).thenReturn(entityIdentifier);
        when(historicalEntity.getProperties()).thenReturn(List.of());
        when(historicalEntity.getRelations()).thenReturn(List.of());

        when(auditReader.find(EntityJpaEntity.class, entityId, 2L)).thenReturn(historicalEntity);

        // When
        List<EntityAuditInfo> result = adapter.getEntityAuditHistory(templateIdentifier,
            entityIdentifier);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().revisionType()).isEqualTo("DELETED");
      }
    }
  }

  @Nested
  @DisplayName("Modified By Field Tests")
  class ModifiedByFieldTests {

    @Test
    @DisplayName("Should use authId as modifiedBy when present")
    void shouldUseAuthIdAsModifiedBy() {
      // Given
      UUID entityId = UUID.randomUUID();
      String templateIdentifier = "web-service";
      String entityIdentifier = "web-api";

      EntityJpaEntity jpaEntity = mock(EntityJpaEntity.class);
      when(jpaEntity.getId()).thenReturn(entityId);

      CustomRevisionEntity revisionEntity = mock(CustomRevisionEntity.class);
      when(revisionEntity.getRev()).thenReturn(1L);
      when(revisionEntity.getRevisionTimestamp()).thenReturn(System.currentTimeMillis());
      when(revisionEntity.getAuthId()).thenReturn("john.doe@company.com");

      Object[] revision = {jpaEntity, revisionEntity, RevisionType.ADD};

      when(jpaEntityRepository.findByTemplateIdentifierAndIdentifier(templateIdentifier,
          entityIdentifier)).thenReturn(Optional.of(jpaEntity));

      try (
          MockedStatic<org.hibernate.envers.AuditReaderFactory> auditReaderFactoryMock = org.mockito.Mockito
              .mockStatic(org.hibernate.envers.AuditReaderFactory.class)) {
        auditReaderFactoryMock
            .when(() -> org.hibernate.envers.AuditReaderFactory.get(entityManager))
            .thenReturn(auditReader);

        when(auditReader.createQuery()).thenReturn(auditQueryCreator);
        when(auditQueryCreator.forRevisionsOfEntity(EntityJpaEntity.class, false, true))
            .thenReturn(auditQuery);
        when(auditQuery.add(any())).thenReturn(auditQuery);
        when(auditQuery.addOrder(any())).thenReturn(auditQuery);
        when(auditQuery.getResultList()).thenReturn(List.<Object[]>of(revision));

        EntityJpaEntity historicalEntity = mock(EntityJpaEntity.class);
        when(historicalEntity.getId()).thenReturn(entityId);
        when(historicalEntity.getTemplateIdentifier()).thenReturn(templateIdentifier);
        when(historicalEntity.getName()).thenReturn("Web API");
        when(historicalEntity.getIdentifier()).thenReturn(entityIdentifier);
        when(historicalEntity.getProperties()).thenReturn(List.of());
        when(historicalEntity.getRelations()).thenReturn(List.of());

        when(auditReader.find(EntityJpaEntity.class, entityId, 1L)).thenReturn(historicalEntity);

        // When
        List<EntityAuditInfo> result = adapter.getEntityAuditHistory(templateIdentifier,
            entityIdentifier);

        // Then
        assertThat(result.getFirst().modifiedBy()).isEqualTo("john.doe@company.com");
      }
    }

    @Test
    @DisplayName("Should use 'system' as modifiedBy when authId is null")
    void shouldUseSystemWhenAuthIdIsNull() {
      // Given
      UUID entityId = UUID.randomUUID();
      String templateIdentifier = "web-service";
      String entityIdentifier = "web-api";

      EntityJpaEntity jpaEntity = mock(EntityJpaEntity.class);
      when(jpaEntity.getId()).thenReturn(entityId);

      CustomRevisionEntity revisionEntity = mock(CustomRevisionEntity.class);
      when(revisionEntity.getRev()).thenReturn(1L);
      when(revisionEntity.getRevisionTimestamp()).thenReturn(System.currentTimeMillis());
      when(revisionEntity.getAuthId()).thenReturn(null);

      Object[] revision = {jpaEntity, revisionEntity, RevisionType.ADD};

      when(jpaEntityRepository.findByTemplateIdentifierAndIdentifier(templateIdentifier,
          entityIdentifier)).thenReturn(Optional.of(jpaEntity));

      try (
          MockedStatic<org.hibernate.envers.AuditReaderFactory> auditReaderFactoryMock = org.mockito.Mockito
              .mockStatic(org.hibernate.envers.AuditReaderFactory.class)) {
        auditReaderFactoryMock
            .when(() -> org.hibernate.envers.AuditReaderFactory.get(entityManager))
            .thenReturn(auditReader);

        when(auditReader.createQuery()).thenReturn(auditQueryCreator);
        when(auditQueryCreator.forRevisionsOfEntity(EntityJpaEntity.class, false, true))
            .thenReturn(auditQuery);
        when(auditQuery.add(any())).thenReturn(auditQuery);
        when(auditQuery.addOrder(any())).thenReturn(auditQuery);
        when(auditQuery.getResultList()).thenReturn(List.<Object[]>of(revision));

        EntityJpaEntity historicalEntity = mock(EntityJpaEntity.class);
        when(historicalEntity.getId()).thenReturn(entityId);
        when(historicalEntity.getTemplateIdentifier()).thenReturn(templateIdentifier);
        when(historicalEntity.getName()).thenReturn("Web API");
        when(historicalEntity.getIdentifier()).thenReturn(entityIdentifier);
        when(historicalEntity.getProperties()).thenReturn(List.of());
        when(historicalEntity.getRelations()).thenReturn(List.of());

        when(auditReader.find(EntityJpaEntity.class, entityId, 1L)).thenReturn(historicalEntity);

        // When
        List<EntityAuditInfo> result = adapter.getEntityAuditHistory(templateIdentifier,
            entityIdentifier);

        // Then
        assertThat(result.getFirst().modifiedBy()).isEqualTo("system");
      }
    }
  }

  @Nested
  @DisplayName("Relation Target Entities Tests")
  class RelationTargetEntitiesTests {

    @Test
    @DisplayName("Should handle mapping changes correctly across target list modifications")
    void shouldHandleTargetEntityListMapping() {
      // Given
      UUID entityId = UUID.randomUUID();
      String templateIdentifier = "web-service";
      String entityIdentifier = "web-api";

      EntityJpaEntity jpaEntity = mock(EntityJpaEntity.class);
      when(jpaEntity.getId()).thenReturn(entityId);

      CustomRevisionEntity revisionEntity = mock(CustomRevisionEntity.class);
      when(revisionEntity.getRev()).thenReturn(1L);
      when(revisionEntity.getRevisionTimestamp()).thenReturn(System.currentTimeMillis());
      when(revisionEntity.getAuthId()).thenReturn("test-user");

      Object[] revision = {jpaEntity, revisionEntity, RevisionType.ADD};

      when(jpaEntityRepository.findByTemplateIdentifierAndIdentifier(templateIdentifier,
          entityIdentifier)).thenReturn(Optional.of(jpaEntity));

      try (
          MockedStatic<org.hibernate.envers.AuditReaderFactory> auditReaderFactoryMock = org.mockito.Mockito
              .mockStatic(org.hibernate.envers.AuditReaderFactory.class)) {
        auditReaderFactoryMock
            .when(() -> org.hibernate.envers.AuditReaderFactory.get(entityManager))
            .thenReturn(auditReader);

        when(auditReader.createQuery()).thenReturn(auditQueryCreator);
        when(auditQueryCreator.forRevisionsOfEntity(EntityJpaEntity.class, false, true))
            .thenReturn(auditQuery);
        when(auditQuery.add(any())).thenReturn(auditQuery);
        when(auditQuery.addOrder(any())).thenReturn(auditQuery);
        when(auditQuery.getResultList()).thenReturn(List.<Object[]>of(revision));

        RelationTargetJpaEntity target1 = mock(RelationTargetJpaEntity.class);
        when(target1.getTargetEntityIdentifier()).thenReturn("id1");
        RelationTargetJpaEntity target2 = mock(RelationTargetJpaEntity.class);
        when(target2.getTargetEntityIdentifier()).thenReturn("id2");

        List<RelationTargetJpaEntity> targetEntities = new java.util.ArrayList<>(
            List.of(target1, target2));
        RelationJpaEntity relation = mock(RelationJpaEntity.class);
        when(relation.getId()).thenReturn(UUID.randomUUID());
        when(relation.getName()).thenReturn("deps");
        when(relation.getTargetTemplateIdentifier()).thenReturn("service");
        when(relation.getTargetEntities()).thenReturn(targetEntities);

        EntityJpaEntity historicalEntity = mock(EntityJpaEntity.class);
        when(historicalEntity.getId()).thenReturn(entityId);
        when(historicalEntity.getTemplateIdentifier()).thenReturn(templateIdentifier);
        when(historicalEntity.getName()).thenReturn("Web API");
        when(historicalEntity.getIdentifier()).thenReturn(entityIdentifier);
        when(historicalEntity.getProperties()).thenReturn(List.of());
        when(historicalEntity.getRelations()).thenReturn(List.of(relation));

        when(auditReader.find(EntityJpaEntity.class, entityId, 1L)).thenReturn(historicalEntity);

        // When
        List<EntityAuditInfo> result = adapter.getEntityAuditHistory(templateIdentifier,
            entityIdentifier);

        // Modify original list setup
        RelationTargetJpaEntity target3 = mock(RelationTargetJpaEntity.class);
        targetEntities.add(target3);

        // Then - snapshot mapping guarantees isolation
        assertThat(result.getFirst().snapshot().relations().getFirst().targetEntityIdentifiers())
            .containsExactly("id1", "id2");
      }
    }

    @Test
    @DisplayName("Should handle null target entities gracefully")
    void shouldHandleNullTargetEntities() {
      // Given
      UUID entityId = UUID.randomUUID();
      String templateIdentifier = "web-service";
      String entityIdentifier = "web-api";

      EntityJpaEntity jpaEntity = mock(EntityJpaEntity.class);
      when(jpaEntity.getId()).thenReturn(entityId);

      CustomRevisionEntity revisionEntity = mock(CustomRevisionEntity.class);
      when(revisionEntity.getRev()).thenReturn(1L);
      when(revisionEntity.getRevisionTimestamp()).thenReturn(System.currentTimeMillis());
      when(revisionEntity.getAuthId()).thenReturn("test-user");

      Object[] revision = {jpaEntity, revisionEntity, RevisionType.ADD};

      when(jpaEntityRepository.findByTemplateIdentifierAndIdentifier(templateIdentifier,
          entityIdentifier)).thenReturn(Optional.of(jpaEntity));

      try (
          MockedStatic<org.hibernate.envers.AuditReaderFactory> auditReaderFactoryMock = org.mockito.Mockito
              .mockStatic(org.hibernate.envers.AuditReaderFactory.class)) {
        auditReaderFactoryMock
            .when(() -> org.hibernate.envers.AuditReaderFactory.get(entityManager))
            .thenReturn(auditReader);

        when(auditReader.createQuery()).thenReturn(auditQueryCreator);
        when(auditQueryCreator.forRevisionsOfEntity(EntityJpaEntity.class, false, true))
            .thenReturn(auditQuery);
        when(auditQuery.add(any())).thenReturn(auditQuery);
        when(auditQuery.addOrder(any())).thenReturn(auditQuery);
        when(auditQuery.getResultList()).thenReturn(List.<Object[]>of(revision));

        RelationJpaEntity relation = mock(RelationJpaEntity.class);
        when(relation.getId()).thenReturn(UUID.randomUUID());
        when(relation.getName()).thenReturn("deps");
        when(relation.getTargetTemplateIdentifier()).thenReturn("service");
        when(relation.getTargetEntities()).thenReturn(null);

        EntityJpaEntity historicalEntity = mock(EntityJpaEntity.class);
        when(historicalEntity.getId()).thenReturn(entityId);
        when(historicalEntity.getTemplateIdentifier()).thenReturn(templateIdentifier);
        when(historicalEntity.getName()).thenReturn("Web API");
        when(historicalEntity.getIdentifier()).thenReturn(entityIdentifier);
        when(historicalEntity.getProperties()).thenReturn(List.of());
        when(historicalEntity.getRelations()).thenReturn(List.of(relation));

        when(auditReader.find(EntityJpaEntity.class, entityId, 1L)).thenReturn(historicalEntity);

        // When
        List<EntityAuditInfo> result = adapter.getEntityAuditHistory(templateIdentifier,
            entityIdentifier);

        // Then
        assertThat(result.getFirst().snapshot().relations().getFirst().targetEntityIdentifiers())
            .isEmpty();
      }
    }
  }
}
