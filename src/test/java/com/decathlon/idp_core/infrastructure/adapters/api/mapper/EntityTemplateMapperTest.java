package com.decathlon.idp_core.infrastructure.adapters.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.PropertyRules;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.domain.model.enums.PropertyFormat;
import com.decathlon.idp_core.domain.model.enums.PropertyType;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityTemplateDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityTemplateCommonFields;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.PropertyDefinitionDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.PropertyRulesDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.RelationDefinitionDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entitytemplate.EntityTemplateDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entitytemplate.PropertyDefinitionDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entitytemplate.PropertyRulesDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entitytemplate.RelationDefinitionDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entitytemplate.EntityTemplateMapper;

@DisplayName("EntityTemplateMapper Tests")
class EntityTemplateMapperTest {

    private EntityTemplateMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new EntityTemplateMapper();
    }

    @Nested
    @DisplayName("EntityTemplate Mapping Tests")
    class EntityTemplateMappingTests {

        @Test
        @DisplayName("Should map EntityTemplateDtoIn to EntityTemplate")
        void shouldMapDtoInToEntity() {
            // Given
            var propertyRules = PropertyRulesDtoIn.builder()
                    .format(PropertyFormat.URL)
                    .enumValues(new String[]{})
                    .regex("")
                    .maxLength(200)
                    .minLength(1)
                    .maxValue(0)
                    .minValue(0)
                    .build();

            var propertyDefinition = PropertyDefinitionDtoIn.builder()
                    .name("applicationName")
                    .description("Name of the application")
                    .type(PropertyType.STRING)
                    .required(true)
                    .rules(propertyRules)
                    .build();

            var relationDefinition = RelationDefinitionDtoIn.builder()
                    .name("dependencies")
                    .targetTemplateIdentifier("service")
                    .required(false)
                    .toMany(true)
                    .build();

            var commonFields = EntityTemplateCommonFields.builder()
                    .description("A service template")
                    .propertiesDefinitions(List.of(propertyDefinition))
                    .relationsDefinitions(List.of(relationDefinition))
                    .build();

            var dto = EntityTemplateDtoIn.builder()
                    .identifier("service-template")
                    .commonFields(commonFields)
                    .build();

            // When
            EntityTemplate result = mapper.fromDtoToEntityTemplate(dto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.identifier()).isEqualTo("service-template");
            assertThat(result.description()).isEqualTo("A service template");
            assertThat(result.propertiesDefinitions()).hasSize(1);
            assertThat(result.relationsDefinitions()).hasSize(1);

            // Check property definition
            PropertyDefinition mappedProperty = result.propertiesDefinitions().get(0);
            assertThat(mappedProperty.name()).isEqualTo("applicationName");
            assertThat(mappedProperty.description()).isEqualTo("Name of the application");
            assertThat(mappedProperty.type()).isEqualTo(PropertyType.STRING);
            assertThat(mappedProperty.required()).isTrue();

            // Check relation definition
            RelationDefinition mappedRelation = result.relationsDefinitions().get(0);
            assertThat(mappedRelation.name()).isEqualTo("dependencies");
            assertThat(mappedRelation.targetTemplateIdentifier()).isEqualTo("service");
            assertThat(mappedRelation.required()).isFalse();
            assertThat(mappedRelation.toMany()).isTrue();
        }

        @Test
        @DisplayName("Should handle null EntityTemplateDtoIn")
        void shouldHandleNullDtoIn() {
            // When
            EntityTemplate result = mapper.fromDtoToEntityTemplate((EntityTemplateDtoIn) null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should map EntityTemplate to EntityTemplateDtoOut")
        void shouldMapEntityToDtoOut() {
            // Given
            var propertyRules = new PropertyRules(
                    UUID.randomUUID(),
                    PropertyFormat.URL,
                    List.of(),
                    "",
                    200,
                    1,
                    0,
                    0
            );

            var propertyDefinition = new PropertyDefinition(
                    UUID.randomUUID(),
                    "applicationName",
                    "Name of the application",
                    PropertyType.STRING,
                    true,
                    propertyRules
            );

            var relationDefinition = new RelationDefinition(
                    UUID.randomUUID(),
                    "dependencies",
                    "service",
                    false,
                    true
            );

            var entity = new EntityTemplate(
                    UUID.randomUUID(),
                    "service-template",
                    "Service Template",
                    "A service template",
                    List.of(propertyDefinition),
                    List.of(relationDefinition)
            );

            // When
            EntityTemplateDtoOut result = mapper.fromEntityTemplatetoDto(entity);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIdentifier()).isEqualTo("service-template");
            assertThat(result.getDescription()).isEqualTo("A service template");
            assertThat(result.getPropertiesDefinitions()).hasSize(1);
            assertThat(result.getRelationsDefinitions()).hasSize(1);

            // Check property definition
            PropertyDefinitionDtoOut mappedProperty = result.getPropertiesDefinitions().get(0);
            assertThat(mappedProperty.getName()).isEqualTo("applicationName");
            assertThat(mappedProperty.getDescription()).isEqualTo("Name of the application");
            assertThat(mappedProperty.getType()).isEqualTo(PropertyType.STRING);
            assertThat(mappedProperty.isRequired()).isTrue();

            // Check relation definition
            RelationDefinitionDtoOut mappedRelation = result.getRelationsDefinitions().get(0);
            assertThat(mappedRelation.getName()).isEqualTo("dependencies");
            assertThat(mappedRelation.getTargetTemplateIdentifier()).isEqualTo("service");
            assertThat(mappedRelation.isRequired()).isFalse();
            assertThat(mappedRelation.isToMany()).isTrue();
        }

        @Test
        @DisplayName("Should handle null EntityTemplate")
        void shouldHandleNullEntity() {
            // When
            EntityTemplateDtoOut result = mapper.fromEntityTemplatetoDto((EntityTemplate) null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should map list of EntityTemplate to list of EntityTemplateDtoOut")
        void shouldMapEntityListToDtoOutList() {
            // Given
            var entity1 = new EntityTemplate(
                    UUID.randomUUID(),
                    "template1",
                    "Template 1",
                    "Description 1",
                    List.of(),
                    List.of()
            );

            var entity2 = new EntityTemplate(
                    UUID.randomUUID(),
                    "template2",
                    "Template 2",
                    "Description 2",
                    List.of(),
                    List.of()
            );

            List<EntityTemplate> entities = List.of(entity1, entity2);

            // When
            List<EntityTemplateDtoOut> result = mapper.fromEntityTemplatesToDtos(entities);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getIdentifier()).isEqualTo("template1");
            assertThat(result.get(1).getIdentifier()).isEqualTo("template2");
        }

        @Test
        @DisplayName("Should handle null list of EntityTemplate")
        void shouldHandleNullEntityList() {
            // When
            List<EntityTemplateDtoOut> result = mapper.fromEntityTemplatesToDtos(null);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("PropertyDefinition Mapping Tests")
    class PropertyDefinitionMappingTests {

        @Test
        @DisplayName("Should map PropertyDefinitionDtoIn to PropertyDefinition")
        void shouldMapPropertyDtoInToEntity() {
            // Given
            var rules = PropertyRulesDtoIn.builder()
                    .format(PropertyFormat.EMAIL)
                    .enumValues(new String[]{"value1", "value2"})
                    .regex(".*@.*")
                    .maxLength(100)
                    .minLength(5)
                    .maxValue(10)
                    .minValue(1)
                    .build();

            var dto = PropertyDefinitionDtoIn.builder()
                    .name("email")
                    .description("User email address")
                    .type(PropertyType.STRING)
                    .required(true)
                    .rules(rules)
                    .build();

            // When
            PropertyDefinition result = mapper.toToPropertyDefinition(dto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("email");
            assertThat(result.description()).isEqualTo("User email address");
            assertThat(result.type()).isEqualTo(PropertyType.STRING);
            assertThat(result.required()).isTrue();
            assertThat(result.rules()).isNotNull();
            assertThat(result.rules().format()).isEqualTo(PropertyFormat.EMAIL);
        }

        @Test
        @DisplayName("Should handle null PropertyDefinitionDtoIn")
        void shouldHandleNullPropertyDtoIn() {
            // When
            PropertyDefinition result = mapper.toToPropertyDefinition((PropertyDefinitionDtoIn) null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should map PropertyDefinition to PropertyDefinitionDtoOut")
        void shouldMapPropertyEntityToDtoOut() {
            // Given
            var rules = new PropertyRules(
                    UUID.randomUUID(),
                    PropertyFormat.EMAIL,
                    List.of("value1", "value2"),
                    ".*@.*",
                    100,
                    5,
                    10,
                    1
            );

            var entity = new PropertyDefinition(
                    UUID.randomUUID(),
                    "email",
                    "User email address",
                    PropertyType.STRING,
                    true,
                    rules
            );

            // When
            PropertyDefinitionDtoOut result = mapper.toDto(entity);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("email");
            assertThat(result.getDescription()).isEqualTo("User email address");
            assertThat(result.getType()).isEqualTo(PropertyType.STRING);
            assertThat(result.isRequired()).isTrue();
            assertThat(result.getRules()).isNotNull();
            assertThat(result.getRules().getId()).isEqualTo(rules.id());
        }

        @Test
        @DisplayName("Should handle null PropertyDefinition")
        void shouldHandleNullPropertyEntity() {
            // When
            PropertyDefinitionDtoOut result = mapper.toDto((PropertyDefinition) null);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("PropertyRules Mapping Tests")
    class PropertyRulesMappingTests {

        @Test
        @DisplayName("Should map PropertyRulesDtoIn to PropertyRules")
        void shouldMapRulesDtoInToEntity() {
            // Given
            var dto = PropertyRulesDtoIn.builder()
                    .format(PropertyFormat.URL)
                    .enumValues(new String[]{"http", "https"})
                    .regex("^https?://.*")
                    .maxLength(500)
                    .minLength(10)
                    .maxValue(100)
                    .minValue(1)
                    .build();

            // When
            PropertyRules result = mapper.toPropertyRules(dto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.format()).isEqualTo(PropertyFormat.URL);
            assertThat(result.enumValues()).containsExactly("http", "https");
            assertThat(result.regex()).isEqualTo("^https?://.*");
            assertThat(result.maxLength()).isEqualTo(500);
            assertThat(result.minLength()).isEqualTo(10);
            assertThat(result.maxValue()).isEqualTo(100);
            assertThat(result.minValue()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle null PropertyRulesDtoIn")
        void shouldHandleNullRulesDtoIn() {
            // When
            PropertyRules result = mapper.toPropertyRules((PropertyRulesDtoIn) null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should map PropertyRules to PropertyRulesDtoOut")
        void shouldMapRulesEntityToDtoOut() {
            // Given
            var entity = new PropertyRules(
                    UUID.randomUUID(),
                    PropertyFormat.URL,
                    List.of("http", "https"),
                    "^https?://.*",
                    500,
                    10,
                    100,
                    1
            );

            // When
            PropertyRulesDtoOut result = mapper.toDto(entity);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(entity.id());
            assertThat(result.getFormat()).isEqualTo(PropertyFormat.URL);
            assertThat(result.getEnumValues()).containsExactly("http", "https");
            assertThat(result.getRegex()).isEqualTo("^https?://.*");
            assertThat(result.getMaxLength()).isEqualTo(500);
            assertThat(result.getMinLength()).isEqualTo(10);
            assertThat(result.getMaxValue()).isEqualTo(100);
            assertThat(result.getMinValue()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle null PropertyRules")
        void shouldHandleNullRulesEntity() {
            // When
            PropertyRulesDtoOut result = mapper.toDto((PropertyRules) null);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("RelationDefinition Mapping Tests")
    class RelationDefinitionMappingTests {

        @Test
        @DisplayName("Should map RelationDefinitionDtoIn to RelationDefinition")
        void shouldMapRelationDtoInToEntity() {
            // Given
            var dto = RelationDefinitionDtoIn.builder()
                    .name("parentService")
                    .targetTemplateIdentifier("service")
                    .required(true)
                    .toMany(false)
                    .build();

            // When
            RelationDefinition result = mapper.toRelationDefinition(dto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("parentService");
            assertThat(result.targetTemplateIdentifier()).isEqualTo("service");
            assertThat(result.required()).isTrue();
            assertThat(result.toMany()).isFalse();
        }

        @Test
        @DisplayName("Should handle null RelationDefinitionDtoIn")
        void shouldHandleNullRelationDtoIn() {
            // When
            RelationDefinition result = mapper.toRelationDefinition((RelationDefinitionDtoIn) null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should map RelationDefinition to RelationDefinitionDtoOut")
        void shouldMapRelationEntityToDtoOut() {
            // Given
            var entity = new RelationDefinition(
                    UUID.randomUUID(),
                    "childServices",
                    "service",
                    false,
                    true
            );

            // When
            RelationDefinitionDtoOut result = mapper.toDto(entity);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("childServices");
            assertThat(result.getTargetTemplateIdentifier()).isEqualTo("service");
            assertThat(result.isRequired()).isFalse();
            assertThat(result.isToMany()).isTrue();
        }

        @Test
        @DisplayName("Should handle null RelationDefinition")
        void shouldHandleNullRelationEntity() {
            // When
            RelationDefinitionDtoOut result = mapper.toDto((RelationDefinition) null);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("List Mapping Tests")
    class ListMappingTests {

        @Test
        @DisplayName("Should map list of PropertyDefinitionDtoIn to list of PropertyDefinition")
        void shouldMapPropertyDtoInListToEntityList() {
            // Given
            var dto1 = PropertyDefinitionDtoIn.builder()
                    .name("prop1")
                    .description("Property 1")
                    .type(PropertyType.STRING)
                    .required(true)
                    .build();

            var dto2 = PropertyDefinitionDtoIn.builder()
                    .name("prop2")
                    .description("Property 2")
                    .type(PropertyType.NUMBER)
                    .required(false)
                    .build();

            List<PropertyDefinitionDtoIn> dtos = List.of(dto1, dto2);

            // When
            List<PropertyDefinition> result = mapper.toPropertyDefinitionEntities(dtos);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("prop1");
            assertThat(result.get(1).name()).isEqualTo("prop2");
        }

        @Test
        @DisplayName("Should map list of PropertyDefinition to list of PropertyDefinitionDtoOut")
        void shouldMapPropertyEntityListToDtoOutList() {
            // Given
            var entity1 = new PropertyDefinition(
                    UUID.randomUUID(),
                    "prop1",
                    "Property 1",
                    PropertyType.STRING,
                    true,
                    null
            );

            var entity2 = new PropertyDefinition(
                    UUID.randomUUID(),
                    "prop2",
                    "Property 2",
                    PropertyType.NUMBER,
                    false,
                    null
            );

            List<PropertyDefinition> entities = List.of(entity1, entity2);

            // When
            List<PropertyDefinitionDtoOut> result = mapper.toPropertyDefinitionDtos(entities);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("prop1");
            assertThat(result.get(1).getName()).isEqualTo("prop2");
        }

        @Test
        @DisplayName("Should map list of RelationDefinitionDtoIn to list of RelationDefinition")
        void shouldMapRelationDtoInListToEntityList() {
            // Given
            var dto1 = RelationDefinitionDtoIn.builder()
                    .name("rel1")
                    .targetTemplateIdentifier("target1")
                    .required(true)
                    .toMany(false)
                    .build();

            var dto2 = RelationDefinitionDtoIn.builder()
                    .name("rel2")
                    .targetTemplateIdentifier("target2")
                    .required(false)
                    .toMany(true)
                    .build();

            List<RelationDefinitionDtoIn> dtos = List.of(dto1, dto2);

            // When
            List<RelationDefinition> result = mapper.toRelationDefinitionEntities(dtos);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("rel1");
            assertThat(result.get(1).name()).isEqualTo("rel2");
        }

        @Test
        @DisplayName("Should map list of RelationDefinition to list of RelationDefinitionDtoOut")
        void shouldMapRelationEntityListToDtoOutList() {
            // Given
            var entity1 = new RelationDefinition(
                    UUID.randomUUID(),
                    "rel1",
                    "target1",
                    true,
                    false
            );

            var entity2 = new RelationDefinition(
                    UUID.randomUUID(),
                    "rel2",
                    "target2",
                    false,
                    true
            );

            List<RelationDefinition> entities = List.of(entity1, entity2);

            // When
            List<RelationDefinitionDtoOut> result = mapper.toRelationDefinitionDtos(entities);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("rel1");
            assertThat(result.get(1).getName()).isEqualTo("rel2");
        }

        @Test
        @DisplayName("Should handle null lists")
        void shouldHandleNullLists() {
            // When & Then
            assertThat(mapper.toPropertyDefinitionEntities(null)).isEmpty();
            assertThat(mapper.toPropertyDefinitionDtos(null)).isEmpty();
            assertThat(mapper.toRelationDefinitionEntities(null)).isEmpty();
            assertThat(mapper.toRelationDefinitionDtos(null)).isEmpty();
        }
    }
}
