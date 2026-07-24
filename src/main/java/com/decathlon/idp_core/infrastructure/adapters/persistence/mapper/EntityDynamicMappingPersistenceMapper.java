package com.decathlon.idp_core.infrastructure.adapters.persistence.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.common.EntityDynamicMappingJsonbHelper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_dynamic_mapping.EntityDynamicMappingJpaEntity;

/// MapStruct persistence mapper for [EntityDynamicMapping].
///
/// Maps between domain model (EntityDynamicMapping) and JPA entity (EntityDynamicMappingJpaEntity).
/// Handles JSONB columns for properties and relations via the dedicated helper.
@Mapper(componentModel = SPRING, uses = EntityDynamicMappingJsonbHelper.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface EntityDynamicMappingPersistenceMapper {

  @Mapping(target = "properties", qualifiedByName = "jsonStringToMap")
  @Mapping(target = "relations", qualifiedByName = "jsonStringToRelationList")
  @Mapping(target = "entityTemplateIdentifier", source = "template.identifier")
  // Explicit self-mapping: MapStruct otherwise silently drops the
  // `entityIdentifier` property, leaving the NOT NULL column unset.
  @Mapping(target = "entityIdentifier", source = "entityIdentifier")
  @Mapping(target = "entityName", source = "entityName")
  EntityDynamicMapping toDomain(EntityDynamicMappingJpaEntity jpa);

  @Mapping(target = "properties", qualifiedByName = "mapToJsonString")
  @Mapping(target = "relations", qualifiedByName = "relationListToJsonString")
  // The template foreign key (UUID) is resolved from the business identifier and
  // set by the persistence adapter, so both template fields are ignored here.
  @Mapping(target = "entityTemplateId", ignore = true)
  @Mapping(target = "template", ignore = true)
  // Explicit self-mapping: MapStruct otherwise silently drops the
  // `entityIdentifier` property, leaving the NOT NULL column unset.
  @Mapping(target = "entityIdentifier", source = "entityIdentifier")
  @Mapping(target = "entityName", source = "entityName")
  EntityDynamicMappingJpaEntity toJpa(EntityDynamicMapping domain);
}
