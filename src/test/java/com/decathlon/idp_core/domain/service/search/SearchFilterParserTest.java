package com.decathlon.idp_core.domain.service.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.constant.SearchConstraints;
import com.decathlon.idp_core.domain.exception.search.InvalidSearchQueryException;
import com.decathlon.idp_core.domain.model.entity.RawSearchFilterNode;
import com.decathlon.idp_core.domain.model.entity.SearchFilterNode;
import com.decathlon.idp_core.domain.model.enums.LogicalConnector;
import com.decathlon.idp_core.domain.model.enums.SearchOperator;

/// Unit tests for [SearchFilterParser].
///
/// The parser converts a [RawSearchFilterNode] tree to a validated [SearchFilterNode] tree,
/// enforcing structural rules, safety limits, and enum resolution.
@DisplayName("SearchFilterParser")
class SearchFilterParserTest {

    private final SearchFilterParser parser = new SearchFilterParser();

    @Nested
    @DisplayName("parse() — null and empty inputs")
    class NullAndEmptyTests {

        @Test
        @DisplayName("null input returns empty AND group")
        void null_returnsEmptyAndGroup() {
            var result = parser.parse(null);
            assertThat(result).isInstanceOf(SearchFilterNode.Group.class);
            var group = (SearchFilterNode.Group) result;
            assertThat(group.connector()).isEqualTo(LogicalConnector.AND);
            assertThat(group.nodes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("parse() — criterion leaf node")
    class CriterionTests {

        @Test
        @DisplayName("valid criterion is correctly parsed")
        void validCriterion_parsed() {
            var raw = new RawSearchFilterNode.Criterion("template", "EQ", "microservice");
            var result = parser.parse(raw);
            assertThat(result).isInstanceOf(SearchFilterNode.Criterion.class);
            var criterion = (SearchFilterNode.Criterion) result;
            assertThat(criterion.field()).isEqualTo("template");
            assertThat(criterion.operation()).isEqualTo(SearchOperator.EQ);
            assertThat(criterion.value()).isEqualTo("microservice");
        }

        @Test
        @DisplayName("operation is case-insensitive")
        void operation_caseInsensitive() {
            var raw = new RawSearchFilterNode.Criterion("identifier", "contains", "api");
            var result = (SearchFilterNode.Criterion) parser.parse(raw);
            assertThat(result.operation()).isEqualTo(SearchOperator.CONTAINS);
        }

        @Test
        @DisplayName("throws when field is null")
        void nullField_throws() {
            var raw = new RawSearchFilterNode.Criterion(null, "EQ", "value");
            assertThatThrownBy(() -> parser.parse(raw))
                    .isInstanceOf(InvalidSearchQueryException.class)
                    .hasMessageContaining("field");
        }

        @Test
        @DisplayName("throws when field is blank")
        void blankField_throws() {
            var raw = new RawSearchFilterNode.Criterion("  ", "EQ", "value");
            assertThatThrownBy(() -> parser.parse(raw))
                    .isInstanceOf(InvalidSearchQueryException.class)
                    .hasMessageContaining("field");
        }

        @Test
        @DisplayName("throws when operation is null")
        void nullOperation_throws() {
            var raw = new RawSearchFilterNode.Criterion("identifier", null, "value");
            assertThatThrownBy(() -> parser.parse(raw))
                    .isInstanceOf(InvalidSearchQueryException.class)
                    .hasMessageContaining("operation");
        }

        @Test
        @DisplayName("throws when value is null")
        void nullValue_throws() {
            var raw = new RawSearchFilterNode.Criterion("identifier", "EQ", null);
            assertThatThrownBy(() -> parser.parse(raw))
                    .isInstanceOf(InvalidSearchQueryException.class)
                    .hasMessageContaining("value");
        }

        @Test
        @DisplayName("throws for invalid operation string")
        void invalidOperation_throws() {
            var raw = new RawSearchFilterNode.Criterion("identifier", "LIKE", "api");
            assertThatThrownBy(() -> parser.parse(raw))
                    .isInstanceOf(InvalidSearchQueryException.class)
                    .hasMessageContaining("LIKE");
        }

        @Test
        @DisplayName("unknown field is accepted by the parser — semantic validation is deferred to SearchFilterValidationService")
        void unknownField_acceptedByParser() {
            var raw = new RawSearchFilterNode.Criterion("badField", "EQ", "value");
            assertThat(parser.parse(raw)).isInstanceOf(SearchFilterNode.Criterion.class);
        }
    }

    @Nested
    @DisplayName("parse() — group nodes")
    class GroupTests {

        @Test
        @DisplayName("valid AND group is correctly parsed")
        void validAndGroup_parsed() {
            var child1 = new RawSearchFilterNode.Criterion("template", "EQ", "microservice");
            var child2 = new RawSearchFilterNode.Criterion("identifier", "CONTAINS", "api");
            var raw = new RawSearchFilterNode.Group("AND", List.of(child1, child2));

            var result = parser.parse(raw);
            assertThat(result).isInstanceOf(SearchFilterNode.Group.class);
            var group = (SearchFilterNode.Group) result;
            assertThat(group.connector()).isEqualTo(LogicalConnector.AND);
            assertThat(group.nodes()).hasSize(2);
        }

        @Test
        @DisplayName("connector is case-insensitive")
        void connector_caseInsensitive() {
            var child = new RawSearchFilterNode.Criterion("template", "EQ", "microservice");
            var raw = new RawSearchFilterNode.Group("or", List.of(child));
            var group = (SearchFilterNode.Group) parser.parse(raw);
            assertThat(group.connector()).isEqualTo(LogicalConnector.OR);
        }

        @Test
        @DisplayName("'IN' connector is rejected")
        void inConnector_rejected() {
            var child = new RawSearchFilterNode.Criterion("template", "EQ", "microservice");
            var raw = new RawSearchFilterNode.Group("IN", List.of(child));
            assertThatThrownBy(() -> parser.parse(raw))
                    .isInstanceOf(InvalidSearchQueryException.class)
                    .hasMessageContaining("IN");
        }

        @Test
        @DisplayName("throws for missing connector in group")
        void missingConnector_throws() {
            var child = new RawSearchFilterNode.Criterion("template", "EQ", "microservice");
            var raw = new RawSearchFilterNode.Group(null, List.of(child));
            assertThatThrownBy(() -> parser.parse(raw))
                    .isInstanceOf(InvalidSearchQueryException.class)
                    .hasMessageContaining("connector");
        }

        @Test
        @DisplayName("throws for empty criteria list in group")
        void emptyCriteria_throws() {
            var raw = new RawSearchFilterNode.Group("AND", List.of());
            assertThatThrownBy(() -> parser.parse(raw))
                    .isInstanceOf(InvalidSearchQueryException.class)
                    .hasMessageContaining("criteria");
        }

        @Test
        @DisplayName("throws for invalid connector string")
        void invalidConnector_throws() {
            var child = new RawSearchFilterNode.Criterion("template", "EQ", "microservice");
            var raw = new RawSearchFilterNode.Group("NAND", List.of(child));
            assertThatThrownBy(() -> parser.parse(raw))
                    .isInstanceOf(InvalidSearchQueryException.class)
                    .hasMessageContaining("NAND");
        }
    }

    @Nested
    @DisplayName("parse() — safety limits")
    class SafetyLimitsTests {

        @Test
        @DisplayName("throws when total criteria exceed maximum")
        void tooManyCriteria_throws() {
            var innerCriteria = new ArrayList<RawSearchFilterNode>();
            for (int i = 0; i <= SearchConstraints.MAX_TOTAL_CRITERIA; i++) {
                innerCriteria.add(new RawSearchFilterNode.Criterion("template", "EQ", "v" + i));
            }
            var raw = new RawSearchFilterNode.Group("OR", innerCriteria);
            assertThatThrownBy(() -> parser.parse(raw))
                    .isInstanceOf(InvalidSearchQueryException.class)
                    .hasMessageContaining(String.valueOf(SearchConstraints.MAX_TOTAL_CRITERIA));
        }

        @Test
        @DisplayName("throws when nesting exceeds maximum depth")
        void nestingTooDeep_throws() {
            RawSearchFilterNode node = new RawSearchFilterNode.Criterion("template", "EQ", "v");
            for (int i = 0; i <= SearchConstraints.MAX_NESTING_DEPTH; i++) {
                node = new RawSearchFilterNode.Group("AND", List.of(node));
            }
            var root = node;
            assertThatThrownBy(() -> parser.parse(root))
                    .isInstanceOf(InvalidSearchQueryException.class)
                    .hasMessageContaining(String.valueOf(SearchConstraints.MAX_NESTING_DEPTH));
        }
    }
}
