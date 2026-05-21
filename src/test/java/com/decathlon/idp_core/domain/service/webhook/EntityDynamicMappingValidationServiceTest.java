package com.decathlon.idp_core.domain.service.webhook;

import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.exception.entity_template.PropertyNameNotFoundEntityTemplatePropertiesException;
import com.decathlon.idp_core.domain.exception.entity_template.RelationNameNotFoundEntityTemplateRelationsException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookTemplateHasNoPropertiesException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.domain.model.enums.PropertyType;
import com.decathlon.idp_core.domain.port.EntityDynamicMapperValidator;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateService;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EntityDynamicMappingValidationService}.
 */
@DisplayName("EntityDynamicMappingValidationService Tests")
@ExtendWith(MockitoExtension.class)
class EntityDynamicMappingValidationServiceTest {

    @Mock
    private EntityTemplateService entityTemplateService;

    @Mock
    private EntityDynamicMapperValidator entityDynamicMapperValidator;

    @Mock
    private EntityTemplateValidationService entityTemplateValidationService;

    private EntityDynamicMappingValidationService service;

    @BeforeEach
    void setUp() {
        service = new EntityDynamicMappingValidationService(
                entityTemplateService,
                entityDynamicMapperValidator,
                entityTemplateValidationService
        );
    }

    private EntityDynamicMapping buildMapping(String templateIdentifier,
                                              Map<String, String> properties,
                                              Map<String, String> relations) {
        return new EntityDynamicMapping(
                templateIdentifier,
                ".eventType == \"DEPLOYED\"",
                ".id",
                ".name",
                properties,
                relations
        );
    }

    private EntityTemplate buildEntityTemplate(List<PropertyDefinition> properties,
                                               List<RelationDefinition> relations) {
        return new EntityTemplate(
                UUID.randomUUID(),
                "deployment",
                "Deployment",
                "A deployment template",
                properties,
                relations
        );
    }

    private PropertyDefinition buildProperty(String name, boolean required) {
        return new PropertyDefinition(UUID.randomUUID(), name, name + " description", PropertyType.STRING, required, null);
    }

    private RelationDefinition buildRelation(String name, boolean required) {
        return new RelationDefinition(UUID.randomUUID(), name, "service", required, false);
    }

    @Nested
    @DisplayName("validateWebhookMapping - happy paths")
    class ValidateWebhookMappingHappyPathTests {

        @Test
        @DisplayName("Should pass with valid mapping having matching properties")
        void shouldPassWithValidMappingMatchingProperties() {
            PropertyDefinition property = buildProperty("environment", false);
            EntityTemplate template = buildEntityTemplate(List.of(property), List.of());
            EntityDynamicMapping mapping = buildMapping("deployment", Map.of("environment", ".env"), Map.of());

            doNothing().when(entityTemplateValidationService).validateTemplateExists("deployment");
            when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);
            doNothing().when(entityTemplateValidationService).validatePropertyNameAlreadyExistInTemplate(
                    template.propertiesDefinitions(), "environment");

            assertThatNoException().isThrownBy(() -> service.validateWebhookMapping(List.of(mapping)));

            verify(entityDynamicMapperValidator).validate(mapping);
        }

        @Test
        @DisplayName("Should pass with empty properties mapping and empty template properties")
        void shouldPassWithEmptyPropertiesAndEmptyTemplateProperties() {
            EntityTemplate template = buildEntityTemplate(List.of(), List.of());
            EntityDynamicMapping mapping = buildMapping("deployment", Map.of(), Map.of());

            doNothing().when(entityTemplateValidationService).validateTemplateExists("deployment");
            when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);

            assertThatNoException().isThrownBy(() -> service.validateWebhookMapping(List.of(mapping)));

            verify(entityDynamicMapperValidator).validate(mapping);
        }

        @Test
        @DisplayName("Should pass with null relations (no relations in mapping)")
        void shouldPassWithNullRelations() {
            PropertyDefinition property = buildProperty("env", false);
            EntityTemplate template = buildEntityTemplate(List.of(property), List.of());
            EntityDynamicMapping mapping = buildMapping("deployment", Map.of("env", ".env"), null);

            doNothing().when(entityTemplateValidationService).validateTemplateExists("deployment");
            when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);
            doNothing().when(entityTemplateValidationService).validatePropertyNameAlreadyExistInTemplate(
                    template.propertiesDefinitions(), "env");

            assertThatNoException().isThrownBy(() -> service.validateWebhookMapping(List.of(mapping)));
        }

        @Test
        @DisplayName("Should pass with empty relations in mapping and no required relations in template")
        void shouldPassWithEmptyRelationsAndNoRequiredRelations() {
            RelationDefinition relation = buildRelation("service", false);
            EntityTemplate template = buildEntityTemplate(List.of(), List.of(relation));
            EntityDynamicMapping mapping = buildMapping("deployment", Map.of(), Map.of());

            doNothing().when(entityTemplateValidationService).validateTemplateExists("deployment");
            when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);

            assertThatNoException().isThrownBy(() -> service.validateWebhookMapping(List.of(mapping)));
        }

        @Test
        @DisplayName("Should validate each mapping in the list")
        void shouldValidateEachMappingInList() {
            PropertyDefinition property1 = buildProperty("env", false);
            EntityTemplate template1 = buildEntityTemplate(List.of(property1), List.of());
            EntityDynamicMapping mapping1 = buildMapping("deployment", Map.of("env", ".env"), Map.of());

            PropertyDefinition property2 = buildProperty("version", false);
            EntityTemplate template2 = buildEntityTemplate(List.of(property2), List.of());
            EntityDynamicMapping mapping2 = new EntityDynamicMapping(
                    "service", ".type == \"SERVICE\"", ".id", ".name",
                    Map.of("version", ".ver"), Map.of()
            );

            doNothing().when(entityTemplateValidationService).validateTemplateExists("deployment");
            doNothing().when(entityTemplateValidationService).validateTemplateExists("service");
            when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template1);
            when(entityTemplateService.getEntityTemplateByIdentifier("service")).thenReturn(template2);
            doNothing().when(entityTemplateValidationService).validatePropertyNameAlreadyExistInTemplate(
                    template1.propertiesDefinitions(), "env");
            doNothing().when(entityTemplateValidationService).validatePropertyNameAlreadyExistInTemplate(
                    template2.propertiesDefinitions(), "version");

            assertThatNoException().isThrownBy(() -> service.validateWebhookMapping(List.of(mapping1, mapping2)));

            verify(entityDynamicMapperValidator).validate(mapping1);
            verify(entityDynamicMapperValidator).validate(mapping2);
        }

        @Test
        @DisplayName("Should pass when mapping has valid relations matching template")
        void shouldPassWithValidRelations() {
            RelationDefinition relation = buildRelation("owner", true);
            EntityTemplate template = buildEntityTemplate(List.of(), List.of(relation));
            EntityDynamicMapping mapping = buildMapping("deployment", Map.of(), Map.of("owner", ".owner"));

            doNothing().when(entityTemplateValidationService).validateTemplateExists("deployment");
            when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);
            doNothing().when(entityTemplateValidationService).validateRelationNameAlreadyExistInTemplate(
                    template.relationsDefinitions(), "owner");

            assertThatNoException().isThrownBy(() -> service.validateWebhookMapping(List.of(mapping)));

            verify(entityDynamicMapperValidator).validate(mapping);
        }
    }

    @Nested
    @DisplayName("validateWebhookMapping - template existence")
    class ValidateTemplateExistenceTests {

        @Test
        @DisplayName("Should throw EntityTemplateNotFoundException when template does not exist")
        void shouldThrowWhenTemplateDoesNotExist() {
            EntityDynamicMapping mapping = buildMapping("unknown-template", Map.of(), Map.of());

            doThrow(new EntityTemplateNotFoundException("identifier", "unknown-template"))
                    .when(entityTemplateValidationService).validateTemplateExists("unknown-template");

            assertThatThrownBy(() -> service.validateWebhookMapping(List.of(mapping)))
                    .isInstanceOf(EntityTemplateNotFoundException.class);

            verify(entityTemplateService, never()).getEntityTemplateByIdentifier("unknown-template");
            verify(entityDynamicMapperValidator, never()).validate(mapping);
        }
    }

    @Nested
    @DisplayName("validateWebhookMapping - properties validation")
    class ValidatePropertiesTests {

        @Test
        @DisplayName("Should throw WebhookTemplateHasNoPropertiesException when mapping has properties but template has none")
        void shouldThrowWhenMappingHasPropertiesButTemplateHasNone() {
            EntityTemplate template = buildEntityTemplate(List.of(), List.of());
            EntityDynamicMapping mapping = buildMapping("deployment", Map.of("env", ".env"), Map.of());

            doNothing().when(entityTemplateValidationService).validateTemplateExists("deployment");
            when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);

            assertThatThrownBy(() -> service.validateWebhookMapping(List.of(mapping)))
                    .isInstanceOf(WebhookTemplateHasNoPropertiesException.class)
                    .hasMessageContaining("no property definitions");

            verify(entityDynamicMapperValidator, never()).validate(mapping);
        }

        @Test
        @DisplayName("Should throw PropertyNameNotFoundEntityTemplatePropertiesException when property not in template")
        void shouldThrowWhenPropertyNotFoundInTemplate() {
            var property = buildProperty("environment", false);
            EntityTemplate template = buildEntityTemplate(List.of(property), List.of());
            EntityDynamicMapping mapping = buildMapping("deployment", Map.of("unknown-prop", ".x"), Map.of());

            doNothing().when(entityTemplateValidationService).validateTemplateExists("deployment");
            when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);
            doThrow(new PropertyNameNotFoundEntityTemplatePropertiesException("Property name unknown-prop not found in entity template properties"))
                    .when(entityTemplateValidationService).validatePropertyNameAlreadyExistInTemplate(
                            template.propertiesDefinitions(), "unknown-prop");

            assertThatThrownBy(() -> service.validateWebhookMapping(List.of(mapping)))
                    .isInstanceOf(PropertyNameNotFoundEntityTemplatePropertiesException.class)
                    .hasMessageContaining("unknown-prop");

            verify(entityDynamicMapperValidator, never()).validate(mapping);
        }

        @Test
        @DisplayName("Should throw WebhookTemplateHasNoPropertiesException when required property is missing from mapping")
        void shouldThrowWhenRequiredPropertyMissingFromMapping() {
            PropertyDefinition requiredProp = buildProperty("env", true);
            EntityTemplate template = buildEntityTemplate(List.of(requiredProp), List.of());
            // mapping does not include the required property "env"
            EntityDynamicMapping mapping = buildMapping("deployment", Map.of(), Map.of());

            doNothing().when(entityTemplateValidationService).validateTemplateExists("deployment");
            when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);

            assertThatThrownBy(() -> service.validateWebhookMapping(List.of(mapping)))
                    .isInstanceOf(WebhookTemplateHasNoPropertiesException.class)
                    .hasMessageContaining("env");

            verify(entityDynamicMapperValidator, never()).validate(mapping);
        }

        @Test
        @DisplayName("Should pass when all required properties are mapped")
        void shouldPassWhenAllRequiredPropertiesMapped() {
            PropertyDefinition requiredProp = buildProperty("env", true);
            EntityTemplate template = buildEntityTemplate(List.of(requiredProp), List.of());
            EntityDynamicMapping mapping = buildMapping("deployment", Map.of("env", ".environment"), Map.of());

            doNothing().when(entityTemplateValidationService).validateTemplateExists("deployment");
            when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);
            doNothing().when(entityTemplateValidationService).validatePropertyNameAlreadyExistInTemplate(
                    template.propertiesDefinitions(), "env");

            assertThatNoException().isThrownBy(() -> service.validateWebhookMapping(List.of(mapping)));

            verify(entityDynamicMapperValidator).validate(mapping);
        }

        @Test
        @DisplayName("Should throw when multiple required properties are missing from mapping")
        void shouldThrowWhenMultipleRequiredPropertiesMissing() {
            PropertyDefinition prop1 = buildProperty("env", true);
            PropertyDefinition prop2 = buildProperty("version", true);
            EntityTemplate template = buildEntityTemplate(List.of(prop1, prop2), List.of());
            EntityDynamicMapping mapping = buildMapping("deployment", Map.of(), Map.of());

            doNothing().when(entityTemplateValidationService).validateTemplateExists("deployment");
            when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);

            assertThatThrownBy(() -> service.validateWebhookMapping(List.of(mapping)))
                    .isInstanceOf(WebhookTemplateHasNoPropertiesException.class)
                    .hasMessageContaining("env")
                    .hasMessageContaining("version");
        }
    }

    @Nested
    @DisplayName("validateWebhookMapping - relations validation")
    class ValidateRelationsTests {

        @Test
        @DisplayName("Should throw RelationNameNotFoundEntityTemplateRelationsException when relation not in template")
        void shouldThrowWhenRelationNotFoundInTemplate() {
            RelationDefinition relation = buildRelation("owner", false);
            EntityTemplate template = buildEntityTemplate(List.of(), List.of(relation));
            EntityDynamicMapping mapping = buildMapping("deployment", Map.of(), Map.of("unknown-relation", ".x"));

            doNothing().when(entityTemplateValidationService).validateTemplateExists("deployment");
            when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);
            doThrow(new RelationNameNotFoundEntityTemplateRelationsException("Relation name unknown-relation not found in entity template relations"))
                    .when(entityTemplateValidationService).validateRelationNameAlreadyExistInTemplate(
                            template.relationsDefinitions(), "unknown-relation");

            assertThatThrownBy(() -> service.validateWebhookMapping(List.of(mapping)))
                    .isInstanceOf(RelationNameNotFoundEntityTemplateRelationsException.class)
                    .hasMessageContaining("unknown-relation");

            verify(entityDynamicMapperValidator, never()).validate(mapping);
        }

        @Test
        @DisplayName("Should throw WebhookTemplateHasNoPropertiesException when required relation is missing from mapping")
        void shouldThrowWhenRequiredRelationMissingFromMapping() {
            RelationDefinition requiredRelation = buildRelation("owner", true);
            EntityTemplate template = buildEntityTemplate(List.of(), List.of(requiredRelation));
            EntityDynamicMapping mapping = buildMapping("deployment", Map.of(), Map.of());

            doNothing().when(entityTemplateValidationService).validateTemplateExists("deployment");
            when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);

            assertThatThrownBy(() -> service.validateWebhookMapping(List.of(mapping)))
                    .isInstanceOf(WebhookTemplateHasNoPropertiesException.class)
                    .hasMessageContaining("owner");

            verify(entityDynamicMapperValidator, never()).validate(mapping);
        }

        @Test
        @DisplayName("Should pass when required relation is mapped")
        void shouldPassWhenRequiredRelationMapped() {
            RelationDefinition requiredRelation = buildRelation("owner", true);
            EntityTemplate template = buildEntityTemplate(List.of(), List.of(requiredRelation));
            EntityDynamicMapping mapping = buildMapping("deployment", Map.of(), Map.of("owner", ".ownerId"));

            doNothing().when(entityTemplateValidationService).validateTemplateExists("deployment");
            when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);
            doNothing().when(entityTemplateValidationService).validateRelationNameAlreadyExistInTemplate(
                    template.relationsDefinitions(), "owner");

            assertThatNoException().isThrownBy(() -> service.validateWebhookMapping(List.of(mapping)));

            verify(entityDynamicMapperValidator).validate(mapping);
        }

        @Test
        @DisplayName("Should throw when multiple required relations are missing from mapping")
        void shouldThrowWhenMultipleRequiredRelationsMissing() {
            RelationDefinition rel1 = buildRelation("owner", true);
            RelationDefinition rel2 = buildRelation("team", true);
            EntityTemplate template = buildEntityTemplate(List.of(), List.of(rel1, rel2));
            EntityDynamicMapping mapping = buildMapping("deployment", Map.of(), Map.of());

            doNothing().when(entityTemplateValidationService).validateTemplateExists("deployment");
            when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);

            assertThatThrownBy(() -> service.validateWebhookMapping(List.of(mapping)))
                    .isInstanceOf(WebhookTemplateHasNoPropertiesException.class)
                    .hasMessageContaining("owner")
                    .hasMessageContaining("team");
        }

        @Test
        @DisplayName("Should skip relation validation when relations map is null")
        void shouldSkipRelationValidationWhenNull() {
            EntityTemplate template = buildEntityTemplate(List.of(), List.of());
            EntityDynamicMapping mapping = buildMapping("deployment", Map.of(), null);

            doNothing().when(entityTemplateValidationService).validateTemplateExists("deployment");
            when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);

            assertThatNoException().isThrownBy(() -> service.validateWebhookMapping(List.of(mapping)));

            verify(entityTemplateValidationService, never())
                    .validateRelationNameAlreadyExistInTemplate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        }
    }

    @Nested
    @DisplayName("validateWebhookMapping - mapper validator delegation")
    class MapperValidatorDelegationTests {

        @Test
        @DisplayName("Should delegate to entityDynamicMapperValidator after all domain checks pass")
        void shouldDelegateToMapperValidator() {
            EntityTemplate template = buildEntityTemplate(List.of(), List.of());
            EntityDynamicMapping mapping = buildMapping("deployment", Map.of(), Map.of());

            doNothing().when(entityTemplateValidationService).validateTemplateExists("deployment");
            when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);

            service.validateWebhookMapping(List.of(mapping));

            verify(entityDynamicMapperValidator).validate(mapping);
        }

        @Test
        @DisplayName("Should NOT call entityDynamicMapperValidator when domain check throws")
        void shouldNotCallMapperValidatorWhenDomainCheckFails() {
            EntityDynamicMapping mapping = buildMapping("bad-template", Map.of(), Map.of());

            doThrow(new EntityTemplateNotFoundException("identifier", "bad-template"))
                    .when(entityTemplateValidationService).validateTemplateExists("bad-template");

            assertThatThrownBy(() -> service.validateWebhookMapping(List.of(mapping)))
                    .isInstanceOf(EntityTemplateNotFoundException.class);

            verify(entityDynamicMapperValidator, never()).validate(mapping);
        }
    }
}
