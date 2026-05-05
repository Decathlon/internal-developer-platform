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
     * The value's runtime Java type is checked first against the expected
     * [PropertyType] (STRING ⇒ {@link String}, NUMBER ⇒ {@link Number},
     * BOOLEAN ⇒ {@link Boolean}). When the type matches, the value is
     * normalized to a string and the type-specific rules are evaluated.
     *
     * @param propertyDefinition property definition with expected type and optional rules
     * @param rawValue raw property value preserving its original JSON type
     * @return list of violations for this value; empty when valid
     */
    public List<String> validatePropertyValue(PropertyDefinition propertyDefinition, Object rawValue) {
        return switch (propertyDefinition.type()) {
            case STRING -> validateStringPropertyValue(propertyDefinition.name(), rawValue, propertyDefinition.rules());
            case NUMBER -> validateNumberPropertyValue(propertyDefinition.name(), rawValue, propertyDefinition.rules());
            case BOOLEAN -> validateBooleanPropertyValue(propertyDefinition.name(), rawValue);
        };
    }


    private List<String> validateStringPropertyValue(String propertyName, Object rawValue, PropertyRules rules) {
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
        if (rules.regex() != null
                && !patternCache.computeIfAbsent(rules.regex(), Pattern::compile).matcher(stringValue).matches()) {
            violations.add(PROPERTY_REGEX_VIOLATION.formatted(propertyName));
        }
        if (rules.enumValues() != null && !rules.enumValues().isEmpty()
                && rules.enumValues().stream().noneMatch(enumValue -> enumValue.equalsIgnoreCase(stringValue))) {
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
                } catch (NumberFormatException _) {
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
