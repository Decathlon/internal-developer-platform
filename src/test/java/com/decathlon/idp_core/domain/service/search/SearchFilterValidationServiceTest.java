package com.decathlon.idp_core.domain.service.search;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.constant.SearchConstraints;
import com.decathlon.idp_core.domain.exception.search.InvalidSearchQueryException;
import com.decathlon.idp_core.domain.model.entity.SearchFilterNode;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.enums.LogicalConnector;
import com.decathlon.idp_core.domain.model.enums.PropertyType;
import com.decathlon.idp_core.domain.model.enums.SearchOperator;
import com.decathlon.idp_core.domain.port.EntityTemplateRepositoryPort;

/// Unit tests for [SearchFilterValidationService].
@DisplayName("SearchFilterValidationService")
class SearchFilterValidationServiceTest {

  private final EntityTemplateRepositoryPort repository = mock(EntityTemplateRepositoryPort.class);
  private final SearchFilterValidationService service = new SearchFilterValidationService(
      repository);

  private static final SearchFilterNode EMPTY_FILTER = new SearchFilterNode.Group(
      LogicalConnector.AND, List.of());

  private PropertyDefinition prop(String name, PropertyType type) {
    return new PropertyDefinition(UUID.randomUUID(), name, "desc", type, false, null);
  }

  private EntityTemplate template(String identifier, PropertyDefinition... props) {
    return new EntityTemplate(UUID.randomUUID(), identifier, identifier, null, List.of(props),
        List.of());
  }

  // =========================================================================
  // Query string length validation
  // =========================================================================

  @Nested
  @DisplayName("Query string length validation")
  class QueryLengthTests {

    @Test
    @DisplayName("null query does not throw")
    void nullQuery_doesNotThrow() {
      assertThatCode(() -> service.validate(EMPTY_FILTER, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("short query does not throw")
    void shortQuery_doesNotThrow() {
      assertThatCode(() -> service.validate(EMPTY_FILTER, "checkout")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("query at exact limit does not throw")
    void queryAtLimit_doesNotThrow() {
      String atLimit = "x".repeat(SearchConstraints.MAX_QUERY_LENGTH);
      assertThatCode(() -> service.validate(EMPTY_FILTER, atLimit)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("query exceeding limit throws")
    void queryOverLimit_throws() {
      String tooLong = "x".repeat(SearchConstraints.MAX_QUERY_LENGTH + 1);
      assertThatThrownBy(() -> service.validate(EMPTY_FILTER, tooLong))
          .isInstanceOf(InvalidSearchQueryException.class)
          .hasMessageContaining(String.valueOf(SearchConstraints.MAX_QUERY_LENGTH));
    }
  }

  // =========================================================================
  // Field name grammar validation
  // =========================================================================

  @Nested
  @DisplayName("Field name validation")
  class FieldNameTests {

    @Test
    @DisplayName("'template' field is accepted")
    void template_accepted() {
      assertThatCode(() -> service.validate(criterion("template", SearchOperator.EQ, "val"), null))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("'identifier' field is accepted")
    void identifier_accepted() {
      assertThatCode(
          () -> service.validate(criterion("identifier", SearchOperator.EQ, "val"), null))
              .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("'name' field is accepted")
    void name_accepted() {
      assertThatCode(() -> service.validate(criterion("name", SearchOperator.EQ, "val"), null))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("'relation' bare field is accepted")
    void relation_accepted() {
      assertThatCode(() -> service.validate(criterion("relation", SearchOperator.EQ, "val"), null))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("'relations_as_target' bare field is accepted")
    void relationsAsTarget_accepted() {
      assertThatCode(
          () -> service.validate(criterion("relations_as_target", SearchOperator.EQ, "val"), null))
              .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("'property.{name}' field is accepted")
    void propertyField_accepted() {
      assertThatCode(
          () -> service.validate(criterion("property.language", SearchOperator.EQ, "val"), null))
              .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("'relation.{name}' field is accepted")
    void relationField_accepted() {
      assertThatCode(
          () -> service.validate(criterion("relation.api-link", SearchOperator.EQ, "val"), null))
              .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("'relations_as_target.{name}.identifier' field is accepted")
    void relationsAsTargetIdentifierField_accepted() {
      assertThatCode(() -> service.validate(
          criterion("relations_as_target.api-link.identifier", SearchOperator.EQ, "val"), null))
              .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("'relations_as_target.{name}.name' field is accepted")
    void relationsAsTargetNameField_accepted() {
      assertThatCode(() -> service
          .validate(criterion("relations_as_target.api-link.name", SearchOperator.EQ, "val"), null))
              .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("unknown field throws")
    void unknownField_throws() {
      assertThatThrownBy(
          () -> service.validate(criterion("badField", SearchOperator.EQ, "val"), null))
              .isInstanceOf(InvalidSearchQueryException.class).hasMessageContaining("badField");
    }

    @Test
    @DisplayName("'relations_as_target' without subfield throws")
    void relationsAsTarget_missingSubfield_throws() {
      assertThatThrownBy(() -> service
          .validate(criterion("relations_as_target.api-link", SearchOperator.EQ, "val"), null))
              .isInstanceOf(InvalidSearchQueryException.class);
    }

    @Test
    @DisplayName("field validation applies to criteria nested inside groups")
    void invalidField_nestedInGroup_throws() {
      var filter = new SearchFilterNode.Group(LogicalConnector.AND,
          List.of(criterion("template", SearchOperator.EQ, "svc"),
              criterion("unknownField", SearchOperator.EQ, "val")));
      assertThatThrownBy(() -> service.validate(filter, null))
          .isInstanceOf(InvalidSearchQueryException.class).hasMessageContaining("unknownField");
    }
  }

  // =========================================================================
  // Numeric operator constraint validation
  // =========================================================================

  @Nested
  @DisplayName("Numeric operator constraints")
  class NumericOperatorTests {

    @Test
    @DisplayName("GT on property.{name} with a numeric value is accepted")
    void gt_onProperty_numericValue_accepted() {
      assertThatCode(
          () -> service.validate(criterion("property.port", SearchOperator.GT, "8080"), null))
              .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("GTE on property.{name} with a decimal value is accepted")
    void gte_onProperty_decimalValue_accepted() {
      assertThatCode(
          () -> service.validate(criterion("property.score", SearchOperator.GTE, "1.5"), null))
              .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("GT on 'template' field throws — numeric ops only on property.{name}")
    void gt_onTemplateField_throws() {
      assertThatThrownBy(
          () -> service.validate(criterion("template", SearchOperator.GT, "5"), null))
              .isInstanceOf(InvalidSearchQueryException.class).hasMessageContaining("GT");
    }

    @Test
    @DisplayName("LT on 'identifier' field throws — numeric ops only on property.{name}")
    void lt_onIdentifierField_throws() {
      assertThatThrownBy(
          () -> service.validate(criterion("identifier", SearchOperator.LT, "5"), null))
              .isInstanceOf(InvalidSearchQueryException.class).hasMessageContaining("LT");
    }

    @Test
    @DisplayName("GT on property.{name} with a non-numeric value throws")
    void gt_nonNumericValue_throws() {
      assertThatThrownBy(
          () -> service.validate(criterion("property.port", SearchOperator.GT, "abc"), null))
              .isInstanceOf(InvalidSearchQueryException.class).hasMessageContaining("abc")
              .hasMessageContaining("GT");
    }

    @Test
    @DisplayName("LTE on property.{name} with alphanumeric non-numeric value throws")
    void lte_nonNumericValue_throws() {
      assertThatThrownBy(
          () -> service.validate(criterion("property.size", SearchOperator.LTE, "10MB"), null))
              .isInstanceOf(InvalidSearchQueryException.class).hasMessageContaining("10MB");
    }
  }

  // =========================================================================
  // Template-scoped property-type validation
  // =========================================================================

  @Nested
  @DisplayName("No numeric operators — no validation triggered")
  class NoNumericOperatorsTests {

    @Test
    @DisplayName("filter with only EQ operators does not throw")
    void eq_only_doesNotThrow() {
      var filter = new SearchFilterNode.Group(LogicalConnector.AND, List.of(
          new SearchFilterNode.Criterion("template", SearchOperator.EQ, "microservice"),
          new SearchFilterNode.Criterion("property.lifecycle", SearchOperator.EQ, "production")));
      assertThatCode(() -> service.validate(filter, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("empty filter does not throw")
    void emptyFilter_doesNotThrow() {
      assertThatCode(() -> service.validate(EMPTY_FILTER, null)).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Numeric operators without a template constraint")
  class NoTemplateConstraintTests {

    @Test
    @DisplayName("GT on property without template constraint does not throw (can't validate)")
    void gt_noTemplateConstraint_doesNotThrow() {
      assertThatCode(() -> service.validate(
          new SearchFilterNode.Criterion("property.port", SearchOperator.GT, "8080"), null))
              .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("GT on property with only non-EQ template constraint does not throw")
    void gt_templateConstraintNotEq_doesNotThrow() {
      var filter = new SearchFilterNode.Group(LogicalConnector.AND,
          List.of(new SearchFilterNode.Criterion("template", SearchOperator.CONTAINS, "service"),
              new SearchFilterNode.Criterion("property.port", SearchOperator.GT, "8080")));
      assertThatCode(() -> service.validate(filter, null)).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Numeric operators with a template constraint (type check enabled)")
  class WithTemplateConstraintTests {

    @Test
    @DisplayName("GT on a NUMBER property does not throw")
    void gt_numberProperty_doesNotThrow() {
      when(repository.findByIdentifier("web-service"))
          .thenReturn(Optional.of(template("web-service", prop("port", PropertyType.NUMBER))));

      var filter = new SearchFilterNode.Group(LogicalConnector.AND,
          List.of(new SearchFilterNode.Criterion("template", SearchOperator.EQ, "web-service"),
              new SearchFilterNode.Criterion("property.port", SearchOperator.GT, "8080")));
      assertThatCode(() -> service.validate(filter, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("GTE, LT, LTE on a NUMBER property do not throw")
    void allNumericOperators_numberProperty_doesNotThrow() {
      when(repository.findByIdentifier("ws"))
          .thenReturn(Optional.of(template("ws", prop("score", PropertyType.NUMBER))));

      for (SearchOperator op : List.of(SearchOperator.GTE, SearchOperator.LT, SearchOperator.LTE)) {
        var filter = new SearchFilterNode.Group(LogicalConnector.AND,
            List.of(new SearchFilterNode.Criterion("template", SearchOperator.EQ, "ws"),
                new SearchFilterNode.Criterion("property.score", op, "5")));
        assertThatCode(() -> service.validate(filter, null))
            .as("operator %s should not throw for NUMBER property", op).doesNotThrowAnyException();
      }
    }

    @Test
    @DisplayName("GT on a STRING property throws")
    void gt_stringProperty_throws() {
      when(repository.findByIdentifier("web-service")).thenReturn(
          Optional.of(template("web-service", prop("programmingLanguage", PropertyType.STRING),
              prop("port", PropertyType.NUMBER))));

      var filter = new SearchFilterNode.Group(LogicalConnector.AND, List.of(
          new SearchFilterNode.Criterion("template", SearchOperator.EQ, "web-service"),
          new SearchFilterNode.Criterion("property.programmingLanguage", SearchOperator.GT, "5")));
      assertThatThrownBy(() -> service.validate(filter, null))
          .isInstanceOf(InvalidSearchQueryException.class)
          .hasMessageContaining("programmingLanguage").hasMessageContaining("web-service")
          .hasMessageContaining("STRING");
    }

    @Test
    @DisplayName("GT on a BOOLEAN property throws")
    void gt_booleanProperty_throws() {
      when(repository.findByIdentifier("svc"))
          .thenReturn(Optional.of(template("svc", prop("isActive", PropertyType.BOOLEAN))));

      var filter = new SearchFilterNode.Group(LogicalConnector.AND,
          List.of(new SearchFilterNode.Criterion("template", SearchOperator.EQ, "svc"),
              new SearchFilterNode.Criterion("property.isActive", SearchOperator.LTE, "1")));
      assertThatThrownBy(() -> service.validate(filter, null))
          .isInstanceOf(InvalidSearchQueryException.class).hasMessageContaining("isActive")
          .hasMessageContaining("BOOLEAN");
    }

    @Test
    @DisplayName("unknown template does not throw — template may not exist yet")
    void unknownTemplate_doesNotThrow() {
      when(repository.findByIdentifier("unknown")).thenReturn(Optional.empty());

      var filter = new SearchFilterNode.Group(LogicalConnector.AND,
          List.of(new SearchFilterNode.Criterion("template", SearchOperator.EQ, "unknown"),
              new SearchFilterNode.Criterion("property.port", SearchOperator.GT, "80")));
      assertThatCode(() -> service.validate(filter, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("property not defined in template does not throw — may be optional")
    void propertyNotInTemplate_doesNotThrow() {
      when(repository.findByIdentifier("ws"))
          .thenReturn(Optional.of(template("ws", prop("port", PropertyType.NUMBER))));

      var filter = new SearchFilterNode.Group(LogicalConnector.AND,
          List.of(new SearchFilterNode.Criterion("template", SearchOperator.EQ, "ws"),
              new SearchFilterNode.Criterion("property.undefinedProp", SearchOperator.GT, "5")));
      assertThatCode(() -> service.validate(filter, null)).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Nested filter trees")
  class NestedTreeTests {

    @Test
    @DisplayName("GT on STRING property nested inside OR group throws")
    void gt_stringProperty_nestedInOr_throws() {
      when(repository.findByIdentifier("svc"))
          .thenReturn(Optional.of(template("svc", prop("name", PropertyType.STRING))));

      var inner = new SearchFilterNode.Group(LogicalConnector.OR,
          List.of(new SearchFilterNode.Criterion("property.name", SearchOperator.GT, "5")));
      var filter = new SearchFilterNode.Group(LogicalConnector.AND,
          List.of(new SearchFilterNode.Criterion("template", SearchOperator.EQ, "svc"), inner));
      assertThatThrownBy(() -> service.validate(filter, null))
          .isInstanceOf(InvalidSearchQueryException.class).hasMessageContaining("name")
          .hasMessageContaining("STRING");
    }
  }

  private static SearchFilterNode.Criterion criterion(String field, SearchOperator op,
      String value) {
    return new SearchFilterNode.Criterion(field, op, value);
  }
}
