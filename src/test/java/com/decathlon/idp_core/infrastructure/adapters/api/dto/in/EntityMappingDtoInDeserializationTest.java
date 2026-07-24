package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/// Tests JSON deserialization for relation mappings accepted as map or array.
@DisplayName("EntityMappingDtoIn Deserialization")
class EntityMappingDtoInDeserializationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("Should deserialize relations from object map format")
  void shouldDeserializeRelationsFromObjectMap() throws Exception {
    String json = """
        {
          "identifier": ".repository.full_name",
          "name": ".repository.name",
          "properties": {
            "applicationName": ".repository.name"
          },
          "relations": {
            "owner": ".sender.login"
          }
        }
        """;

    EntityMappingDtoIn dto = objectMapper.readValue(json, EntityMappingDtoIn.class);

    assertThat(dto.relations()).hasSize(1);
    assertThat(dto.relations().getFirst().name()).isEqualTo("owner");
    assertThat(dto.relations().getFirst().targetEntityIdentifiers())
        .containsExactly(".sender.login");
  }

  @Test
  @DisplayName("Should deserialize relations from array entry format")
  void shouldDeserializeRelationsFromArrayFormat() throws Exception {
    String json = """
        {
          "identifier": ".repository.full_name",
          "name": ".repository.name",
          "properties": {
            "applicationName": ".repository.name"
          },
          "relations": [
            {
              "name": "apim-api-consumed_by-component",
              "target_entity_identifiers": [".relations.\\"apim-api-consumed_by-component\\""]
            }
            ,
            {
              "name": "apim-api-provided_by-component",
              "target_entity_identifiers": [".relations.\\"apim-api-provided_by-component\\""]
            }
          ]
        }
        """;

    EntityMappingDtoIn dto = objectMapper.readValue(json, EntityMappingDtoIn.class);

    assertThat(dto.relations()).hasSize(2);
    assertThat(dto.relations())
        .anyMatch(relation -> relation.name().equals("apim-api-consumed_by-component") && relation
            .targetEntityIdentifiers().contains(".relations.\"apim-api-consumed_by-component\""));
    assertThat(dto.relations())
        .anyMatch(relation -> relation.name().equals("apim-api-provided_by-component") && relation
            .targetEntityIdentifiers().contains(".relations.\"apim-api-provided_by-component\""));
  }

  @Test
  @DisplayName("Should fail when relation value is not a string or array of strings")
  void shouldFailWhenRelationValueIsNotAString() {
    String json = """
        {
          "identifier": ".repository.full_name",
          "name": ".repository.name",
          "properties": {
            "applicationName": ".repository.name"
          },
          "relations": {
            "apim-api-consumed_by-component": {
              "identifier": "f2e2ab44-5d19-44de-a77a-42ef6aa51676",
              "name": "user-profile-sync",
              "criticality": "MEDIUM"
            }
          }
        }
        """;

    assertThatThrownBy(() -> objectMapper.readValue(json, EntityMappingDtoIn.class))
        .hasMessageContaining("must be either a string or an array of strings");
  }
}
