package com.decathlon.idp_core.infrastructure.adapters.entity_mapping.jslt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_mapping.RelationMapping;
import com.fasterxml.jackson.databind.ObjectMapper;

/// Unit tests for JsltMappingEngineAdapter.
///
/// Covers relation extraction for scalar, array and object-based mapping expressions.
@DisplayName("JsltMappingEngineAdapter")
class JsltMappingEngineAdapterTest {

  private JsltMappingEngineAdapter adapter;

  @BeforeEach
  void setUp() {
    var jsltEngine = new JsltEngine();
    adapter = new JsltMappingEngineAdapter(jsltEngine, new ObjectMapper(),
        new JsltExpressionEvaluator(jsltEngine));
  }

  @Test
  @DisplayName("Should extract a relation from a scalar target identifier")
  void shouldExtractRelationFromScalarIdentifier() {
    var mapping = new EntityDynamicMapping(null, "mapping", "microservice", ".action == \"pushed\"",
        "Mapping", "desc", ".repository.full_name", ".repository.name",
        Map.of("applicationName", ".repository.name"),
        List.of(new RelationMapping("owner", List.of(".ownerId"))));

    var payload = """
        {
          "action": "pushed",
          "repository": {
            "full_name": "org/repo",
            "name": "repo"
          },
          "ownerId": "team-a"
        }
        """;

    var entity = adapter.mapToEntity(payload, mapping);

    assertThat(entity).isNotNull();
    assertThat(entity.relations()).hasSize(1);
    assertThat(entity.relations().getFirst().name()).isEqualTo("owner");
    assertThat(entity.relations().getFirst().targetTemplateIdentifier()).isNull();
    assertThat(entity.relations().getFirst().targetEntityIdentifiers()).containsExactly("team-a");
  }

  @Test
  @DisplayName("Should extract a relation from an array of target identifiers")
  void shouldExtractRelationFromArrayOfIdentifiers() {
    var mapping = new EntityDynamicMapping(null, "mapping", "microservice", ".action == \"pushed\"",
        "Mapping", "desc", ".repository.full_name", ".repository.name",
        Map.of("applicationName", ".repository.name"),
        List.of(new RelationMapping("dependents", List.of(".dependentIds"))));

    var payload = """
        {
          "action": "pushed",
          "repository": {
            "full_name": "org/repo",
            "name": "repo"
          },
          "dependentIds": ["service-a", "service-b"]
        }
        """;

    var entity = adapter.mapToEntity(payload, mapping);

    assertThat(entity).isNotNull();
    assertThat(entity.relations()).hasSize(1);
    assertThat(entity.relations().getFirst().name()).isEqualTo("dependents");
    assertThat(entity.relations().getFirst().targetEntityIdentifiers()).containsExactly("service-a",
        "service-b");
  }

  @Test
  @DisplayName("Should extract a relation from an array of objects containing identifier and name")
  void shouldExtractRelationFromArrayOfObjects() {
    var mapping = new EntityDynamicMapping(null, "mapping", "microservice", ".action == \"pushed\"",
        "Mapping", "desc", ".repository.full_name", ".repository.name",
        Map.of("applicationName", ".repository.name"),
        List.of(new RelationMapping("provided-by", List.of(".relations.providedBy"))));

    var payload = """
        {
          "action": "pushed",
          "repository": {
            "full_name": "org/repo",
            "name": "repo"
          },
          "relations": {
            "providedBy": [
              {
                "identifier": "398cb790-9117-4bfe-830e-bbb0e423c26e",
                "name": "return-lookup"
              }
            ]
          }
        }
        """;

    var entity = adapter.mapToEntity(payload, mapping);

    assertThat(entity).isNotNull();
    assertThat(entity.relations()).hasSize(1);
    assertThat(entity.relations().getFirst().targetEntityIdentifiers())
        .containsExactly("398cb790-9117-4bfe-830e-bbb0e423c26e");
  }

  @Test
  @DisplayName("Should ignore relation when expression resolves to null")
  void shouldIgnoreRelationWhenExpressionResolvesToNull() {
    var mapping = new EntityDynamicMapping(null, "mapping", "microservice", ".action == \"pushed\"",
        "Mapping", "desc", ".repository.full_name", ".repository.name",
        Map.of("applicationName", ".repository.name"),
        List.of(new RelationMapping("owner", List.of(".missingOwner"))));

    var payload = """
        {
          "action": "pushed",
          "repository": {
            "full_name": "org/repo",
            "name": "repo"
          }
        }
        """;

    var entity = adapter.mapToEntity(payload, mapping);

    assertThat(entity).isNotNull();
    assertThat(entity.relations()).isEmpty();
  }
}
