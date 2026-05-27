package com.decathlon.idp_core.infrastructure.adapters.persistence.specification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.decathlon.idp_core.infrastructure.adapters.api.controller.EntityControllerTest;

/// Unit tests for [EntitySpecification].
///
/// Focuses on the LIKE wildcard escaping logic which is security-critical.
/// Integration-level specification behavior is verified through the
/// [EntityControllerTest] integration tests.
@DisplayName("EntitySpecification")
@SuppressWarnings("java:S2187")
class EntitySpecificationTest {

    @Nested
    @DisplayName("escapeLikeWildcards")
    class EscapeLikeWildcardsTests {

        @Test
        @DisplayName("escapes percent sign")
        void escapes_percent() {
            assertThat(EntitySpecification.escapeLikeWildcards("100%"))
                    .isEqualTo("100\\%");
        }

        @Test
        @DisplayName("escapes underscore")
        void escapes_underscore() {
            assertThat(EntitySpecification.escapeLikeWildcards("my_value"))
                    .isEqualTo("my\\_value");
        }

        @Test
        @DisplayName("escapes backslash before other wildcards")
        void escapes_backslash() {
            assertThat(EntitySpecification.escapeLikeWildcards("path\\to%file"))
                    .isEqualTo("path\\\\to\\%file");
        }

        @Test
        @DisplayName("escapes multiple wildcards")
        void escapes_multipleWildcards() {
            assertThat(EntitySpecification.escapeLikeWildcards("100%_success"))
                    .isEqualTo("100\\%\\_success");
        }

        @Test
        @DisplayName("returns plain string unchanged")
        void leaves_plainString_unchanged() {
            assertThat(EntitySpecification.escapeLikeWildcards("hello"))
                    .isEqualTo("hello");
        }

        @ParameterizedTest(name = "escapes ''{0}'' correctly")
        @ValueSource(strings = {"%", "_", "%%", "__", "%_", "_%"})
        @DisplayName("escapes various wildcard combinations")
        void escapes_wildcardCombinations(String input) {
            String escaped = EntitySpecification.escapeLikeWildcards(input);
            // Strip all valid escape sequences, then verify no bare wildcards remain
            String stripped = escaped.replace("\\%", "").replace("\\_", "").replace("\\\\", "");
            assertThat(stripped)
                    .doesNotContain("%")
                    .doesNotContain("_");
            assertThat(escaped).contains("\\");
        }
    }
}
