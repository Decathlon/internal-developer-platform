package com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.common;

import java.util.Map;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/// Technical helper for JSONB serialization/deserialization in the persistence layer.
///
/// Provides named conversion methods used by [com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.EntityDynamicMappingPersistenceMapper]
/// via MapStruct's `qualifiedByName` annotation.
///
/// This is a pure utility class with no Spring dependencies, facilitating testability and reusability.
@Component
public class EntityDynamicMappingJsonbHelper {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /// Converts JSONB string to `Map<String, String>`.
  /// Used when loading from database.
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
  /// Used when persisting to database.
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
}
