package com.decathlon.idp_core.infrastructure.adapters.entity_mapping.jslt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.constant.JsltConstraints;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingJsltErrorException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schibsted.spt.data.jslt.Function;

/// Unit tests for the DoS protections of JsltEngine: expression length and evaluation timeout.
@DisplayName("JsltEngine")
class JsltEngineTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("Should evaluate a valid expression")
  void shouldEvaluateValidExpression() throws Exception {
    var engine = new JsltEngine(List.of());
    JsonNode payload = objectMapper.readTree("{\"name\":\"repo\"}");

    JsonNode result = engine.evaluate(".name", payload);

    assertThat(result.asText()).isEqualTo("repo");
  }

  @Test
  @DisplayName("Should reject an expression longer than the maximum length on validation")
  void shouldRejectOversizedExpressionOnValidation() {
    var engine = new JsltEngine(List.of());

    assertThatThrownBy(() -> engine.validateExpression(oversizedExpression()))
        .isInstanceOf(EntityDynamicMappingJsltErrorException.class)
        .hasMessageContaining("maximum length");
  }

  @Test
  @DisplayName("Should reject an expression longer than the maximum length on evaluation")
  void shouldRejectOversizedExpressionOnEvaluation() throws Exception {
    var engine = new JsltEngine(List.of());
    JsonNode payload = objectMapper.readTree("{}");

    assertThatThrownBy(() -> engine.evaluate(oversizedExpression(), payload))
        .isInstanceOf(EntityDynamicMappingJsltErrorException.class)
        .hasMessageContaining("maximum length");
  }

  @Test
  @DisplayName("Should fail evaluation that exceeds the evaluation timeout")
  void shouldFailEvaluationExceedingTimeout() throws Exception {
    var engine = new JsltEngine(List.of(new SlowFunction()));
    JsonNode payload = objectMapper.readTree("{}");

    assertThatThrownBy(() -> engine.evaluate("slow()", payload))
        .isInstanceOf(EntityDynamicMappingJsltErrorException.class)
        .hasMessageContaining("exceeded timeout");
  }

  private static String oversizedExpression() {
    return "\"" + "a".repeat(JsltConstraints.MAX_EXPRESSION_LENGTH) + "\"";
  }

  private static final class SlowFunction implements Function {

    @Override
    public String getName() {
      return "slow";
    }

    @Override
    public int getMinArguments() {
      return 0;
    }

    @Override
    public int getMaxArguments() {
      return 0;
    }

    @Override
    public JsonNode call(JsonNode input, JsonNode[] arguments) {
      LockSupport.parkNanos(Duration.ofSeconds(30).toNanos());
      return input;
    }
  }
}
