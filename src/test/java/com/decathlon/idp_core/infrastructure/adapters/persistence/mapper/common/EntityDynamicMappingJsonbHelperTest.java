package com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.model.entity_mapping.RelationMapping;

/// Unit tests for JSONB relation mapping compatibility.
@DisplayName("EntityDynamicMappingJsonbHelper")
class EntityDynamicMappingJsonbHelperTest {

  private final EntityDynamicMappingJsonbHelper helper = new EntityDynamicMappingJsonbHelper();

  @Test
  @DisplayName("Should deserialize legacy V6.4 'expression' field as expressions list")
  void shouldDeserializeLegacyExpressionField() {
    String json = """
        [
          {
            "name": "owner",
            "expressions": [".sender.login"]
          }
        ]
        """;

    List<RelationMapping> relations = helper.toRelationList(json);

    assertThat(relations).hasSize(1);
    assertThat(relations.getFirst().name()).isEqualTo("owner");
    assertThat(relations.getFirst().expressions()).containsExactly(".sender.login");
  }

  @Test
  @DisplayName("Should deserialize current 'expressions' array field")
  void shouldDeserializeExpressionsArrayField() {
    String json = """
        [
          {
            "name": "owner",
            "expressions": [".sender.login", ".sender.name"]
          }
        ]
        """;

    var relations = helper.toRelationList(json);

    assertThat(relations).hasSize(1);
    assertThat(relations.getFirst().name()).isEqualTo("owner");
    assertThat(relations.getFirst().expressions()).containsExactly(".sender.login", ".sender.name");
  }
}
