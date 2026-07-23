package com.decathlon.idp_core.domain.service.property;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingHasNoPropertiesException;
import com.decathlon.idp_core.domain.exception.entity_template.PropertyNameNotFoundEntityTemplatePropertiesException;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.PropertyRules;
import com.decathlon.idp_core.domain.model.enums.PropertyFormat;
import com.decathlon.idp_core.domain.model.enums.PropertyType;
import com.decathlon.idp_core.domain.service.entity.Violations;

/**
 * Domain service validating entity property values against template
 * definitions.
 */
@Service
public class PropertyValidationService {

  private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
  private static final Pattern URL_PATTERN = Pattern.compile("^https?://.*$");

  /// Cache of compiled regex patterns keyed by their source string.
  /// Avoids recompiling the same pattern on every property validation call.
  private final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();

  /**
   * Validates a concrete property value against its property definition. The
   * value's runtime Java type is checked first against the expected
   * [PropertyType] (STRING ⇒ {@link String}, NUMBER ⇒ {@link Number}, BOOLEAN ⇒
   * {@link Boolean}). When the type matches, the value is normalized to a string
   * and the type-specific rules are evaluated.
   *
   * @param propertyDefinition
   *          property definition with expected type and optional rules
   * @param rawValue
   *          raw property value preserving its original JSON type
   * @return list of violations for this value; empty when valid
   */
  public List<String> validatePropertyValue(PropertyDefinition propertyDefinition,
      Object rawValue) {
    return switch (propertyDefinition.type()) {
      case STRING -> validateStringPropertyValue(propertyDefinition.name(), rawValue,
          propertyDefinition.rules());
      case NUMBER -> validateNumberPropertyValue(propertyDefinition.name(), rawValue,
          propertyDefinition.rules());
      case BOOLEAN -> validateBooleanPropertyValue(propertyDefinition.name(), rawValue);
    };
  }

  /// Validates that all required properties defined in the template are present
  /// and conform to their definitions.
  /// Also validates that all provided properties are actually defined in the
  /// template.
  /// For each property definition, checks if the corresponding property is
  /// provided and non-blank. If a required property is missing, adds a violation.
  /// If the property is present, validates its value against the definition's
  /// rules and accumulates any violations found.
  ///
  /// @param template the entity template whose property definitions are used for
  /// validation
  /// @param definitions the list of property definitions from the template
  /// @param propertiesByName a map of provided properties keyed by their name for
  /// quick lookup
  /// @param violations the accumulator for any validation violations found during
  /// the process
  /// @throws EntityValidationException if any required property is missing or if
  /// any property value violates its definition rules
  /// @implNote This method focuses on validating the presence and correctness of
  /// properties as defined by the template. It iterates through each property
  /// definition, checks for the corresponding provided property, and applies the
  /// appropriate validation logic based on the property's type and rules.
  public void validatePropertiesAgainstTemplate(final EntityTemplate template,
      final List<PropertyDefinition> definitions, final Map<String, Property> propertiesByName,
      final Violations violations) {
    var definedPropertyNames = definitions.stream().map(PropertyDefinition::name)
        .collect(Collectors.toSet());

    for (String providedPropertyName : propertiesByName.keySet()) {
      if (!definedPropertyNames.contains(providedPropertyName)) {
        violations.add(PROPERTY_NOT_DEFINED_IN_TEMPLATE, providedPropertyName,
            template.identifier());
      }
    }

    for (PropertyDefinition definition : definitions) {
      Property property = propertiesByName.get(definition.name());
      boolean missing = property == null || property.value() == null
          || (property.value().isBlank());

      if (missing) {
        if (definition.required()) {
          violations.add(PROPERTY_REQUIRED_MISSING, definition.name(), template.identifier());
        }
        continue;
      }

      validatePropertyValue(definition, property.value()).forEach(violations::add);
    }
  }

  public void validateMappingPropertiesAgainstTemplate(EntityTemplate template,
      List<String> mappedPropertyNames) {
    validateNamesExistInTemplate(template, mappedPropertyNames);
    validateRequiredPropertiesAreMapped(template, mappedPropertyNames);
  }

  private void validateNamesExistInTemplate(EntityTemplate template, List<String> propertyNames) {
    if (propertyNames == null || propertyNames.isEmpty()) {
      return;
    }

    Set<String> definedPropertyNames = getDefinedPropertyNames(template);

    propertyNames.stream().filter(name -> !definedPropertyNames.contains(name)).findFirst()
        .ifPresent(name -> {
          throw new PropertyNameNotFoundEntityTemplatePropertiesException(
              PROPERTY_NOT_EXPECTED_FORMAT.formatted(name));
        });
  }

  /// Validates that all required property definitions in the template are mapped.
  private void validateRequiredPropertiesAreMapped(EntityTemplate template,
      List<String> mappedPropertyNames) {
    List<PropertyDefinition> definitions = template.propertiesDefinitions() != null
        ? template.propertiesDefinitions()
        : List.of();

    List<String> mappedNames = mappedPropertyNames != null ? mappedPropertyNames : List.of();

    List<String> missingProperties = definitions.stream().filter(PropertyDefinition::required)
        .map(PropertyDefinition::name).filter(requiredName -> !mappedNames.contains(requiredName))
        .toList();

    if (!missingProperties.isEmpty()) {
      throw new EntityDynamicMappingHasNoPropertiesException(
          String.format(ENTITY_DYNAMIC_MAPPING_ENTITY_PROPERTIES_MISSING, missingProperties));
    }
  }

  private Set<String> getDefinedPropertyNames(EntityTemplate template) {
    if (template.propertiesDefinitions() == null) {
      return Set.of();
    }
    return template.propertiesDefinitions().stream().map(PropertyDefinition::name)
        .collect(Collectors.toSet());
  }

  private List<String> validateStringPropertyValue(String propertyName, Object rawValue,
      PropertyRules rules) {
    if (!(rawValue instanceof String stringValue)) {
      return List.of(PROPERTY_TYPE_MISMATCH.formatted(propertyName, PropertyType.STRING));
    }

    if (rules == null) {
      return List.of();
    }

    var violations = new ArrayList<String>();

    if (rules.minLength() != null && stringValue.length() < rules.minLength()) {
      violations.add(PROPERTY_MIN_LENGTH_VIOLATION.formatted(propertyName, rules.minLength()));
    }
    if (rules.maxLength() != null && stringValue.length() > rules.maxLength()) {
      violations.add(PROPERTY_MAX_LENGTH_VIOLATION.formatted(propertyName, rules.maxLength()));
    }
    if (rules.regex() != null && !patternCache.computeIfAbsent(rules.regex(), Pattern::compile)
        .matcher(stringValue).matches()) {
      violations.add(PROPERTY_REGEX_VIOLATION.formatted(propertyName));
    }
    if (rules.enumValues() != null && !rules.enumValues().isEmpty() && rules.enumValues().stream()
        .noneMatch(enumValue -> enumValue.equalsIgnoreCase(stringValue))) {
      violations.add(PROPERTY_ENUM_VIOLATION.formatted(propertyName, rules.enumValues()));
    }
    if (rules.format() != null && !matchesFormat(rules.format(), stringValue)) {
      violations.add(PROPERTY_FORMAT_VIOLATION.formatted(propertyName, rules.format()));
    }

    return List.copyOf(violations);
  }

  private List<String> validateNumberPropertyValue(String propertyName, Object rawValue, PropertyRules rules) {
        final BigDecimal parsedValue;
        switch (rawValue) {
            case Number number -> parsedValue = new BigDecimal(number.toString());
            case String string -> {
                try {
                    parsedValue = new BigDecimal(string);
                } catch (NumberFormatException ignored) {
                    return List.of(PROPERTY_TYPE_MISMATCH.formatted(propertyName, PropertyType.NUMBER));
                }
            }
            case null, default -> {
                return List.of(PROPERTY_TYPE_MISMATCH.formatted(propertyName, PropertyType.NUMBER));
            }
        }

        if (rules == null) {
            return List.of();
        }

        var violations = new ArrayList<String>();

        if (rules.minValue() != null && parsedValue.compareTo(BigDecimal.valueOf(rules.minValue())) < 0) {
            violations.add(PROPERTY_MIN_VALUE_VIOLATION.formatted(propertyName, rules.minValue()));
        }
        if (rules.maxValue() != null && parsedValue.compareTo(BigDecimal.valueOf(rules.maxValue())) > 0) {
            violations.add(PROPERTY_MAX_VALUE_VIOLATION.formatted(propertyName, rules.maxValue()));
        }

        return List.copyOf(violations);
    }

  private List<String> validateBooleanPropertyValue(String propertyName, Object rawValue) {
    if (rawValue instanceof Boolean) {
      return List.of();
    }
    if (rawValue instanceof String string
        && ("true".equalsIgnoreCase(string) || "false".equalsIgnoreCase(string))) {
      return List.of();
    }
    return List.of(PROPERTY_TYPE_MISMATCH.formatted(propertyName, PropertyType.BOOLEAN));
  }

  private boolean matchesFormat(PropertyFormat format, String value) {
    return switch (format) {
      case EMAIL -> EMAIL_PATTERN.matcher(value).matches();
      case URL -> URL_PATTERN.matcher(value).matches();
    };
  }
}
