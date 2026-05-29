package com.decathlon.idp_core.infrastructure.adapters.persistence.specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import com.decathlon.idp_core.domain.model.entity.SearchFilterNode;
import com.decathlon.idp_core.domain.model.enums.LogicalConnector;
import com.decathlon.idp_core.domain.model.enums.SearchOperator;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.EntityJpaEntity;

/// Unit tests for [EntitySearchSpecification].
///
/// Tests the static specification building logic and edge cases for various field types
/// and operators. Wildcard escaping logic is tested in [JpaPredicateBuilderTest].
/// Integration-level behavior is verified in [EntityControllerTest].
@DisplayName("EntitySearchSpecification")
class EntitySearchSpecificationTest {

    @Nested
    @DisplayName("of() — empty and null filter")
    class EmptyFilterTests {

        @Test
        @DisplayName("empty group returns non-null specification")
        void emptyGroup_returnsSpec() {
            var filter = new SearchFilterNode.Group(LogicalConnector.AND, List.of());
            Specification<EntityJpaEntity> spec = EntitySearchSpecification.of(filter);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("single criterion returns non-null specification")
        void singleCriterion_returnsSpec() {
            var filter = new SearchFilterNode.Criterion("template", SearchOperator.EQ, "microservice");
            Specification<EntityJpaEntity> spec = EntitySearchSpecification.of(filter);
            assertThat(spec).isNotNull();
        }
    }

    @Nested
    @DisplayName("of() — group connectors")
    class GroupConnectorTests {

        @Test
        @DisplayName("AND group returns non-null specification")
        void andGroup_returnsSpec() {
            var filter = new SearchFilterNode.Group(LogicalConnector.AND, List.of(
                    new SearchFilterNode.Criterion("template", SearchOperator.EQ, "microservice"),
                    new SearchFilterNode.Criterion("identifier", SearchOperator.CONTAINS, "api")
            ));
            assertThat(EntitySearchSpecification.of(filter)).isNotNull();
        }

        @Test
        @DisplayName("OR group returns non-null specification")
        void orGroup_returnsSpec() {
            var filter = new SearchFilterNode.Group(LogicalConnector.OR, List.of(
                    new SearchFilterNode.Criterion("template", SearchOperator.EQ, "microservice"),
                    new SearchFilterNode.Criterion("template", SearchOperator.EQ, "web-service")
            ));
            assertThat(EntitySearchSpecification.of(filter)).isNotNull();
        }

        @Test
        @DisplayName("nested group returns non-null specification")
        void nestedGroup_returnsSpec() {
            var inner = new SearchFilterNode.Group(LogicalConnector.OR, List.of(
                    new SearchFilterNode.Criterion("template", SearchOperator.EQ, "microservice"),
                    new SearchFilterNode.Criterion("template", SearchOperator.EQ, "web-service")
            ));
            var outer = new SearchFilterNode.Group(LogicalConnector.AND, List.of(
                    inner,
                    new SearchFilterNode.Criterion("property.language", SearchOperator.EQ, "JAVA")
            ));
            assertThat(EntitySearchSpecification.of(outer)).isNotNull();
        }
    }

    @Nested
    @DisplayName("of() — field types")
    class FieldTypeTests {

        @Test
        @DisplayName("template field returns non-null spec")
        void templateField_returnsSpec() {
            assertThat(specFor("template", SearchOperator.EQ, "microservice")).isNotNull();
        }

        @Test
        @DisplayName("identifier field returns non-null spec")
        void identifierField_returnsSpec() {
            assertThat(specFor("identifier", SearchOperator.EQ, "my-entity")).isNotNull();
        }

        @Test
        @DisplayName("name field returns non-null spec")
        void nameField_returnsSpec() {
            assertThat(specFor("name", SearchOperator.CONTAINS, "Service")).isNotNull();
        }

        @Test
        @DisplayName("property.{name} field returns non-null spec")
        void propertyField_returnsSpec() {
            assertThat(specFor("property.language", SearchOperator.EQ, "JAVA")).isNotNull();
        }

        @Test
        @DisplayName("relation.{name} field returns non-null spec")
        void relationField_returnsSpec() {
            assertThat(specFor("relation.api-link", SearchOperator.EQ, "microservice-1")).isNotNull();
        }

        @Test
        @DisplayName("relation.{name}.identifier field returns non-null spec")
        void relationIdentifierField_returnsSpec() {
            assertThat(specFor("relation.api-link.identifier", SearchOperator.EQ, "microservice-1")).isNotNull();
        }

        @Test
        @DisplayName("relation.{name}.name field returns non-null spec")
        void relationNameField_returnsSpec() {
            assertThat(specFor("relation.api-link.name", SearchOperator.CONTAINS, "Microservice")).isNotNull();
        }

        @Test
        @DisplayName("relations_as_target.{name}.identifier field returns non-null spec")
        void relationsAsTargetIdentifierField_returnsSpec() {
            assertThat(specFor("relations_as_target.api-link.identifier", SearchOperator.EQ, "web-api-1")).isNotNull();
        }

        @Test
        @DisplayName("relations_as_target.{name}.name field returns non-null spec")
        void relationsAsTargetNameField_returnsSpec() {
            assertThat(specFor("relations_as_target.api-link.name", SearchOperator.CONTAINS, "Web")).isNotNull();
        }

        @Test
        @DisplayName("bare 'relations_as_target' field (filter on reverse relation name) returns non-null spec")
        void bareRelationsAsTargetField_returnsSpec() {
            assertThat(specFor("relations_as_target", SearchOperator.NOT_CONTAINS, "used_by")).isNotNull();
        }

        @Test
        @DisplayName("bare 'relation' field (filter on relation name) returns non-null spec")
        void bareRelationField_returnsSpec() {
            assertThat(specFor("relation", SearchOperator.CONTAINS, "api-link")).isNotNull();
        }

        @Test
        @DisplayName("unknown field throws IllegalArgumentException")
        void unknownField_throwsException() {
            var criterion = new SearchFilterNode.Criterion("unknown_field", SearchOperator.EQ, "value");
            assertThatThrownBy(() -> EntitySearchSpecification.of(criterion))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unknown_field");
        }
    }

    @Nested
    @DisplayName("of() — all operators")
    class OperatorTests {

        @Test
        @DisplayName("EQ operator returns non-null spec")
        void eq_returnsSpec() {
            assertThat(specFor("identifier", SearchOperator.EQ, "val")).isNotNull();
        }

        @Test
        @DisplayName("NEQ operator returns non-null spec")
        void neq_returnsSpec() {
            assertThat(specFor("identifier", SearchOperator.NEQ, "val")).isNotNull();
        }

        @Test
        @DisplayName("CONTAINS operator returns non-null spec")
        void contains_returnsSpec() {
            assertThat(specFor("name", SearchOperator.CONTAINS, "service")).isNotNull();
        }

        @Test
        @DisplayName("NOT_CONTAINS operator returns non-null spec")
        void notContains_returnsSpec() {
            assertThat(specFor("name", SearchOperator.NOT_CONTAINS, "legacy")).isNotNull();
        }

        @Test
        @DisplayName("STARTS_WITH operator returns non-null spec")
        void startsWith_returnsSpec() {
            assertThat(specFor("name", SearchOperator.STARTS_WITH, "Web")).isNotNull();
        }

        @Test
        @DisplayName("ENDS_WITH operator returns non-null spec")
        void endsWith_returnsSpec() {
            assertThat(specFor("name", SearchOperator.ENDS_WITH, "Service")).isNotNull();
        }

        @Test
        @DisplayName("GT operator returns non-null spec")
        void gt_returnsSpec() {
            assertThat(specFor("property.version", SearchOperator.GT, "1.0")).isNotNull();
        }

        @Test
        @DisplayName("GTE operator returns non-null spec")
        void gte_returnsSpec() {
            assertThat(specFor("property.version", SearchOperator.GTE, "1.0")).isNotNull();
        }

        @Test
        @DisplayName("LT operator returns non-null spec")
        void lt_returnsSpec() {
            assertThat(specFor("property.version", SearchOperator.LT, "2.0")).isNotNull();
        }

        @Test
        @DisplayName("LTE operator returns non-null spec")
        void lte_returnsSpec() {
            assertThat(specFor("property.version", SearchOperator.LTE, "2.0")).isNotNull();
        }
    }

    private static Specification<EntityJpaEntity> specFor(String field, SearchOperator op, String value) {
        return EntitySearchSpecification.of(new SearchFilterNode.Criterion(field, op, value));
    }

    @Nested
    @DisplayName("globalTextSearch()")
    class GlobalTextSearchTests {

        @Test
        @DisplayName("returns non-null specification for a plain query")
        void plainQuery_returnsSpec() {
            assertThat(EntitySearchSpecification.globalTextSearch("checkout")).isNotNull();
        }

        @Test
        @DisplayName("returns non-null specification for a query with LIKE wildcards")
        void queryWithWildcards_returnsSpec() {
            assertThat(EntitySearchSpecification.globalTextSearch("a%b_c")).isNotNull();
        }

        @Test
        @DisplayName("returns non-null specification for an upper-case query")
        void upperCaseQuery_returnsSpec() {
            assertThat(EntitySearchSpecification.globalTextSearch("JAVA")).isNotNull();
        }
    }
}
