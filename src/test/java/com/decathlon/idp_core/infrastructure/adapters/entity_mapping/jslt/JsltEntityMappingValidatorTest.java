package com.decathlon.idp_core.infrastructure.adapters.entity_mapping.jslt;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingJsltErrorException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_mapping.RelationMapping;

@DisplayName("JsltEntityMappingValidator")
class JsltEntityMappingValidatorTest {

  private JsltEntityMappingValidator validator;

  @BeforeEach
  void setUp() {
    validator = new JsltEntityMappingValidator(new JsltEngine());
  }

  // ---------------------------------------------------------------------------
  // validate
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("validate")
  class ValidateTests {

    @Test
    @DisplayName("Should pass when all JSLT expressions are valid")
    void shouldPassWhenAllExpressionsValid() {
      var mapping = buildMapping(".action == \"pushed\"", ".repository.full_name",
          ".repository.name", Map.of("applicationName", ".repository.name"),
          List.of(new RelationMapping("owner", List.of(".sender.login"))));

      assertThatCode(() -> validator.validate(mapping)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should pass when properties and relations are empty (false branch)")
    void shouldPassWhenPropertiesAndRelationsEmpty() {
      var mapping = buildMapping(".action", ".repository.full_name", ".repository.name", Map.of(),
          List.of());

      assertThatCode(() -> validator.validate(mapping)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw when the filter expression is an invalid JSLT")
    void shouldThrowWhenFilterExpressionInvalid() {
      var mapping = buildMapping("[", ".repository.full_name", ".repository.name", Map.of(),
          List.of());

      assertThatThrownBy(() -> validator.validate(mapping))
          .isInstanceOf(EntityDynamicMappingJsltErrorException.class)
          .hasMessageContaining("Validation failed with").hasMessageContaining("filter");
    }

    @Test
    @DisplayName("Should report a required-field error when a property expression is blank")
    void shouldReportRequiredErrorWhenPropertyExpressionBlank() {
      var properties = new HashMap<String, String>();
      properties.put("applicationName", "   ");

      var mapping = buildMapping(".action", ".repository.full_name", ".repository.name", properties,
          List.of());

      assertThatThrownBy(() -> validator.validate(mapping))
          .isInstanceOf(EntityDynamicMappingJsltErrorException.class)
          .hasMessageContaining("properties.applicationName")
          .hasMessageContaining("is required and must contain an expression");
    }

    @Test
    @DisplayName("Should report an error when a relation expression is invalid JSLT")
    void shouldReportErrorWhenRelationExpressionInvalid() {
      var mapping = buildMapping(".action", ".repository.full_name", ".repository.name", Map.of(),
          List.of(new RelationMapping("owner", List.of("["))));

      assertThatThrownBy(() -> validator.validate(mapping))
          .isInstanceOf(EntityDynamicMappingJsltErrorException.class)
          .hasMessageContaining("relations.owner");
    }

    @Test
    @DisplayName("Should aggregate multiple errors into a single exception message")
    void shouldAggregateMultipleErrors() {
      var properties = new HashMap<String, String>();
      properties.put("applicationName", "   ");

      var mapping = buildMapping("[", ".repository.full_name", ".repository.name", properties,
          List.of(new RelationMapping("owner", List.of("["))));

      assertThatThrownBy(() -> validator.validate(mapping))
          .isInstanceOf(EntityDynamicMappingJsltErrorException.class)
          .hasMessageContaining("Validation failed with 3 errors");
    }
  }

  // ---------------------------------------------------------------------------
  // formatErrorMessage (private — exercised through reflection)
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("formatErrorMessage")
  class FormatJsltErrorMessageTests {

    @Test
    @DisplayName("Should return generic message when raw message is null")
    void shouldReturnGenericMessageWhenNull() {
      assertThat(formatJsltErrorMessage(null)).isEqualTo("Expression syntax error.");
    }

    @Test
    @DisplayName("Should return generic message when raw message is blank")
    void shouldReturnGenericMessageWhenBlank() {
      assertThat(formatJsltErrorMessage("   ")).isEqualTo("Expression syntax error.");
    }

    @Test
    @DisplayName("Should strip the 'Parse error:' prefix and normalize whitespace")
    void shouldStripParseErrorPrefixAndNormalize() {
      assertThat(formatJsltErrorMessage("Parse error:   something    went   wrong"))
          .isEqualTo("something went wrong");
    }

    @Test
    @DisplayName("Should format message with line, column and token when all present")
    void shouldFormatWithLineColumnAndToken() {
      var raw = "Parse error: at line 3, column 7 Encountered \"}\" but expected something";

      assertThat(formatJsltErrorMessage(raw))
          .isEqualTo("Syntax error at line 3, column 7 (unexpected token: }).");
    }

    @Test
    @DisplayName("Should format message with line and column only when token absent")
    void shouldFormatWithLineColumnOnly() {
      var raw = "Unexpected failure at line 5, column 2 in the expression";

      assertThat(formatJsltErrorMessage(raw)).isEqualTo("Syntax error at line 5, column 2.");
    }

    @Test
    @DisplayName("Should fall back to normalized message when no location is present")
    void shouldFallBackToNormalizedMessage() {
      assertThat(formatJsltErrorMessage("Totally   unexpected   message"))
          .isEqualTo("Totally unexpected message");
    }
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private EntityDynamicMapping buildMapping(String filter, String entityIdentifier,
      String entityTitle, Map<String, String> properties, List<RelationMapping> relations) {
    return new EntityDynamicMapping(UUID.randomUUID(), "my-mapping", "microservice", filter,
        "My Mapping", "description", entityIdentifier, entityTitle, properties, relations);
  }

  /// Invokes the private `formatErrorMessage` method through reflection so
  /// every formatting branch can be exercised deterministically.
  private String formatJsltErrorMessage(String rawMessage) {
    try {
      Method method = JsltEntityMappingValidator.class.getDeclaredMethod("formatErrorMessage",
          String.class);
      method.setAccessible(true);
      return (String) method.invoke(validator, rawMessage);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Unable to invoke formatErrorMessage", e);
    }
  }
}
