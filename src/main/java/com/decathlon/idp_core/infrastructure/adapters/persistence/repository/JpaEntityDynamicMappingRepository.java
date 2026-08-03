package com.decathlon.idp_core.infrastructure.adapters.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_dynamic_mapping.EntityDynamicMappingJpaEntity;

/// JPA repository for EntityDynamicMapping persistence.
///
/// Manages the `entity_dynamic_mapping` table which stores mapping configurations
/// (JSLT filters, property/relation mappings) used by webhook template mappings.
///
/// Read methods that are mapped back to the domain model eagerly load the
/// `template` association (via [EntityGraph] or an explicit `join fetch`). The
/// persistence mapper navigates `template.identifier`, so leaving the LAZY proxy
/// uninitialized would raise a `LazyInitializationException` once the mapping
/// happens outside the session.
@Repository
public interface JpaEntityDynamicMappingRepository
    extends
      JpaRepository<EntityDynamicMappingJpaEntity, UUID> {

  /// Filters on the associated template business identifier. An explicit query
  /// with `join fetch` is used because a derived query traversing
  /// `template.identifier` does not bind reliably under Hibernate.
  @Query("""
      select edm from EntityDynamicMappingJpaEntity edm
      join fetch edm.template t
      where t.identifier = :identifier
      """)
  List<EntityDynamicMappingJpaEntity> findByTemplateIdentifier(
      @Param("identifier") String identifier);

  List<EntityDynamicMappingJpaEntity> findByTemplateId(UUID templateId);

  @Query("""
      select count(edm) > 0 from EntityDynamicMappingJpaEntity edm
      where edm.template.identifier = :identifier
      """)
  boolean existsByTemplateIdentifier(@Param("identifier") String identifier);

  boolean existsByIdentifier(String identifier);

  @EntityGraph(attributePaths = "template")
  Optional<EntityDynamicMappingJpaEntity> findByIdentifier(String identifier);

  @Override
  @NonNull
  @EntityGraph(attributePaths = "template")
  Page<EntityDynamicMappingJpaEntity> findAll(@NonNull Pageable pageable);

  @Override
  @NonNull
  @EntityGraph(attributePaths = "template")
  List<EntityDynamicMappingJpaEntity> findAllById(@NonNull Iterable<UUID> ids);

  void deleteByIdentifier(String identifier);

}
