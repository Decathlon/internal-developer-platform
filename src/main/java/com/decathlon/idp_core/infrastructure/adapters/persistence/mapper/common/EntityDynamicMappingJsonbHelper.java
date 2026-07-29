package com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.common;

import java.util.List;
import java.util.Map;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity_mapping.RelationMapping;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
      return OBJECT_MAPPER.readValue(json, new TypeReference<>() {
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
  /// Expected format: `[{"name": "owner", "expressions": [".sender.login"]}]`
  ///
  /// Backward compatibility:
  /// - Legacy entries using singular `expression` are normalized to
  /// `expressions: [<expression>]` before deserialization.
  /// - Legacy pre-migration object format `{"relation":"<expression>"}` is
  /// also supported.
  ///
  /// Handles edge cases:
  /// - Java `null` or empty/blank string → returns empty list.
  /// - JSON `null` literal (e.g. PostgreSQL `NULL` cast to text) → returns empty
  /// list.
  ///
  /// Used when loading relations from database.
  @Named("jsonStringToRelationList")
  public List<RelationMapping> toRelationList(String json) {
    if (json == null || json.trim().isEmpty() || "null".equalsIgnoreCase(json.trim())) {
      return List.of();
    }
    try {
      JsonNode relationsNode = OBJECT_MAPPER.readTree(json);
      if (relationsNode == null || relationsNode.isNull()) {
        return List.of();
      }

      if (relationsNode.isObject()) {
        Map<String, String> legacyMap = OBJECT_MAPPER.convertValue(relationsNode,
            new TypeReference<>() {
            });
        return legacyMap.entrySet().stream()
            .map(entry -> new RelationMapping(entry.getKey(), List.of(entry.getValue()))).toList();
      }

      if (!relationsNode.isArray()) {
        throw new IllegalArgumentException("Invalid JSON relation list configuration");
      }

      ArrayNode normalizedRelations = OBJECT_MAPPER.createArrayNode();
      relationsNode.forEach(
          relationNode -> normalizedRelations.add(normalizeLegacyRelationNode(relationNode)));

      return OBJECT_MAPPER.readValue(OBJECT_MAPPER.writeValueAsString(normalizedRelations),
          new TypeReference<>() {
          });
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid JSON relation list configuration", e);
    }
  }

  private JsonNode normalizeLegacyRelationNode(JsonNode relationNode) {
    if (!(relationNode instanceof ObjectNode objectNode)) {
      return relationNode;
    }

    if (objectNode.has("expressions") || !objectNode.has("expression")) {
      return objectNode;
    }

    ObjectNode normalizedNode = objectNode.deepCopy();
    JsonNode legacyExpression = normalizedNode.remove("expression");

    ArrayNode expressionsNode = OBJECT_MAPPER.createArrayNode();
    if (legacyExpression != null && !legacyExpression.isNull()) {
      if (legacyExpression.isArray()) {
        legacyExpression.forEach(expressionsNode::add);
      } else {
        expressionsNode.add(legacyExpression.asText());
      }
    }

    normalizedNode.set("expressions", expressionsNode);
    return normalizedNode;
  }

  /// Converts `List<RelationMapping>` to JSONB array string.
  ///
  /// Output format: `[{"name": "owner", "expressions": [".sender.login"]}]`
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
