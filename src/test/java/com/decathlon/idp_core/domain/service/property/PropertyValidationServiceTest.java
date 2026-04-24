package com.decathlon.idp_core.domain.service.property;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.constant.ValidationMessages;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.PropertyRules;
import com.decathlon.idp_core.domain.model.enums.PropertyFormat;
import com.decathlon.idp_core.domain.model.enums.PropertyType;

@DisplayName("PropertyValidationService Tests")
class PropertyValidationServiceTest {

    private final PropertyValidationService service = new PropertyValidationService();

    @Test
    @DisplayName("Should report type mismatch for non numeric NUMBER value")
    void shouldReportTypeMismatchWhenNumberValueIsInvalid() {
        var definition = propertyDefinition("score", PropertyType.NUMBER, null);

        var violations = service.validatePropertyValue(definition, "not-a-number");

        assertEquals(List.of(ValidationMessages.PROPERTY_TYPE_MISMATCH.formatted("score", PropertyType.NUMBER)), violations);
    }

    @Test
    @DisplayName("Should report string constraint violations")
    void shouldReportStringRuleViolations() {
        var definition = propertyDefinition("name", PropertyType.STRING, new PropertyRules(
                null,
                PropertyFormat.EMAIL,
                List.of("prod", "dev"),
                "^[a-z]+$",
                5,
                3,
                null,
                null));

        var violations = service.validatePropertyValue(definition, "AA");

        assertEquals(4, violations.size());
    }

    @Test
    @DisplayName("Should report number bound violations")
    void shouldReportNumberBoundViolations() {
        var definition = propertyDefinition("size", PropertyType.NUMBER, new PropertyRules(
                null,
                null,
                null,
                null,
                null,
                null,
                10,
                5));

        var violations = service.validatePropertyValue(definition, "3");

        assertEquals(List.of(ValidationMessages.PROPERTY_MIN_VALUE_VIOLATION.formatted("size", 5)), violations);
    }

    @Test
    @DisplayName("Should accept valid boolean value")
    void shouldAcceptBooleanValues() {
        var definition = propertyDefinition("enabled", PropertyType.BOOLEAN, null);

        var violations = service.validatePropertyValue(definition, "true");

        assertEquals(List.of(), violations);
    }

    private PropertyDefinition propertyDefinition(String name, PropertyType type, PropertyRules rules) {
        return new PropertyDefinition(null, name, "description", type, true, rules);
    }
}
