package com.decathlon.idp_core.domain.service.entity;

import java.util.List;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

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
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.domain.service.EntityQueryParserService;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateService;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateValidationService;

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
  /// **Contract:** Returns paginated entities conforming to the specified
  /// template
  /// that additionally satisfy all criteria in filter (when provided). Template
  /// existence is validated first. When filter is null or empty, the result
  /// includes all entities for the template.
  ///
  /// @param pageable pagination configuration for large entity sets
  /// @param templateIdentifier business identifier of the entity template
  /// @param entityFilter the parsed query filter; null or
  /// [EntityFilter#empty()] for no filtering
  /// @return paginated entities matching the template and all filter criteria
  /// @throws EntityTemplateNotFoundException when template doesn't exist
  @Transactional
  public Page<Entity> getEntitiesByTemplateIdentifier(Pageable pageable, String templateIdentifier,
      EntityFilter entityFilter) {
    EntityTemplate template = entityTemplateService
        .getEntityTemplateByIdentifier(templateIdentifier);
    EntityFilter filter = entityFilter != null ? entityFilter : EntityFilter.empty();
    entityQueryParserService.validateFilterPropertyTypes(filter, template);
    return entityRepository.findByTemplateIdentifierWithFilter(templateIdentifier, filter,
        pageable);
  }

  /// Provides lightweight entity summaries for efficient bulk operations.
  ///
  /// **Contract:** Returns summary projections without full entity data,
  /// optimized
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
  /// @param entityIdentifier unique business identifier of the entity within
  /// template
  /// @return the entity matching both identifiers
  /// @throws EntityTemplateNotFoundException when template doesn't exist
  /// @throws EntityNotFoundException when entity doesn't exist
  @Transactional
  public Entity getEntityByTemplateIdentifierAndIdentifier(String templateIdentifier,
      String entityIdentifier) {
    entityTemplateValidationService.validateTemplateExists(templateIdentifier);
    return entityRepository
        .findByTemplateIdentifierAndIdentifier(templateIdentifier, entityIdentifier)
        .orElseThrow(() -> new EntityNotFoundException(templateIdentifier, entityIdentifier));
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
  /// exist
  /// @throws EntityAlreadyExistsException when an entity with the same
  /// identifier already exists for this
  /// template
  /// @throws EntityValidationException when entity, property, or relation
  /// data is invalid
  @Transactional
  public Entity createEntity(@Valid Entity entity) {
    EntityTemplate template = entityTemplateService
        .getEntityTemplateByIdentifier(entity.templateIdentifier());

    // Enrich relations with target template identifiers from template definition
    Entity enrichedEntity = enrichRelationsWithTargetTemplates(entity, template);

    entityValidationService.validateForCreation(enrichedEntity, template);

    return entityRepository.save(enrichedEntity);
  }

  /// Updates an existing entity identified by template and entity identifiers.
  ///
  /// **Contract:** Validates template existence, then entity existence within the
  /// template scope. Validates updated entity data against the template
  /// constraints before persisting changes.
  ///
  /// @param templateIdentifier template identifier from the request path
  /// @param entityIdentifier entity identifier from the request path
  /// @param entity validated entity payload
  /// @return persisted updated entity
  /// @throws EntityTemplateNotFoundException when template doesn't exist
  /// @throws EntityNotFoundException when target entity doesn't exist
  /// @throws EntityValidationException when payload violates
  /// template constraints
  @Transactional
  public Entity updateEntity(String templateIdentifier, String entityIdentifier,
      @Valid Entity entity) {
    EntityTemplate template = entityTemplateService
        .getEntityTemplateByIdentifier(templateIdentifier);
    Entity existingEntity = entityRepository
        .findByTemplateIdentifierAndIdentifier(templateIdentifier, entityIdentifier)
        .orElseThrow(() -> new EntityNotFoundException(templateIdentifier, entityIdentifier));

    Entity entityToSave = new Entity(existingEntity.id(), templateIdentifier, entity.name(),
        entityIdentifier, entity.properties(), entity.relations());

    // Enrich relations with target template identifiers from template definition
    Entity enrichedEntity = enrichRelationsWithTargetTemplates(entityToSave, template);

    entityValidationService.validateForUpdate(enrichedEntity, template);
    return entityRepository.save(enrichedEntity);
  }

  /// Enriches entity relations with target template identifiers from template
  /// definition.
  ///
  /// **Business purpose:** Resolves target template identifiers for each relation
  /// based on the relation name defined in the entity template. This allows the
  /// API layer to accept minimalistic relation payloads (relation name + target
  /// entity identifiers) while maintaining referential integrity at the domain
  /// level.
  ///
  /// **Contract:** For each relation in the entity, looks up the corresponding
  /// relation definition in the template and replaces the target template
  /// identifier with the one specified in the template. Relations without
  /// matching
  /// definitions are left unchanged (validation will catch these later).
  ///
  /// @param entity the entity with relations to enrich
  /// @param template the template containing relation definitions
  /// @return new entity with enriched relations containing correct target
  /// template identifiers
  private Entity enrichRelationsWithTargetTemplates(Entity entity, EntityTemplate template) {
    List<Relation> enrichedRelations = entity.relations().stream().map(relation -> {
      // Look up relation definition in template
      RelationDefinition definition = template.relationsDefinitions().stream()
          .filter(def -> def.name().equals(relation.name())).findFirst().orElse(null);
      if (definition == null) {
        // Leave unchanged - validation will catch undefined relations
        return relation;
      }
      // Replace target template identifier with the one from template definition
      return new Relation(relation.id(), relation.name(), definition.targetTemplateIdentifier(),
          relation.targetEntityIdentifiers());
    }).toList();

    return new Entity(entity.id(), entity.templateIdentifier(), entity.name(), entity.identifier(),
        entity.properties(), enrichedRelations);
  }

}
