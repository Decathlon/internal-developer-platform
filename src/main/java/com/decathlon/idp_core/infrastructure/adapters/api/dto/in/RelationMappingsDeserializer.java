package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

/// Deserializes `relations` as a JSON object map or array.
///
/// **Object format:** `{ "owner": [".sender.login"] }`
/// or with single expression: `{ "owner": ".sender.login" }`
///
/// **Array format:** `[{ "name": "owner", "target_entity_identifiers": [".sender.login"] }]`
public class RelationMappingsDeserializer extends JsonDeserializer<List<RelationMappingDtoIn>> {

  private static final String FIELD_NAME = "name";
  private static final String FIELD_TARGET_ENTITY_IDENTIFIERS = "target_entity_identifiers";

  private static final String MESSAGE_RELATIONS_MUST_BE_OBJECT_OR_ARRAY = "'relations' must be either an object map or an array of {name, target_entity_identifiers}";
  private static final String MESSAGE_RELATION_EXPRESSIONS_MUST_NOT_BE_NULL = "Relation expressions for '%s' must not be null";
  private static final String MESSAGE_RELATION_ENTRY_MUST_CONTAIN_NAME = "Each relation entry must contain a non-blank string field 'name'";
  private static final String MESSAGE_RELATION_ENTRY_MUST_CONTAIN_TARGETS = "Each relation entry must contain a 'target_entity_identifiers' field";
  private static final String MESSAGE_RELATION_EXPRESSION_MUST_BE_NON_BLANK = "Expression for relation '%s' must be a non-blank string";
  private static final String MESSAGE_EACH_RELATION_EXPRESSION_MUST_BE_NON_BLANK = "Each expression in relation '%s' must be a non-blank string";
  private static final String MESSAGE_RELATION_MUST_HAVE_AT_LEAST_ONE_EXPRESSION = "Relation '%s' must have at least one expression";
  private static final String MESSAGE_RELATION_TARGETS_INVALID_TYPE = "Field 'target_entity_identifiers' for relation '%s' must be either a string or an array of strings";

  @Override
  public List<RelationMappingDtoIn> deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    JsonNode node = parser.getCodec().readTree(parser);

    if (node == null || node.isNull()) {
      return List.of();
    }

    if (node.isObject()) {
      return parseObjectFormat(node, parser);
    }

    if (node.isArray()) {
      return parseArrayFormat(node, parser);
    }

    throw MismatchedInputException.from(parser, List.class,
        MESSAGE_RELATIONS_MUST_BE_OBJECT_OR_ARRAY);
  }

  private List<RelationMappingDtoIn> parseObjectFormat(JsonNode node, JsonParser parser)
      throws IOException {
    Map<String, List<String>> relations = new LinkedHashMap<>();

    for (var entry : node.properties()) {
      relations.put(entry.getKey(), parseObjectRelationExpressions(entry, parser));
    }

    return relations.entrySet().stream()
        .map(entry -> new RelationMappingDtoIn(entry.getKey(), entry.getValue())).toList();
  }

  private List<RelationMappingDtoIn> parseArrayFormat(JsonNode node, JsonParser parser)
      throws IOException {
    List<RelationMappingDtoIn> relations = new ArrayList<>();

    for (JsonNode relationNode : node) {
      JsonNode nameNode = relationNode.get(FIELD_NAME);
      JsonNode expressionsNode = relationNode.get(FIELD_TARGET_ENTITY_IDENTIFIERS);

      if (nameNode == null || !nameNode.isTextual() || nameNode.asText().isBlank()) {
        throw MismatchedInputException.from(parser, String.class,
            MESSAGE_RELATION_ENTRY_MUST_CONTAIN_NAME);
      }
      if (expressionsNode == null || expressionsNode.isNull()) {
        throw MismatchedInputException.from(parser, String.class,
            MESSAGE_RELATION_ENTRY_MUST_CONTAIN_TARGETS);
      }

      List<String> expressions = parseExpressions(expressionsNode, parser, nameNode.asText());
      relations.add(new RelationMappingDtoIn(nameNode.asText(), expressions));
    }

    return List.copyOf(relations);
  }

  private List<String> parseObjectRelationExpressions(Map.Entry<String, JsonNode> relationEntry,
      JsonParser parser) throws IOException {
    String relationName = relationEntry.getKey();
    JsonNode expressionsNode = relationEntry.getValue();

    if (expressionsNode == null || expressionsNode.isNull()) {
      throw MismatchedInputException.from(parser, String.class,
          MESSAGE_RELATION_EXPRESSIONS_MUST_NOT_BE_NULL.formatted(relationName));
    }

    return parseExpressions(expressionsNode, parser, relationName);
  }

  /**
   * Dispatches expression parsing to the appropriate handler based on the JSON
   * node type. Accepts either a single string or an array of strings.
   */
  private List<String> parseExpressions(JsonNode expressionsNode, JsonParser parser,
      String relationName) throws IOException {
    if (expressionsNode.isTextual()) {
      return parseSingleExpression(expressionsNode, parser, relationName);
    }
    if (expressionsNode.isArray()) {
      return parseArrayExpressions(expressionsNode, parser, relationName);
    }
    throw MismatchedInputException.from(parser, String.class,
        MESSAGE_RELATION_TARGETS_INVALID_TYPE.formatted(relationName));
  }

  /**
   * Parses a single string expression, rejecting blank values.
   */
  private List<String> parseSingleExpression(JsonNode expressionsNode, JsonParser parser,
      String relationName) throws IOException {
    String expr = expressionsNode.asText();
    if (expr.isBlank()) {
      throw MismatchedInputException.from(parser, String.class,
          MESSAGE_RELATION_EXPRESSION_MUST_BE_NON_BLANK.formatted(relationName));
    }
    return List.of(expr);
  }

  /**
   * Parses an array of string expressions, rejecting blank items and empty
   * arrays.
   */
  private List<String> parseArrayExpressions(JsonNode expressionsNode, JsonParser parser,
      String relationName) throws IOException {
    List<String> expressions = new ArrayList<>();
    for (JsonNode exprNode : expressionsNode) {
      if (exprNode == null || !exprNode.isTextual() || exprNode.asText().isBlank()) {
        throw MismatchedInputException.from(parser, String.class,
            MESSAGE_EACH_RELATION_EXPRESSION_MUST_BE_NON_BLANK.formatted(relationName));
      }
      expressions.add(exprNode.asText());
    }
    if (expressions.isEmpty()) {
      throw MismatchedInputException.from(parser, String.class,
          MESSAGE_RELATION_MUST_HAVE_AT_LEAST_ONE_EXPRESSION.formatted(relationName));
    }
    return List.copyOf(expressions);
  }
}
