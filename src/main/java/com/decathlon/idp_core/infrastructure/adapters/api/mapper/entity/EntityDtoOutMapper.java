package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntitySummary;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.RelationAsTargetSummary;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.enums.PropertyType;
import com.decathlon.idp_core.domain.service.entity.EntityService;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateService;
import com.decathlon.idp_core.domain.service.relation.RelationService;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntityDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntitySummaryDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/// Adapter mapper for converting domain [Entity] objects to API DTOs.
///
/// **Infrastructure mapping responsibilities:**
/// - Domain-to-DTO conversion for API response serialization
/// - Type-safe property mapping using [PropertyDefinition] metadata
/// - Efficient bulk entity processing with optimized summary lookups
/// - Relationship resolution and DTO projection for API consumers
///
/// **Performance optimizations:**
/// - Batch summary lookups minimize repository round trips
/// - Template-based type conversion avoids runtime type checking
/// - Lazy relation resolution for large entity graphs
///
/// **Technology specifics:**
/// - Uses Spring's conversion framework for type mappings
/// - Integrates with Jackson for JSON serialization patterns
/// - Stateless design ensures thread safety in web containers
@Component
@Slf4j
@RequiredArgsConstructor
public class EntityDtoOutMapper {

  private final EntityTemplateService entityTemplateService;
  private final EntityService entityService;
  private final RelationService relationService;

  /// Maps a single domain entity to API DTO using template-based conversion.
  ///
  /// **Infrastructure mapping:** Resolves entity template dynamically and
  /// performs
  /// complete domain-to-DTO transformation including properties and
  /// relationships.
  ///
  /// @param entity domain entity to convert for API response
  /// @return fully mapped entity DTO with resolved template metadata
  public EntityDtoOut fromEntity(Entity entity) {
    EntityTemplate entityTemplate = entityTemplateService
        .getEntityTemplateByIdentifier(entity.templateIdentifier());
    return fromEntityUsingEntityTemplate(entity, entityTemplate);
  }

  /// Maps paginated domain entities to API DTOs with optimized bulk operations.
  ///
  /// **Performance optimization:** Batches template resolution and relationship
  /// lookups
  /// to minimize database queries. Builds summary maps for efficient relationship
  /// resolution across the entire page.
  ///
  /// @param entities paginated domain entities from repository layer
  /// @param entityTemplateIdentifier template identifier for batch template
  /// resolution
  /// @return paginated API DTOs with complete relationship data
  public Page<EntityDtoOut> fromEntitiesPageToDtoPage(Page<Entity> entities,
      String entityTemplateIdentifier) {

    Map<String, EntitySummaryDto> pageEntitiesSummaries = buildRelatedEntitiesSummaryMapByPage(
        entities);
    Map<String, List<RelationAsTargetSummary>> relationTargetOwnershipsMap = buildRelationsAsTargetSummaryMapByPage(
        entities);

    EntityTemplate pageEntityTemplate = entityTemplateService
        .getEntityTemplateByIdentifier(entityTemplateIdentifier);
    return entities.map(entity -> fromEntityUsingEntityTemplateAndSummaryMap(entity,
        pageEntityTemplate, pageEntitiesSummaries, relationTargetOwnershipsMap));
  }

  /// Maps a single entity to its DTO using the provided entity template.
  ///
  /// @param entity the entity to map
  /// @param entityTemplate the template for property type mapping
  /// @return the mapped DTO with unified relations
  private EntityDtoOut fromEntityUsingEntityTemplate(Entity entity, EntityTemplate entityTemplate) {
    Map<String, Object> props = mapPropertiesDto(entity, entityTemplate);

    List<String> allTargetIdentifiers = getAllTargetIdentifiersFromEntityRelations(entity);
    Map<String, EntitySummaryDto> relatedEntitiesSummaryMap = buildEntitiesSummariesMap(
        allTargetIdentifiers);
    Map<String, List<RelationAsTargetSummary>> relatedEntitiesByTargetSummaryMap = buildRelationsAsTargetSummaryMapByEntity(
        entity);

    Map<String, List<EntitySummaryDto>> unifiedRelations = buildUnifiedRelationsMap(entity,
        relatedEntitiesSummaryMap, relatedEntitiesByTargetSummaryMap);

    return new EntityDtoOut(entity.identifier(), entity.name(), entity.templateIdentifier(), props,
        unifiedRelations);
  }

  /// Maps a single entity to its DTO using pre-built summary and
  /// relation-as-target maps.
  ///
  /// @param entity the entity to map
  /// @param entityTemplate the template for property type mapping
  /// @param relatedEntitiesSummaries map of entity summaries for relation targets
  /// @param relationTargetOwnershipsMap map of relations-as-target for the entity
  /// @return the mapped DTO with unified relations
  private EntityDtoOut fromEntityUsingEntityTemplateAndSummaryMap(Entity entity,
      EntityTemplate entityTemplate, Map<String, EntitySummaryDto> relatedEntitiesSummaries,
      Map<String, List<RelationAsTargetSummary>> relationTargetOwnershipsMap) {

    Map<String, Object> props = mapPropertiesDto(entity, entityTemplate);
    Map<String, List<EntitySummaryDto>> unifiedRelations = buildUnifiedRelationsMap(entity,
        relatedEntitiesSummaries, relationTargetOwnershipsMap);

    return new EntityDtoOut(entity.identifier(), entity.name(), entity.templateIdentifier(), props,
        unifiedRelations);
  }

  /// Maps the properties of an entity to a map of property names to typed values,
  /// using the entity template for type conversion.
  /// Properties with a null value are excluded from the output.
  ///
  /// @param entity the entity whose properties to map
  /// @param entityTemplate the template for property type mapping
  /// @return a map of property names to typed values
  private Map<String, Object> mapPropertiesDto(Entity entity, EntityTemplate entityTemplate) {
    if (entity.properties() == null) {
      return Collections.emptyMap();
    }

    Map<String, PropertyDefinition> propertiesDefinitions = entityTemplate.propertiesDefinitions()
        .stream().collect(Collectors.toMap(PropertyDefinition::name, Function.identity()));

    return entity.properties().stream().filter(prop -> prop.value() != null)
        .collect(Collectors.toMap(Property::name,
            prop -> convertPropertyValue(prop, propertiesDefinitions.get(prop.name()))));
  }

  /// Converts a property value to its typed representation based on the property
  /// definition.
  ///
  /// @param property the property to convert
  /// @param definition the property definition for type information, may be null
  /// @return the typed value, falling back to the raw string value
  private Object convertPropertyValue(Property property, PropertyDefinition definition) {
    String value = property.value();
    if (definition == null) {
      return value;
    }
    PropertyType type = definition.type();
    if (PropertyType.NUMBER.equals(type)) {
      try {
        return Double.valueOf(value);
      } catch (NumberFormatException _) {
        return value;
      }
    } else if (PropertyType.BOOLEAN.equals(type)) {
      return Boolean.valueOf(value);
    }
    return value;
  }

  /// Builds a unified relations map combining outbound and inbound relations.
  ///
  /// **Unification logic:**
  /// - Outbound relations: keyed by relation name, values are target summaries
  /// - Inbound relations: keyed by relation name, values are source summaries
  /// - Both directions merged under the same relation key
  ///
  /// @param entity the entity whose relations to unify
  /// @param relatedEntitiesSummaries map of target entity summaries (for
  /// outbound)
  /// @param relationTargetOwnershipsMap map of inbound relations
  /// @return unified relations map with combined outbound and inbound relations
  private Map<String, List<EntitySummaryDto>> buildUnifiedRelationsMap(Entity entity,
      Map<String, EntitySummaryDto> relatedEntitiesSummaries,
      Map<String, List<RelationAsTargetSummary>> relationTargetOwnershipsMap) {

    java.util.HashMap<String, List<EntitySummaryDto>> unifiedRelations = new java.util.HashMap<>();

    // Add outbound relations (entity is source)
    if (entity.relations() != null) {
      entity.relations().forEach(relation -> {
        String relationKey = relation.name();
        List<EntitySummaryDto> targets = relation.targetEntityIdentifiers().stream()
            .map(relatedEntitiesSummaries::get).filter(Objects::nonNull).toList();
        unifiedRelations.put(relationKey, new ArrayList<>(targets));
      });
    }

    // Add inbound relations (entity is target) — merge if key exists
    List<RelationAsTargetSummary> inboundRelations = relationTargetOwnershipsMap
        .get(entity.identifier());
    if (inboundRelations != null) {
      inboundRelations.forEach(inboundRelation -> {
        String relationKey = inboundRelation.relationName();
        EntitySummaryDto source = new EntitySummaryDto(inboundRelation.sourceEntityIdentifier(),
            inboundRelation.sourceEntityName(), inboundRelation.sourceTemplateIdentifier());

        unifiedRelations.merge(relationKey, List.of(source), (existing, incoming) -> {
          List<EntitySummaryDto> merged = new ArrayList<>(existing);
          merged.addAll(incoming);
          return merged;
        });
      });
    }

    return unifiedRelations;
  }

  /// Builds a map of relation target ownerships for a page of entities, grouping
  /// by target entity identifier.
  ///
  /// @param entitiesPage the page of entities to analyze
  /// @return a map from target entity identifier to list of relation-as-target
  /// summaries
  private Map<String, List<RelationAsTargetSummary>> buildRelationsAsTargetSummaryMapByPage(
      Page<Entity> entitiesPage) {
    if (entitiesPage == null || entitiesPage.getContent().isEmpty()) {
      return Collections.emptyMap();
    }
    List<String> entitiesIdentifiers = entitiesPage.getContent().stream().map(Entity::identifier)
        .filter(Objects::nonNull).toList();
    List<RelationAsTargetSummary> relationTargetOwnerships = relationService
        .findRelationsSummariesByTargetEntityIdentifiers(entitiesIdentifiers);
    return relationTargetOwnerships.stream()
        .collect(Collectors.groupingBy(RelationAsTargetSummary::targetEntityIdentifier));
  }

  /// Builds a map of relation target ownerships for a single entity, grouping by
  /// target entity identifier.
  ///
  /// @param entity the entity to analyze
  /// @return a map from target entity identifier to list of relation-as-target
  /// summaries
  private Map<String, List<RelationAsTargetSummary>> buildRelationsAsTargetSummaryMapByEntity(
      Entity entity) {
    if (entity == null || entity.identifier() == null) {
      return Collections.emptyMap();
    }
    List<RelationAsTargetSummary> relationTargetOwnerships = relationService
        .findRelationsSummariesByTargetEntityIdentifiers(List.of(entity.identifier()));
    return relationTargetOwnerships.stream()
        .collect(Collectors.groupingBy(RelationAsTargetSummary::targetEntityIdentifier));
  }

  /// Gets all unique target entity identifiers from the relations of a single
  /// entity.
  ///
  /// @param entity the entity to analyze
  /// @return a list of unique target entity identifiers
  private List<String> getAllTargetIdentifiersFromEntityRelations(Entity entity) {
    return entity.relations() == null
        ? Collections.emptyList()
        : new ArrayList<>(entity.relations().stream()
            .flatMap(rel -> rel.targetEntityIdentifiers().stream()).collect(Collectors.toSet()));
  }

  /// Gets all unique target entity identifiers from the relations of all entities
  /// in a page.
  ///
  /// @param entities the page of entities to analyze
  /// @return a list of unique target entity identifiers
  private List<String> getUniqueTargetIdentifiersInPage(Page<Entity> entities) {
    return new ArrayList<>(entities.stream()
        .flatMap(entity -> entity.relations() == null
            ? Stream.empty()
            : entity.relations().stream().flatMap(rel -> rel.targetEntityIdentifiers().stream()))
        .collect(Collectors.toSet()));
  }

  /// Builds a map of entity summaries for all unique target identifiers in a page
  /// of entities.
  ///
  /// @param entities the page of entities
  /// @return a map from entity identifier to summary DTO
  private Map<String, EntitySummaryDto> buildRelatedEntitiesSummaryMapByPage(
      Page<Entity> entities) {
    return buildEntitiesSummariesMap(getUniqueTargetIdentifiersInPage(entities));
  }

  /// Builds a map of entity summaries for a list of target identifiers.
  ///
  /// Includes template identifier for each entity, enabling frontend to determine
  /// relation direction without additional queries.
  ///
  /// @param targetIdentifiers the list of target entity identifiers
  /// @return a map from entity identifier to summary DTO with template info
  private Map<String, EntitySummaryDto> buildEntitiesSummariesMap(List<String> targetIdentifiers) {
    return targetIdentifiers.isEmpty()
        ? Collections.emptyMap()
        : entityService.getEntitiesSummariesByIdentifiers(targetIdentifiers).stream()
            .collect(Collectors.toMap(EntitySummary::identifier,
                es -> new EntitySummaryDto(es.identifier(), es.name(), es.templateIdentifier())));
  }

  /// Maps paginated search results to API DTOs with optimized bulk operations.
  ///
  /// **Performance optimization:** Batches template resolution across all
  /// templates
  /// referenced in the page — unlike [#fromEntitiesPageToDtoPage] which is scoped
  /// to a single template, this method handles multi-template result sets.
  ///
  /// @param entities paginated domain entities, possibly spanning several
  /// templates
  /// @return paginated API DTOs with complete relationship data
  public Page<EntityDtoOut> fromEntitiesSearchPageToDtoPage(Page<Entity> entities) {
    if (entities.isEmpty()) {
      return entities.map(entity -> entityDtoOutMapper(entity, Map.of(), Map.of()));
    }

    Map<String, EntitySummaryDto> pageEntitiesSummaries = buildRelatedEntitiesSummaryMapByPage(
        entities);
    Map<String, List<RelationAsTargetSummary>> relationTargetOwnershipsMap = buildRelationsAsTargetSummaryMapByPage(
        entities);

    Map<String, EntityTemplate> templatesByIdentifier = entities.stream()
        .map(Entity::templateIdentifier).filter(Objects::nonNull).distinct().collect(Collectors
            .toMap(Function.identity(), entityTemplateService::getEntityTemplateByIdentifier));

    return entities.map(entity -> {
      EntityTemplate template = templatesByIdentifier.get(entity.templateIdentifier());
      if (template == null) {
        return entityDtoOutMapper(entity, pageEntitiesSummaries, relationTargetOwnershipsMap);
      }
      return fromEntityUsingEntityTemplateAndSummaryMap(entity, template, pageEntitiesSummaries,
          relationTargetOwnershipsMap);
    });
  }

  private EntityDtoOut entityDtoOutMapper(Entity entity, Map<String, EntitySummaryDto> summaries,
      Map<String, List<RelationAsTargetSummary>> relationsAsTargetMap) {
    Map<String, List<EntitySummaryDto>> unifiedRelations = buildUnifiedRelationsMap(entity,
        summaries, relationsAsTargetMap);
    return new EntityDtoOut(entity.identifier(), entity.name(), entity.templateIdentifier(),
        Collections.emptyMap(), unifiedRelations);
  }

}
