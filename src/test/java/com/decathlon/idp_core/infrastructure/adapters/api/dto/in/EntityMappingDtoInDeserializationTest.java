package com.decathlon.idp_core.infrastructure.adapters.api.dto.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/// Tests JSON deserialization for entity dynamic mapping input.
@DisplayName("EntityMappingDtoIn Deserialization")
class EntityMappingDtoInDeserializationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("Should deserialize when properties and relations are omitted")
  void shouldDeserializeWhenPropertiesAndRelationsAreOmitted() throws Exception {
    String json = """
        {
          "identifier": ".identifier",
          "name": ".name"
        }
        """;

    EntityMappingDtoIn dto = objectMapper.readValue(json, EntityMappingDtoIn.class);

    assertThat(dto.properties()).isNotNull().isEmpty();
    assertThat(dto.relations()).isNotNull().isEmpty();
  }

  @Test
  @DisplayName("Should fail when relations are provided as object map")
  void shouldFailWhenRelationsAreObjectMap() {
    String json = """
        {
          "identifier": ".repository.full_name",
          "name": ".repository.name",
          "properties": {
            "applicationName": ".repository.name"
          },
          "relations": {
            "owner": [".sender.login"]
          }
        }
        """;

    assertThatThrownBy(() -> objectMapper.readValue(json, EntityMappingDtoIn.class))
        .hasMessageContaining("relations").hasMessageContaining("ArrayList");
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
  @DisplayName("Should fail when relation entry target_entity_identifiers is an object")
  void shouldFailWhenRelationValueIsNotArrayOfStrings() {
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
              "target_entity_identifiers": {
                "identifier": "f2e2ab44-5d19-44de-a77a-42ef6aa51676"
              }
            }
          ]
        }
        """;

    assertThatThrownBy(() -> objectMapper.readValue(json, EntityMappingDtoIn.class))
        .hasMessageContaining("target_entity_identifiers").hasMessageContaining("ArrayList");
  }

  @Test
  @DisplayName("Should fail when relation entry target_entity_identifiers is a single string")
  void shouldFailWhenRelationEntryTargetsIsSingleString() {
    String json = """
        {
          "identifier": ".repository.full_name",
          "name": ".repository.name",
          "properties": {
            "applicationName": ".repository.name"
          },
          "relations": [
            {
              "name": "owner",
              "target_entity_identifiers": ".sender.login"
            }
          ]
        }
        """;

    assertThatThrownBy(() -> objectMapper.readValue(json, EntityMappingDtoIn.class))
        .hasMessageContaining("target_entity_identifiers").hasMessageContaining("ArrayList");
  }
}
