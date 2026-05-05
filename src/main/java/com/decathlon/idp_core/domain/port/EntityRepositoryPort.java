package com.decathlon.idp_core.domain.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntitySummary;

/// Driven port defining the contract for [Entity] persistence operations.
///
/// **Contract expectations for implementations:**
/// - `save()` must persist the entity and return the saved version with any generated IDs
/// - `findById()` must return empty Optional for non-existent entities
/// - `findByTemplateIdentifierAndIdentifier()` enforces business uniqueness constraints
/// - `findByTemplateIdentifier()` must support pagination for large entity sets
/// - `findByIdentifierIn()` optimizes bulk entity lookups for relationship resolution
/// - `findByRelationIdIn()` enables reverse relationship navigation
///
/// **Transaction behavior:** Implementations should handle transaction boundaries
/// appropriately for the underlying persistence technology.
public interface EntityRepositoryPort {

    Entity save(Entity entity);

    Optional<Entity> findById(UUID id);

    Optional<Entity> findByTemplateIdentifierAndIdentifier(String templateIdentifier, String identifier);

    Optional<Entity> findByTemplateIdentifierAndName(String templateIdentifier, String entityName);

    Optional<Page<Entity>> findByTemplateIdentifier(String templateIdentifier, Pageable pageable);

    List<EntitySummary> findByIdentifierIn(List<String> identifiers);

    List<EntitySummary> findByRelationIdIn(List<UUID> relationIds);
}
