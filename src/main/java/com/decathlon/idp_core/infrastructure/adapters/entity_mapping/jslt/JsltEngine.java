package com.decathlon.idp_core.infrastructure.adapters.entity_mapping.jslt;

import java.io.StringReader;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingJsltErrorException;
import com.decathlon.idp_core.infrastructure.adapters.entity_mapping.engine.ExpressionEngine;
import com.fasterxml.jackson.databind.JsonNode;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.JsltException;
import com.schibsted.spt.data.jslt.Parser;

@Component
public class JsltEngine implements ExpressionEngine {

  public Expression compile(String expression) {
    return new Parser(new StringReader(expression)).compile();
  }

  @Override
  public void validateExpression(String expression) {
    try {
      compile(expression);
    } catch (JsltException exception) {
      throw new EntityDynamicMappingJsltErrorException(exception.getMessage());
    }
  }

  @Override
  public JsonNode evaluate(String expression, JsonNode payload) {
    try {
      return compile(expression).apply(payload);
    } catch (JsltException exception) {
      throw new EntityDynamicMappingJsltErrorException(exception.getMessage());
    }
  }

}
