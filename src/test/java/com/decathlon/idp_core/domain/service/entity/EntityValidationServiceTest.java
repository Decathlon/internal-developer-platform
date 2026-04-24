package com.decathlon.idp_core.domain.service.entity;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.ENTITY_IDENTIFIER_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.ENTITY_NAME_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_NAME_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_REQUIRED_MISSING;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.PROPERTY_VALUE_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_NAME_MANDATORY_SIMPLE;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_TARGET_IDENTIFIERS_NOT_NULL;
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

import com.decathlon.idp_core.domain.exception.EntityAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.exception.EntityValidationException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.PropertyRules;
import com.decathlon.idp_core.domain.model.enums.PropertyType;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.domain.port.EntityTemplateRepositoryPort;
import com.decathlon.idp_core.domain.service.property.PropertyValidationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("EntityValidationService Tests")
class EntityValidationServiceTest {

    @Mock
    private EntityRepositoryPort entityRepository;

    @Mock
    private EntityTemplateRepositoryPort entityTemplateRepository;

    @Mock
    private PropertyValidationService propertyValidationService;

    @InjectMocks
    private EntityValidationService entityValidationService;

    @Test
    @DisplayName("Should pass checkTemplateExist when template exists")
    void shouldPassCheckTemplateExistWhenTemplateExists() {
        when(entityTemplateRepository.existsByIdentifier("web-service")).thenReturn(true);

        assertDoesNotThrow(() -> entityValidationService.checkTemplateExist("web-service"));
    }

    @Test
    @DisplayName("Should throw checkTemplateExist when template does not exist")
    void shouldThrowCheckTemplateExistWhenTemplateDoesNotExist() {
        when(entityTemplateRepository.existsByIdentifier("missing-template")).thenReturn(false);

        assertThrows(EntityTemplateNotFoundException.class,
                () -> entityValidationService.checkTemplateExist("missing-template"));
    }

    @Test
    @DisplayName("Should throw when entity with same identifier already exists")
    void shouldThrowWhenEntityAlreadyExists() {
        var entity = entity("web-service", "catalog-api", "Catalog API", List.of(), List.of());
        when(entityRepository.findByTemplateIdentifierAndIdentifier("web-service", "catalog-api"))
                .thenReturn(Optional.of(entity));

        assertThrows(EntityAlreadyExistsException.class, () -> entityValidationService.checkEntityAlreadyExist(entity));
    }

    @Test
    @DisplayName("Should not query repository when identifier is null")
    void shouldNotQueryRepositoryWhenIdentifierIsNull() {
        var entity = entity("web-service", null, "Catalog API", List.of(), List.of());

        assertDoesNotThrow(() -> entityValidationService.checkEntityAlreadyExist(entity));

        verify(entityRepository, never()).findByTemplateIdentifierAndIdentifier(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Should throw when template is missing during validateEntity")
    void shouldThrowWhenTemplateMissingDuringValidateEntity() {
        var entity = entity("missing-template", "catalog-api", "Catalog API", List.of(), List.of());
        when(entityTemplateRepository.findByIdentifier("missing-template")).thenReturn(Optional.empty());

        assertThrows(EntityTemplateNotFoundException.class, () -> entityValidationService.validateEntity(entity));
    }

    @Test
    @DisplayName("Should aggregate entity, property, relation, required and rule violations")
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

        var mockedRelation = org.mockito.Mockito.mock(Relation.class);
        when(mockedRelation.name()).thenReturn(" ");
        when(mockedRelation.targetEntityIdentifiers()).thenReturn(null);

        var entity = entity(
                "web-service",
                " ",
                " ",
                List.of(new Property(UUID.randomUUID(), " ", " "), new Property(UUID.randomUUID(), "port", "80")),
                List.of(mockedRelation));

        when(entityTemplateRepository.findByIdentifier("web-service")).thenReturn(Optional.of(template));
        when(entityRepository.findByTemplateIdentifierAndIdentifier("web-service", " ")).thenReturn(Optional.empty());
        when(propertyValidationService.validatePropertyValue(portDefinition, "80"))
                .thenReturn(List.of("Property 'port' value must be greater than or equal to 1024"));

        var exception = assertThrows(EntityValidationException.class, () -> entityValidationService.validateEntity(entity));

        assertEquals(8, exception.getViolations().size());
        assertEquals(ENTITY_NAME_MANDATORY, exception.getViolations().get(0));
        assertEquals(ENTITY_IDENTIFIER_MANDATORY, exception.getViolations().get(1));
        assertEquals("Property[0]: " + PROPERTY_NAME_MANDATORY, exception.getViolations().get(2));
        assertEquals("Property[0]: " + PROPERTY_VALUE_MANDATORY, exception.getViolations().get(3));
        assertEquals("Relation[0]: " + RELATION_NAME_MANDATORY_SIMPLE, exception.getViolations().get(4));
        assertEquals("Relation[0]: " + RELATION_TARGET_IDENTIFIERS_NOT_NULL, exception.getViolations().get(5));
        assertEquals(PROPERTY_REQUIRED_MISSING.formatted("ownerEmail", "web-service"), exception.getViolations().get(6));
        assertEquals("Property 'port' value must be greater than or equal to 1024", exception.getViolations().get(7));

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

        when(entityTemplateRepository.findByIdentifier("web-service")).thenReturn(Optional.of(template));
        when(entityRepository.findByTemplateIdentifierAndIdentifier("web-service", "catalog-api"))
                .thenReturn(Optional.empty());
        when(propertyValidationService.validatePropertyValue(versionDefinition, "1.0.0")).thenReturn(List.of());

        assertDoesNotThrow(() -> entityValidationService.validateEntity(entity));
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

        when(entityTemplateRepository.findByIdentifier("web-service")).thenReturn(Optional.of(template));
        when(entityRepository.findByTemplateIdentifierAndIdentifier("web-service", "catalog-api"))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> entityValidationService.validateEntity(entity));
        verifyNoInteractions(propertyValidationService);
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
