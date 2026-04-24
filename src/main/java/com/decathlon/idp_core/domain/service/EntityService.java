package com.decathlon.idp_core.domain.service;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.ENTITY_IDENTIFIER_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.ENTITY_NAME_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_NAME_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_VALUE_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_NAME_MANDATORY_SIMPLE;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_TARGET_IDENTIFIERS_NOT_NULL;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.exception.EntityAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.EntityNotFoundException;
import com.decathlon.idp_core.domain.exception.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.exception.EntityValidationException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntitySummary;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.domain.port.EntityTemplateRepositoryPort;
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
@AllArgsConstructor
public class EntityService {
    private final EntityRepositoryPort entityRepository;
    private final EntityTemplateRepositoryPort entityTemplateRepository;

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

        if (!entityTemplateRepository.existsByIdentifier(templateIdentifier)) {
            throw new EntityTemplateNotFoundException("identifier", templateIdentifier);
        }
        return entityRepository.findByTemplateIdentifier(templateIdentifier, pageable);
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
    public Entity getEntityByTemplateIdentifierAnIdentifier(String templateIdentifier, String entityIdentifier) {
        if (!entityTemplateRepository.existsByIdentifier(templateIdentifier)) {
            throw new EntityTemplateNotFoundException("identifier", templateIdentifier);
        }
        return entityRepository.findByTemplateIdentifierAndIdentifier(templateIdentifier, entityIdentifier)
                .orElseThrow(() -> new EntityNotFoundException(templateIdentifier,
                        entityIdentifier));
    }

    /// Creates and persists a new entity with business validation.
    ///
    /// **Contract:** Validates template existence, entity identifier uniqueness within
    /// the template scope, and entity/property/relation data integrity before persisting.
    ///
    /// @param entity validated entity to create and persist
    /// @return the persisted entity with generated identifiers
    /// @throws EntityTemplateNotFoundException when the referenced template doesn't exist
    /// @throws EntityAlreadyExistsException    when an entity with the same identifier already exists for this template
    /// @throws EntityValidationException       when entity, property, or relation data is invalid
    @Transactional
    public Entity createEntity(@Valid Entity entity) {
        if (!entityTemplateRepository.existsByIdentifier(entity.templateIdentifier())) {
            throw new EntityTemplateNotFoundException("identifier", entity.templateIdentifier());
        }

        if (entity.identifier() != null &&
                entityRepository.findByTemplateIdentifierAndIdentifier(entity.templateIdentifier(), entity.identifier()).isPresent()) {
            throw new EntityAlreadyExistsException(entity.templateIdentifier(), entity.identifier());
        }

        validateEntity(entity);

        return entityRepository.save(entity);
    }

    /// Validates intrinsic entity data integrity, including nested properties and relations.
    ///
    /// **Validation rules enforced (mirroring Jakarta annotations on domain records):**
    /// - Entity: name and identifier must not be null or blank
    /// - Property: name and value must not be null or blank
    /// - Relation: name must not be null or blank, targetEntityIdentifiers must not be null
    ///
    /// @param entity the entity to validate
    /// @throws EntityValidationException when one or more validation rules are violated
    private void validateEntity(Entity entity) {
        List<String> violations = Stream.<String>of()
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        // Entity-level
        addIfBlank(violations, entity.name(), ENTITY_NAME_MANDATORY);
        addIfBlank(violations, entity.identifier(), ENTITY_IDENTIFIER_MANDATORY);

        // Property-level
        if (entity.properties() != null) {
            for (int i = 0; i < entity.properties().size(); i++) {
                var prop = entity.properties().get(i);
                addIfBlank(violations, prop.name(), "Property[%d]: %s".formatted(i, PROPERTY_NAME_MANDATORY));
                addIfBlank(violations, prop.value(), "Property[%d]: %s".formatted(i, PROPERTY_VALUE_MANDATORY));
            }
        }

        // Relation-level
        if (entity.relations() != null) {
            for (int i = 0; i < entity.relations().size(); i++) {
                var rel = entity.relations().get(i);
                addIfBlank(violations, rel.name(), "Relation[%d]: %s".formatted(i, RELATION_NAME_MANDATORY_SIMPLE));
                if (rel.targetEntityIdentifiers() == null) {
                    violations.add("Relation[%d]: %s".formatted(i, RELATION_TARGET_IDENTIFIERS_NOT_NULL));
                }
            }
        }

        if (!violations.isEmpty()) {
            throw new EntityValidationException(violations);
        }
    }

    private void addIfBlank(List<String> violations, String value, String message) {
        if (value == null || value.isBlank()) {
            violations.add(message);
        }
    }
}
