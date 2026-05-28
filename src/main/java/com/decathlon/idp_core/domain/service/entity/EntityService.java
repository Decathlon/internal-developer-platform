package com.decathlon.idp_core.domain.service.entity;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.decathlon.idp_core.domain.exception.entity.EntityAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntityFilter;
import com.decathlon.idp_core.domain.model.entity.EntitySummary;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.domain.service.EntityQueryParserService;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateService;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateValidationService;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/// Domain service orchestrating [Entity] business operations and validations.
///
/// **Business purpose:** Coordinates entity lifecycle management while enforcing
/// business rules and maintaining data consistency across the entity-template
/// domain. Serves as the primary entry point for entity operations from
/// application layer.
///
/// **Key responsibilities:**
/// - Entity retrieval with template validation
/// - Entity creation with business rule enforcement
/// - Entity data integrity validation (entity, properties, relations)
/// - Entity summary generation for efficient queries
@Service
@Validated
@RequiredArgsConstructor
public class EntityService {
    private final EntityRepositoryPort entityRepository;
    private final EntityValidationService entityValidationService;
    private final EntityTemplateValidationService entityTemplateValidationService;
    private final EntityTemplateService entityTemplateService;
    private final EntityQueryParserService entityQueryParserService;

    /// Retrieves entities filtered by template with optional query filter.
    ///
    /// **Contract:** Returns paginated entities conforming to the specified template
    /// that additionally satisfy all criteria in filter (when provided). Template
    /// existence is validated first. When filter is null or empty, the result
    /// includes all entities for the template.
    ///
    /// @param pageable           pagination configuration for large entity sets
    /// @param templateIdentifier business identifier of the entity template
    /// @param entityFilter             the parsed query filter; null or [EntityFilter#empty()] for no filtering
    /// @return paginated entities matching the template and all filter criteria
    /// @throws EntityTemplateNotFoundException when template doesn't exist
    @Transactional
    public Page<Entity> getEntitiesByTemplateIdentifier(
            Pageable pageable, String templateIdentifier, EntityFilter entityFilter) {
        EntityTemplate template = entityTemplateService.getEntityTemplateByIdentifier(templateIdentifier);
        EntityFilter filter = entityFilter != null ? entityFilter : EntityFilter.empty();
        entityQueryParserService.validateFilterPropertyTypes(filter, template);
        return entityRepository.findByTemplateIdentifierWithFilter(templateIdentifier, filter, pageable);
    }

    /// Provides lightweight entity summaries for efficient bulk operations.
    ///
    /// **Contract:** Returns summary projections without full entity data, optimized
    /// for UI lists and relationship resolution scenarios.
    ///
    /// @param identifiers business identifiers of entities to summarize
    /// @return lightweight entity summaries for the specified identifiers
    public List<EntitySummary> getEntitiesSummariesByIdentifiers(List<String> identifiers) {
        return entityRepository.findByIdentifierIn(identifiers);
    }

    /// Retrieves a specific entity with template and entity validation.
    ///
    /// **Contract:** Returns the entity identified by both template and entity
    /// identifiers. Validates template existence first, then entity existence,
    /// ensuring referential integrity.
    ///
    /// @param templateIdentifier business identifier of the entity template
    /// @param entityIdentifier   unique business identifier of the entity within
    ///                           template
    /// @return the entity matching both identifiers
    /// @throws EntityTemplateNotFoundException when template doesn't exist
    /// @throws EntityNotFoundException         when entity doesn't exist
    @Transactional
    public Entity getEntityByTemplateIdentifierAndIdentifier(String templateIdentifier, String entityIdentifier) {
        entityTemplateValidationService.validateTemplateExists(templateIdentifier);
        return entityRepository.findByTemplateIdentifierAndIdentifier(templateIdentifier, entityIdentifier)
                .orElseThrow(() -> new EntityNotFoundException(templateIdentifier,
                        entityIdentifier));
    }

    /// Creates and persists a new entity with business validation.
    ///
    /// **Contract:** Resolves the referenced template (single round-trip — combined
    /// existence check and fetch), enforces entity identifier uniqueness within the
    /// template scope, then validates entity/property data integrity against the
    /// resolved template before persisting.
    ///
    /// @param entity validated entity to create and persist
    /// @return the persisted entity with generated identifiers
    /// @throws EntityTemplateNotFoundException when the referenced template doesn't
    ///                                         exist
    /// @throws EntityAlreadyExistsException    when an entity with the same
    ///                                         identifier already exists for this
    ///                                         template
    /// @throws EntityValidationException       when entity, property, or relation
    ///                                         data is invalid
    @Transactional
    public Entity createEntity(@Valid Entity entity) {
        EntityTemplate template = entityTemplateService.getEntityTemplateByIdentifier(entity.templateIdentifier());
        entityValidationService.validateForCreation(entity, template);
        return entityRepository.save(entity);
    }

    /// Updates an existing entity identified by template and entity identifiers.
    ///
    /// **Contract:** Validates template existence, then entity existence within the
    /// template scope. Validates updated entity data against the template constraints
    /// before persisting changes.
    ///
    /// @param templateIdentifier template identifier from the request path
    /// @param entityIdentifier entity identifier from the request path
    /// @param entity validated entity payload
    /// @return persisted updated entity
    /// @throws EntityTemplateNotFoundException when template doesn't exist
    /// @throws EntityNotFoundException when target entity doesn't exist
    /// @throws EntityValidationException when payload violates template constraints
    @Transactional
    public Entity updateEntity(String templateIdentifier, String entityIdentifier, @Valid Entity entity) {
        EntityTemplate template = entityTemplateService.getEntityTemplateByIdentifier(templateIdentifier);
        Entity existingEntity = entityRepository.findByTemplateIdentifierAndIdentifier(templateIdentifier, entityIdentifier)
                .orElseThrow(() -> new EntityNotFoundException(templateIdentifier, entityIdentifier));

        Entity entityToSave = new Entity(
                existingEntity.id(),
                templateIdentifier,
                entity.name(),
                entityIdentifier,
                entity.properties(),
                entity.relations());

        entityValidationService.validateForUpdate(entityToSave, template);
        return entityRepository.save(entityToSave);
    }


}
