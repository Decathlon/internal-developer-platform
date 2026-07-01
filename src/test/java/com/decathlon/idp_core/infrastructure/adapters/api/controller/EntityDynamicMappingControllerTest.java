package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.decathlon.idp_core.AbstractIntegrationTest;

import lombok.extern.slf4j.Slf4j;

/// Integration tests for EntityDynamicMappingController, covering all CRUD operations on entity dynamic mappings.
///
/// Covers the full HTTP contract (status codes, response shape, validation errors)
/// for all CRUD operations on entity dynamic mappings.
@DisplayName("EntityDynamicMappingController Integration Tests")
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@Sql(scripts = {"/db/test/R__1_Insert_test_data.sql",
    "/db/test/R__4_insert_webhook_test_data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Slf4j
class EntityDynamicMappingControllerTest extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  private static final String MAPPING_PATH = "/api/v1/entity-dynamic-mappings";

  /// Builds a valid entity dynamic mapping creation payload.
  private String buildCreatePayload(String mappingIdentifier) {
    return """
        {
          "identifier": "%s",
          "template": "microservice",
          "filter": ".action == \\"pushed\\"",
          "name":"microservice name",
          "description":"description",
          "entity": {
            "identifier": ".repository.full_name",
            "title": ".repository.name",
            "properties": {
              "applicationName": ".repository.name",
              "ownerEmail": ".sender.email",
              "environment": "\\"DEV\\"",
              "version": ".ref",
              "port": "8080",
              "programmingLanguage": ".repository.language"
            },
            "relations": {}
          }
        }
        """.formatted(mappingIdentifier);
  }

  /// Builds a valid entity dynamic mapping update payload.
  private String buildUpdatePayload() {
    return """
        {
          "template": "microservice",
          "filter": ".action == \\"released\\"",
          "name":"microservice name updated",
          "description":"updated description",
          "entity": {
            "identifier": ".release.tag_name",
            "title": ".release.name",
            "properties": {
              "applicationName": ".repository.name",
              "ownerEmail": ".release.author.email",
              "environment": "\\"PROD\\"",
              "version": ".release.tag_name",
              "port": "8080",
              "programmingLanguage": ".repository.language"
            },
            "relations": {}
          }
        }
        """;
  }

  @Nested
  @DisplayName("GET /api/v1/entity-dynamic-mappings - Get mappings paginated")
  @Order(1)
  class GetMappingsPaginatedTests {

    @Test
    @DisplayName("Should return 401 without authentication")
    void getMappings_401_without_user_token() throws Exception {
      mockMvc.perform(get(MAPPING_PATH).accept(APPLICATION_JSON))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 200 with page containing mappings from test data")
    void getMappings_200_with_data() throws Exception {
      mockMvc.perform(get(MAPPING_PATH).accept(APPLICATION_JSON)).andExpect(status().isOk())
          .andExpect(content().contentType(APPLICATION_JSON))
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.page.total_elements").value(1))
          .andExpect(jsonPath("$.content[0].identifier").value("microservice-mapping"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 200 with custom pagination")
    void getMappings_200_with_custom_pagination() throws Exception {
      mockMvc
          .perform(get(MAPPING_PATH).param("page", "0").param("size", "5")
              .param("sort", "identifier,asc").accept(APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(content().contentType(APPLICATION_JSON))
          .andExpect(jsonPath("$.page.size").value(5))
          .andExpect(jsonPath("$.page.number").value(0));
    }
  }

  @Nested
  @DisplayName("POST /api/v1/entity-dynamic-mappings - Create mapping")
  @Order(2)
  class PostMappingTests {

    @Test
    @DisplayName("Should return 401 without authentication")
    void postMapping_401_without_user_token() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(MAPPING_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content(buildCreatePayload("new-mapping")))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Should create mapping and return 201")
    void postMapping_201() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(MAPPING_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf())
              .content(buildCreatePayload("new-test-mapping")))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.identifier").value("new-test-mapping"))
          .andExpect(jsonPath("$.template").value("microservice"))
          .andExpect(jsonPath("$.filter").value(".action == \"pushed\""))
          .andExpect(jsonPath("$.entity.identifier").value(".repository.full_name"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 409 when identifier already exists")
    void postMapping_409_identifier_already_exists() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(MAPPING_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf())
              .content(buildCreatePayload("microservice-mapping")))
          .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("CONFLICT"))
          .andExpect(jsonPath("$.error_description").value(containsString("microservice-mapping")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when mappingIdentifier is missing")
    void postMapping_400_identifier_missing() throws Exception {
      var payload = """
          {
            "template": "microservice",
            "filter": ".action == \\"pushed\\"",
            "entity": {
              "identifier": ".repository.full_name",
              "title": ".repository.name",
              "properties": {
                "applicationName": ".repository.name",
                "ownerEmail": ".sender.email",
                "environment": "\\"DEV\\"",
                "version": ".ref",
                "port": "8080",
                "programmingLanguage": ".repository.language"
              },
              "relations": {}
            }
          }
          """;

      mockMvc
          .perform(MockMvcRequestBuilders.post(MAPPING_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"))
          .andExpect(jsonPath("$.error_description").value(containsString("mapping identifier")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when template is missing")
    void postMapping_400_template_missing() throws Exception {
      var payload = """
          {
            "identifier": "test-mapping",
            "filter": ".action == \\"pushed\\"",
            "entity": {
              "identifier": ".repository.full_name",
              "title": ".repository.name",
              "properties": {},
              "relations": {}
            }
          }
          """;

      mockMvc
          .perform(MockMvcRequestBuilders.post(MAPPING_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"))
          .andExpect(jsonPath("$.error_description").value(containsString("template")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when template does not exist")
    void postMapping_404_template_not_found() throws Exception {
      var payload = """
          {
            "identifier": "test-mapping",
            "template": "non-existent-template",
            "filter": ".action == \\"pushed\\"",
            "name":"test mapping",
            "description":"descrption",
            "entity": {
              "identifier": ".repository.full_name",
              "title": ".repository.name",
              "properties": {},
              "relations": {}
            }
          }
          """;

      mockMvc
          .perform(MockMvcRequestBuilders.post(MAPPING_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("NOT_FOUND"))
          .andExpect(
              jsonPath("$.error_description").value(containsString("non-existent-template")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when required template properties are missing")
    void postMapping_400_missing_required_properties() throws Exception {
      var payload = """
          {
            "identifier": "test-mapping",
            "template": "microservice",
            "filter": ".action == \\"pushed\\"",
            "name":"test mapping name",
            "description":"descrption",
            "entity": {
              "identifier": ".repository.full_name",
              "title": ".repository.name",
              "properties": {
                "applicationName": ".repository.name"
              },
              "relations": {}
            }
          }
          """;

      mockMvc
          .perform(MockMvcRequestBuilders.post(MAPPING_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"))
          .andExpect(jsonPath("$.error_description").value(containsString("missing required")));
    }
  }

  @Nested
  @DisplayName("GET /api/v1/entity-dynamic-mappings/{identifier} - Get by identifier")
  @Order(3)
  class GetMappingByIdentifierTests {

    @Test
    @DisplayName("Should return 401 without authentication")
    void getMapping_401_without_user_token() throws Exception {
      mockMvc.perform(get(MAPPING_PATH + "/microservice-mapping").accept(APPLICATION_JSON))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 200 with mapping details from test data")
    void getMapping_200() throws Exception {
      mockMvc.perform(get(MAPPING_PATH + "/microservice-mapping").accept(APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.identifier").value("microservice-mapping"))
          .andExpect(jsonPath("$.template").value("microservice"))
          .andExpect(jsonPath("$.filter").value(".action == \"pushed\""));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when mapping does not exist")
    void getMapping_404_not_found() throws Exception {
      mockMvc.perform(get(MAPPING_PATH + "/non-existent-mapping").accept(APPLICATION_JSON))
          .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("NOT_FOUND"))
          .andExpect(jsonPath("$.error_description").exists());
    }
  }

  @Nested
  @DisplayName("PUT /api/v1/entity-dynamic-mappings/{identifier} - Update mapping")
  @Order(4)
  class PutMappingTests {

    @Test
    @DisplayName("Should return 401 without authentication")
    void putMapping_401_without_user_token() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.put(MAPPING_PATH + "/microservice-mapping")
          .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf())
          .content(buildUpdatePayload())).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when mapping does not exist")
    void putMapping_404_not_found() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.put(MAPPING_PATH + "/non-existent-mapping")
              .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf())
              .content(buildUpdatePayload()))
          .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("NOT_FOUND"))
          .andExpect(jsonPath("$.error_description").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("Should update mapping and return 200")
    void putMapping_200() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.post(MAPPING_PATH).contentType(APPLICATION_JSON)
          .accept(APPLICATION_JSON).with(csrf()).content(buildCreatePayload("mapping-to-update")))
          .andExpect(status().isCreated());

      mockMvc
          .perform(MockMvcRequestBuilders.put(MAPPING_PATH + "/mapping-to-update")
              .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf())
              .content(buildUpdatePayload()))
          .andExpect(status().isOk()).andExpect(jsonPath("$.identifier").value("mapping-to-update"))
          .andExpect(jsonPath("$.filter").value(".action == \"released\""))
          .andExpect(jsonPath("$.entity.identifier").value(".release.tag_name"));

      mockMvc.perform(get(MAPPING_PATH + "/mapping-to-update").accept(APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.filter").value(".action == \"released\""));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when template is missing in update")
    void putMapping_400_template_missing() throws Exception {
      var payload = """
          {
            "filter": ".action == \\"released\\"",
            "entity": {
              "identifier": ".release.tag_name",
              "title": ".release.name",
              "properties": {},
              "relations": {}
            }
          }
          """;

      mockMvc
          .perform(MockMvcRequestBuilders.put(MAPPING_PATH + "/microservice-mapping")
              .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }
  }

  @Nested
  @DisplayName("DELETE /api/v1/entity-dynamic-mappings/{identifier} - Delete mapping")
  @Order(5)
  class DeleteMappingTests {

    @Test
    @DisplayName("Should return 401 without authentication")
    void deleteMapping_401_without_user_token() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.delete(MAPPING_PATH + "/microservice-mapping")
          .accept(APPLICATION_JSON).with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Should delete mapping and return 204")
    void deleteMapping_204() throws Exception {
      // First create a mapping to delete
      mockMvc.perform(MockMvcRequestBuilders.post(MAPPING_PATH).contentType(APPLICATION_JSON)
          .accept(APPLICATION_JSON).with(csrf()).content(buildCreatePayload("mapping-to-delete")))
          .andExpect(status().isCreated());

      // Delete the mapping
      mockMvc.perform(MockMvcRequestBuilders.delete(MAPPING_PATH + "/mapping-to-delete")
          .accept(APPLICATION_JSON).with(csrf())).andExpect(status().isNoContent());

      // Verify the mapping no longer exists
      mockMvc.perform(get(MAPPING_PATH + "/mapping-to-delete").accept(APPLICATION_JSON))
          .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when mapping does not exist")
    void deleteMapping_404_not_found() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.delete(MAPPING_PATH + "/non-existent-mapping")
              .accept(APPLICATION_JSON).with(csrf()))
          .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("NOT_FOUND"))
          .andExpect(jsonPath("$.error_description").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 409 when mapping is in use by a webhook")
    void deleteMapping_409_in_use() throws Exception {
      // The microservice-mapping is used by github-dora-connector from test data
      mockMvc
          .perform(MockMvcRequestBuilders.delete(MAPPING_PATH + "/microservice-mapping")
              .accept(APPLICATION_JSON).with(csrf()))
          .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("CONFLICT"))
          .andExpect(jsonPath("$.error_description").value(containsString("in use")));
    }
  }
}
