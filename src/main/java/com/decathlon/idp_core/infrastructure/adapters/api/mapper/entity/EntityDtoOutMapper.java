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
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity.RelationAsTargetSummary;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.enums.PropertyType;
import com.decathlon.idp_core.domain.service.EntityService;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateService;
import com.decathlon.idp_core.domain.service.RelationService;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntityDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntitySummaryDto;

import lombok.AllArgsConstructor;

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
@AllArgsConstructor
public class EntityDtoOutMapper {

    private final EntityTemplateService entityTemplateService;
    private final EntityService entityService;
    private final RelationService relationService;

    /// Maps a single domain entity to API DTO using template-based conversion.
    ///
    /// **Infrastructure mapping:** Resolves entity template dynamically and performs
    /// complete domain-to-DTO transformation including properties and relationships.
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
    /// **Performance optimization:** Batches template resolution and relationship lookups
    /// to minimize database queries. Builds summary maps for efficient relationship
    /// resolution across the entire page.
    ///
    /// @param entities paginated domain entities from repository layer
    /// @param entityTemplateIdentifier template identifier for batch template resolution
    /// @return paginated API DTOs with complete relationship data
    public Page<EntityDtoOut> fromEntitiesPageToDtoPage(Page<Entity> entities,
            String entityTemplateIdentifier) {

        Map<String, EntitySummaryDto> pageEntitiesSummaries = buildRelatedEntitiesSummaryMapByPage(entities);
        Map<String, List<RelationAsTargetSummary>> relationTargetOwnershipsMap = buildRelationsAsTargetSummaryMapByPage(
                entities);

        EntityTemplate pageEntityTemplate = entityTemplateService
                .getEntityTemplateByIdentifier(entityTemplateIdentifier);
        return entities.map(entity -> fromEntityUsingEntityTemplateAndSummaryMap(entity, pageEntityTemplate,
                pageEntitiesSummaries, relationTargetOwnershipsMap));
    }


    /// Maps a single entity to its DTO using the provided entity template.
    ///
    /// @param entity         the entity to map
    /// @param entityTemplate the template for property type mapping
    /// @return the mapped DTO
      private EntityDtoOut fromEntityUsingEntityTemplate(Entity entity, EntityTemplate entityTemplate) {
        Map<String, Object> props = mapPropertiesDto(entity, entityTemplate);

        List<String> allTargetIdentifiers = getAllTargetIdentifiersFromEntityRelations(entity);
        Map<String, EntitySummaryDto> relatedEntitiesSummaryMap = buildEntitiesSummariesMap(allTargetIdentifiers);
        Map<String, List<EntitySummaryDto>> relationMap = mapRelationsDto(entity, relatedEntitiesSummaryMap);
        Map<String, List<RelationAsTargetSummary>> relatedEntitiesByTargetSummaryMap = buildRelationsAsTargetSummaryMapByEntity(
                entity);
        Map<String, List<EntitySummaryDto>> relationAsTargetMap = mapRelationsAsTargetDto(entity,
                relatedEntitiesByTargetSummaryMap);

        return EntityDtoOut.builder()
                .templateIdentifier(entity.templateIdentifier())
                .name(entity.name())
                .identifier(entity.identifier())
                .properties(props)
                .relations(relationMap)
                .relationsAsTarget(relationAsTargetMap)
                .build();
    }

    /// Maps a single entity to its DTO using pre-built summary and
    /// relation-as-target maps.
    ///
    /// @param entity                      the entity to map
    /// @param entityTemplate              the template for property type mapping
    /// @param relatedEntitiesSummaries          map of entity summaries for relation
    ///                                    targets
    /// @param relationTargetOwnershipsMap map of relations-as-target for the entity
    /// @return the mapped DTO
    private EntityDtoOut fromEntityUsingEntityTemplateAndSummaryMap(Entity entity, EntityTemplate entityTemplate,
            Map<String, EntitySummaryDto> relatedEntitiesSummaries,
            Map<String, List<RelationAsTargetSummary>> relationTargetOwnershipsMap) {

        Map<String, Object> props = mapPropertiesDto(entity, entityTemplate);
        Map<String, List<EntitySummaryDto>> relationMap = mapRelationsDto(entity, relatedEntitiesSummaries);
        Map<String, List<EntitySummaryDto>> relationAsTargetMap = mapRelationsAsTargetDto(entity,
                relationTargetOwnershipsMap);

        return EntityDtoOut.builder()
                .templateIdentifier(entity.templateIdentifier())
                .name(entity.name())
                .identifier(entity.identifier())
                .properties(props)
                .relations(relationMap)
                .relationsAsTarget(relationAsTargetMap)
                .build();
    }

    /// Maps the properties of an entity to a map of property names to typed values,
    /// using the entity template for type conversion.
    ///
    /// @param entity         the entity whose properties to map
    /// @param entityTemplate the template for property type mapping
    /// @return a map of property names to typed values
    private Map<String, Object> mapPropertiesDto(Entity entity, EntityTemplate entityTemplate) {

        if (entity.properties() == null) {
            return Collections.emptyMap();
        }

        Map<String, PropertyDefinition> propertiesDefinitions = entityTemplate.propertiesDefinitions().stream()
                .collect(Collectors.toMap(PropertyDefinition::name, Function.identity()));

        return entity.properties().stream()
                .collect(Collectors.toMap(
                        Property::name,
                        prop -> {
                            PropertyDefinition def = propertiesDefinitions.get(prop.name());
                            if (def != null) {
                                PropertyType type = def.type();
                                String value = prop.value();
                                if (PropertyType.NUMBER.equals(type)) {
                                    try {
                                        return Double.valueOf(value);
                                    } catch (NumberFormatException _) {
                                        return null;
                                    }
                                } else if (PropertyType.BOOLEAN.equals(type)) {
                                    return Boolean.valueOf(value);
                                }
                                // Default to string
                                return value;
                            } else {
                                // Fallback if propertyDefinition is missing
                                return prop.value();
                            }
                        }));
    }

    /// Maps the relations of an entity to a map of relation names to lists of target
    /// entity summaries.
    ///
    /// @param entity     the entity whose relations to map
    /// @param relatedEntitiesSummaries map of entity summaries for relation targets
    /// @return a map of relation names to lists of target entity summaries
    private Map<String, List<EntitySummaryDto>> mapRelationsDto(Entity entity,
            Map<String, EntitySummaryDto> relatedEntitiesSummaries) {
        return entity.relations() == null
                ? Collections.emptyMap()
                : entity.relations().stream()
                        .collect(Collectors.groupingBy(
                                Relation::name,
                                Collectors.flatMapping(rel -> rel.targetEntityIdentifiers().stream()
                                        .map(relatedEntitiesSummaries::get)
                                        .filter(Objects::nonNull),
                                        Collectors.toList())));
    }

    ///
    /// Maps the relations-as-target for an entity to a map of relation names to
    /// lists of source entity summaries.
    ///
    /// @param entity                      the entity whose relations-as-target to
    ///                                    map
    /// @param relationTargetOwnershipsMap map of relations-as-target for the entity
    /// @return a map of relation names to lists of source entity summaries
    private Map<String, List<EntitySummaryDto>> mapRelationsAsTargetDto(Entity entity,
            Map<String, List<RelationAsTargetSummary>> relationTargetOwnershipsMap) {
        List<RelationAsTargetSummary> relationAsTargetSummaries = relationTargetOwnershipsMap
                .get(entity.identifier());
        if (relationAsTargetSummaries == null) {
            return Collections.emptyMap();
        }

        return relationAsTargetSummaries.stream()
                .collect(Collectors.groupingBy(
                        RelationAsTargetSummary::relationName,
                        Collectors.mapping(
                                r -> new EntitySummaryDto(r.sourceEntityIdentifier(), r.sourceEntityName()),
                                Collectors.toList())));
    }

    /// Builds a map of relation target ownerships for a list of entities, grouping
    /// by target entity identifier.
    ///
    /// @param entitiesPage the list of entities to analyze
    /// @return a map from target entity identifier to list of relation-as-target summaries
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

    ///
    /// Builds a map of relation target ownerships for a single entity, grouping by
    /// target entity identifier.
    ///
    /// @param entity the entity to analyze
    /// @return a map from target entity identifier to list of relation-as-target
    /// summaries
    private Map<String, List<RelationAsTargetSummary>> buildRelationsAsTargetSummaryMapByEntity(Entity entity) {
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
                        .flatMap(rel -> rel.targetEntityIdentifiers().stream())
                        .collect(Collectors.toSet()));
    }

    ///
    /// Gets all unique target entity identifiers from the relations of all entities
    /// in a page.
    ///
    /// @param entities the page of entities to analyze
    /// @return a list of unique target entity identifiers
    private List<String> getUniqueTargetIdentifiersInPage(Page<Entity> entities) {
        return new ArrayList<>(entities.stream()
                .flatMap(entity -> entity.relations() == null
                        ? Stream.empty()
                        : entity.relations().stream()
                                .flatMap(rel -> rel.targetEntityIdentifiers().stream()))
                .collect(Collectors.toSet()));

    }

    /// Builds a map of entity summaries for all unique target identifiers in a page
    /// of entities.
    ///
    /// @param entities the page of entities
    /// @return a map from entity identifier to summary DTO
    private Map<String, EntitySummaryDto> buildRelatedEntitiesSummaryMapByPage(Page<Entity> entities) {
        return buildEntitiesSummariesMap(
                getUniqueTargetIdentifiersInPage(entities));
    }

    /// Builds a map of entity summaries for a list of target identifiers.
    ///
    /// @param targetIdentifiers the list of target entity identifiers
    /// @return a map from entity identifier to summary DTO
    private Map<String, EntitySummaryDto> buildEntitiesSummariesMap(List<String> targetIdentifiers) {
        return targetIdentifiers.isEmpty()
                ? Collections.emptyMap()
                : entityService.getEntitiesSummariesByIndentifiers(targetIdentifiers)
                        .stream()
                        .collect(Collectors.toMap(
                                EntitySummary::identifier,
                                es -> new EntitySummaryDto(es.identifier(), es.name())));
    }

}
