package com.decathlon.idp_core.infrastructure.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.EntityPersistenceMapper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.EntityJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaEntityRepository;

/// Unit tests for PostgresEntityAdapter.
/// Covers entity persistence operations.
@DisplayName("PostgresEntityAdapter Tests")
class PostgresEntityAdapterTest {

  @Mock
  private JpaEntityRepository jpaEntityRepository;

  @Mock
  private EntityPersistenceMapper mapper;

  @InjectMocks
  private PostgresEntityAdapter adapter;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Nested
  @DisplayName("findAllByTemplateIdentifierAndIdentifierIn Tests")
  class FindAllByTemplateIdentifierAndIdentifierInTests {

    @Test
    @DisplayName("Should retrieve entities for matching identifiers")
    void shouldRetrieveEntitiesForMatchingIdentifiers() {
      // Given
      String templateIdentifier = "web-service";
      List<String> identifiers = List.of("web-api", "mobile-api", "backend-api");

      EntityJpaEntity jpaEntity1 = mock(EntityJpaEntity.class);
      EntityJpaEntity jpaEntity2 = mock(EntityJpaEntity.class);
      EntityJpaEntity jpaEntity3 = mock(EntityJpaEntity.class);
      List<EntityJpaEntity> jpaEntities = List.of(jpaEntity1, jpaEntity2, jpaEntity3);

      Entity entity1 = mock(Entity.class);
      Entity entity2 = mock(Entity.class);
      Entity entity3 = mock(Entity.class);

      when(jpaEntityRepository.findAllByTemplateIdentifierAndIdentifierIn(templateIdentifier,
          identifiers)).thenReturn(jpaEntities);
      when(mapper.toDomain(jpaEntity1)).thenReturn(entity1);
      when(mapper.toDomain(jpaEntity2)).thenReturn(entity2);
      when(mapper.toDomain(jpaEntity3)).thenReturn(entity3);

      // When
      List<Entity> result = adapter.findAllByTemplateIdentifierAndIdentifierIn(templateIdentifier,
          identifiers);

      // Then
      assertThat(result).containsExactly(entity1, entity2, entity3);
      verify(jpaEntityRepository).findAllByTemplateIdentifierAndIdentifierIn(templateIdentifier,
          identifiers);
      verify(mapper).toDomain(jpaEntity1);
      verify(mapper).toDomain(jpaEntity2);
      verify(mapper).toDomain(jpaEntity3);
    }

    @Test
    @DisplayName("Should return empty list when no entities found")
    void shouldReturnEmptyListWhenNoEntitiesFound() {
      // Given
      String templateIdentifier = "non-existent";
      List<String> identifiers = List.of("non-existent-1", "non-existent-2");

      when(jpaEntityRepository.findAllByTemplateIdentifierAndIdentifierIn(templateIdentifier,
          identifiers)).thenReturn(Collections.emptyList());

      // When
      List<Entity> result = adapter.findAllByTemplateIdentifierAndIdentifierIn(templateIdentifier,
          identifiers);

      // Then
      assertThat(result).isEmpty();
      verify(jpaEntityRepository).findAllByTemplateIdentifierAndIdentifierIn(templateIdentifier,
          identifiers);
    }

    @Test
    @DisplayName("Should handle single identifier")
    void shouldHandleSingleIdentifier() {
      // Given
      String templateIdentifier = "service";
      List<String> identifiers = List.of("single-api");

      EntityJpaEntity jpaEntity = mock(EntityJpaEntity.class);
      Entity entity = mock(Entity.class);

      when(jpaEntityRepository.findAllByTemplateIdentifierAndIdentifierIn(templateIdentifier,
          identifiers)).thenReturn(List.of(jpaEntity));
      when(mapper.toDomain(jpaEntity)).thenReturn(entity);

      // When
      List<Entity> result = adapter.findAllByTemplateIdentifierAndIdentifierIn(templateIdentifier,
          identifiers);

      // Then
      assertThat(result).containsExactly(entity);
      verify(jpaEntityRepository).findAllByTemplateIdentifierAndIdentifierIn(templateIdentifier,
          identifiers);
    }

    @Test
    @DisplayName("Should handle empty identifier list")
    void shouldHandleEmptyIdentifierList() {
      // Given
      String templateIdentifier = "service";
      List<String> identifiers = Collections.emptyList();

      when(jpaEntityRepository.findAllByTemplateIdentifierAndIdentifierIn(templateIdentifier,
          identifiers)).thenReturn(Collections.emptyList());

      // When
      List<Entity> result = adapter.findAllByTemplateIdentifierAndIdentifierIn(templateIdentifier,
          identifiers);

      // Then
      assertThat(result).isEmpty();
      verify(jpaEntityRepository).findAllByTemplateIdentifierAndIdentifierIn(templateIdentifier,
          identifiers);
    }

    @Test
    @DisplayName("Should return entities in order from repository")
    void shouldReturnEntitiesInOrderFromRepository() {
      // Given
      String templateIdentifier = "web-service";
      List<String> identifiers = List.of("api-a", "api-b", "api-c");

      EntityJpaEntity jpaEntity1 = mock(EntityJpaEntity.class);
      EntityJpaEntity jpaEntity2 = mock(EntityJpaEntity.class);
      EntityJpaEntity jpaEntity3 = mock(EntityJpaEntity.class);

      Entity entity1 = mock(Entity.class);
      Entity entity2 = mock(Entity.class);
      Entity entity3 = mock(Entity.class);

      when(jpaEntityRepository.findAllByTemplateIdentifierAndIdentifierIn(templateIdentifier,
          identifiers)).thenReturn(List.of(jpaEntity1, jpaEntity2, jpaEntity3));
      when(mapper.toDomain(jpaEntity1)).thenReturn(entity1);
      when(mapper.toDomain(jpaEntity2)).thenReturn(entity2);
      when(mapper.toDomain(jpaEntity3)).thenReturn(entity3);

      // When
      List<Entity> result = adapter.findAllByTemplateIdentifierAndIdentifierIn(templateIdentifier,
          identifiers);

      // Then
      assertThat(result).containsExactly(entity1, entity2, entity3);
    }
  }
}
