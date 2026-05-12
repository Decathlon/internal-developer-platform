package com.decathlon.idp_core.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.decathlon.idp_core.domain.constant.ValidationMessages;
import com.decathlon.idp_core.domain.exception.InvalidQueryException;
import com.decathlon.idp_core.domain.model.entity.EntityFilter;
import com.decathlon.idp_core.domain.model.entity.FilterCriterion;
import com.decathlon.idp_core.domain.model.enums.FilterKeyType;
import com.decathlon.idp_core.domain.model.enums.FilterOperator;

@DisplayName("EntityQueryParserService")
class EntityQueryParserServiceTest {

    private final EntityQueryParserService parser = new EntityQueryParserService();

    private void assertSingleCriterion(
            EntityFilter result,
            FilterKeyType expectedKeyType,
            String expectedKeyName,
            FilterOperator expectedOperator,
            String expectedValue) {
        assertThat(result.criteria()).hasSize(1);
        assertCriterion(result.criteria().getFirst(), expectedKeyType, expectedKeyName, expectedOperator, expectedValue);
    }

    private void assertCriterion(
            FilterCriterion criterion,
            FilterKeyType expectedKeyType,
            String expectedKeyName,
            FilterOperator expectedOperator,
            String expectedValue) {
        assertThat(criterion.keyType()).isEqualTo(expectedKeyType);
        assertThat(criterion.key()).isEqualTo(expectedKeyName);
        assertThat(criterion.operator()).isEqualTo(expectedOperator);
        assertThat(criterion.value()).isEqualTo(expectedValue);
    }

    @Nested
    @DisplayName("Attribute filters")
    class AttributeFilterTests {

        @Test
        @DisplayName("identifier equals")
        void parse_attributeIdentifierEquals() {
            var result = parser.parse("identifier=web-api-1");
            assertSingleCriterion(result, FilterKeyType.ATTRIBUTE, "identifier", FilterOperator.EQUALS, "web-api-1");
        }

        @Test
        @DisplayName("name contains")
        void parse_attributeNameContains() {
            var result = parser.parse("name:API");
            assertSingleCriterion(result, FilterKeyType.ATTRIBUTE, "name", FilterOperator.CONTAINS, "API");
        }

        @Test
        @DisplayName("name less than")
        void parse_attributeNameLessThan() {
            var result = parser.parse("name<Z");
            assertSingleCriterion(result, FilterKeyType.ATTRIBUTE, "name", FilterOperator.LESS_THAN, "Z");
        }

        @Test
        @DisplayName("name greater than")
        void parse_attributeNameGreaterThan() {
            var result = parser.parse("name>A");
            assertSingleCriterion(result, FilterKeyType.ATTRIBUTE, "name", FilterOperator.GREATER_THAN, "A");
        }
    }

    @Nested
    @DisplayName("Property filters")
    class PropertyFilterTests {

        @Test
        @DisplayName("property equals")
        void parse_propertyEquals() {
            var result = parser.parse("property.language=JAVA");
            assertSingleCriterion(result, FilterKeyType.PROPERTY, "language", FilterOperator.EQUALS, "JAVA");
        }

        @Test
        @DisplayName("property contains")
        void parse_propertyContains() {
            var result = parser.parse("property.version:1.0");
            assertSingleCriterion(result, FilterKeyType.PROPERTY, "version", FilterOperator.CONTAINS, "1.0");
        }

        @Test
        @DisplayName("property less than")
        void parse_propertyLessThan() {
            var result = parser.parse("property.port<9000");
            assertSingleCriterion(result, FilterKeyType.PROPERTY, "port", FilterOperator.LESS_THAN, "9000");
        }

        @Test
        @DisplayName("property greater than")
        void parse_propertyGreaterThan() {
            var result = parser.parse("property.port>1000");
            assertSingleCriterion(result, FilterKeyType.PROPERTY, "port", FilterOperator.GREATER_THAN, "1000");
        }
    }

    @Nested
    @DisplayName("Relation name filters")
    class RelationNameFilterTests {

        @Test
        @DisplayName("relation name equals")
        void parse_relationNameEquals() {
            var result = parser.parse("relation=api-link");
            assertSingleCriterion(result, FilterKeyType.RELATION_NAME, "", FilterOperator.EQUALS, "api-link");
        }

        @Test
        @DisplayName("relation name contains")
        void parse_relationNameContains() {
            var result = parser.parse("relation:rover");
            assertSingleCriterion(result, FilterKeyType.RELATION_NAME, "", FilterOperator.CONTAINS, "rover");
        }
    }

    @Nested
    @DisplayName("Relation entity filters")
    class RelationEntityFilterTests {

        @Test
        @DisplayName("relation entity equals")
        void parse_relationEntityEquals() {
            var result = parser.parse("relation.database=my-db");
            assertSingleCriterion(result, FilterKeyType.RELATION_ENTITY, "database", FilterOperator.EQUALS, "my-db");
        }

        @Test
        @DisplayName("relation entity contains")
        void parse_relationEntityContains() {
            var result = parser.parse("relation.database:my");
            assertSingleCriterion(result, FilterKeyType.RELATION_ENTITY, "database", FilterOperator.CONTAINS, "my");
        }
    }

    @Nested
    @DisplayName("Relation template filters")
    class RelationTemplateFilterTests {

        @Test
        @DisplayName("relation template equals")
        void parse_relationTemplateEquals() {
            var result = parser.parse("relation.database.template=postgresql");
            assertSingleCriterion(result, FilterKeyType.RELATION_TEMPLATE, "database", FilterOperator.EQUALS, "postgresql");
        }

        @Test
        @DisplayName("relation template contains")
        void parse_relationTemplateContains() {
            var result = parser.parse("relation.database.template:post");
            assertSingleCriterion(result, FilterKeyType.RELATION_TEMPLATE, "database", FilterOperator.CONTAINS, "post");
        }
    }

    @Nested
    @DisplayName("Relation property filters")
    class RelationPropertyFilterTests {

        @Test
        @DisplayName("relation property equals")
        void parse_relationPropertyEquals() {
            var result = parser.parse("relation.api-link.identifier=microservice-1");
            assertSingleCriterion(result, FilterKeyType.RELATION_PROPERTY, "api-link.identifier", FilterOperator.EQUALS, "microservice-1");
        }

        @Test
        @DisplayName("relation property contains")
        void parse_relationPropertyContains() {
            var result = parser.parse("relation.api-link.name:microservice");
            assertSingleCriterion(result, FilterKeyType.RELATION_PROPERTY, "api-link.name", FilterOperator.CONTAINS, "microservice");
        }

        @Test
        @DisplayName("relation property with hyphenated names")
        void parse_relationPropertyHyphenated() {
            var result = parser.parse("relation.my-link.custom-prop=value");
            assertSingleCriterion(result, FilterKeyType.RELATION_PROPERTY, "my-link.custom-prop", FilterOperator.EQUALS, "value");
        }
    }

    @Nested
    @DisplayName("Relations as target filters")
    class RelationsAsTargetFilterTests {

        @Test
        @DisplayName("relations_as_target name equals")
        void parse_relationsAsTargetNameEquals() {
            var result = parser.parse("relations_as_target=api-link");
            assertSingleCriterion(result, FilterKeyType.RELATIONS_AS_TARGET_NAME, "", FilterOperator.EQUALS, "api-link");
        }

        @Test
        @DisplayName("relations_as_target name contains")
        void parse_relationsAsTargetNameContains() {
            var result = parser.parse("relations_as_target:rover");
            assertSingleCriterion(result, FilterKeyType.RELATIONS_AS_TARGET_NAME, "", FilterOperator.CONTAINS, "rover");
        }

        @Test
        @DisplayName("relations_as_target property identifier equals")
        void parse_relationsAsTargetPropertyIdentifierEquals() {
            var result = parser.parse("relations_as_target.api-link.identifier=web-api-1");
            assertSingleCriterion(result, FilterKeyType.RELATIONS_AS_TARGET_PROPERTY, "api-link.identifier", FilterOperator.EQUALS, "web-api-1");
        }

        @Test
        @DisplayName("relations_as_target property name contains")
        void parse_relationsAsTargetPropertyNameContains() {
            var result = parser.parse("relations_as_target.api-link.name:microservice");
            assertSingleCriterion(result, FilterKeyType.RELATIONS_AS_TARGET_PROPERTY, "api-link.name", FilterOperator.CONTAINS, "microservice");
        }

        @Test
        @DisplayName("throws exception for unsupported property in relations_as_target")
        void parse_relationsAsTargetInvalidProperty_throwsException() {
            assertThatThrownBy(() -> parser.parse("relations_as_target.api-link.language=JAVA"))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("only 'identifier' and 'name' are supported");
        }

        @Test
        @DisplayName("throws exception for relations_as_target without property")
        void parse_relationsAsTargetWithoutProperty_throwsException() {
            assertThatThrownBy(() -> parser.parse("relations_as_target.api-link=web-api-1"))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("relations_as_target requires the form");
        }
    }

    @Nested
    @DisplayName("Combined AND criteria")
    class CombinedCriteriaTests {

        @Test
        @DisplayName("two criteria separated by semicolon")
        void parse_twoCriteriaWithSemicolon() {
            var result = parser.parse("name:API;property.language=JAVA");
            assertThat(result.criteria()).hasSize(2);
            assertCriterion(result.criteria().get(0), FilterKeyType.ATTRIBUTE, "name", FilterOperator.CONTAINS, "API");
            assertCriterion(result.criteria().get(1), FilterKeyType.PROPERTY, "language", FilterOperator.EQUALS, "JAVA");
        }

        @Test
        @DisplayName("four criteria of different key types")
        void parse_fourCriteria() {
            var result = parser.parse("name:API;property.language=JAVA;relation.database=my-db;relation.cache.template=redis");
            assertThat(result.criteria()).hasSize(4);
            assertCriterion(result.criteria().get(0), FilterKeyType.ATTRIBUTE, "name", FilterOperator.CONTAINS, "API");
            assertCriterion(result.criteria().get(1), FilterKeyType.PROPERTY, "language", FilterOperator.EQUALS, "JAVA");
            assertCriterion(result.criteria().get(2), FilterKeyType.RELATION_ENTITY, "database", FilterOperator.EQUALS, "my-db");
            assertCriterion(result.criteria().get(3), FilterKeyType.RELATION_TEMPLATE, "cache", FilterOperator.EQUALS, "redis");
        }

        @Test
        @DisplayName("five criteria including relation property")
        void parse_fiveCriteriaWithRelationProperty() {
            var result = parser.parse("name:API;property.language=JAVA;relation.database=my-db;relation.cache.template=redis;relation.api-link.identifier=service-1");
            assertThat(result.criteria()).hasSize(5);
            assertCriterion(result.criteria().get(0), FilterKeyType.ATTRIBUTE, "name", FilterOperator.CONTAINS, "API");
            assertCriterion(result.criteria().get(1), FilterKeyType.PROPERTY, "language", FilterOperator.EQUALS, "JAVA");
            assertCriterion(result.criteria().get(2), FilterKeyType.RELATION_ENTITY, "database", FilterOperator.EQUALS, "my-db");
            assertCriterion(result.criteria().get(3), FilterKeyType.RELATION_TEMPLATE, "cache", FilterOperator.EQUALS, "redis");
            assertCriterion(result.criteria().get(4), FilterKeyType.RELATION_PROPERTY, "api-link.identifier", FilterOperator.EQUALS, "service-1");
        }
    }

    @Nested
    @DisplayName("Invalid query syntax")
    class InvalidQueryTests {

        @ParameterizedTest(name = "missing operator in: ''{0}''")
        @ValueSource(strings = {"noOperatorHere", "property.lang", "relation.db"})
        @DisplayName("throws InvalidQueryException when operator is missing")
        void parse_missingOperator_throwsException(String query) {
            assertThatThrownBy(() -> parser.parse(query))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessage(ValidationMessages.FILTER_INVALID_FORMAT);
        }

        @Test
        @DisplayName("throws InvalidQueryException for unknown attribute")
        void parse_unknownAttribute_throwsException() {
            assertThatThrownBy(() -> parser.parse("unknownField=value"))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("Unknown attribute");
        }

        @Test
        @DisplayName("throws InvalidQueryException for blank value")
        void parse_blankValue_throwsException() {
            assertThatThrownBy(() -> parser.parse("name="))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("value must not be blank");
        }

        @Test
        @DisplayName("throws InvalidQueryException for blank key")
        void parse_blankKey_throwsException() {
            assertThatThrownBy(() -> parser.parse("=value"))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("key must not be blank");
        }

        @Test
        @DisplayName("throws InvalidQueryException for blank property name after prefix")
        void parse_blankPropertyName_throwsException() {
            assertThatThrownBy(() -> parser.parse("property.=JAVA"))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("key name must not be blank");
        }
    }

    @Nested
    @DisplayName("Security constraints")
    class SecurityConstraintTests {

        @Test
        @DisplayName("throws InvalidQueryException when criteria count exceeds limit")
        void parse_tooManyCriteria_throwsException() {
            var query = "property.a=1;property.b=2;property.c=3;property.d=4;property.e=5;"
                    + "property.f=6;property.g=7;property.h=8;property.i=9;property.j=10;"
                    + "property.k=11";
            assertThatThrownBy(() -> parser.parse(query))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("maximum of %d".formatted(EntityQueryParserService.MAX_CRITERIA_COUNT));
        }

        @Test
        @DisplayName("accepts exactly the maximum number of criteria")
        void parse_exactlyMaxCriteria_succeeds() {
            var query = "property.a=1;property.b=2;property.c=3;property.d=4;property.e=5;"
                    + "property.f=6;property.g=7;property.h=8;property.i=9;property.j=10";
            var result = parser.parse(query);
            assertThat(result.criteria()).hasSize(EntityQueryParserService.MAX_CRITERIA_COUNT);
        }

        @Test
        @DisplayName("throws InvalidQueryException when value exceeds max length")
        void parse_valueTooLong_throwsException() {
            var longValue = "a".repeat(EntityQueryParserService.MAX_KEY_VALUE_LENGTH + 1);
            assertThatThrownBy(() -> parser.parse("name=" + longValue))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("must not exceed %d".formatted(EntityQueryParserService.MAX_KEY_VALUE_LENGTH));
        }

        @Test
        @DisplayName("throws InvalidQueryException when key exceeds max length")
        void parse_keyTooLong_throwsException() {
            var longKey = "property." + "a".repeat(EntityQueryParserService.MAX_KEY_VALUE_LENGTH);
            assertThatThrownBy(() -> parser.parse(longKey + "=value"))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("must not exceed %d".formatted(EntityQueryParserService.MAX_KEY_VALUE_LENGTH));
        }

        @ParameterizedTest(name = "invalid key name: ''{0}''")
        @ValueSource(strings = {
                "property.lang@ge=JAVA",
                "property.my key=JAVA",
                "property.lang/age=JAVA",
                "relation.db$name=my-db"
        })
        @DisplayName("throws InvalidQueryException for invalid key name characters")
        void parse_invalidKeyNameChars_throwsException(String query) {
            assertThatThrownBy(() -> parser.parse(query))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("Invalid key name");
        }

        @ParameterizedTest(name = "valid key name: ''{0}''")
        @ValueSource(strings = {
                "property.language=JAVA",
                "property.my-key=value",
                "property.my_key=value",
                "property.key123=value",
                "relation.database=my-db",
                "relation.my-cache.template=redis"
        })
        @DisplayName("accepts valid key name characters")
        void parse_validKeyNameChars_succeeds(String query) {
            var result = parser.parse(query);
            assertThat(result.criteria()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Duplicate criterion detection")
    class DuplicateCriterionTests {

        @ParameterizedTest(name = "duplicate criterion in: ''{0}''")
        @ValueSource(strings = {
                "name=A;name=B",
                "property.language=JAVA;property.language=PYTHON",
                "relation=api-link;relation=database"
        })
        @DisplayName("throws InvalidQueryException for duplicate criteria")
        void parse_duplicateCriterion_throwsException(String query) {
            assertThatThrownBy(() -> parser.parse(query))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessage(ValidationMessages.FILTER_DUPLICATE_CRITERION);
        }

        @Test
        @DisplayName("accepts distinct attribute criteria")
        void parse_distinctAttributeCriteria_succeeds() {
            var result = parser.parse("identifier=web-api-1;name=Web API 1");
            assertThat(result.criteria()).hasSize(2);
        }

        @Test
        @DisplayName("accepts distinct property criteria")
        void parse_distinctPropertyCriteria_succeeds() {
            var result = parser.parse("property.language=JAVA;property.environment=PROD");
            assertThat(result.criteria()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Type mismatch validation")
    class TypeMismatchTests {

        @ParameterizedTest(name = "comparison operator on: ''{0}''")
        @ValueSource(strings = {"relation<api-link", "relation>api-link"})
        @DisplayName("throws InvalidQueryException for less/greater than on relation name")
        void parse_comparisonOnRelationName_throwsException(String query) {
            assertThatThrownBy(() -> parser.parse(query))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("is not applicable for field");
        }

        @ParameterizedTest(name = "comparison operator on: ''{0}''")
        @ValueSource(strings = {"relation.database<my-db", "relation.database>my-db"})
        @DisplayName("throws InvalidQueryException for less/greater than on relation entity")
        void parse_comparisonOnRelationEntity_throwsException(String query) {
            assertThatThrownBy(() -> parser.parse(query))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("is not applicable for field");
        }

        @ParameterizedTest(name = "comparison operator on: ''{0}''")
        @ValueSource(strings = {"relation.database.template<postgresql", "relation.database.template>postgresql"})
        @DisplayName("throws InvalidQueryException for less/greater than on relation template")
        void parse_comparisonOnRelationTemplate_throwsException(String query) {
            assertThatThrownBy(() -> parser.parse(query))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("is not applicable for field");
        }

        @ParameterizedTest(name = "allowed comparison on: ''{0}''")
        @ValueSource(strings = {"name<Z", "name>A", "identifier<z", "property.port<9000", "property.port>1000"})
        @DisplayName("accepts less/greater than on attributes and properties")
        void parse_comparisonOnAttributeOrProperty_succeeds(String query) {
            var result = parser.parse(query);
            assertThat(result.criteria()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("consecutive semicolons produce empty filter")
        void parse_consecutiveSemicolons_ignoresEmptyTokens() {
            var result = parser.parse("name=API;;property.lang=JAVA");
            assertThat(result.criteria()).hasSize(2);
        }

        @Test
        @DisplayName("trailing semicolon is ignored")
        void parse_trailingSemicolon_ignored() {
            var result = parser.parse("name=API;");
            assertThat(result.criteria()).hasSize(1);
        }

        @Test
        @DisplayName("leading semicolon is ignored")
        void parse_leadingSemicolon_ignored() {
            var result = parser.parse(";name=API");
            assertThat(result.criteria()).hasSize(1);
        }

        @Test
        @DisplayName("values containing SQL LIKE wildcards are accepted")
        void parse_valuesWithLikeWildcards_accepted() {
            var result = parser.parse("name:100%_success");
            assertSingleCriterion(result, FilterKeyType.ATTRIBUTE, "name", FilterOperator.CONTAINS, "100%_success");
        }
    }

}
