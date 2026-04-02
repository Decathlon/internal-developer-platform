package com.decathlon.idp_core.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntitySummary;

/**
 * Repository interface for accessing and managing {@link Entity} domain objects.
 * <p>
 * Provides methods for querying entities by identifier, relation, and template, as well as paginated access and projections for summary data.
 * </p>
 */
@Repository
public interface EntityRepository extends JpaRepository<Entity, UUID> {

    /**
     * Finds entity summaries for the given list of entity identifiers.
     *
     * @param identifiers the list of entity identifiers
     * @return a list of {@link EntitySummary} projections
     */
    @Query("SELECT e.identifier AS identifier, e.name AS name, e.templateIdentifier AS templateIdentifier FROM Entity e WHERE e.identifier IN :identifiers")
    List<EntitySummary> findByIdentifierIn(List<String> identifiers);

    /**
     * Finds entity summaries for entities that have relations with the given relation IDs.
     *
     * @param relationIds the list of relation UUIDs
     * @return a list of {@link EntitySummary} projections
     */
    @Query("SELECT e.identifier AS identifier, e.name AS name, e.templateIdentifier AS templateIdentifier " +
       "FROM Entity e JOIN e.relations r WHERE r.id IN :relationIds")
    List<EntitySummary> findByRelationIdIn(List<UUID> relationIds);

    /**
     * Finds an entity by its unique identifier.
     *
     * @param identifier the entity identifier
     * @return an {@link Optional} containing the entity if found, or empty otherwise
     */
    Optional<Entity> findByTemplateIdentifierAndIdentifier(String templateIdentifier, String identifier);

    /**
     * Finds entities by their template identifier in a paginated fashion.
     *
     * @param templateIdentifier the template identifier
     * @param pageable the pagination information
     * @return a page of entities matching the template identifier
     */
    Page<Entity> findByTemplateIdentifier(String templateIdentifier, Pageable pageable);

}
