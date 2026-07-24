package com.decathlon.idp_core.infrastructure.adapters.entity_mapping.jslt;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingConfigurationException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingJsltErrorException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.port.EntityDynamicMapperValidator;
import com.decathlon.idp_core.infrastructure.adapters.entity_mapping.engine.ExpressionEngine;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JsltEntityMappingValidator implements EntityDynamicMapperValidator {

  private static final Pattern LOCATION_PATTERN = Pattern
      .compile("line\\s+(\\d+),\\s+column\\s+(\\d+)");
  private static final Pattern TOKEN_PATTERN = Pattern.compile("Encountered\\s+\"([^\"]+)\"");

  private final ExpressionEngine expressionEngine;

  @Override
  public void validate(EntityDynamicMapping mapping) {
    List<String> errors = new ArrayList<>();

    checkExpression(errors, "filter", mapping.filter());
    checkExpression(errors, "entityIdentifier", mapping.entityIdentifier());
    checkExpression(errors, "entityName", mapping.entityName());

    if (mapping.properties() != null && !mapping.properties().isEmpty()) {
      mapping.properties()
          .forEach((key, expr) -> checkExpression(errors, "properties." + key, expr));
    }
    if (mapping.relations() != null && !mapping.relations().isEmpty()) {
      mapping.relations().forEach(relation -> {
        if (relation.expressions() != null && !relation.expressions().isEmpty()) {
          relation.expressions().forEach(
              expr -> checkExpression(errors, "relations." + relation.name() + "[]", expr));
        }
      });
    }

    if (!errors.isEmpty()) {
      throw new EntityDynamicMappingJsltErrorException(String.format(
          "Validation failed with %d errors: %s", errors.size(), String.join(" | ", errors)));
    }
  }

  /**
   * Validates a single expression field via
   * {@link ExpressionEngine#validateExpression(String)}. Catches engine-specific
   * exceptions and formats them into user-friendly messages.
   *
   * @param errors
   *          accumulator for validation errors
   * @param fieldName
   *          human-readable field name for error messages
   * @param expression
   *          the expression to validate
   */
  private void checkExpression(List<String> errors, String fieldName, String expression) {
    if (!StringUtils.hasText(expression)) {
      errors
          .add(String.format("Field '%s' is required and must contain an expression.", fieldName));
      return;
    }

    try {
      // Goes through the ExpressionEngine port — works for JSLT today, JQ tomorrow.
      expressionEngine.validateExpression(expression);
    } catch (EntityDynamicMappingConfigurationException exception) {
      errors.add(String.format("Invalid expression for '%s': %s", fieldName,
          formatErrorMessage(exception.getMessage())));
    }
  }

  private String formatErrorMessage(String rawMessage) {
    if (!StringUtils.hasText(rawMessage)) {
      return "Expression syntax error.";
    }

    String normalized = rawMessage.replaceAll("\\s+", " ").trim();
    if (normalized.startsWith("Parse error:")) {
      normalized = normalized.substring("Parse error:".length()).trim();
    }

    Matcher locationMatcher = LOCATION_PATTERN.matcher(rawMessage);
    String line = null;
    String column = null;
    if (locationMatcher.find()) {
      line = locationMatcher.group(1);
      column = locationMatcher.group(2);
    }

    Matcher tokenMatcher = TOKEN_PATTERN.matcher(rawMessage);
    String token = null;
    if (tokenMatcher.find()) {
      token = tokenMatcher.group(1);
    }

    if (line != null && column != null && token != null) {
      return String.format("Syntax error at line %s, column %s (unexpected token: %s).", line,
          column, token);
    }
    if (line != null && column != null) {
      return String.format("Syntax error at line %s, column %s.", line, column);
    }

    return normalized;
  }
}
