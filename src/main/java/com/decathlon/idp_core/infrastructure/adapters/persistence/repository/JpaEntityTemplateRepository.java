package com.decathlon.idp_core.infrastructure.adapters.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_template.EntityTemplateJpaEntity;

@Repository
public interface JpaEntityTemplateRepository extends JpaRepository<EntityTemplateJpaEntity, UUID> {

    @EntityGraph(attributePaths = {"propertiesDefinitions", "propertiesDefinitions.rules", "relationsDefinitions"})
    Optional<EntityTemplateJpaEntity> findByIdentifier(String templateIdentifier);

    @Override
    @EntityGraph(attributePaths = {"propertiesDefinitions", "propertiesDefinitions.rules", "relationsDefinitions"})
    Optional<EntityTemplateJpaEntity> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = {"propertiesDefinitions", "propertiesDefinitions.rules", "relationsDefinitions"})
    Page<EntityTemplateJpaEntity> findAll(Pageable pageable);


    boolean existsByIdentifier(String identifier);

    boolean existsByName(String name);

    @Transactional
    void deleteByIdentifier(String identifier);
}
