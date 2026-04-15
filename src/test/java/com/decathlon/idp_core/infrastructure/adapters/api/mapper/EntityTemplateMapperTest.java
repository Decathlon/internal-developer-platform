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
                    .targetEntityIdentifier("service")
                    .required(false)
                    .toMany(true)
                    .build();

            var dto = EntityTemplateDtoIn.builder()
                    .identifier("service-template")
                    .description("A service template")
                    .propertiesDefinitions(List.of(propertyDefinition))
                    .relationsDefinitions(List.of(relationDefinition))
                    .build();

            // When
            EntityTemplate result = mapper.fromDtoToEntityTemplate(dto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIdentifier()).isEqualTo("service-template");
            assertThat(result.getDescription()).isEqualTo("A service template");
            assertThat(result.getPropertiesDefinitions()).hasSize(1);
            assertThat(result.getRelationsDefinitions()).hasSize(1);

            // Check property definition
            PropertyDefinition mappedProperty = result.getPropertiesDefinitions().get(0);
            assertThat(mappedProperty.getName()).isEqualTo("applicationName");
            assertThat(mappedProperty.getDescription()).isEqualTo("Name of the application");
            assertThat(mappedProperty.getType()).isEqualTo(PropertyType.STRING);
            assertThat(mappedProperty.isRequired()).isTrue();

            // Check relation definition
            RelationDefinition mappedRelation = result.getRelationsDefinitions().get(0);
            assertThat(mappedRelation.getName()).isEqualTo("dependencies");
            assertThat(mappedRelation.getTargetEntityIdentifier()).isEqualTo("service");
            assertThat(mappedRelation.isRequired()).isFalse();
            assertThat(mappedRelation.isToMany()).isTrue();
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
            var propertyRules = PropertyRules.builder()
                    .id(UUID.randomUUID())
                    .format(PropertyFormat.URL)
                    .enumValues(new String[]{})
                    .regex("")
                    .maxLength(200)
                    .minLength(1)
                    .maxValue(0)
                    .minValue(0)
                    .build();

            var propertyDefinition = PropertyDefinition.builder()
                    .id(UUID.randomUUID())
                    .name("applicationName")
                    .description("Name of the application")
                    .type(PropertyType.STRING)
                    .required(true)
                    .rules(propertyRules)
                    .build();

            var relationDefinition = RelationDefinition.builder()
                    .id(UUID.randomUUID())
                    .name("dependencies")
                    .targetEntityIdentifier("service")
                    .required(false)
                    .toMany(true)
                    .build();

            var entity = EntityTemplate.builder()
                    .id(UUID.randomUUID())
                    .identifier("service-template")
                    .description("A service template")
                    .propertiesDefinitions(List.of(propertyDefinition))
                    .relationsDefinitions(List.of(relationDefinition))
                    .build();

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
            assertThat(mappedRelation.getTargetEntityIdentifier()).isEqualTo("service");
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
            var entity1 = EntityTemplate.builder()
                    .id(UUID.randomUUID())
                    .identifier("template1")
                    .description("Template 1")
                    .propertiesDefinitions(List.of())
                    .relationsDefinitions(List.of())
                    .build();

            var entity2 = EntityTemplate.builder()
                    .id(UUID.randomUUID())
                    .identifier("template2")
                    .description("Template 2")
                    .propertiesDefinitions(List.of())
                    .relationsDefinitions(List.of())
                    .build();

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
            assertThat(result.getName()).isEqualTo("email");
            assertThat(result.getDescription()).isEqualTo("User email address");
            assertThat(result.getType()).isEqualTo(PropertyType.STRING);
            assertThat(result.isRequired()).isTrue();
            assertThat(result.getRules()).isNotNull();
            assertThat(result.getRules().getFormat()).isEqualTo(PropertyFormat.EMAIL);
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
            var rules = PropertyRules.builder()
                    .id(UUID.randomUUID())
                    .format(PropertyFormat.EMAIL)
                    .enumValues(new String[]{"value1", "value2"})
                    .regex(".*@.*")
                    .maxLength(100)
                    .minLength(5)
                    .maxValue(10)
                    .minValue(1)
                    .build();

            var entity = PropertyDefinition.builder()
                    .id(UUID.randomUUID())
                    .name("email")
                    .description("User email address")
                    .type(PropertyType.STRING)
                    .required(true)
                    .rules(rules)
                    .build();

            // When
            PropertyDefinitionDtoOut result = mapper.toDto(entity);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("email");
            assertThat(result.getDescription()).isEqualTo("User email address");
            assertThat(result.getType()).isEqualTo(PropertyType.STRING);
            assertThat(result.isRequired()).isTrue();
            assertThat(result.getRules()).isNotNull();
            assertThat(result.getRules().getId()).isEqualTo(rules.getId());
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
            assertThat(result.getFormat()).isEqualTo(PropertyFormat.URL);
            assertThat(result.getEnumValues()).containsExactly("http", "https");
            assertThat(result.getRegex()).isEqualTo("^https?://.*");
            assertThat(result.getMaxLength()).isEqualTo(500);
            assertThat(result.getMinLength()).isEqualTo(10);
            assertThat(result.getMaxValue()).isEqualTo(100);
            assertThat(result.getMinValue()).isEqualTo(1);
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
            var entity = PropertyRules.builder()
                    .id(UUID.randomUUID())
                    .format(PropertyFormat.URL)
                    .enumValues(new String[]{"http", "https"})
                    .regex("^https?://.*")
                    .maxLength(500)
                    .minLength(10)
                    .maxValue(100)
                    .minValue(1)
                    .build();

            // When
            PropertyRulesDtoOut result = mapper.toDto(entity);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(entity.getId());
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
                    .targetEntityIdentifier("service")
                    .required(true)
                    .toMany(false)
                    .build();

            // When
            RelationDefinition result = mapper.toRelationDefinition(dto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("parentService");
            assertThat(result.getTargetEntityIdentifier()).isEqualTo("service");
            assertThat(result.isRequired()).isTrue();
            assertThat(result.isToMany()).isFalse();
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
            var entity = RelationDefinition.builder()
                    .id(UUID.randomUUID())
                    .name("childServices")
                    .targetEntityIdentifier("service")
                    .required(false)
                    .toMany(true)
                    .build();

            // When
            RelationDefinitionDtoOut result = mapper.toDto(entity);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("childServices");
            assertThat(result.getTargetEntityIdentifier()).isEqualTo("service");
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
            assertThat(result.get(0).getName()).isEqualTo("prop1");
            assertThat(result.get(1).getName()).isEqualTo("prop2");
        }

        @Test
        @DisplayName("Should map list of PropertyDefinition to list of PropertyDefinitionDtoOut")
        void shouldMapPropertyEntityListToDtoOutList() {
            // Given
            var entity1 = PropertyDefinition.builder()
                    .id(UUID.randomUUID())
                    .name("prop1")
                    .description("Property 1")
                    .type(PropertyType.STRING)
                    .required(true)
                    .build();

            var entity2 = PropertyDefinition.builder()
                    .id(UUID.randomUUID())
                    .name("prop2")
                    .description("Property 2")
                    .type(PropertyType.NUMBER)
                    .required(false)
                    .build();

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
                    .targetEntityIdentifier("target1")
                    .required(true)
                    .toMany(false)
                    .build();

            var dto2 = RelationDefinitionDtoIn.builder()
                    .name("rel2")
                    .targetEntityIdentifier("target2")
                    .required(false)
                    .toMany(true)
                    .build();

            List<RelationDefinitionDtoIn> dtos = List.of(dto1, dto2);

            // When
            List<RelationDefinition> result = mapper.toRelationDefinitionEntities(dtos);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("rel1");
            assertThat(result.get(1).getName()).isEqualTo("rel2");
        }

        @Test
        @DisplayName("Should map list of RelationDefinition to list of RelationDefinitionDtoOut")
        void shouldMapRelationEntityListToDtoOutList() {
            // Given
            var entity1 = RelationDefinition.builder()
                    .id(UUID.randomUUID())
                    .name("rel1")
                    .targetEntityIdentifier("target1")
                    .required(true)
                    .toMany(false)
                    .build();

            var entity2 = RelationDefinition.builder()
                    .id(UUID.randomUUID())
                    .name("rel2")
                    .targetEntityIdentifier("target2")
                    .required(false)
                    .toMany(true)
                    .build();

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
