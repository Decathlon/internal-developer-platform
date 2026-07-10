package com.decathlon.idp_core.infrastructure.adapters.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_template.EntityTemplateJpaEntity;

@Repository
public interface JpaEntityTemplateRepository extends JpaRepository<EntityTemplateJpaEntity, UUID> {

  @EntityGraph(attributePaths = {"propertiesDefinitions", "propertiesDefinitions.rules",
      "relationsDefinitions"})
  Optional<EntityTemplateJpaEntity> findByIdentifier(String templateIdentifier);

  @Override
  @EntityGraph(attributePaths = {"propertiesDefinitions", "propertiesDefinitions.rules",
      "relationsDefinitions"})
  Optional<EntityTemplateJpaEntity> findById(UUID id);

  @Override
  @EntityGraph(attributePaths = {"propertiesDefinitions", "propertiesDefinitions.rules",
      "relationsDefinitions"})
  Page<EntityTemplateJpaEntity> findAll(Pageable pageable);

  boolean existsByIdentifier(String identifier);

  boolean existsByName(String name);

  @Transactional
  void deleteByIdentifier(String identifier);

  /// Counts templates (excluding the one identified by `identifier` itself) that have
  /// at least one relation definition whose `targetTemplateIdentifier` equals `identifier`.
  ///
  /// Used to enforce the template relation-target integrity business rule before deletion.
  @Query("""
      SELECT COUNT(et)
      FROM EntityTemplateJpaEntity et
      JOIN et.relationsDefinitions rd
      WHERE rd.targetTemplateIdentifier = :identifier
        AND et.identifier <> :identifier
      """)
  long countRelationTargetingTemplate(@Param("identifier") String identifier);
}
