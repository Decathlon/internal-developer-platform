package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.decathlon.idp_core.domain.constant.ValidationMessages;

import lombok.extern.slf4j.Slf4j;

/// Integration tests for InboundWebhookManagementController, covering all CRUD operations on inbound webhook connectors.
///
/// Covers the full HTTP contract (status codes, response shape, validation errors)
/// for all CRUD operations on inbound webhook connectors.
@DisplayName("InboundWebhookManagementController Integration Tests")
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@Sql(scripts = {"/db/test/R__1_Insert_test_data.sql",
    "/db/test/R__4_insert_webhook_test_data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Slf4j
class InboundWebhookManagementControllerTest extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  private static final String WEBHOOK_PATH = "/api/v1/inbound-webhooks";
  private static final String ENTITY_DYNAMIC_MAPPING_PATH = "/api/v1/entity-dynamic-mappings";
  private static final String JSON_PATH = "integration_test/json/webhook/v1/";

  /// Creates an entity dynamic mapping via API required for webhook connector
  /// creation.
  private void createEntityDynamicMapping(String mappingIdentifier) throws Exception {
    var payload = """
        {
          "identifier": "%s",
          "template": "microservice",
          "filter": ".action == \\"pushed\\"",
          "name":"test mapping name",
          "description":"descrption",
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

    mockMvc
        .perform(MockMvcRequestBuilders.post(ENTITY_DYNAMIC_MAPPING_PATH)
            .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf()).content(payload))
        .andExpect(status().isCreated());
  }

  private void createWebhookConnector(String identifier, String title) throws Exception {
    // Create a unique entity dynamic mapping for this webhook connector
    String uniqueMappingIdentifier = identifier + "-mapping";
    createEntityDynamicMapping(uniqueMappingIdentifier);

    var payload = """
        {
          "identifier": "%s",
          "title": "%s",
          "description": "test connector",
          "enabled": true,
          "mapping_identifiers": ["%s"],
          "security": {
            "type": "NONE",
            "config": {}
          }
        }
        """.formatted(identifier, title, uniqueMappingIdentifier);

    mockMvc
        .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
            .accept(APPLICATION_JSON).with(csrf()).content(payload))
        .andExpect(status().isCreated());
  }

  private String buildPutPayload(String title, boolean enabled, String mappingIdentifier) {
    return """
        {
          "identifier": "ignored-by-put",
          "title": "%s",
          "description": "updated description",
          "enabled": %s,
          "mapping_identifiers": ["%s"],
          "security": {
            "type": "STATIC_TOKEN",
            "config": {
              "header_name": "X-Token",
              "secret_alias": "MY_TOKEN"
            }
          }
        }
        """.formatted(title, enabled, mappingIdentifier);
  }

  @Nested
  @DisplayName("GET /api/v1/inbound-webhooks")
  @Order(1)
  class GetWebhooksPaginatedTests {

    @Test
    @DisplayName("Should return 401 without authentication")
    void getWebhooks_401_without_user_token() throws Exception {
      mockMvc.perform(get(WEBHOOK_PATH).accept(APPLICATION_JSON))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 200 with page containing connectors from test data")
    void getWebhooks_200_with_data() throws Exception {
      mockMvc.perform(get(WEBHOOK_PATH).accept(APPLICATION_JSON)).andExpect(status().isOk())
          .andExpect(content().contentType(APPLICATION_JSON))
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.page.total_elements").value(3))
          .andExpect(jsonPath("$.content[0].identifier").value("github-dora-connector"))
          .andExpect(jsonPath("$.content[1].identifier").value("public-connector"))
          .andExpect(jsonPath("$.content[2].identifier").value("token-connector"));
    }
  }

  @Nested
  @DisplayName("POST /api/v1/inbound-webhooks - Create connector")
  @Order(2)
  class PostWebhookTests {

    @Test
    @DisplayName("Should return 401 without authentication")
    void postWebhook_401_without_user_token() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf())
              .content(getJsonTestFileContent(JSON_PATH + "postWebhook_201.json")))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Should create connector and return 201")
    void postWebhook_201() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf())
              .content(getJsonTestFileContent(JSON_PATH + "postWebhook_201.json")))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.identifier").value("github-dora-connector-test"))
          .andExpect(jsonPath("$.title").value("GitHub DORA Connector test"))
          .andExpect(jsonPath("$.security.type").value("HMAC_SHA256"));

      assertWebhookTemplateMapping("github-dora-connector-test");
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 409 when identifier already exists")
    void postWebhook_409_identifier_already_exists() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf())
              .content(getJsonTestFileContent(
                  JSON_PATH + "postWebhook_409_identifier_already_exists.json")))
          .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("CONFLICT"))
          .andExpect(jsonPath("$.error_description")
              .value(containsString(ValidationMessages.WEBHOOK_CONNECTOR_ALREADY_EXIST)));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when identifier is missing")
    void postWebhook_400_identifier_missing() throws Exception {
      var result = postBadRequestAndAssertEquals(WEBHOOK_PATH,
          JSON_PATH + "postWebhook_400_identifier_missing.json",
          ValidationMessages.WEBHOOK_CONNECTOR_IDENTIFIER_MANDATORY);
      assertNotNull(result);
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when identifier is blank")
    void postWebhook_400_identifier_blank() throws Exception {
      var result = postBadRequestAndAssertEquals(WEBHOOK_PATH,
          JSON_PATH + "postWebhook_400_identifier_blank.json",
          ValidationMessages.WEBHOOK_CONNECTOR_IDENTIFIER_MANDATORY);
      assertNotNull(result);
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when security type is unknown")
    void postWebhook_400_invalid_security_type() throws Exception {
      var result = postBadRequestAndAssertContains(WEBHOOK_PATH,
          JSON_PATH + "postWebhook_400_invalid_security_type.json", "UNKNOWN_TYPE");
      assertNotNull(result);
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when mapping identifier does not exist")
    void postWebhook_404_mapping_not_found() throws Exception {
      var payload = """
          {
            "identifier": "webhook-with-unknown-mapping",
            "title": "Webhook With Unknown Mapping",
            "description": "Should fail due to non-existent mapping",
            "enabled": true,
            "mapping_identifiers": ["non-existent-mapping"],
            "security": {
              "type": "NONE",
              "config": {}
            }
          }
          """;

      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("NOT_FOUND"))
          .andExpect(jsonPath("$.error_description").value(containsString("non-existent-mapping")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when security config is missing required fields (HMAC_SHA256)")
    void postWebhook_400_missing_security_config_fields() throws Exception {
      var payload = """
          {
            "identifier": "missing-security-fields",
            "title": "Missing Security Fields",
            "enabled": true,
            "mapping_identifiers": [],
            "security": {
              "type": "HMAC_SHA256",
              "config": {
                "header_name": "X-Hub-Signature-256"
              }
            }
          }
          """;

      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"))
          .andExpect(jsonPath("$.error_description").value(containsString("secret_alias")));
    }
  }

  @Nested
  @DisplayName("GET /api/v1/inbound-webhooks/{identifier} - Get by identifier")
  @Order(3)
  class GetWebhookByIdentifierTests {

    @Test
    @WithMockUser
    @DisplayName("Should return 200 with connector details from test data")
    void getWebhook_200() throws Exception {
      mockMvc.perform(get(WEBHOOK_PATH + "/github-dora-connector").accept(APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.identifier").value("github-dora-connector"))
          .andExpect(jsonPath("$.title").value("GitHub Connector"))
          .andExpect(jsonPath("$.security.type").value("HMAC_SHA256"))
          .andExpect(jsonPath("$.mappings[0].template").value("microservice"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when connector does not exist")
    void getWebhook_404_not_found() throws Exception {
      mockMvc.perform(get(WEBHOOK_PATH + "/non-existent-connector").accept(APPLICATION_JSON))
          .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("NOT_FOUND"))
          .andExpect(jsonPath("$.error_description").exists());
    }

    @Test
    @DisplayName("Should return 401 without authentication")
    void getWebhook_401_without_user_token() throws Exception {
      mockMvc.perform(get(WEBHOOK_PATH + "/github-dora-connector").accept(APPLICATION_JSON))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PUT /api/v1/inbound-webhooks/{identifier} - Update connector")
  @Order(4)
  class PutWebhookTests {

    @Test
    @DisplayName("Should return 401 without authentication")
    void putWebhook_401_without_user_token() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.put(WEBHOOK_PATH + "/github-dora-connector")
              .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf())
              .content(getJsonTestFileContent(JSON_PATH + "putWebhook_200.json")))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Should update connector and return 200")
    void putWebhook_200() throws Exception {
      createWebhookConnector("connector-put-200", "Connector Put Initial Title");

      mockMvc
          .perform(MockMvcRequestBuilders.put(WEBHOOK_PATH + "/connector-put-200")
              .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf())
              .content(buildPutPayload("Connector Put Updated Title", false,
                  "connector-put-200-mapping")))
          .andExpect(status().isOk()).andExpect(jsonPath("$.identifier").value("connector-put-200"))
          .andExpect(jsonPath("$.title").value("Connector Put Updated Title"))
          .andExpect(jsonPath("$.enabled").value(false))
          .andExpect(jsonPath("$.security.type").value("STATIC_TOKEN"));

      mockMvc.perform(get(WEBHOOK_PATH + "/connector-put-200").accept(APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.title").value("Connector Put Updated Title"))
          .andExpect(jsonPath("$.enabled").value(false));

      assertWebhookTemplateMapping("connector-put-200");
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 409 when updating title to an existing one")
    void putWebhook_409_title_already_exists() throws Exception {
      createWebhookConnector("connector-put-409-a", "Connector A Title");
      createWebhookConnector("connector-put-409-b", "Connector B Title");

      mockMvc
          .perform(MockMvcRequestBuilders.put(WEBHOOK_PATH + "/connector-put-409-b")
              .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf())
              .content(buildPutPayload("Connector A Title", true, "connector-put-409-b-mapping")))
          .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("CONFLICT"))
          .andExpect(jsonPath("$.error_description").value(containsString(
              "Webhook Connector already exist with the same name:Connector A Title")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when connector does not exist")
    void putWebhook_404_not_found() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.put(WEBHOOK_PATH + "/non-existent-connector")
              .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf())
              .content(buildPutPayload("Updated Title", false, "microservice-mapping")))
          .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("NOT_FOUND"))
          .andExpect(jsonPath("$.error_description").exists());
    }
  }

  @Nested
  @DisplayName("DELETE /api/v1/inbound-webhooks/{identifier} - Delete connector")
  @Order(5)
  class DeleteWebhookTests {

    @Test
    @DisplayName("Should return 401 without authentication")
    void deleteWebhook_401_without_user_token() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.delete(WEBHOOK_PATH + "/github-dora-connector")
          .accept(APPLICATION_JSON).with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Should delete connector and return 204")
    void deleteWebhook_204() throws Exception {
      createWebhookConnector("connector-delete-204", "Connector To Delete");

      mockMvc.perform(MockMvcRequestBuilders.delete(WEBHOOK_PATH + "/connector-delete-204")
          .accept(APPLICATION_JSON).with(csrf())).andExpect(status().isNoContent());

      // Verify the webhook no longer exists
      mockMvc.perform(get(WEBHOOK_PATH + "/connector-delete-204").accept(APPLICATION_JSON))
          .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when connector does not exist")
    void deleteWebhook_404_not_found() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.delete(WEBHOOK_PATH + "/non-existent-connector")
              .accept(APPLICATION_JSON).with(csrf()))
          .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("NOT_FOUND"))
          .andExpect(jsonPath("$.error_description").exists());
    }
  }

  /// Asserts that the webhook connector has at least one mapping via API.
  private void assertWebhookTemplateMapping(String identifier) throws Exception {
    mockMvc.perform(get(WEBHOOK_PATH + "/" + identifier).accept(APPLICATION_JSON))
        .andExpect(status().isOk()).andExpect(jsonPath("$.mappings").isArray())
        .andExpect(jsonPath("$.mappings").isNotEmpty())
        .andExpect(jsonPath("$.mappings[0].filter").value(".action == \"pushed\""));
  }
}
