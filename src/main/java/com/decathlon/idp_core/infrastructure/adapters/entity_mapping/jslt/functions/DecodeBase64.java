package com.decathlon.idp_core.infrastructure.adapters.entity_mapping.jslt.functions;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.schibsted.spt.data.jslt.Function;

/// JSLT custom function that decodes a Base64-encoded string from a input
/// payload.
///
/// **Usage in JSLT:** `DecodeBase64(<base64-encoded-string>)`
///
/// Returns `null` if the input is absent, null, or blank.
@Component
public final class DecodeBase64 implements Function {

  public static final String FUNCTION_NAME = "base64-decode";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  // Accepts exactly one argument: the Base64-encoded string node
  private static final int MIN_ARGUMENTS = 1;
  private static final int MAX_ARGUMENTS = 1;

  @Override
  public String getName() {
    return FUNCTION_NAME;
  }

  @Override
  public int getMinArguments() {
    return MIN_ARGUMENTS;
  }

  @Override
  public int getMaxArguments() {
    return MAX_ARGUMENTS;
  }

  /// Decodes a Base64-encoded `JsonNode` string argument.
  ///
  /// **Behavior:**
  /// - Returns `NullNode` if the input is absent, null, or blank
  /// - Attempts to parse the decoded string as JSON; falls back to plain text if parsing fails
  /// - Throws `IllegalArgumentException` if the argument is non-textual (e.g., number, object, array)
  /// - Throws `IllegalArgumentException` if the Base64 string is malformed
  ///
  /// @param input the JSLT input context (unused)
  /// @param args array of JsonNode arguments; expects exactly one text node containing Base64-encoded data
  /// @return a `JsonNode` (either parsed JSON, plain `TextNode`, or `NullNode`)
  /// @throws IllegalArgumentException if arg is not textual or contains invalid Base64
  @Override
  public JsonNode call(JsonNode input, JsonNode[] args) {
    if (args == null || args.length == 0) {
      return NullNode.getInstance();
    }

    JsonNode arg = args[0];

    // 1. Graceful: Null or absent inputs return NullNode
    if (arg == null || arg.isNull()) {
      return NullNode.getInstance();
    }

    // 2. Strict: Non-textual types (Numbers, Objects, Arrays) throw an exception
    if (!arg.isTextual()) {
      throw new IllegalArgumentException(
          "DecodeBase64 expects a string argument, but received: " + arg.getNodeType());
    }

    // 3. Graceful: Blank strings return NullNode
    String textValue = arg.asText();
    if (textValue.isBlank()) {
      return NullNode.getInstance();
    }

    // 4. Strict: Malformed Base64 strings throw an exception
    try {
      byte[] decodedBytes = Base64.getDecoder().decode(textValue);
      String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
      return parseDecodedString(decodedString);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid Base64 string payload: '" + textValue + "'", e);
    }
  }

  private JsonNode parseDecodedString(String decodedString) {
    // Try parsing as structured JSON first
    try {
      JsonNode parsed = OBJECT_MAPPER.readTree(decodedString);
      // Jackson returns Java null if decodedString is empty ("")
      return (parsed != null) ? parsed : TextNode.valueOf(decodedString);
    } catch (JsonProcessingException _) {
      // Fallback to plain TextNode if the decoded string is not JSON
      return TextNode.valueOf(decodedString);
    }
  }
}