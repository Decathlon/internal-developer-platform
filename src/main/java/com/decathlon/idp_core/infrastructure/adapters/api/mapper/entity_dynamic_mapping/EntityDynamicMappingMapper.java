package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity_dynamic_mapping;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityDynamicMappingCreateDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityDynamicMappingDtoInCommonFields;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityDynamicMappingUpdateDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_dynamic_mapping.EntityDynamicMappingDtoOut;

@Component
public class EntityDynamicMappingMapper {

  public EntityDynamicMapping toDomain(EntityDynamicMappingCreateDtoIn mapping) {
    // Map each DTO field explicitly to its matching domain field. The
    // EntityDynamicMapping
    // constructor order is (id, identifier, entityTemplateIdentifier, filter,
    // entityIdentifier,
    // entityName, properties, relations); keeping this alignment prevents the
    // entityTemplateIdentifier
    // identifier and the filter expression from being swapped.
    EntityDynamicMappingDtoInCommonFields fields = mapping.commonFields();
    return new EntityDynamicMapping(null, // id (assigned by persistence layer)
        mapping.identifier(), // identifier
        fields.entityTemplateIdentifier(), // entityTemplateIdentifier
        fields.filter(), // filter
        fields.name(), // name
        fields.description(), fields.entity().identifier(), // entityIdentifier
        fields.entity().name(), // entityName
        safeMap(fields.entity().properties()), // properties
        safeMap(fields.entity().relations())); // relations
  }

  public EntityDynamicMappingDtoOut fromEntityMappingToDto(EntityDynamicMapping mapping) {
    return new EntityDynamicMappingDtoOut(mapping.identifier(), mapping.entityTemplateIdentifier(),
        mapping.filter(), mapping.name(), mapping.description(),
        new EntityDynamicMappingDtoOut.InboundWebhookEntityMappingDtoOut(mapping.entityIdentifier(),
            mapping.entityName(), Map.copyOf(mapping.properties()),
            Map.copyOf(mapping.relations())));
  }

  /// Converts an update DTO to domain model, using the identifier from the path.
  ///
  /// @param identifier the mapping identifier from the URL path
  /// @param dto the update request body
  /// @return the domain model for update
  public EntityDynamicMapping toDomainForUpdate(String identifier,
      EntityDynamicMappingUpdateDtoIn dto) {
    var fields = dto.commonFields();
    return new EntityDynamicMapping(null, // id (will be set from existing entity)
        identifier, // identifier from path
        fields.entityTemplateIdentifier(), // entityTemplateIdentifier
        fields.filter(), // filter
        fields.name(), // titre
        fields.description(), fields.entity().identifier(), // entityIdentifier
        fields.entity().name(), // entityName
        safeMap(fields.entity().properties()), // properties
        safeMap(fields.entity().relations())); // relations
  }

  private Map<String, String> safeMap(Map<String, String> input) {
    return input == null ? Map.of() : Map.copyOf(input);
  }
}
