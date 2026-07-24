package com.decathlon.idp_core.domain.service.webhook;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingHasNoPropertiesException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingHasNoRelationsException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.exception.entity_template.PropertyNameNotFoundEntityTemplatePropertiesException;
import com.decathlon.idp_core.domain.exception.entity_template.RelationNameNotFoundEntityTemplateRelationsException;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_mapping.RelationMapping;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.domain.model.enums.PropertyType;
import com.decathlon.idp_core.domain.port.EntityDynamicMapperValidator;
import com.decathlon.idp_core.domain.service.entity_dynamic_mapping.EntityDynamicMappingValidationService;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateService;
import com.decathlon.idp_core.domain.service.property.PropertyValidationService;
import com.decathlon.idp_core.domain.service.relation.RelationValidationService;

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
  private PropertyValidationService propertyValidationService;

  @Mock
  private RelationValidationService relationValidationService;

  private EntityDynamicMappingValidationService service;

  @BeforeEach
  void setUp() {
    service = new EntityDynamicMappingValidationService(entityTemplateService,
        entityDynamicMapperValidator, propertyValidationService, relationValidationService);
  }

  private EntityDynamicMapping buildMapping(String templateIdentifier,
      String entityDynamicMappingIdentifier, Map<String, String> properties,
      Map<String, String> relations) {
    return new EntityDynamicMapping(null, entityDynamicMappingIdentifier, templateIdentifier,
        ".eventType == \"DEPLOYED\"", "name", "description", ".id", ".name", properties,
        toRelationMappings(relations));
  }

  private List<RelationMapping> toRelationMappings(Map<String, String> relations) {
    if (relations == null) {
      return null;
    }
    return relations.entrySet().stream()
        .map(entry -> new RelationMapping(entry.getKey(), List.of(entry.getValue()))).toList();
  }

  private EntityTemplate buildEntityTemplate(List<PropertyDefinition> properties,
      List<RelationDefinition> relations) {
    return new EntityTemplate(UUID.randomUUID(), "deployment", "Deployment",
        "A deployment entityTemplateIdentifier", properties, relations);
  }

  private PropertyDefinition buildProperty(String name, boolean required) {
    return new PropertyDefinition(UUID.randomUUID(), name, name + " description",
        PropertyType.STRING, required, null);
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
      EntityDynamicMapping mapping = buildMapping("deployment", "deployment_mapping",
          Map.of("environment", ".env"), Map.of());

      when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);

      assertThatNoException().isThrownBy(() -> service.validateMappings(List.of(mapping)));

      verify(entityDynamicMapperValidator).validate(mapping);
    }

    @Test
    @DisplayName("Should pass with empty properties mapping and empty entityTemplateIdentifier properties")
    void shouldPassWithEmptyPropertiesAndEmptyTemplateProperties() {
      EntityTemplate template = buildEntityTemplate(List.of(), List.of());
      EntityDynamicMapping mapping = buildMapping("deployment", "deployment_mapping", Map.of(),
          Map.of());

      when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);

      assertThatNoException().isThrownBy(() -> service.validateMappings(List.of(mapping)));

      verify(entityDynamicMapperValidator).validate(mapping);
    }

    @Test
    @DisplayName("Should pass with null relations (no relations in mapping)")
    void shouldPassWithNullRelations() {
      PropertyDefinition property = buildProperty("env", false);
      EntityTemplate template = buildEntityTemplate(List.of(property), List.of());
      EntityDynamicMapping mapping = buildMapping("deployment", "deployment_mapping",
          Map.of("env", ".env"), null);

      when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);

      assertThatNoException().isThrownBy(() -> service.validateMappings(List.of(mapping)));
    }

    @Test
    @DisplayName("Should pass with empty relations in mapping and no required relations in entityTemplateIdentifier")
    void shouldPassWithEmptyRelationsAndNoRequiredRelations() {
      RelationDefinition relation = buildRelation("service", false);
      EntityTemplate template = buildEntityTemplate(List.of(), List.of(relation));
      EntityDynamicMapping mapping = buildMapping("deployment", "deployment_mapping", Map.of(),
          Map.of());

      when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);

      assertThatNoException().isThrownBy(() -> service.validateMappings(List.of(mapping)));
    }

    @Test
    @DisplayName("Should validate each mapping in the list")
    void shouldValidateEachMappingInList() {
      PropertyDefinition property1 = buildProperty("env", false);
      EntityTemplate template1 = buildEntityTemplate(List.of(property1), List.of());
      EntityDynamicMapping mapping1 = buildMapping("deployment", "deployment_mapping",
          Map.of("env", ".env"), Map.of());

      PropertyDefinition property2 = buildProperty("version", false);
      EntityTemplate template2 = buildEntityTemplate(List.of(property2), List.of());
      EntityDynamicMapping mapping2 = new EntityDynamicMapping(null, "service_mapping", "service",
          ".type == \"SERVICE\"", "service mapping", "service mapping description", ".id", ".name",
          Map.of("version", ".ver"), List.of());

      when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template1);
      when(entityTemplateService.getEntityTemplateByIdentifier("service")).thenReturn(template2);

      assertThatNoException()
          .isThrownBy(() -> service.validateMappings(List.of(mapping1, mapping2)));

      verify(entityDynamicMapperValidator).validate(mapping1);
      verify(entityDynamicMapperValidator).validate(mapping2);
    }

    @Test
    @DisplayName("Should pass when mapping has valid relations matching entityTemplateIdentifier")
    void shouldPassWithValidRelations() {
      RelationDefinition relation = buildRelation("owner", true);
      EntityTemplate template = buildEntityTemplate(List.of(), List.of(relation));
      EntityDynamicMapping mapping = buildMapping("deployment", "deployment_mapping", Map.of(),
          Map.of("owner", ".owner"));

      when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);

      assertThatNoException().isThrownBy(() -> service.validateMappings(List.of(mapping)));

      verify(entityDynamicMapperValidator).validate(mapping);
    }
  }

  @Nested
  @DisplayName("validateWebhookMapping - entityTemplateIdentifier existence")
  class ValidateTemplateExistenceTests {

    @Test
    @DisplayName("Should throw EntityTemplateNotFoundException when entityTemplateIdentifier does not exist")
    void shouldThrowWhenTemplateDoesNotExist() {
      EntityDynamicMapping mapping = buildMapping("unknown-entityTemplateIdentifier",
          "unknown_mapping", Map.of(), Map.of());
      List<EntityDynamicMapping> mappings = List.of(mapping);

      // Existence is now enforced by getEntityTemplateByIdentifier, which throws
      // EntityTemplateNotFoundException when the template is missing.
      doThrow(new EntityTemplateNotFoundException("identifier", "unknown-entityTemplateIdentifier"))
          .when(entityTemplateService)
          .getEntityTemplateByIdentifier("unknown-entityTemplateIdentifier");

      assertThatThrownBy(() -> service.validateMappings(mappings))
          .isInstanceOf(EntityTemplateNotFoundException.class);

      verify(entityDynamicMapperValidator, never()).validate(mapping);
    }
  }

  @Nested
  @DisplayName("validateWebhookMapping - properties validation")
  class ValidatePropertiesTests {

    @Test
    @DisplayName("Should throw PropertyNameNotFoundEntityTemplatePropertiesException when mapping has properties but entityTemplateIdentifier has none")
    void shouldThrowWhenMappingHasPropertiesButTemplateHasNone() {
      EntityTemplate template = buildEntityTemplate(List.of(), List.of());
      EntityDynamicMapping mapping = buildMapping("deployment", "deployment_mapping",
          Map.of("env", ".env"), Map.of());
      var mappings = List.of(mapping);

      when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);
      doThrow(new PropertyNameNotFoundEntityTemplatePropertiesException(
          "Property name env not found in entity template properties"))
              .when(propertyValidationService)
              .validateMappingPropertiesAgainstTemplate(eq(template), anyList());

      assertThatThrownBy(() -> service.validateMappings(mappings))
          .isInstanceOf(PropertyNameNotFoundEntityTemplatePropertiesException.class)
          .hasMessageContaining("env");

      verify(entityDynamicMapperValidator, never()).validate(mapping);
    }

    @Test
    @DisplayName("Should throw PropertyNameNotFoundEntityTemplatePropertiesException when property not in entityTemplateIdentifier")
    void shouldThrowWhenPropertyNotFoundInTemplate() {
      PropertyDefinition property = buildProperty("environment", false);
      EntityTemplate template = buildEntityTemplate(List.of(property), List.of());
      EntityDynamicMapping mapping = buildMapping("deployment", "deployment_mapping",
          Map.of("unknown-prop", ".x"), Map.of());
      var mappings = List.of(mapping);

      when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);
      doThrow(new PropertyNameNotFoundEntityTemplatePropertiesException(
          "Property name unknown-prop not found in entity template properties"))
              .when(propertyValidationService)
              .validateMappingPropertiesAgainstTemplate(eq(template), anyList());

      assertThatThrownBy(() -> service.validateMappings(mappings))
          .isInstanceOf(PropertyNameNotFoundEntityTemplatePropertiesException.class)
          .hasMessageContaining("unknown-prop");

      verify(entityDynamicMapperValidator, never()).validate(mapping);
    }

    @Test
    @DisplayName("Should throw EntityDynamicMappingHasNoPropertiesException when required property is missing from mapping")
    void shouldThrowWhenRequiredPropertyMissingFromMapping() {
      PropertyDefinition requiredProp = buildProperty("env", true);
      EntityTemplate template = buildEntityTemplate(List.of(requiredProp), List.of());
      // mapping does not include the required property "env"
      EntityDynamicMapping mapping = buildMapping("deployment", "deployment_mapping", Map.of(),
          Map.of());
      var mappings = List.of(mapping);

      when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);
      doThrow(
          new EntityDynamicMappingHasNoPropertiesException("Missing required properties: [env]"))
              .when(propertyValidationService)
              .validateMappingPropertiesAgainstTemplate(template, List.of());

      assertThatThrownBy(() -> service.validateMappings(mappings))
          .isInstanceOf(EntityDynamicMappingHasNoPropertiesException.class)
          .hasMessageContaining("env");

      verify(entityDynamicMapperValidator, never()).validate(mapping);
    }

    @Test
    @DisplayName("Should pass when all required properties are mapped")
    void shouldPassWhenAllRequiredPropertiesMapped() {
      PropertyDefinition requiredProp = buildProperty("env", true);
      EntityTemplate template = buildEntityTemplate(List.of(requiredProp), List.of());
      EntityDynamicMapping mapping = buildMapping("deployment", "deployment_mapping",
          Map.of("env", ".environment"), Map.of());

      when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);

      assertThatNoException().isThrownBy(() -> service.validateMappings(List.of(mapping)));

      verify(entityDynamicMapperValidator).validate(mapping);
    }

    @Test
    @DisplayName("Should throw when multiple required properties are missing from mapping")
    void shouldThrowWhenMultipleRequiredPropertiesMissing() {
      PropertyDefinition prop1 = buildProperty("env", true);
      PropertyDefinition prop2 = buildProperty("version", true);
      EntityTemplate template = buildEntityTemplate(List.of(prop1, prop2), List.of());
      EntityDynamicMapping mapping = buildMapping("deployment", "deployment_mapping", Map.of(),
          Map.of());
      var mappings = List.of(mapping);

      when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);
      doThrow(new EntityDynamicMappingHasNoPropertiesException(
          "Missing required properties: [env, version]")).when(propertyValidationService)
              .validateMappingPropertiesAgainstTemplate(template, List.of());

      assertThatThrownBy(() -> service.validateMappings(mappings))
          .isInstanceOf(EntityDynamicMappingHasNoPropertiesException.class)
          .hasMessageContaining("env").hasMessageContaining("version");
      verify(entityDynamicMapperValidator, never()).validate(mapping);
    }
  }

  @Nested
  @DisplayName("validateWebhookMapping - relations validation")
  class ValidateRelationsTests {

    @Test
    @DisplayName("Should throw RelationNameNotFoundEntityTemplateRelationsException when relation not in entityTemplateIdentifier")
    void shouldThrowWhenRelationNotFoundInTemplate() {
      RelationDefinition relation = buildRelation("owner", false);
      EntityTemplate template = buildEntityTemplate(List.of(), List.of(relation));
      EntityDynamicMapping mapping = buildMapping("deployment", "deployment_mapping", Map.of(),
          Map.of("unknown-relation", ".x"));
      var mappings = List.of(mapping);

      when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);
      doThrow(new RelationNameNotFoundEntityTemplateRelationsException(
          "Relation name unknown-relation not found in entity template relations"))
              .when(relationValidationService)
              .validateMappingRelationsAgainstTemplate(template, List.of("unknown-relation"));

      assertThatThrownBy(() -> service.validateMappings(mappings))
          .isInstanceOf(RelationNameNotFoundEntityTemplateRelationsException.class)
          .hasMessageContaining("unknown-relation");

      verify(entityDynamicMapperValidator, never()).validate(mapping);
    }

    @Test
    @DisplayName("Should throw EntityDynamicMappingHasNoRelationsException when required relation is missing from mapping")
    void shouldThrowWhenRequiredRelationMissingFromMapping() {
      RelationDefinition requiredRelation = buildRelation("owner", true);
      EntityTemplate template = buildEntityTemplate(List.of(), List.of(requiredRelation));
      EntityDynamicMapping mapping = buildMapping("deployment", "deployment_mapping", Map.of(),
          Map.of());
      List<EntityDynamicMapping> mappings = List.of(mapping);

      when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);
      doThrow(
          new EntityDynamicMappingHasNoRelationsException("Missing required relations: [owner]"))
              .when(relationValidationService)
              .validateMappingRelationsAgainstTemplate(template, List.of());

      assertThatThrownBy(() -> service.validateMappings(mappings))
          .isInstanceOf(EntityDynamicMappingHasNoRelationsException.class)
          .hasMessageContaining("owner");

      verify(entityDynamicMapperValidator, never()).validate(mapping);
    }

    @Test
    @DisplayName("Should pass when required relation is mapped")
    void shouldPassWhenRequiredRelationMapped() {
      RelationDefinition requiredRelation = buildRelation("owner", true);
      EntityTemplate template = buildEntityTemplate(List.of(), List.of(requiredRelation));
      EntityDynamicMapping mapping = buildMapping("deployment", "deployment_mapping", Map.of(),
          Map.of("owner", ".ownerId"));

      when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);

      assertThatNoException().isThrownBy(() -> service.validateMappings(List.of(mapping)));

      verify(entityDynamicMapperValidator).validate(mapping);
    }

    @Test
    @DisplayName("Should throw when multiple required relations are missing from mapping")
    void shouldThrowWhenMultipleRequiredRelationsMissing() {
      RelationDefinition rel1 = buildRelation("owner", true);
      RelationDefinition rel2 = buildRelation("team", true);
      EntityTemplate template = buildEntityTemplate(List.of(), List.of(rel1, rel2));
      EntityDynamicMapping mapping = buildMapping("deployment", "deployment_mapping", Map.of(),
          Map.of());
      var mappings = List.of(mapping);

      when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);
      doThrow(new EntityDynamicMappingHasNoRelationsException(
          "Missing required relations: [owner, team]")).when(relationValidationService)
              .validateMappingRelationsAgainstTemplate(template, List.of());

      assertThatThrownBy(() -> service.validateMappings(mappings))
          .isInstanceOf(EntityDynamicMappingHasNoRelationsException.class)
          .hasMessageContaining("owner").hasMessageContaining("team");
    }

    @Test
    @DisplayName("Should skip relation validation when relations map is null")
    void shouldSkipRelationValidationWhenNull() {
      EntityTemplate template = buildEntityTemplate(List.of(), List.of());
      EntityDynamicMapping mapping = buildMapping("deployment", "deployment_mapping", Map.of(),
          null);

      when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);

      assertThatNoException().isThrownBy(() -> service.validateMappings(List.of(mapping)));

      verify(entityDynamicMapperValidator).validate(mapping);
    }
  }

  @Nested
  @DisplayName("validateWebhookMapping - mapper validator delegation")
  class MapperValidatorDelegationTests {

    @Test
    @DisplayName("Should delegate to entityDynamicMapperValidator after all domain checks pass")
    void shouldDelegateToMapperValidator() {
      EntityTemplate template = buildEntityTemplate(List.of(), List.of());
      EntityDynamicMapping mapping = buildMapping("deployment", "deployment_mapping", Map.of(),
          Map.of());

      when(entityTemplateService.getEntityTemplateByIdentifier("deployment")).thenReturn(template);

      service.validateMappings(List.of(mapping));

      verify(entityDynamicMapperValidator).validate(mapping);
    }

    @Test
    @DisplayName("Should NOT call entityDynamicMapperValidator when domain check throws")
    void shouldNotCallMapperValidatorWhenDomainCheckFails() {
      EntityDynamicMapping mapping = buildMapping("bad-entityTemplateIdentifier",
          "deployment_mapping", Map.of(), Map.of());
      var mappings = List.of(mapping);

      doThrow(new EntityTemplateNotFoundException("identifier", "bad-entityTemplateIdentifier"))
          .when(entityTemplateService)
          .getEntityTemplateByIdentifier("bad-entityTemplateIdentifier");

      assertThatThrownBy(() -> service.validateMappings(mappings))
          .isInstanceOf(EntityTemplateNotFoundException.class);

      verify(entityDynamicMapperValidator, never()).validate(mapping);
    }
  }
}
