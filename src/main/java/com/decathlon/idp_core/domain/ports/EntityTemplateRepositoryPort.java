package com.decathlon.idp_core.domain.ports;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;

/// Driven port defining the contract for [EntityTemplate] persistence operations.
///
/// **Contract expectations for implementations:**
/// - `findByIdentifier()` enforces business uniqueness - must return empty for non-existent templates
/// - `findById()` supports UUID-based lookups for internal references
/// - `findAll()` must support pagination for template management interfaces
/// - `existsByIdentifier()` enables efficient existence checks without full object loading
/// - `save()` must persist template with all nested definitions and return saved version
/// - `deleteByIdentifier()` must cascade appropriately to related entities per business rules
///
/// **Business constraints:** Implementations must enforce template identifier uniqueness
/// and handle referential integrity with existing entities.
public interface EntityTemplateRepositoryPort {

    Optional<EntityTemplate> findByIdentifier(String templateIdentifier);

    Optional<EntityTemplate> findById(UUID id);

    Page<EntityTemplate> findAll(Pageable pageable);

    boolean existsByIdentifier(String identifier);

    EntityTemplate save(EntityTemplate entityTemplate);

    void deleteByIdentifier(String identifier);
}
