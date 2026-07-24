package com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.common;

import java.util.List;
import java.util.Map;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity_mapping.RelationMapping;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/// Technical helper for JSONB serialization/deserialization in the persistence layer.
///
/// Provides named conversion methods used by [com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.EntityDynamicMappingPersistenceMapper]
/// via MapStruct's `qualifiedByName` annotation.
///
/// - Properties are serialized as a flat JSON object: {"key": "expression"}
/// - Relations are serialized as a JSON array: [{"name": "owner", "expression": ".sender.login"}]
///
/// This is a pure utility class with no Spring dependencies, facilitating testability and reusability.
@Component
public class EntityDynamicMappingJsonbHelper {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /// Converts JSONB string to `Map<String, String>`.
  /// Used when loading properties from database.
  @Named("jsonStringToMap")
  public Map<String, String> toMap(String json) {
    if (json == null || json.trim().isEmpty()) {
      return Map.of();
    }
    try {
      return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, String>>() {
      });
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid JSON mapping configuration", e);
    }
  }

  /// Converts `Map<String, String>` to JSONB string.
  /// Used when persisting properties to database.
  @Named("mapToJsonString")
  public String toJsonString(Map<String, String> map) {
    if (map == null || map.isEmpty()) {
      return "{}";
    }
    try {
      return OBJECT_MAPPER.writeValueAsString(map);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Unable to serialize mapping configuration", e);
    }
  }

  /// Converts JSONB array string to `List<RelationMapping>`.
  ///
  /// Expected format: `[{"name": "owner", "expression": ".sender.login"}]`
  ///
  /// Used when loading relations from database.
  @Named("jsonStringToRelationList")
  public List<RelationMapping> toRelationList(String json) {
    if (json == null || json.trim().isEmpty()) {
      return List.of();
    }
    try {
      return OBJECT_MAPPER.readValue(json, new TypeReference<List<RelationMapping>>() {
      });
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid JSON relation list configuration", e);
    }
  }

  /// Converts `List<RelationMapping>` to JSONB array string.
  ///
  /// Output format: `[{"name": "owner", "expression": ".sender.login"}]`
  ///
  /// Used when persisting relations to database.
  @Named("relationListToJsonString")
  public String toRelationJsonString(List<RelationMapping> relations) {
    if (relations == null || relations.isEmpty()) {
      return "[]";
    }
    try {
      return OBJECT_MAPPER.writeValueAsString(relations);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Unable to serialize relation list configuration", e);
    }
  }
}
