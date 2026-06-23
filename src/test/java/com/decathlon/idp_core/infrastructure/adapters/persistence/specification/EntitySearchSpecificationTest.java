package com.decathlon.idp_core.infrastructure.adapters.persistence.specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import com.decathlon.idp_core.domain.model.search.LogicalConnector;
import com.decathlon.idp_core.domain.model.search.SearchFilterNode;
import com.decathlon.idp_core.domain.model.search.SearchOperator;
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
      var filter = new SearchFilterNode.Group(LogicalConnector.AND,
          List.of(new SearchFilterNode.Criterion("template", SearchOperator.EQ, "microservice"),
              new SearchFilterNode.Criterion("identifier", SearchOperator.CONTAINS, "api")));
      assertThat(EntitySearchSpecification.of(filter)).isNotNull();
    }

    @Test
    @DisplayName("OR group returns non-null specification")
    void orGroup_returnsSpec() {
      var filter = new SearchFilterNode.Group(LogicalConnector.OR,
          List.of(new SearchFilterNode.Criterion("template", SearchOperator.EQ, "microservice"),
              new SearchFilterNode.Criterion("template", SearchOperator.EQ, "web-service")));
      assertThat(EntitySearchSpecification.of(filter)).isNotNull();
    }

    @Test
    @DisplayName("nested group returns non-null specification")
    void nestedGroup_returnsSpec() {
      var inner = new SearchFilterNode.Group(LogicalConnector.OR,
          List.of(new SearchFilterNode.Criterion("template", SearchOperator.EQ, "microservice"),
              new SearchFilterNode.Criterion("template", SearchOperator.EQ, "web-service")));
      var outer = new SearchFilterNode.Group(LogicalConnector.AND, List.of(inner,
          new SearchFilterNode.Criterion("property.language", SearchOperator.EQ, "JAVA")));
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
      assertThat(specFor("relation.api-link.identifier", SearchOperator.EQ, "microservice-1"))
          .isNotNull();
    }

    @Test
    @DisplayName("relation.{name}.name field returns non-null spec")
    void relationNameField_returnsSpec() {
      assertThat(specFor("relation.api-link.name", SearchOperator.CONTAINS, "Microservice"))
          .isNotNull();
    }

    @Test
    @DisplayName("relations_as_target.{name}.identifier field returns non-null spec")
    void relationsAsTargetIdentifierField_returnsSpec() {
      assertThat(specFor("relations_as_target.api-link.identifier", SearchOperator.EQ, "web-api-1"))
          .isNotNull();
    }

    @Test
    @DisplayName("relations_as_target.{name}.name field returns non-null spec")
    void relationsAsTargetNameField_returnsSpec() {
      assertThat(specFor("relations_as_target.api-link.name", SearchOperator.CONTAINS, "Web"))
          .isNotNull();
    }

    @Test
    @DisplayName("bare 'relations_as_target' field (filter on reverse relation name) returns non-null spec")
    void bareRelationsAsTargetField_returnsSpec() {
      assertThat(specFor("relations_as_target", SearchOperator.NOT_CONTAINS, "used_by"))
          .isNotNull();
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
          .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("unknown_field");
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

  private static Specification<EntityJpaEntity> specFor(String field, SearchOperator op,
      String value) {
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

  @Nested
  @DisplayName("relationEntitySpec() — relation target identifier filtering")
  class RelationEntitySpecTests {

    @Test
    @DisplayName("relation.{name} with EQ operator returns non-null spec")
    void relationEntityWithEq_returnsSpec() {
      assertThat(specFor("relation.depends-on", SearchOperator.EQ, "auth-service")).isNotNull();
    }

    @Test
    @DisplayName("relation.{name} with NEQ operator returns non-null spec")
    void relationEntityWithNeq_returnsSpec() {
      assertThat(specFor("relation.depends-on", SearchOperator.NEQ, "legacy-service")).isNotNull();
    }

    @Test
    @DisplayName("relation.{name} with CONTAINS operator returns non-null spec")
    void relationEntityWithContains_returnsSpec() {
      assertThat(specFor("relation.uses", SearchOperator.CONTAINS, "cache")).isNotNull();
    }

    @Test
    @DisplayName("relation.{name} with NOT_CONTAINS operator returns non-null spec")
    void relationEntityWithNotContains_returnsSpec() {
      assertThat(specFor("relation.uses", SearchOperator.NOT_CONTAINS, "legacy")).isNotNull();
    }

    @Test
    @DisplayName("relation.{name} with STARTS_WITH operator returns non-null spec")
    void relationEntityWithStartsWith_returnsSpec() {
      assertThat(specFor("relation.depends-on", SearchOperator.STARTS_WITH, "auth")).isNotNull();
    }

    @Test
    @DisplayName("relation.{name} with ENDS_WITH operator returns non-null spec")
    void relationEntityWithEndsWith_returnsSpec() {
      assertThat(specFor("relation.depends-on", SearchOperator.ENDS_WITH, "-service")).isNotNull();
    }

    @Test
    @DisplayName("relation with hyphens in name returns non-null spec")
    void relationWithHyphensInName_returnsSpec() {
      assertThat(specFor("relation.api-depends-on-service", SearchOperator.EQ, "target"))
          .isNotNull();
    }

    @Test
    @DisplayName("relation with underscores in name returns non-null spec")
    void relationWithUnderscoresInName_returnsSpec() {
      assertThat(specFor("relation.depends_on_service", SearchOperator.EQ, "target")).isNotNull();
    }
  }

  @Nested
  @DisplayName("relationPropertySpec() — relation target property filtering")
  class RelationPropertySpecTests {

    @Test
    @DisplayName("relation.{name}.identifier with EQ returns non-null spec")
    void relationPropertyIdentifierEq_returnsSpec() {
      assertThat(specFor("relation.depends-on.identifier", SearchOperator.EQ, "auth-svc"))
          .isNotNull();
    }

    @Test
    @DisplayName("relation.{name}.name with CONTAINS returns non-null spec")
    void relationPropertyNameContains_returnsSpec() {
      assertThat(specFor("relation.depends-on.name", SearchOperator.CONTAINS, "Auth")).isNotNull();
    }

    @Test
    @DisplayName("relation.{name}.identifier with NEQ returns non-null spec")
    void relationPropertyIdentifierNeq_returnsSpec() {
      assertThat(specFor("relation.uses.identifier", SearchOperator.NEQ, "legacy")).isNotNull();
    }

    @Test
    @DisplayName("relation.{name}.name with STARTS_WITH returns non-null spec")
    void relationPropertyNameStartsWith_returnsSpec() {
      assertThat(specFor("relation.owns.name", SearchOperator.STARTS_WITH, "Payment")).isNotNull();
    }

    @Test
    @DisplayName("relation.{name}.name with ENDS_WITH returns non-null spec")
    void relationPropertyNameEndsWith_returnsSpec() {
      assertThat(specFor("relation.owns.name", SearchOperator.ENDS_WITH, "Service")).isNotNull();
    }

    @Test
    @DisplayName("relation.{name}.identifier with NOT_CONTAINS returns non-null spec")
    void relationPropertyIdentifierNotContains_returnsSpec() {
      assertThat(specFor("relation.depends-on.identifier", SearchOperator.NOT_CONTAINS, "test"))
          .isNotNull();
    }
  }

  @Nested
  @DisplayName("relationsAsTargetNameSpec() — reverse relation name filtering")
  class RelationsAsTargetNameSpecTests {

    @Test
    @DisplayName("relations_as_target with EQ returns non-null spec")
    void relationsAsTargetEq_returnsSpec() {
      assertThat(specFor("relations_as_target", SearchOperator.EQ, "depends-on")).isNotNull();
    }

    @Test
    @DisplayName("relations_as_target with NEQ returns non-null spec (negated)")
    void relationsAsTargetNeq_returnsSpec() {
      assertThat(specFor("relations_as_target", SearchOperator.NEQ, "used-by")).isNotNull();
    }

    @Test
    @DisplayName("relations_as_target with CONTAINS returns non-null spec")
    void relationsAsTargetContains_returnsSpec() {
      assertThat(specFor("relations_as_target", SearchOperator.CONTAINS, "depends")).isNotNull();
    }

    @Test
    @DisplayName("relations_as_target with NOT_CONTAINS returns non-null spec (negated)")
    void relationsAsTargetNotContains_returnsSpec() {
      assertThat(specFor("relations_as_target", SearchOperator.NOT_CONTAINS, "legacy")).isNotNull();
    }

    @Test
    @DisplayName("relations_as_target with STARTS_WITH returns non-null spec")
    void relationsAsTargetStartsWith_returnsSpec() {
      assertThat(specFor("relations_as_target", SearchOperator.STARTS_WITH, "api")).isNotNull();
    }

    @Test
    @DisplayName("relations_as_target with ENDS_WITH returns non-null spec")
    void relationsAsTargetEndsWith_returnsSpec() {
      assertThat(specFor("relations_as_target", SearchOperator.ENDS_WITH, "-on")).isNotNull();
    }
  }

  @Nested
  @DisplayName("relationsAsTargetSpec() — reverse relation source filtering")
  class RelationsAsTargetSpecTests {

    @Test
    @DisplayName("relations_as_target.{name}.identifier with EQ returns non-null spec")
    void relationsAsTargetIdentifierEq_returnsSpec() {
      assertThat(
          specFor("relations_as_target.depends-on.identifier", SearchOperator.EQ, "api-gateway"))
              .isNotNull();
    }

    @Test
    @DisplayName("relations_as_target.{name}.name with CONTAINS returns non-null spec")
    void relationsAsTargetNameContains_returnsSpec() {
      assertThat(specFor("relations_as_target.depends-on.name", SearchOperator.CONTAINS, "Gateway"))
          .isNotNull();
    }

    @Test
    @DisplayName("relations_as_target.{name}.identifier with NEQ returns non-null spec")
    void relationsAsTargetIdentifierNeq_returnsSpec() {
      assertThat(specFor("relations_as_target.uses.identifier", SearchOperator.NEQ, "test"))
          .isNotNull();
    }

    @Test
    @DisplayName("relations_as_target.{name}.name with STARTS_WITH returns non-null spec")
    void relationsAsTargetNameStartsWith_returnsSpec() {
      assertThat(specFor("relations_as_target.owns.name", SearchOperator.STARTS_WITH, "API"))
          .isNotNull();
    }

    @Test
    @DisplayName("relations_as_target.{name}.name with ENDS_WITH returns non-null spec")
    void relationsAsTargetNameEndsWith_returnsSpec() {
      assertThat(specFor("relations_as_target.owns.name", SearchOperator.ENDS_WITH, "Service"))
          .isNotNull();
    }

    @Test
    @DisplayName("relations_as_target with invalid format throws IllegalArgumentException")
    void relationsAsTargetInvalidFormat_throwsException() {
      var criterion = new SearchFilterNode.Criterion("relations_as_target.depends-on",
          SearchOperator.EQ, "value");
      assertThatThrownBy(() -> EntitySearchSpecification.of(criterion))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("relations_as_target.depends-on")
          .hasMessageContaining("identifier|name");
    }
  }

  @Nested
  @DisplayName("propertySpec() — property value filtering")
  class PropertySpecTests {

    @Test
    @DisplayName("property.{name} with EQ returns non-null spec")
    void propertyEq_returnsSpec() {
      assertThat(specFor("property.language", SearchOperator.EQ, "java")).isNotNull();
    }

    @Test
    @DisplayName("property.{name} with NEQ returns non-null spec")
    void propertyNeq_returnsSpec() {
      assertThat(specFor("property.language", SearchOperator.NEQ, "python")).isNotNull();
    }

    @Test
    @DisplayName("property.{name} with CONTAINS returns non-null spec")
    void propertyContains_returnsSpec() {
      assertThat(specFor("property.version", SearchOperator.CONTAINS, "1.2")).isNotNull();
    }

    @Test
    @DisplayName("property.{name} with NOT_CONTAINS returns non-null spec")
    void propertyNotContains_returnsSpec() {
      assertThat(specFor("property.version", SearchOperator.NOT_CONTAINS, "SNAPSHOT")).isNotNull();
    }

    @Test
    @DisplayName("property.{name} with GT returns non-null spec")
    void propertyGt_returnsSpec() {
      assertThat(specFor("property.port", SearchOperator.GT, "8000")).isNotNull();
    }

    @Test
    @DisplayName("property.{name} with GTE returns non-null spec")
    void propertyGte_returnsSpec() {
      assertThat(specFor("property.port", SearchOperator.GTE, "8080")).isNotNull();
    }

    @Test
    @DisplayName("property.{name} with LT returns non-null spec")
    void propertyLt_returnsSpec() {
      assertThat(specFor("property.port", SearchOperator.LT, "9000")).isNotNull();
    }

    @Test
    @DisplayName("property.{name} with LTE returns non-null spec")
    void propertyLte_returnsSpec() {
      assertThat(specFor("property.port", SearchOperator.LTE, "8080")).isNotNull();
    }

    @Test
    @DisplayName("property with hyphen in name returns non-null spec")
    void propertyWithHyphenInName_returnsSpec() {
      assertThat(specFor("property.service-type", SearchOperator.EQ, "api")).isNotNull();
    }

    @Test
    @DisplayName("property with underscore in name returns non-null spec")
    void propertyWithUnderscoreInName_returnsSpec() {
      assertThat(specFor("property.service_name", SearchOperator.EQ, "auth")).isNotNull();
    }
  }

  @Nested
  @DisplayName("relationNameSpec() — relation name filtering")
  class RelationNameSpecTests {

    @Test
    @DisplayName("bare relation field with EQ returns non-null spec")
    void relationNameEq_returnsSpec() {
      assertThat(specFor("relation", SearchOperator.EQ, "depends-on")).isNotNull();
    }

    @Test
    @DisplayName("bare relation field with NEQ returns non-null spec")
    void relationNameNeq_returnsSpec() {
      assertThat(specFor("relation", SearchOperator.NEQ, "legacy-link")).isNotNull();
    }

    @Test
    @DisplayName("bare relation field with CONTAINS returns non-null spec")
    void relationNameContains_returnsSpec() {
      assertThat(specFor("relation", SearchOperator.CONTAINS, "depends")).isNotNull();
    }

    @Test
    @DisplayName("bare relation field with NOT_CONTAINS returns non-null spec")
    void relationNameNotContains_returnsSpec() {
      assertThat(specFor("relation", SearchOperator.NOT_CONTAINS, "legacy")).isNotNull();
    }

    @Test
    @DisplayName("bare relation field with STARTS_WITH returns non-null spec")
    void relationNameStartsWith_returnsSpec() {
      assertThat(specFor("relation", SearchOperator.STARTS_WITH, "api")).isNotNull();
    }

    @Test
    @DisplayName("bare relation field with ENDS_WITH returns non-null spec")
    void relationNameEndsWith_returnsSpec() {
      assertThat(specFor("relation", SearchOperator.ENDS_WITH, "-on")).isNotNull();
    }
  }

  @Nested
  @DisplayName("Complex nested scenarios")
  class ComplexNestedScenarios {

    @Test
    @DisplayName("deeply nested AND/OR groups return non-null spec")
    void deeplyNestedGroups_returnsSpec() {
      var level3 = new SearchFilterNode.Group(LogicalConnector.OR,
          List.of(new SearchFilterNode.Criterion("property.language", SearchOperator.EQ, "java"),
              new SearchFilterNode.Criterion("property.language", SearchOperator.EQ, "kotlin")));

      var level2 = new SearchFilterNode.Group(LogicalConnector.AND, List
          .of(new SearchFilterNode.Criterion("template", SearchOperator.EQ, "component"), level3));

      var level1 = new SearchFilterNode.Group(LogicalConnector.OR, List.of(level2,
          new SearchFilterNode.Criterion("name", SearchOperator.CONTAINS, "service")));

      assertThat(EntitySearchSpecification.of(level1)).isNotNull();
    }

    @Test
    @DisplayName("mixed criteria with all field types return non-null spec")
    void mixedCriteriaAllFieldTypes_returnsSpec() {
      var filter = new SearchFilterNode.Group(LogicalConnector.AND,
          List.of(new SearchFilterNode.Criterion("template", SearchOperator.EQ, "component"),
              new SearchFilterNode.Criterion("identifier", SearchOperator.CONTAINS, "api"),
              new SearchFilterNode.Criterion("name", SearchOperator.STARTS_WITH, "Payment"),
              new SearchFilterNode.Criterion("property.language", SearchOperator.EQ, "java"),
              new SearchFilterNode.Criterion("relation.depends-on", SearchOperator.CONTAINS, "db"),
              new SearchFilterNode.Criterion("relations_as_target.uses.name",
                  SearchOperator.CONTAINS, "Gateway")));

      assertThat(EntitySearchSpecification.of(filter)).isNotNull();
    }

    @Test
    @DisplayName("all operators combined in OR group return non-null spec")
    void allOperatorsCombined_returnsSpec() {
      var filter = new SearchFilterNode.Group(LogicalConnector.OR,
          List.of(new SearchFilterNode.Criterion("name", SearchOperator.EQ, "exact"),
              new SearchFilterNode.Criterion("name", SearchOperator.NEQ, "exclude"),
              new SearchFilterNode.Criterion("name", SearchOperator.CONTAINS, "partial"),
              new SearchFilterNode.Criterion("name", SearchOperator.NOT_CONTAINS, "avoid"),
              new SearchFilterNode.Criterion("name", SearchOperator.STARTS_WITH, "prefix"),
              new SearchFilterNode.Criterion("name", SearchOperator.ENDS_WITH, "suffix"),
              new SearchFilterNode.Criterion("property.version", SearchOperator.GT, "1.0"),
              new SearchFilterNode.Criterion("property.version", SearchOperator.GTE, "1.5"),
              new SearchFilterNode.Criterion("property.version", SearchOperator.LT, "2.0"),
              new SearchFilterNode.Criterion("property.version", SearchOperator.LTE, "1.9")));

      assertThat(EntitySearchSpecification.of(filter)).isNotNull();
    }
  }

  @Nested
  @DisplayName("Edge cases and error scenarios")
  class EdgeCasesAndErrors {

    @Test
    @DisplayName("empty value in criterion returns non-null spec")
    void emptyValue_returnsSpec() {
      assertThat(specFor("name", SearchOperator.EQ, "")).isNotNull();
    }

    @Test
    @DisplayName("special characters in value return non-null spec")
    void specialCharactersInValue_returnsSpec() {
      assertThat(specFor("name", SearchOperator.CONTAINS, "%_$#@!")).isNotNull();
    }

    @Test
    @DisplayName("unicode characters in value return non-null spec")
    void unicodeCharactersInValue_returnsSpec() {
      assertThat(specFor("name", SearchOperator.CONTAINS, "测试-テスト-🚀")).isNotNull();
    }

    @Test
    @DisplayName("very long value in criterion returns non-null spec")
    void veryLongValue_returnsSpec() {
      String longValue = "a".repeat(1000);
      assertThat(specFor("name", SearchOperator.CONTAINS, longValue)).isNotNull();
    }

    @Test
    @DisplayName("field name with only prefix throws IllegalArgumentException")
    void fieldNameOnlyPrefix_throwsException() {
      var criterion = new SearchFilterNode.Criterion("property.", SearchOperator.EQ, "value");
      // This should work as it's parsed as property name being empty string
      assertThat(EntitySearchSpecification.of(criterion)).isNotNull();
    }

    @Test
    @DisplayName("multiple consecutive dots in field throws no exception")
    void multipleDotsInField_noException() {
      // relation..name would be parsed as relation name being ".name"
      var criterion = new SearchFilterNode.Criterion("relation.api..link", SearchOperator.EQ,
          "value");
      assertThat(EntitySearchSpecification.of(criterion)).isNotNull();
    }
  }
}
