package com.decathlon.idp_core.domain.service.property;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_ENUM_VIOLATION;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_FORMAT_VIOLATION;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_MAX_LENGTH_VIOLATION;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_MAX_VALUE_VIOLATION;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_MIN_LENGTH_VIOLATION;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_MIN_VALUE_VIOLATION;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_REGEX_VIOLATION;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_TYPE_MISMATCH;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.PropertyRules;
import com.decathlon.idp_core.domain.model.enums.PropertyFormat;
import com.decathlon.idp_core.domain.model.enums.PropertyType;

/**
 * Domain service validating entity property values against template definitions.
 */
@Service
public class PropertyValidationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://.*$");

    /// Cache of compiled regex patterns keyed by their source string.
    /// Avoids recompiling the same pattern on every property validation call.
    private final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();

    /**
     * Validates a concrete property value against its property definition.
     * Type compatibility is checked first against the original raw value
     * before applying any rule-based validations.
     *
     * @param propertyDefinition property definition with expected type and optional rules
     * @param rawValue raw property value as string
     * @param originalValue the original untyped value from the API input for type checking,
     *                      may be null when loaded from persistence
     * @return list of violations for this value; empty when valid
     */
    public List<String> validatePropertyValue(PropertyDefinition propertyDefinition, String rawValue, Object originalValue) {
        List<String> typeMismatch = checkOriginalValueType(propertyDefinition.name(), propertyDefinition.type(), originalValue);
        if (!typeMismatch.isEmpty()) {
            return typeMismatch;
        }
        return switch (propertyDefinition.type()) {
            case STRING -> validateStringPropertyValue(propertyDefinition.name(), rawValue, propertyDefinition.rules());
            case NUMBER -> validateNumberPropertyValue(propertyDefinition.name(), rawValue, propertyDefinition.rules());
            case BOOLEAN -> validateBooleanPropertyValue(propertyDefinition.name(), rawValue);
        };
    }

    /// Checks that the original JSON value type is compatible with the expected [PropertyType].
    ///
    /// When `originalValue` is non-null, its Java type is inspected:
    /// - STRING expects a Java `String`
    /// - NUMBER expects a Java `Number`
    /// - BOOLEAN expects a Java `Boolean`
    ///
    /// If `originalValue` is null (e.g. loaded from persistence), the check is skipped
    /// and type validation falls through to the string-based validators.
    ///
    /// @param propertyName   property name for error reporting
    /// @param expectedType   the expected property type from the template definition
    /// @param originalValue  the original untyped value from the API input
    /// @return a single-element list with a type mismatch message, or an empty list if compatible
    private List<String> checkOriginalValueType(String propertyName, PropertyType expectedType, Object originalValue) {
        if (originalValue == null) {
            return List.of();
        }
        boolean compatible = switch (expectedType) {
            case STRING  -> originalValue instanceof String;
            case NUMBER  -> originalValue instanceof Number || originalValue instanceof String;
            case BOOLEAN -> originalValue instanceof Boolean || originalValue instanceof String;
        };
        if (!compatible) {
            return List.of(PROPERTY_TYPE_MISMATCH.formatted(propertyName, expectedType));
        }
        return List.of();
    }

    private List<String> validateStringPropertyValue(String propertyName, String rawValue, PropertyRules rules) {
        if (rawValue == null) {
            return List.of(PROPERTY_TYPE_MISMATCH.formatted(propertyName, PropertyType.STRING));
        }

        if (rules == null) {
            return List.of();
        }

        var violations = new ArrayList<String>();

        if (rules.minLength() != null && rawValue.length() < rules.minLength()) {
            violations.add(PROPERTY_MIN_LENGTH_VIOLATION.formatted(propertyName, rules.minLength()));
        }
        if (rules.maxLength() != null && rawValue.length() > rules.maxLength()) {
            violations.add(PROPERTY_MAX_LENGTH_VIOLATION.formatted(propertyName, rules.maxLength()));
        }
        if (rules.regex() != null
                && !patternCache.computeIfAbsent(rules.regex(), Pattern::compile).matcher(rawValue).matches()) {
            violations.add(PROPERTY_REGEX_VIOLATION.formatted(propertyName));
        }
        if (rules.enumValues() != null && !rules.enumValues().isEmpty()
                && rules.enumValues().stream().noneMatch(enumValue -> enumValue.equalsIgnoreCase(rawValue))) {
            violations.add(PROPERTY_ENUM_VIOLATION.formatted(propertyName, rules.enumValues()));
        }
        if (rules.format() != null && !matchesFormat(rules.format(), rawValue)) {
            violations.add(PROPERTY_FORMAT_VIOLATION.formatted(propertyName, rules.format()));
        }

        return List.copyOf(violations);
    }

    private List<String> validateNumberPropertyValue(String propertyName, String rawValue, PropertyRules rules) {
        final BigDecimal parsedValue;
        try {
            parsedValue = new BigDecimal(rawValue);
        } catch (NumberFormatException _) {
            return List.of(PROPERTY_TYPE_MISMATCH.formatted(propertyName, PropertyType.NUMBER));
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

    private List<String> validateBooleanPropertyValue(String propertyName, String rawValue) {
        if ("true".equalsIgnoreCase(rawValue) || "false".equalsIgnoreCase(rawValue)) {
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
