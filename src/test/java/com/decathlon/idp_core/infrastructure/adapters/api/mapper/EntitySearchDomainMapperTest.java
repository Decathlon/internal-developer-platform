package com.decathlon.idp_core.infrastructure.adapters.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.exception.InvalidQueryException;
import com.decathlon.idp_core.domain.model.entity.SearchFilterNode;
import com.decathlon.idp_core.domain.model.enums.LogicalConnector;
import com.decathlon.idp_core.domain.model.enums.SearchOperator;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.FilterNodeDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity.EntitySearchDomainMapper;

/// Unit tests for [EntitySearchDomainMapper].
@DisplayName("EntitySearchDomainMapper")
class EntitySearchDomainMapperTest {

    private final EntitySearchDomainMapper mapper = new EntitySearchDomainMapper();

    @Nested
    @DisplayName("toDomain() — null and empty inputs")
    class NullAndEmptyTests {

        @Test
        @DisplayName("null DTO returns empty AND group")
        void null_returnsEmptyGroup() {
            var result = mapper.toDomain(null);
            assertThat(result).isInstanceOf(SearchFilterNode.Group.class);
            var group = (SearchFilterNode.Group) result;
            assertThat(group.connector()).isEqualTo(LogicalConnector.AND);
            assertThat(group.nodes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("toDomain() — criterion leaf node")
    class CriterionTests {

        @Test
        @DisplayName("valid criterion is correctly mapped")
        void validCriterion_mapped() {
            var dto = new FilterNodeDtoIn(null, null, "template", "EQ", "microservice");
            var result = mapper.toDomain(dto);
            assertThat(result).isInstanceOf(SearchFilterNode.Criterion.class);
            var criterion = (SearchFilterNode.Criterion) result;
            assertThat(criterion.field()).isEqualTo("template");
            assertThat(criterion.operation()).isEqualTo(SearchOperator.EQ);
            assertThat(criterion.value()).isEqualTo("microservice");
        }

        @Test
        @DisplayName("operation is case-insensitive")
        void operation_caseInsensitive() {
            var dto = new FilterNodeDtoIn(null, null, "identifier", "contains", "api");
            var result = (SearchFilterNode.Criterion) mapper.toDomain(dto);
            assertThat(result.operation()).isEqualTo(SearchOperator.CONTAINS);
        }

        @Test
        @DisplayName("throws when field is null")
        void nullField_throws() {
            var dto = new FilterNodeDtoIn(null, null, null, "EQ", "value");
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("field");
        }

        @Test
        @DisplayName("throws when operation is null")
        void nullOperation_throws() {
            var dto = new FilterNodeDtoIn(null, null, "identifier", null, "value");
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("operation");
        }

        @Test
        @DisplayName("throws when value is null")
        void nullValue_throws() {
            var dto = new FilterNodeDtoIn(null, null, "identifier", "EQ", null);
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("value");
        }

        @Test
        @DisplayName("throws for invalid operation string")
        void invalidOperation_throws() {
            var dto = new FilterNodeDtoIn(null, null, "identifier", "LIKE", "api");
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("LIKE");
        }

        @Test
        @DisplayName("throws for unknown field")
        void unknownField_throws() {
            var dto = new FilterNodeDtoIn(null, null, "badField", "EQ", "value");
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("badField");
        }
    }

    @Nested
    @DisplayName("toDomain() — group nodes")
    class GroupTests {

        @Test
        @DisplayName("valid AND group is correctly mapped")
        void validAndGroup_mapped() {
            var child1 = new FilterNodeDtoIn(null, null, "template", "EQ", "microservice");
            var child2 = new FilterNodeDtoIn(null, null, "identifier", "CONTAINS", "api");
            var dto = new FilterNodeDtoIn("AND", List.of(child1, child2), null, null, null);

            var result = mapper.toDomain(dto);
            assertThat(result).isInstanceOf(SearchFilterNode.Group.class);
            var group = (SearchFilterNode.Group) result;
            assertThat(group.connector()).isEqualTo(LogicalConnector.AND);
            assertThat(group.nodes()).hasSize(2);
        }

        @Test
        @DisplayName("connector is case-insensitive")
        void connector_caseInsensitive() {
            var child = new FilterNodeDtoIn(null, null, "template", "EQ", "microservice");
            var dto = new FilterNodeDtoIn("or", List.of(child), null, null, null);
            var group = (SearchFilterNode.Group) mapper.toDomain(dto);
            assertThat(group.connector()).isEqualTo(LogicalConnector.OR);
        }

        @Test
        @DisplayName("'IN' is rejected as an unsupported connector")
        void inConnector_rejectedAsInvalidConnector() {
            var child = new FilterNodeDtoIn(null, null, "template", "EQ", "microservice");
            var dto = new FilterNodeDtoIn("IN", List.of(child), null, null, null);
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("IN");
        }

        @Test
        @DisplayName("throws for missing connector in group")
        void missingConnector_throws() {
            var child = new FilterNodeDtoIn(null, null, "template", "EQ", "microservice");
            var dto = new FilterNodeDtoIn(null, List.of(child), null, null, null);
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("connector");
        }

        @Test
        @DisplayName("throws for empty criteria list in group")
        void emptyCriteria_throws() {
            var dto = new FilterNodeDtoIn("AND", List.of(), null, null, null);
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("criteria");
        }

        @Test
        @DisplayName("throws for invalid connector string")
        void invalidConnector_throws() {
            var child = new FilterNodeDtoIn(null, null, "template", "EQ", "microservice");
            var dto = new FilterNodeDtoIn("NAND", List.of(child), null, null, null);
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("NAND");
        }
    }

    @Nested
    @DisplayName("toDomain() — valid fields")
    class FieldValidationTests {

        @Test
        @DisplayName("'template' field is accepted")
        void template_accepted() {
            assertThat(criterionFor("template")).isNotNull();
        }

        @Test
        @DisplayName("'identifier' field is accepted")
        void identifier_accepted() {
            assertThat(criterionFor("identifier")).isNotNull();
        }

        @Test
        @DisplayName("'name' field is accepted")
        void name_accepted() {
            assertThat(criterionFor("name")).isNotNull();
        }

        @Test
        @DisplayName("'property.{name}' field is accepted")
        void propertyField_accepted() {
            assertThat(criterionFor("property.language")).isNotNull();
        }

        @Test
        @DisplayName("'relation.{name}' field is accepted")
        void relationField_accepted() {
            assertThat(criterionFor("relation.api-link")).isNotNull();
        }

        @Test
        @DisplayName("'relation.{name}.identifier' field is accepted")
        void relationIdentifierField_accepted() {
            assertThat(criterionFor("relation.api-link.identifier")).isNotNull();
        }

        @Test
        @DisplayName("'relations_as_target.{name}.identifier' field is accepted")
        void relationsAsTargetIdentifierField_accepted() {
            assertThat(criterionFor("relations_as_target.api-link.identifier")).isNotNull();
        }

        @Test
        @DisplayName("'relations_as_target.{name}.name' field is accepted")
        void relationsAsTargetNameField_accepted() {
            assertThat(criterionFor("relations_as_target.api-link.name")).isNotNull();
        }

        @Test
        @DisplayName("'relations_as_target' without subfield throws")
        void relationsAsTarget_missingSubfield_throws() {
            var dto = new FilterNodeDtoIn(null, null, "relations_as_target.api-link", "EQ", "value");
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(InvalidQueryException.class);
        }

        private SearchFilterNode criterionFor(String field) {
            return mapper.toDomain(new FilterNodeDtoIn(null, null, field, "EQ", "value"));
        }
    }

    @Nested
    @DisplayName("toDomain() — safety limits")
    class SafetyLimitsTests {

        @Test
        @DisplayName("throws when total criteria exceed maximum")
        void tooManyCriteria_throws() {
            var innerCriteria = new java.util.ArrayList<FilterNodeDtoIn>();
            for (int i = 0; i <= EntitySearchDomainMapper.MAX_TOTAL_CRITERIA; i++) {
                innerCriteria.add(new FilterNodeDtoIn(null, null, "template", "EQ", "v" + i));
            }
            var dto = new FilterNodeDtoIn("OR", innerCriteria, null, null, null);
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining(String.valueOf(EntitySearchDomainMapper.MAX_TOTAL_CRITERIA));
        }

        @Test
        @DisplayName("throws when nesting exceeds maximum depth")
        void nestingTooDeep_throws() {
            FilterNodeDtoIn node = new FilterNodeDtoIn(null, null, "template", "EQ", "v");
            for (int i = 0; i <= EntitySearchDomainMapper.MAX_NESTING_DEPTH; i++) {
                node = new FilterNodeDtoIn("AND", List.of(node), null, null, null);
            }
            var root = node;
            assertThatThrownBy(() -> mapper.toDomain(root))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining(String.valueOf(EntitySearchDomainMapper.MAX_NESTING_DEPTH));
        }
    }

    @Nested
    @DisplayName("toDomain() — numeric operator validation")
    class NumericOperatorTests {

        @Test
        @DisplayName("GT on property.{name} with a numeric value is accepted")
        void gt_onProperty_numericValue_accepted() {
            var dto = new FilterNodeDtoIn(null, null, "property.port", "GT", "8080");
            assertThat(mapper.toDomain(dto)).isInstanceOf(SearchFilterNode.Criterion.class);
        }

        @Test
        @DisplayName("GTE on property.{name} with a decimal value is accepted")
        void gte_onProperty_decimalValue_accepted() {
            var dto = new FilterNodeDtoIn(null, null, "property.score", "GTE", "1.5");
            assertThat(mapper.toDomain(dto)).isInstanceOf(SearchFilterNode.Criterion.class);
        }

        @Test
        @DisplayName("GT on 'template' field throws — numeric ops only on property.{name}")
        void gt_onTemplateField_throws() {
            var dto = new FilterNodeDtoIn(null, null, "template", "GT", "5");
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("GT");
        }

        @Test
        @DisplayName("LT on 'identifier' field throws — numeric ops only on property.{name}")
        void lt_onIdentifierField_throws() {
            var dto = new FilterNodeDtoIn(null, null, "identifier", "LT", "5");
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("LT");
        }

        @Test
        @DisplayName("'MIN' is rejected as an unsupported operator")
        void min_rejectedAsInvalidOperator() {
            var dto = new FilterNodeDtoIn(null, null, "name", "MIN", "5");
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("MIN");
        }

        @Test
        @DisplayName("GT on property.{name} with a non-numeric value throws")
        void gt_nonNumericValue_throws() {
            var dto = new FilterNodeDtoIn(null, null, "property.port", "GT", "abc");
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("abc")
                    .hasMessageContaining("GT");
        }

        @Test
        @DisplayName("LTE on property.{name} with blank non-numeric value throws")
        void lte_nonNumericValueWithSpecialChars_throws() {
            var dto = new FilterNodeDtoIn(null, null, "property.size", "LTE", "10MB");
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("10MB");
        }

        @Test
        @DisplayName("'MAX' is rejected as an unsupported operator")
        void max_rejectedAsInvalidOperator() {
            var dto = new FilterNodeDtoIn(null, null, "relation.api-link", "MAX", "5");
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining("MAX");
        }
    }

    @Nested
    @DisplayName("validateQuery()")
    class ValidateQueryTests {

        @Test
        @DisplayName("null query does not throw")
        void nullQuery_doesNotThrow() {
            mapper.validateQuery(null);
        }

        @Test
        @DisplayName("query within limit does not throw")
        void shortQuery_doesNotThrow() {
            mapper.validateQuery("checkout");
        }

        @Test
        @DisplayName("query at exact limit does not throw")
        void queryAtLimit_doesNotThrow() {
            mapper.validateQuery("x".repeat(EntitySearchDomainMapper.MAX_QUERY_LENGTH));
        }

        @Test
        @DisplayName("query exceeding limit throws InvalidQueryException")
        void queryOverLimit_throws() {
            String tooLong = "x".repeat(EntitySearchDomainMapper.MAX_QUERY_LENGTH + 1);
            assertThatThrownBy(() -> mapper.validateQuery(tooLong))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessageContaining(String.valueOf(EntitySearchDomainMapper.MAX_QUERY_LENGTH));
        }
    }
}
