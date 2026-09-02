package com.decathlon.idp_core.infrastructure.adapters.entity_mapping.jslt;

import java.util.Collection;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingJsltErrorException;
import com.decathlon.idp_core.infrastructure.adapters.entity_mapping.engine.ExpressionEngine;
import com.fasterxml.jackson.databind.JsonNode;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Function;
import com.schibsted.spt.data.jslt.Parser;

@Component
public class JsltEngine implements ExpressionEngine {

  private final Collection<Function> customFunctions;

  // Creates a JSLT engine with the custom functions discovered by Spring.
  /// @param customFunctions custom JSLT functions to register
  public JsltEngine(Collection<Function> customFunctions) {
    this.customFunctions = customFunctions;
  }

  public Expression compile(String expression) {
    return Parser.compileString(expression, customFunctions);
  }

  @Override
  public void validateExpression(String expression) {
    try {
      compile(expression);
    } catch (Exception exception) {
      throw new EntityDynamicMappingJsltErrorException(exception.getMessage());
    }
  }

  @Override
  public JsonNode evaluate(String expression, JsonNode payload) {
    try {
      return compile(expression).apply(payload);
    } catch (Exception exception) {
      throw new EntityDynamicMappingJsltErrorException(exception.getMessage());
    }
  }

}
