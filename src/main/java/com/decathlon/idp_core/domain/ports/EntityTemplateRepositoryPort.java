package com.decathlon.idp_core.domain.ports;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;

/**
 * Driven Port for EntityTemplate persistence operations.
 */
public interface EntityTemplateRepositoryPort {

    Optional<EntityTemplate> findByIdentifier(String templateIdentifier);

    Optional<EntityTemplate> findById(UUID id);

    Page<EntityTemplate> findAll(Pageable pageable);

    boolean existsByIdentifier(String identifier);

    EntityTemplate save(EntityTemplate entityTemplate);

    void deleteByIdentifier(String identifier);
}
