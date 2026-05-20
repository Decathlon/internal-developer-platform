package com.decathlon.idp_core.domain.service;

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

import com.decathlon.idp_core.domain.exception.InvalidQueryException;
import com.decathlon.idp_core.domain.model.entity.SearchFilterNode;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.enums.LogicalConnector;
import com.decathlon.idp_core.domain.model.enums.PropertyType;
import com.decathlon.idp_core.domain.model.enums.SearchOperator;
import com.decathlon.idp_core.domain.port.EntityTemplateRepositoryPort;

/// Unit tests for [EntitySearchService].
@DisplayName("SearchFilterValidationService")
class EntitySearchServiceTest {

    private final EntityTemplateRepositoryPort repository = mock(EntityTemplateRepositoryPort.class);
    private final EntitySearchService service = new EntitySearchService(repository);

    private PropertyDefinition prop(String name, PropertyType type) {
        return new PropertyDefinition(UUID.randomUUID(), name, "desc", type, false, null);
    }

    private EntityTemplate template(String identifier, PropertyDefinition... props) {
        return new EntityTemplate(UUID.randomUUID(), identifier, identifier, null, List.of(props), List.of());
    }

    @Nested
    @DisplayName("No numeric operators — no validation triggered")
    class NoNumericOperatorsTests {

        @Test
        @DisplayName("filter with only EQ operators does not throw")
        void eq_only_doesNotThrow() {
            var filter = new SearchFilterNode.Group(LogicalConnector.AND, List.of(
                    new SearchFilterNode.Criterion("template", SearchOperator.EQ, "microservice"),
                    new SearchFilterNode.Criterion("property.lifecycle", SearchOperator.EQ, "production")
            ));
            assertThatCode(() -> service.validate(filter)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("empty filter does not throw")
        void emptyFilter_doesNotThrow() {
            var filter = new SearchFilterNode.Group(LogicalConnector.AND, List.of());
            assertThatCode(() -> service.validate(filter)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Numeric operators without a template constraint")
    class NoTemplateConstraintTests {

        @Test
        @DisplayName("GT on property without template constraint does not throw (can't validate)")
        void gt_noTemplateConstraint_doesNotThrow() {
            var filter = new SearchFilterNode.Criterion("property.port", SearchOperator.GT, "8080");
            assertThatCode(() -> service.validate(filter)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GT on property with only non-EQ template constraint does not throw")
        void gt_templateConstraintNotEq_doesNotThrow() {
            var filter = new SearchFilterNode.Group(LogicalConnector.AND, List.of(
                    new SearchFilterNode.Criterion("template", SearchOperator.CONTAINS, "service"),
                    new SearchFilterNode.Criterion("property.port", SearchOperator.GT, "8080")
            ));
            assertThatCode(() -> service.validate(filter)).doesNotThrowAnyException();
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

            var filter = new SearchFilterNode.Group(LogicalConnector.AND, List.of(
                    new SearchFilterNode.Criterion("template", SearchOperator.EQ, "web-service"),
                    new SearchFilterNode.Criterion("property.port", SearchOperator.GT, "8080")
            ));
            assertThatCode(() -> service.validate(filter)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GTE, LT, LTE on a NUMBER property do not throw")
        void allNumericOperators_numberProperty_doesNotThrow() {
            when(repository.findByIdentifier("ws"))
                    .thenReturn(Optional.of(template("ws", prop("score", PropertyType.NUMBER))));

            for (SearchOperator op : List.of(
                    SearchOperator.GTE, SearchOperator.LT, SearchOperator.LTE)) {
                var filter = new SearchFilterNode.Group(LogicalConnector.AND, List.of(
                        new SearchFilterNode.Criterion("template", SearchOperator.EQ, "ws"),
                        new SearchFilterNode.Criterion("property.score", op, "5")
                ));
                assertThatCode(() -> service.validate(filter))
                        .as("operator %s should not throw for NUMBER property", op)
                        .doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("GT on a STRING property throws InvalidQueryException")
        void gt_stringProperty_throws() {
            when(repository.findByIdentifier("web-service"))
                    .thenReturn(Optional.of(template("web-service",
                            prop("programmingLanguage", PropertyType.STRING),
                            prop("port", PropertyType.NUMBER))));

            var filter = new SearchFilterNode.Group(LogicalConnector.AND, List.of(
                    new SearchFilterNode.Criterion("template", SearchOperator.EQ, "web-service"),
                    new SearchFilterNode.Criterion("property.programmingLanguage", SearchOperator.GT, "5")
            ));
            assertThatThrownBy(() -> service.validate(filter))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("programmingLanguage")
                    .hasMessageContaining("web-service")
                    .hasMessageContaining("STRING");
        }

        @Test
        @DisplayName("GT on a BOOLEAN property throws InvalidQueryException")
        void gt_booleanProperty_throws() {
            when(repository.findByIdentifier("svc"))
                    .thenReturn(Optional.of(template("svc", prop("isActive", PropertyType.BOOLEAN))));

            var filter = new SearchFilterNode.Group(LogicalConnector.AND, List.of(
                    new SearchFilterNode.Criterion("template", SearchOperator.EQ, "svc"),
                    new SearchFilterNode.Criterion("property.isActive", SearchOperator.LTE, "1")
            ));
            assertThatThrownBy(() -> service.validate(filter))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("isActive")
                    .hasMessageContaining("BOOLEAN");
        }

        @Test
        @DisplayName("unknown template (not found) does not throw — template may not exist yet")
        void unknownTemplate_doesNotThrow() {
            when(repository.findByIdentifier("unknown")).thenReturn(Optional.empty());

            var filter = new SearchFilterNode.Group(LogicalConnector.AND, List.of(
                    new SearchFilterNode.Criterion("template", SearchOperator.EQ, "unknown"),
                    new SearchFilterNode.Criterion("property.port", SearchOperator.GT, "80")
            ));
            assertThatCode(() -> service.validate(filter)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("property not defined in template does not throw — may be optional")
        void propertyNotInTemplate_doesNotThrow() {
            when(repository.findByIdentifier("ws"))
                    .thenReturn(Optional.of(template("ws", prop("port", PropertyType.NUMBER))));

            var filter = new SearchFilterNode.Group(LogicalConnector.AND, List.of(
                    new SearchFilterNode.Criterion("template", SearchOperator.EQ, "ws"),
                    new SearchFilterNode.Criterion("property.undefinedProp", SearchOperator.GT, "5")
            ));
            assertThatCode(() -> service.validate(filter)).doesNotThrowAnyException();
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

            var inner = new SearchFilterNode.Group(LogicalConnector.OR, List.of(
                    new SearchFilterNode.Criterion("property.name", SearchOperator.GT, "5")
            ));
            var filter = new SearchFilterNode.Group(LogicalConnector.AND, List.of(
                    new SearchFilterNode.Criterion("template", SearchOperator.EQ, "svc"),
                    inner
            ));
            assertThatThrownBy(() -> service.validate(filter))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("name")
                    .hasMessageContaining("STRING");
        }
    }
}
