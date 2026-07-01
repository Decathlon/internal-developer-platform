package com.decathlon.idp_core.infrastructure.adapters.entity_mapping.jslt;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.exception.entity_mapping.EntityDynamicMappingConfigurationException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;

@DisplayName("JsltEntityMappingValidator")
class JsltEntityMappingValidatorTest {

  private JsltEntityMappingValidator validator;

  @BeforeEach
  void setUp() {
    validator = new JsltEntityMappingValidator();
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
          Map.of("owner", ".sender.login"));

      assertThatCode(() -> validator.validate(mapping)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should pass when properties and relations are empty (false branch)")
    void shouldPassWhenPropertiesAndRelationsEmpty() {
      var mapping = buildMapping(".action", ".repository.full_name", ".repository.name", Map.of(),
          Map.of());

      assertThatCode(() -> validator.validate(mapping)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw when the filter expression is an invalid JSLT")
    void shouldThrowWhenFilterExpressionInvalid() {
      var mapping = buildMapping("[", ".repository.full_name", ".repository.name", Map.of(),
          Map.of());

      assertThatThrownBy(() -> validator.validate(mapping))
          .isInstanceOf(EntityDynamicMappingConfigurationException.class)
          .hasMessageContaining("Validation failed with").hasMessageContaining("filter");
    }

    @Test
    @DisplayName("Should report a required-field error when a property expression is blank")
    void shouldReportRequiredErrorWhenPropertyExpressionBlank() {
      var properties = new HashMap<String, String>();
      properties.put("applicationName", "   ");

      var mapping = buildMapping(".action", ".repository.full_name", ".repository.name", properties,
          Map.of());

      assertThatThrownBy(() -> validator.validate(mapping))
          .isInstanceOf(EntityDynamicMappingConfigurationException.class)
          .hasMessageContaining("properties.applicationName")
          .hasMessageContaining("is required and must contain a JSLT expression");
    }

    @Test
    @DisplayName("Should report an error when a relation expression is invalid JSLT")
    void shouldReportErrorWhenRelationExpressionInvalid() {
      var mapping = buildMapping(".action", ".repository.full_name", ".repository.name", Map.of(),
          Map.of("owner", "["));

      assertThatThrownBy(() -> validator.validate(mapping))
          .isInstanceOf(EntityDynamicMappingConfigurationException.class)
          .hasMessageContaining("relations.owner");
    }

    @Test
    @DisplayName("Should aggregate multiple errors into a single exception message")
    void shouldAggregateMultipleErrors() {
      var properties = new HashMap<String, String>();
      properties.put("applicationName", "   ");

      var mapping = buildMapping("[", ".repository.full_name", ".repository.name", properties,
          Map.of("owner", "["));

      assertThatThrownBy(() -> validator.validate(mapping))
          .isInstanceOf(EntityDynamicMappingConfigurationException.class)
          .hasMessageContaining("Validation failed with 3 errors");
    }
  }

  // ---------------------------------------------------------------------------
  // formatJsltErrorMessage (private — exercised through reflection)
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("formatJsltErrorMessage")
  class FormatJsltErrorMessageTests {

    @Test
    @DisplayName("Should return generic message when raw message is null")
    void shouldReturnGenericMessageWhenNull() {
      assertThat(formatJsltErrorMessage(null)).isEqualTo("JSLT syntax error.");
    }

    @Test
    @DisplayName("Should return generic message when raw message is blank")
    void shouldReturnGenericMessageWhenBlank() {
      assertThat(formatJsltErrorMessage("   ")).isEqualTo("JSLT syntax error.");
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
          .isEqualTo("JSLT syntax error at line 3, column 7 (unexpected token: }).");
    }

    @Test
    @DisplayName("Should format message with line and column only when token absent")
    void shouldFormatWithLineColumnOnly() {
      var raw = "Unexpected failure at line 5, column 2 in the expression";

      assertThat(formatJsltErrorMessage(raw)).isEqualTo("JSLT syntax error at line 5, column 2.");
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
      String entityTitle, Map<String, String> properties, Map<String, String> relations) {
    return new EntityDynamicMapping(UUID.randomUUID(), "my-mapping", "microservice", filter,
        "My Mapping", "description", entityIdentifier, entityTitle, properties, relations);
  }

  /// Invokes the private `formatJsltErrorMessage` method through reflection so
  /// every formatting branch can be exercised deterministically.
  private String formatJsltErrorMessage(String rawMessage) {
    try {
      Method method = JsltEntityMappingValidator.class.getDeclaredMethod("formatJsltErrorMessage",
          String.class);
      method.setAccessible(true);
      return (String) method.invoke(validator, rawMessage);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Unable to invoke formatJsltErrorMessage", e);
    }
  }
}
