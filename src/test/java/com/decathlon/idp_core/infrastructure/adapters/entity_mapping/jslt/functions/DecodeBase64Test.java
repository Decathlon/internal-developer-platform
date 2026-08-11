package com.decathlon.idp_core.infrastructure.adapters.entity_mapping.jslt.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingJsltErrorException;
import com.decathlon.idp_core.infrastructure.adapters.entity_mapping.jslt.JsltEngine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;

// Unit test for the DecodeBase64 JSLT custom function, 
// covering direct invocation and integration with the JsltEngine.
@DisplayName("DecodeBase64")
class DecodeBase64Test {

  private DecodeBase64 decodeBase64;
  private JsltEngine jsltEngine;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    decodeBase64 = new DecodeBase64();
    jsltEngine = new JsltEngine(List.of(decodeBase64));
    objectMapper = new ObjectMapper();
  }

  @Test
  @DisplayName("Should return correct metadata signature")
  void testMetadata() {
    assertEquals("base64-decode", decodeBase64.getName());
    assertEquals(1, decodeBase64.getMinArguments());
    assertEquals(1, decodeBase64.getMaxArguments());
  }

  @Test
  @DisplayName("Should successfully decode a valid Base64 string directly")
  void testDirectDecodeValidBase64() {
    // "SGVsbG8gV29ybGQ=" is Base64 for "Hello World"
    JsonNode context = NullNode.getInstance();
    JsonNode[] args = new JsonNode[]{TextNode.valueOf("SGVsbG8gV29ybGQ=")};

    JsonNode result = decodeBase64.call(context, args);

    assertNotNull(result);
    assertTrue(result.isTextual());
    assertEquals("Hello World", result.asText());
  }

  @Test
  @DisplayName("Should gracefully return NullNode for null, absent, or blank inputs")
  void testDirectDecodeGracefulNullAndBlank() {
    JsonNode context = NullNode.getInstance();

    // Null array element
    JsonNode[] nullElementArgs = new JsonNode[]{null};
    assertEquals(NullNode.getInstance(), decodeBase64.call(context, nullElementArgs));

    // NullNode instance
    JsonNode[] nullNodeArgs = new JsonNode[]{NullNode.getInstance()};
    assertEquals(NullNode.getInstance(), decodeBase64.call(context, nullNodeArgs));

    // Blank string
    JsonNode[] blankStringArgs = new JsonNode[]{TextNode.valueOf("   ")};
    assertEquals(NullNode.getInstance(), decodeBase64.call(context, blankStringArgs));
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when input is not a textual type")
  void testDirectDecodeWrongTypeThrowsException() {
    JsonNode context = NullNode.getInstance();
    JsonNode[] nonTextualArgs = new JsonNode[]{new IntNode(123)};

    assertThrows(IllegalArgumentException.class, () -> decodeBase64.call(context, nonTextualArgs));
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when string payload is malformed Base64")
  void testDirectDecodeMalformedBase64ThrowsException() {
    JsonNode context = NullNode.getInstance();
    JsonNode[] malformedBase64Args = new JsonNode[]{TextNode.valueOf("NotValidBase64!!!")};

    assertThrows(IllegalArgumentException.class,
        () -> decodeBase64.call(context, malformedBase64Args));
  }

  @Test
  @DisplayName("Should execute DecodeBase64 function inside JSLT engine evaluation successfully")
  void testJsltEngineIntegrationSuccess() throws Exception {
    String jsltExpression = "{ \"decoded\": base64-decode(.encoded_data) }";
    JsonNode payload = objectMapper.readTree("{\"encoded_data\": \"SGVsbG8=\"}"); // "SGVsbG8=" ->
                                                                                  // "Hello"

    JsonNode result = jsltEngine.evaluate(jsltExpression, payload);

    assertNotNull(result);
    assertNotNull(result.get("decoded"));
    assertTrue(result.get("decoded").isTextual());
    assertEquals("Hello", result.get("decoded").asText());
  }

  @Test
  @DisplayName("Should wrap Base64 decoding exception in EntityDynamicMappingJsltErrorException during engine evaluation")
  void testJsltEngineIntegrationFailure() throws Exception {
    String jsltExpression = "{ \"decoded\": base64-decode(.encoded_data) }";
    JsonNode payloadWithInvalidBase64 = objectMapper
        .readTree("{\"encoded_data\": \"NotValidBase64!!!\"}");

    assertThrows(EntityDynamicMappingJsltErrorException.class,
        () -> jsltEngine.evaluate(jsltExpression, payloadWithInvalidBase64));
  }
}
