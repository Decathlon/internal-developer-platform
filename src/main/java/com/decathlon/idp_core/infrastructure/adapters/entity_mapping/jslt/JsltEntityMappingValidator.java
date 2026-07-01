package com.decathlon.idp_core.infrastructure.adapters.entity_mapping.jslt;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.decathlon.idp_core.domain.exception.entity_mapping.EntityDynamicMappingConfigurationException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.port.EntityDynamicMapperValidator;
import com.schibsted.spt.data.jslt.JsltException;
import com.schibsted.spt.data.jslt.Parser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JsltEntityMappingValidator implements EntityDynamicMapperValidator {

  private static final Pattern LOCATION_PATTERN = Pattern
      .compile("line\\s+(\\d+),\\s+column\\s+(\\d+)");
  private static final Pattern TOKEN_PATTERN = Pattern.compile("Encountered\\s+\"([^\"]+)\"");

  @Override
  public void validate(EntityDynamicMapping mapping) {
    List<String> errors = new ArrayList<>();

    checkExpression(errors, "filter", mapping.filter());

    checkExpression(errors, "entityIdentifier", mapping.entityIdentifier());
    checkExpression(errors, "entityTitle", mapping.entityTitle());

    if (mapping.properties() != null && !mapping.properties().isEmpty()) {
      mapping.properties()
          .forEach((key, expr) -> checkExpression(errors, "properties." + key, expr));
    }
    if (mapping.relations() != null && !mapping.relations().isEmpty()) {
      mapping.relations().forEach((key, expr) -> checkExpression(errors, "relations." + key, expr));
    }

    if (!errors.isEmpty()) {
      throw new EntityDynamicMappingConfigurationException(String.format(
          "Validation failed with %d errors: %s", errors.size(), String.join(" | ", errors)));
    }
  }

  private void checkExpression(List<String> errors, String fieldName, String expression) {
    if (!StringUtils.hasText(expression)) {
      errors.add(
          String.format("Field '%s' is required and must contain a JSLT expression.", fieldName));
      return;
    }

    try {
      new Parser(new StringReader(expression)).compile();
    } catch (JsltException exception) {
      errors.add(String.format("Invalid expression for '%s': %s", fieldName,
          formatJsltErrorMessage(exception.getMessage())));
    }
  }

  private String formatJsltErrorMessage(String rawMessage) {
    if (!StringUtils.hasText(rawMessage)) {
      return "JSLT syntax error.";
    }

    String normalized = rawMessage.replaceAll("\\s+", " ").trim();
    if (normalized.startsWith("Parse error:")) {
      normalized = normalized.substring("Parse error:".length()).trim();
    }

    String line = null;
    String column = null;
    Matcher locationMatcher = LOCATION_PATTERN.matcher(rawMessage);
    if (locationMatcher.find()) {
      line = locationMatcher.group(1);
      column = locationMatcher.group(2);
    }

    String token = null;
    Matcher tokenMatcher = TOKEN_PATTERN.matcher(rawMessage);
    if (tokenMatcher.find()) {
      token = tokenMatcher.group(1);
    }

    if (line != null && column != null && token != null) {
      return String.format("JSLT syntax error at line %s, column %s (unexpected token: %s).", line,
          column, token);
    }
    if (line != null && column != null) {
      return String.format("JSLT syntax error at line %s, column %s.", line, column);
    }

    return normalized;
  }
}
