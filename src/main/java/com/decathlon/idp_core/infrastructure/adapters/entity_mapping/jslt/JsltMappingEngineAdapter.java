package com.decathlon.idp_core.infrastructure.adapters.entity_mapping.jslt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingConfigurationException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_mapping.RelationMapping;
import com.decathlon.idp_core.domain.port.MappingEnginePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/// JSLT implementation of MappingEnginePort.
///
/// Responsibilities:
/// - Parse raw JSON payloads.
/// - Apply mapping filter and projection expressions.
/// - Build domain Entity instances from evaluated values.
/// - Delegate expression resolution and payload traversal to JsltExpressionEvaluator.
@Slf4j
@Component
@RequiredArgsConstructor
public class JsltMappingEngineAdapter implements MappingEnginePort {

  private final JsltEngine jsltEngine;
  private final ObjectMapper objectMapper;
  private final JsltExpressionEvaluator jsltEvaluator;

  /// Maps a raw payload to one domain entity, or returns `null` when the filter
  /// excludes the payload.
  @Override
  public Entity mapToEntity(String rawPayload, EntityDynamicMapping mapping) {
    JsonNode rootPayload = parsePayload(rawPayload);
    return mapRootPayloadToEntity(rootPayload, mapping);
  }

  /// Applies the filter and maps one payload item to an Entity.
  /// Returns null when the filter evaluates to false/null (skipped item).
  private Entity mapRootPayloadToEntity(JsonNode rootPayload, EntityDynamicMapping mapping) {
    JsonNode filterResult = jsltEngine.evaluate(mapping.filter(), rootPayload);
    if (filterResult.isNull() || (filterResult.isBoolean() && !filterResult.asBoolean())) {
      log.debug("Filter expression returned false/null, skipping item for template: {}",
          mapping.entityTemplateIdentifier());
      return null;
    }
    return extractEntity(rootPayload, rootPayload, mapping);
  }

  /// Extracts one domain Entity from a payload node using mapping expressions.
  private Entity extractEntity(JsonNode currentNode, JsonNode rootPayload,
      EntityDynamicMapping mapping) {
    String identifier = requireStringValue(
        jsltEvaluator.resolveExpression(mapping.entityIdentifier(), currentNode, rootPayload),
        "entityIdentifier");

    String title = requireStringValue(
        jsltEvaluator.resolveExpression(mapping.entityName(), currentNode, rootPayload),
        "entityName");

    List<Property> properties = extractEntityProperties(currentNode, rootPayload,
        mapping.properties());
    List<Relation> relations = extractEntityRelations(currentNode, rootPayload,
        mapping.relations());

    return new Entity(null, mapping.entityTemplateIdentifier(), title, identifier, properties,
        relations);
  }

  /// Extracts all mapped relations for one entity node.
  /// Null or missing values are excluded from the output list.
  private List<Relation> extractEntityRelations(JsonNode currentNode, JsonNode rootPayload,
      List<RelationMapping> relationMappings) {
    if (relationMappings == null || relationMappings.isEmpty()) {
      return List.of();
    }
    return relationMappings.stream().map(rm -> extractRelation(rm, currentNode, rootPayload))
        .flatMap(Stream::ofNullable).toList();
  }

  /// Extracts all mapped properties for one entity node.
  /// Null/missing values are excluded from the output list.
  private List<Property> extractEntityProperties(JsonNode currentNode, JsonNode rootPayload,
      Map<String, String> propertyExpressions) {
    if (propertyExpressions == null || propertyExpressions.isEmpty()) {
      return List.of();
    }
    return propertyExpressions.entrySet().stream()
        .map(entry -> extractProperty(entry, currentNode, rootPayload)).flatMap(Stream::ofNullable)
        .toList();
  }

  /// Extracts one Property from a property expression entry.
  /// Returns null when the expression evaluates to null/missing.
  private Property extractProperty(Map.Entry<String, String> entry, JsonNode currentNode,
      JsonNode rootPayload) {
    JsonNode valueNode = jsltEvaluator.resolveExpression(entry.getValue(), currentNode,
        rootPayload);
    if (valueNode != null && !valueNode.isNull() && !valueNode.isMissingNode()) {
      String value = valueNode.isTextual() ? valueNode.asText() : valueNode.toString();
      return new Property(null, entry.getKey(), value);
    }
    return null;
  }

  /// Extracts one Relation from a relation mapping entry.
  /// Evaluates each expression in the list and combines all target identifiers.
  private Relation extractRelation(RelationMapping relationMapping, JsonNode currentNode,
      JsonNode rootPayload) {
    List<String> allTargetIdentifiers = new ArrayList<>();

    if (relationMapping.expressions() != null && !relationMapping.expressions().isEmpty()) {
      for (String expression : relationMapping.expressions()) {
        JsonNode valueNode = jsltEvaluator.resolveExpression(expression, currentNode, rootPayload);
        List<String> targetIdentifiers = extractTargetEntityIdentifiers(valueNode);
        allTargetIdentifiers.addAll(targetIdentifiers);
      }
    }

    if (allTargetIdentifiers.isEmpty()) {
      return null;
    }

    return new Relation(null, relationMapping.name(), null, allTargetIdentifiers);
  }

  /// Extracts relation target identifiers from a scalar or array result node.
  private List<String> extractTargetEntityIdentifiers(JsonNode valueNode) {
    if (valueNode == null || valueNode.isNull() || valueNode.isMissingNode()) {
      return List.of();
    }

    if (valueNode.isArray()) {
      return StreamSupport.stream(valueNode.spliterator(), false)
          .filter(node -> !node.isNull() && !node.isMissingNode())
          .map(this::toRelationTargetIdentifier).filter(identifier -> !identifier.isBlank())
          .toList();
    }

    String targetIdentifier = toRelationTargetIdentifier(valueNode);
    return targetIdentifier.isBlank() ? List.of() : List.of(targetIdentifier);
  }

  private String toRelationTargetIdentifier(JsonNode valueNode) {
    if (valueNode.isTextual()) {
      return valueNode.asText();
    }

    // Support relation payload entries provided as objects containing an identifier
    // field.
    if (valueNode.isObject()) {
      JsonNode identifierNode = valueNode.get("identifier");
      if (identifierNode != null && identifierNode.isTextual()) {
        return identifierNode.asText();
      }
      return "";
    }

    return valueNode.toString();
  }

  /// Returns node text value and fails when node is null or missing.
  private String requireStringValue(JsonNode node, String fieldName) {
    if (node == null || node.isNull() || node.isMissingNode()) {
      throw new EntityDynamicMappingConfigurationException(
          "Expression for '" + fieldName + "' returned null");
    }
    return node.asText();
  }

  /// Extracts mapped properties from a payload using property expressions.
  /// Reuses extractEntityProperties and excludes null/missing values.
  @Override
  public Map<String, Object> extractProperties(String rawPayload,
      Map<String, String> propertyExpressions) {
    if (propertyExpressions == null || propertyExpressions.isEmpty()) {
      return Map.of();
    }
    JsonNode rootPayload = parsePayload(rawPayload);
    return extractEntityProperties(rootPayload, rootPayload, propertyExpressions).stream()
        .collect(Collectors.toUnmodifiableMap(Property::name, Property::value));
  }

  /// Parses raw JSON and translates parsing failures to a domain exception.
  private JsonNode parsePayload(String rawPayload) {
    try {
      return objectMapper.readTree(rawPayload);
    } catch (JsonProcessingException e) {
      throw new EntityDynamicMappingConfigurationException("Failed to parse JSON payload", e);
    }
  }
}
