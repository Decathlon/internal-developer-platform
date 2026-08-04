package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity_dynamic_mapping;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_mapping.RelationMapping;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityDynamicMappingCreateDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityDynamicMappingDtoInCommonFields;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityDynamicMappingRelationDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityDynamicMappingUpdateDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_dynamic_mapping.EntityDynamicMappingDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_dynamic_mapping.RelationMappingDtoOut;

@Component
public class EntityDynamicMappingMapper {

  public EntityDynamicMapping toDomain(EntityDynamicMappingCreateDtoIn mapping) {
    EntityDynamicMappingDtoInCommonFields fields = mapping.commonFields();
    return new EntityDynamicMapping(null, // id (assigned by persistence layer)
        mapping.identifier(), // identifier
        fields.entityTemplateIdentifier(), // entityTemplateIdentifier
        defaultFilter(fields.filter()), // filter
        fields.action(), // action (provided by caller)
        fields.name(), // name
        fields.description(), // description
        fields.entity().identifier(), // entityIdentifier
        fields.entity().name(), // entityName
        safeMap(fields.entity().properties()), // properties
        toRelationMappings(fields.entity().relations())); // relations
  }

  public EntityDynamicMappingDtoOut fromEntityMappingToDto(EntityDynamicMapping mapping) {
    return new EntityDynamicMappingDtoOut(mapping.identifier(), mapping.entityTemplateIdentifier(),
        mapping.filter(), mapping.name(), mapping.description(),
        new EntityDynamicMappingDtoOut.InboundWebhookEntityMappingDtoOut(mapping.entityIdentifier(),
            mapping.entityName(), copyNullableProperties(mapping.properties()),
            toRelationMappingDtoOut(mapping.relations())));
  }
  /// Defensive copy for outbound DTO with null safety.
  /// Returns an empty map for null input, maintaining consistency with relations
  /// handling.
  private Map<String, String> copyNullableProperties(Map<String, String> properties) {
    return properties == null ? Map.of() : Map.copyOf(properties);
  }

  /// Converts an update DTO to domain model, using the identifier from the path.
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
        fields.action(), // action (provided by caller)
        fields.name(), // name
        fields.description(), // description
        fields.entity().identifier(), // entityIdentifier
        fields.entity().name(), // entityName
        safeMap(fields.entity().properties()), // properties
        toRelationMappings(fields.entity().relations())); // relations
  }

  /// For create/dry-run payloads, a missing filter means "process everything".
  private String defaultFilter(String filter) {
    return (filter == null || filter.isBlank()) ? "true" : filter;
  }

  private Map<String, String> safeMap(Map<String, String> input) {
    return input == null ? Map.of() : Map.copyOf(input);
  }

  /// Converts inbound relation DTOs to domain RelationMapping records.
  /// Preserves declaration order; duplicates are handled by downstream
  /// validation.
  private List<RelationMapping> toRelationMappings(List<EntityDynamicMappingRelationDtoIn> input) {
    if (input == null || input.isEmpty()) {
      return List.of();
    }
    return input.stream()
        .map(dto -> new RelationMapping(dto.name(), List.copyOf(dto.targetEntityIdentifiers())))
        .toList();
  }

  /// Converts domain RelationMapping records to output DTOs.
  /// Handles null safety for relations, relation names, and
  /// targetIdentifiersExpressions.
  private List<RelationMappingDtoOut> toRelationMappingDtoOut(List<RelationMapping> relations) {
    if (relations == null || relations.isEmpty()) {
      return List.of();
    }
    return relations.stream().filter(rm -> rm != null && rm.name() != null)
        .map(rm -> new RelationMappingDtoOut(rm.name(),
            rm.targetIdentifiersExpressions() == null
                ? List.of()
                : List.copyOf(rm.targetIdentifiersExpressions())))
        .toList();
  }
}
