package com.decathlon.idp_core.domain.service.entity;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.decathlon.idp_core.domain.exception.entity.EntityAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntitySummary;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.domain.port.EntityTemplateRepositoryPort;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateValidationService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

/// Domain service orchestrating [Entity] business operations and validations.
///
/// **Business purpose:** Coordinates entity lifecycle management while enforcing
/// business rules and maintaining data consistency across the entity-template domain.
/// Serves as the primary entry point for entity operations from application layer.
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
    private final EntityTemplateRepositoryPort entityTemplateRepository;
    private final EntityValidationService entityValidationService;
    private final EntityTemplateValidationService entityTemplateValidationService;

    /// Retrieves entities filtered by template with existence validation.
    ///
    /// **Contract:** Returns paginated entities that conform to the specified template.
    /// Template existence is validated first to ensure meaningful results.
    ///
    /// @param pageable           pagination configuration for large entity sets
    /// @param templateIdentifier business identifier of the entity template
    /// @return paginated entities matching the template
    /// @throws EntityTemplateNotFoundException when template doesn't exist
    @Transactional
    public Page<Entity> getEntitiesByTemplateIdentifier(Pageable pageable, String templateIdentifier) {

        return entityRepository.findByTemplateIdentifier(templateIdentifier, pageable)
                .orElseThrow(() -> new EntityTemplateNotFoundException(templateIdentifier));
    }

    /// Provides lightweight entity summaries for efficient bulk operations.
    ///
    /// **Contract:** Returns summary projections without full entity data,
    /// optimized for UI lists and relationship resolution scenarios.
    ///
    /// @param identifiers business identifiers of entities to summarize
    /// @return lightweight entity summaries for the specified identifiers
    public List<EntitySummary> getEntitiesSummariesByIndentifiers(List<String> identifiers) {
        return entityRepository.findByIdentifierIn(identifiers);
    }

    /// Retrieves a specific entity with template and entity validation.
    ///
    /// **Contract:** Returns the entity identified by both template and entity identifiers.
    /// Validates template existence first, then entity existence, ensuring referential integrity.
    ///
    /// @param templateIdentifier business identifier of the entity template
    /// @param entityIdentifier   unique business identifier of the entity within template
    /// @return the entity matching both identifiers
    /// @throws EntityTemplateNotFoundException when template doesn't exist
    /// @throws EntityNotFoundException         when entity doesn't exist
    @Transactional
    public Entity getEntityByTemplateIdentifierAndIdentifier(String templateIdentifier, String entityIdentifier) {
        entityTemplateValidationService.checkTemplateExists(templateIdentifier);
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
    /// @throws EntityTemplateNotFoundException when the referenced template doesn't exist
    /// @throws EntityAlreadyExistsException    when an entity with the same identifier already exists for this template
    /// @throws EntityValidationException       when entity, property, or relation data is invalid
    @Transactional
    public Entity createEntity(@Valid Entity entity) {
        EntityTemplate template = entityTemplateRepository.findByIdentifier(entity.templateIdentifier())
                .orElseThrow(() -> new EntityTemplateNotFoundException("identifier", entity.templateIdentifier()));
        entityValidationService.checkUniqueness(entity);
        entityValidationService.validateEntity(entity, template);
        return entityRepository.save(entity);
    }


}
