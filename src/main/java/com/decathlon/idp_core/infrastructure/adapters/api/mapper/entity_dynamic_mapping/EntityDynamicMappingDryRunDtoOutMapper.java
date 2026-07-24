package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity_dynamic_mapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity_mapping.DryRunResult;
import com.decathlon.idp_core.domain.model.entity_mapping.DryRunResult.DryRunEntityResult;
import com.decathlon.idp_core.domain.model.entity_mapping.DryRunResult.DryRunError;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_dynamic_mapping.EntityDynamicMappingDryRunDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_dynamic_mapping.EntityDynamicMappingDryRunDtoOut.DryRunEntityDto;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_dynamic_mapping.EntityDynamicMappingDryRunDtoOut.DryRunEntityResultDto;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_dynamic_mapping.EntityDynamicMappingDryRunDtoOut.DryRunErrorDto;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_dynamic_mapping.EntityDynamicMappingDryRunDtoOut.DryRunRelationDto;

/// Mapper converting a domain [DryRunResult] to the entity dynamic mapping
/// dry-run response DTO.
@Component
public class EntityDynamicMappingDryRunDtoOutMapper {

  public EntityDynamicMappingDryRunDtoOut toDto(DryRunResult result) {
    List<DryRunEntityResultDto> resultDtos = result.entityResults().stream()
        .map(this::toEntityResultDto).toList();
    return new EntityDynamicMappingDryRunDtoOut(resultDtos);
  }

  private DryRunEntityResultDto toEntityResultDto(DryRunEntityResult result) {
    return new DryRunEntityResultDto(result.mappingTemplateIdentifier(), result.success(),
        result.entity() != null ? toEntityDto(result.entity()) : null,
        result.error() != null ? toErrorDto(result.error()) : null);
  }

  private DryRunEntityDto toEntityDto(Entity entity) {
    Map<String, String> properties = new HashMap<>();
    if (entity.properties() != null) {
      for (Property prop : entity.properties()) {
        properties.put(prop.name(), prop.value());
      }
    }

    List<DryRunRelationDto> relations = entity.relations() != null
        ? entity.relations().stream().map(this::toRelationDto).toList()
        : List.of();

    return new DryRunEntityDto(entity.templateIdentifier(), entity.name(), entity.identifier(),
        properties, relations);
  }

  private DryRunRelationDto toRelationDto(Relation relation) {
    return new DryRunRelationDto(relation.name(), relation.targetEntityIdentifiers());
  }

  private DryRunErrorDto toErrorDto(DryRunError error) {
    return new DryRunErrorDto(error.type().name(), error.message());
  }
}
