package com.decathlon.idp_core.domain.service.entity;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_REQUIRED_MISSING;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.decathlon.idp_core.domain.exception.entity.EntityAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.PropertyRules;
import com.decathlon.idp_core.domain.model.enums.PropertyType;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.domain.service.property.PropertyValidationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("EntityValidationService Tests")
class EntityValidationServiceTest {

    @Mock
    private EntityRepositoryPort entityRepository;


    @Mock
    private PropertyValidationService propertyValidationService;

    @InjectMocks
    private EntityValidationService entityValidationService;

    @Test
    @DisplayName("Should throw when entity with same identifier already exists")
    void shouldThrowWhenEntityAlreadyExists() {
        var entity = entity("web-service", "catalog-api", "Catalog API", List.of(), List.of());
        when(entityRepository.findByTemplateIdentifierAndIdentifier("web-service", "catalog-api"))
                .thenReturn(Optional.of(entity));

        assertThrows(EntityAlreadyExistsException.class, () -> entityValidationService.checkUniqueness(entity));
    }

    @Test
    @DisplayName("Should not query repository when identifier is null")
    void shouldNotQueryRepositoryWhenIdentifierIsNull() {
        var entity = entity("web-service", null, "Catalog API", List.of(), List.of());

        assertDoesNotThrow(() -> entityValidationService.checkUniqueness(entity));

        verify(entityRepository, never()).findByTemplateIdentifierAndIdentifier(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }


    @Test
    @DisplayName("Should aggregate property requirements and rule violations")
    void shouldAggregateAllViolationsDuringValidateEntity() {
        var portDefinition = new PropertyDefinition(
                UUID.randomUUID(),
                "port",
                "Port",
                PropertyType.NUMBER,
                true,
                new PropertyRules(null, null, null, null, null, null, 65535, 1024));

        var requiredDefinition = new PropertyDefinition(
                UUID.randomUUID(),
                "ownerEmail",
                "Owner email",
                PropertyType.STRING,
                true,
                null);

        var template = new EntityTemplate(
                UUID.randomUUID(),
                "web-service",
                "Web Service",
                "desc",
                List.of(requiredDefinition, portDefinition),
                List.of());

        var entity = entity(
                "web-service",
                " ", // Blank identifier (handled by Jakarta, not this service)
                " ", // Blank name (handled by Jakarta, not this service)
                List.of(new Property(UUID.randomUUID(), " ", " "), new Property(UUID.randomUUID(), "port", "80")),
                List.of()); // No relations

        when(propertyValidationService.validatePropertyValue(portDefinition, "80"))
                .thenReturn(List.of("Property 'port' value must be greater than or equal to 1024"));

        var exception = assertThrows(EntityValidationException.class, () -> entityValidationService.validateEntity(entity, template));

        // Expecting exactly 2 errors: the missing required property, and the invalid port value.
        assertEquals(2, exception.getViolations().size());
        assertEquals(PROPERTY_REQUIRED_MISSING.formatted("ownerEmail", "web-service"), exception.getViolations().get(0));
        assertEquals("Property 'port' value must be greater than or equal to 1024", exception.getViolations().get(1));

        verify(propertyValidationService).validatePropertyValue(portDefinition, "80");
    }

    @Test
    @DisplayName("Should validate entity successfully when no violations")
    void shouldValidateEntitySuccessfullyWhenNoViolations() {
        var versionDefinition = new PropertyDefinition(
                UUID.randomUUID(),
                "version",
                "Version",
                PropertyType.STRING,
                false,
                null);

        var template = new EntityTemplate(
                UUID.randomUUID(),
                "web-service",
                "Web Service",
                "desc",
                List.of(versionDefinition),
                List.of());

        var entity = entity(
                "web-service",
                "catalog-api",
                "Catalog API",
                List.of(new Property(UUID.randomUUID(), "version", "1.0.0")),
                null);


        when(propertyValidationService.validatePropertyValue(versionDefinition, "1.0.0")).thenReturn(List.of());

        assertDoesNotThrow(() -> entityValidationService.validateEntity(entity, template));
        verify(propertyValidationService).validatePropertyValue(versionDefinition, "1.0.0");
    }

    @Test
    @DisplayName("Should skip property rule validation for missing optional property")
    void shouldSkipPropertyRuleValidationWhenOptionalPropertyMissing() {
        var optionalDefinition = new PropertyDefinition(
                UUID.randomUUID(),
                "version",
                "Version",
                PropertyType.STRING,
                false,
                null);

        var template = new EntityTemplate(
                UUID.randomUUID(),
                "web-service",
                "Web Service",
                "desc",
                List.of(optionalDefinition),
                List.of());

        var entity = entity("web-service", "catalog-api", "Catalog API", List.of(), List.of());

        assertDoesNotThrow(() -> entityValidationService.validateEntity(entity, template));
        verifyNoInteractions(propertyValidationService);
    }

    @Test
    @DisplayName("Should validate property of type STRING with a numeric string value '1234'")
    void shouldValidateStringPropertyWithNumericStringValue() {
        var stringDefinition = new PropertyDefinition(
                UUID.randomUUID(),
                "versionCode",
                "Version Code as String",
                PropertyType.STRING,
                false,
                null
        );

        var template = new EntityTemplate(
                UUID.randomUUID(),
                "web-service",
                "Web Service",
                "desc",
                List.of(stringDefinition),
                List.of());

        var entity = entity(
                "web-service",
                "catalog-api",
                "Catalog API",
                List.of(new Property(UUID.randomUUID(), "versionCode", "1234")),
                null);
        when(propertyValidationService.validatePropertyValue(stringDefinition, "1234")).thenReturn(List.of());

        assertDoesNotThrow(() -> entityValidationService.validateEntity(entity, template));
        verify(propertyValidationService).validatePropertyValue(stringDefinition, "1234");
    }

    private Entity entity(
            String templateIdentifier,
            String identifier,
            String name,
            List<Property> properties,
            List<Relation> relations) {
        return new Entity(UUID.randomUUID(), templateIdentifier, name, identifier, properties, relations);
    }
}
