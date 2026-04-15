package com.decathlon.idp_core.domain.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntitySummary;

/**
 * Driven Port for Entity persistence operations.
 */
public interface EntityRepositoryPort {

    Entity save(Entity entity);

    Optional<Entity> findById(UUID id);

    Optional<Entity> findByTemplateIdentifierAndIdentifier(String templateIdentifier, String identifier);

    Page<Entity> findByTemplateIdentifier(String templateIdentifier, Pageable pageable);

    List<EntitySummary> findByIdentifierIn(List<String> identifiers);

    List<EntitySummary> findByRelationIdIn(List<UUID> relationIds);
}
