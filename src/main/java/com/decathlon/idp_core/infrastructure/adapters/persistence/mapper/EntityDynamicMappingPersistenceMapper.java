package com.decathlon.idp_core.infrastructure.adapters.persistence.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.common.EntityDynamicMappingJsonbHelper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity_mapping.EntityDynamicMappingJpaEntity;

/// MapStruct persistence mapper for [EntityDynamicMapping].
///
/// Maps between domain model (EntityDynamicMapping) and JPA entity (EntityDynamicMappingJpaEntity).
/// Handles JSONB columns for properties and relations via the dedicated helper.
@Mapper(componentModel = SPRING, uses = EntityDynamicMappingJsonbHelper.class)
public interface EntityDynamicMappingPersistenceMapper {

  @Mapping(target = "properties", qualifiedByName = "jsonStringToMap")
  @Mapping(target = "relations", qualifiedByName = "jsonStringToMap")
  EntityDynamicMapping toDomain(EntityDynamicMappingJpaEntity jpa);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "properties", qualifiedByName = "mapToJsonString")
  @Mapping(target = "relations", qualifiedByName = "mapToJsonString")
  EntityDynamicMappingJpaEntity toJpa(EntityDynamicMapping domain);
}
