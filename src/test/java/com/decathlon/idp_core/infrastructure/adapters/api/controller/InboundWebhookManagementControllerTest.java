package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.decathlon.idp_core.AbstractIntegrationTest;
import com.decathlon.idp_core.domain.constant.ValidationMessages;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorConfigurationException;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_dynamic_mapping.InboundWebhookEntityMappingDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.WebhookTemplateMappingPersistenceMapper;
import com.decathlon.idp_core.infrastructure.adapters.webhook.model.BasicAuthConfig;
import com.decathlon.idp_core.infrastructure.adapters.webhook.model.HmacConfig;
import com.decathlon.idp_core.infrastructure.adapters.webhook.model.JwtBearerConfig;
import com.decathlon.idp_core.infrastructure.adapters.webhook.model.SecurityConfig;
import com.decathlon.idp_core.infrastructure.adapters.webhook.model.StaticTokenConfig;

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

  @Autowired
  private WebhookTemplateMappingPersistenceMapper webhookTemplateMappingPersistenceMapper;

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

    @Test
    @WithMockUser
    @DisplayName("Should create connector with NONE security when security field is omitted")
    void postWebhook_201_security_defaults_to_none_when_omitted() throws Exception {
      var payload = """
          {
            "identifier": "no-security-webhook",
            "title": "No Security Webhook",
            "enabled": true,
            "mapping_identifiers": []
          }
          """;

      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isCreated()).andExpect(jsonPath("$.security.type").value("NONE"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should create connector with STATIC_TOKEN security and return 201")
    void postWebhook_201_static_token_security() throws Exception {
      var payload = """
          {
            "identifier": "static-token-webhook",
            "title": "Static Token Webhook",
            "enabled": true,
            "mapping_identifiers": [],
            "security": {
              "type": "STATIC_TOKEN",
              "config": {
                "header_name": "X-Auth-Token",
                "secret_alias": "TOKEN_SECRET"
              }
            }
          }
          """;

      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.identifier").value("static-token-webhook"))
          .andExpect(jsonPath("$.security.type").value("STATIC_TOKEN"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should create connector with BASIC_AUTH security and return 201")
    void postWebhook_201_basic_auth_security() throws Exception {
      var payload = """
          {
            "identifier": "basic-auth-webhook",
            "title": "Basic Auth Webhook",
            "enabled": true,
            "mapping_identifiers": [],
            "security": {
              "type": "BASIC_AUTH",
              "config": {
                "username": "admin",
                "secret_alias": "BASIC_PASS"
              }
            }
          }
          """;

      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.identifier").value("basic-auth-webhook"))
          .andExpect(jsonPath("$.security.type").value("BASIC_AUTH"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should force enabled to false when no mappings provided")
    void postWebhook_201_enabled_forced_false_when_no_mapping() throws Exception {
      var payload = """
          {
            "identifier": "no-mapping-webhook",
            "title": "No Mapping Webhook",
            "enabled": true,
            "mapping_identifiers": [],
            "security": {
              "type": "NONE",
              "config": {}
            }
          }
          """;

      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.identifier").value("no-mapping-webhook"))
          // Even though enabled=true was sent, it must be forced to false (no mappings)
          .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when title exceeds 255 characters")
    void postWebhook_400_title_too_long() throws Exception {
      var tooLongTitle = "A".repeat(256);
      var payload = """
          {
            "identifier": "long-title-webhook",
            "title": "%s",
            "enabled": true,
            "mapping_identifiers": [],
            "security": {
              "type": "NONE",
              "config": {}
            }
          }
          """.formatted(tooLongTitle);

      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when title is blank")
    void postWebhook_400_title_blank() throws Exception {
      var payload = """
          {
            "identifier": "blank-title-webhook",
            "title": "   ",
            "enabled": true,
            "mapping_identifiers": [],
            "security": {
              "type": "NONE",
              "config": {}
            }
          }
          """;

      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when security config is missing required fields (BASIC_AUTH)")
    void postWebhook_400_basic_auth_missing_fields() throws Exception {
      var payload = """
          {
            "identifier": "basic-auth-missing-fields",
            "title": "Basic Auth Missing Fields",
            "enabled": true,
            "mapping_identifiers": [],
            "security": {
              "type": "BASIC_AUTH",
              "config": {
                "username": "admin"
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
    @DisplayName("Should return full mapping shape including entity fields")
    void getWebhook_200_full_mapping_shape() throws Exception {
      mockMvc.perform(get(WEBHOOK_PATH + "/github-dora-connector").accept(APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(jsonPath("$.mappings").isArray())
          .andExpect(jsonPath("$.mappings[0].identifier").exists())
          .andExpect(jsonPath("$.mappings[0].template").value("microservice"))
          .andExpect(jsonPath("$.mappings[0].filter").value(".action == \"pushed\""))
          .andExpect(jsonPath("$.mappings[0].entity.identifier").exists())
          .andExpect(jsonPath("$.mappings[0].entity.title").exists())
          .andExpect(jsonPath("$.mappings[0].entity.properties").exists())
          .andExpect(jsonPath("$.mappings[0].entity.relations").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return security type and config in response")
    void getWebhook_200_security_shape() throws Exception {
      mockMvc.perform(get(WEBHOOK_PATH + "/github-dora-connector").accept(APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(jsonPath("$.security").exists())
          .andExpect(jsonPath("$.security.type").value("HMAC_SHA256"))
          .andExpect(jsonPath("$.security.config").exists())
          .andExpect(jsonPath("$.security.config.header_name").value("X-Hub-Signature-256"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return enabled and description fields")
    void getWebhook_200_base_fields() throws Exception {
      mockMvc.perform(get(WEBHOOK_PATH + "/public-connector").accept(APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(jsonPath("$.identifier").value("public-connector"))
          .andExpect(jsonPath("$.title").value("Public Connector"))
          .andExpect(jsonPath("$.description").exists())
          .andExpect(jsonPath("$.enabled").isBoolean());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return STATIC_TOKEN security type")
    void getWebhook_200_static_token_security() throws Exception {
      mockMvc.perform(get(WEBHOOK_PATH + "/token-connector").accept(APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(jsonPath("$.security.type").value("STATIC_TOKEN"))
          .andExpect(jsonPath("$.security.config.header_name").value("X-Auth-Token"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return NONE security type with empty config")
    void getWebhook_200_none_security() throws Exception {
      mockMvc.perform(get(WEBHOOK_PATH + "/public-connector").accept(APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(jsonPath("$.security.type").value("NONE"));
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

    @Test
    @WithMockUser
    @DisplayName("Should force enabled to false when updating with empty mappings")
    void putWebhook_200_enabled_forced_false_when_no_mapping() throws Exception {
      createWebhookConnector("connector-put-no-mapping", "Connector No Mapping Title");

      var payload = """
          {
            "identifier": "ignored-by-put",
            "title": "Connector No Mapping Title",
            "description": "updated without mappings",
            "enabled": true,
            "mapping_identifiers": [],
            "security": {
              "type": "NONE",
              "config": {}
            }
          }
          """;

      mockMvc
          .perform(MockMvcRequestBuilders.put(WEBHOOK_PATH + "/connector-put-no-mapping")
              .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.identifier").value("connector-put-no-mapping"))
          // enabled=true sent but no mappings → must be forced to false
          .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when PUT security config is missing required fields")
    void putWebhook_400_missing_security_config_fields() throws Exception {
      createWebhookConnector("connector-put-bad-security", "Connector Bad Security");

      var payload = """
          {
            "identifier": "ignored-by-put",
            "title": "Connector Bad Security",
            "enabled": true,
            "mapping_identifiers": ["connector-put-bad-security-mapping"],
            "security": {
              "type": "HMAC_SHA256",
              "config": {
                "header_name": "X-Sig"
              }
            }
          }
          """;

      mockMvc
          .perform(MockMvcRequestBuilders.put(WEBHOOK_PATH + "/connector-put-bad-security")
              .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"))
          .andExpect(jsonPath("$.error_description").value(containsString("secret_alias")));
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

  @Nested
  @DisplayName("Security config records")
  @Order(6)
  class SecurityConfigRecordTests {

    @Test
    @DisplayName("BasicAuthConfig should expose BASIC_AUTH type and values")
    void basicAuthConfig_shouldExposeTypeAndValues() {
      SecurityConfig config = new BasicAuthConfig("admin", "BASIC_SECRET");

      var basicAuthConfig = assertInstanceOf(BasicAuthConfig.class, config);
      assertEquals(WebhookSecurityType.BASIC_AUTH, config.type());
      assertEquals("admin", basicAuthConfig.username());
      assertEquals("BASIC_SECRET", basicAuthConfig.secretAlias());
    }

    @Test
    @DisplayName("HmacConfig should expose HMAC_SHA256 type and default optional values")
    void hmacConfig_shouldExposeTypeAndDefaultOptionalValues() {
      SecurityConfig config = new HmacConfig("X-Hub-Signature-256", "HMAC_SECRET", null, null);

      var hmacConfig = assertInstanceOf(HmacConfig.class, config);
      assertEquals(WebhookSecurityType.HMAC_SHA256, config.type());
      assertEquals("X-Hub-Signature-256", hmacConfig.headerName());
      assertEquals("HMAC_SECRET", hmacConfig.secretAlias());
      assertEquals("", hmacConfig.prefix());
      assertEquals(HmacConfig.DEFAULT_ENCODING, hmacConfig.encoding());
    }

    @Test
    @DisplayName("StaticTokenConfig should expose STATIC_TOKEN type and values")
    void staticTokenConfig_shouldExposeTypeAndValues() {
      SecurityConfig config = new StaticTokenConfig("X-Auth-Token", "TOKEN_SECRET");

      var staticTokenConfig = assertInstanceOf(StaticTokenConfig.class, config);
      assertEquals(WebhookSecurityType.STATIC_TOKEN, config.type());
      assertEquals("X-Auth-Token", staticTokenConfig.headerName());
      assertEquals("TOKEN_SECRET", staticTokenConfig.secretAlias());
    }

    @Test
    @DisplayName("JwtBearerConfig should expose JWT_BEARER type and jwksUri")
    void jwtBearerConfig_shouldExposeTypeAndValues() {
      SecurityConfig config = new JwtBearerConfig("https://auth.example.com/.well-known/jwks.json");

      var jwtBearerConfig = assertInstanceOf(JwtBearerConfig.class, config);
      assertEquals(WebhookSecurityType.JWT_BEARER, config.type());
      assertEquals("https://auth.example.com/.well-known/jwks.json", jwtBearerConfig.jwksUri());
    }
  }

  @Nested
  @DisplayName("Webhook support components coverage")
  @Order(7)
  class WebhookSupportComponentsCoverageTests {

    @Test
    @DisplayName("InboundWebhookEntityMappingDtoOut should defensively copy maps")
    void inboundWebhookEntityMappingDtoOut_shouldDefensivelyCopyMaps() {
      var properties = Map.of("applicationName", ".repository.name");
      var relations = Map.of("owner", ".sender.login");

      var dto = new InboundWebhookEntityMappingDtoOut(".repository.full_name", ".repository.name",
          properties, relations);

      assertEquals(".repository.full_name", dto.identifier());
      assertEquals(".repository.name", dto.title());
      assertEquals(properties, dto.properties());
      assertEquals(relations, dto.relations());
      var copiedProperties = dto.properties();
      var copiedRelations = dto.relations();
      assertThrows(UnsupportedOperationException.class,
          () -> copiedProperties.put("environment", "DEV"));
      assertThrows(UnsupportedOperationException.class,
          () -> copiedRelations.put("team", ".team.slug"));
    }

    @Test
    @DisplayName("WebhookConnectorConfigurationException should expose its message")
    void webhookConnectorConfigurationException_shouldExposeMessage() {
      var exception = new WebhookConnectorConfigurationException(
          "Webhook connector title is mandatory");

      assertInstanceOf(RuntimeException.class, exception);
      assertEquals("Webhook connector title is mandatory", exception.getMessage());
    }

    @Test
    @DisplayName("WebhookTemplateMappingPersistenceMapper should build link entity from explicit ids")
    void webhookTemplateMappingPersistenceMapper_shouldBuildLinkEntityFromExplicitIds() {
      var webhookId = UUID.fromString("770e8400-e29b-41d4-a716-446655440001");
      var templateId = UUID.fromString("550e8400-e29b-41d4-a716-446655440071");
      var entityMappingId = UUID.fromString("880e8400-e29b-41d4-a716-446655440001");
      var jsltFilter = ".action == \"pushed\"";

      var jpa = webhookTemplateMappingPersistenceMapper.toJpa(webhookId, templateId,
          entityMappingId, jsltFilter);

      assertEquals(webhookId, jpa.getWebhookId());
      assertEquals(templateId, jpa.getTemplateId());
      assertEquals(entityMappingId, jpa.getEntityMappingId());
      assertEquals(jsltFilter, jpa.getJsltFilter());
    }
  }

  @Nested
  @DisplayName("Security config validation - all types")
  @Order(8)
  class SecurityConfigTests {

    @Test
    @WithMockUser
    @DisplayName("NONE — Should create connector with empty config")
    void security_none_empty_config_201() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf())
              .content(buildSecurityPayload("sec-none", "None Security", "NONE", "{}")))
          .andExpect(status().isCreated()).andExpect(jsonPath("$.security.type").value("NONE"));
    }

    @Test
    @WithMockUser
    @DisplayName("NONE — Should create connector without config field")
    void security_none_no_config_201() throws Exception {
      var payload = """
          {
            "identifier": "sec-none-no-config",
            "title": "None No Config",
            "enabled": false,
            "mapping_identifiers": [],
            "security": { "type": "NONE", "config":{} }
          }
          """;
      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isCreated()).andExpect(jsonPath("$.security.type").value("NONE"));
    }

    @Test
    @WithMockUser
    @DisplayName("HMAC_SHA256 — Should create with valid header_name and secret_alias")
    void security_hmac_valid_201() throws Exception {
      var config = """
          { "header_name": "X-Hub-Signature-256", "secret_alias": "MY_SECRET" }
          """;
      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf())
              .content(buildSecurityPayload("sec-hmac-valid", "HMAC Valid", "HMAC_SHA256", config)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.security.type").value("HMAC_SHA256"))
          .andExpect(jsonPath("$.security.config.header_name").value("X-Hub-Signature-256"));
    }

    @Test
    @WithMockUser
    @DisplayName("HMAC_SHA256 — Should return 400 when header_name is missing")
    void security_hmac_missing_header_name_400() throws Exception {
      var config = """
          { "secret_alias": "MY_SECRET" }
          """;
      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf())
              .content(buildSecurityPayload("sec-hmac-no-header", "HMAC No Header", "HMAC_SHA256",
                  config)))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"))
          .andExpect(jsonPath("$.error_description").value(containsString("header_name")));
    }

    @Test
    @WithMockUser
    @DisplayName("HMAC_SHA256 — Should return 400 when secret_alias is missing")
    void security_hmac_missing_secret_alias_400() throws Exception {
      var config = """
          { "header_name": "X-Hub-Signature-256" }
          """;
      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf())
              .content(buildSecurityPayload("sec-hmac-no-secret", "HMAC No Secret", "HMAC_SHA256",
                  config)))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"))
          .andExpect(jsonPath("$.error_description").value(containsString("secret_alias")));
    }

    @Test
    @WithMockUser
    @DisplayName("HMAC_SHA256 — Should return 400 when secret_alias has invalid format (lowercase)")
    void security_hmac_invalid_secret_alias_format_400() throws Exception {
      var config = """
          { "header_name": "X-Sig", "secret_alias": "invalid-lowercase" }
          """;
      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf())
              .content(buildSecurityPayload("sec-hmac-bad-alias", "HMAC Bad Alias", "HMAC_SHA256",
                  config)))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"))
          .andExpect(jsonPath("$.error_description").value(containsString("UPPER_SNAKE_CASE")));
    }

    @Test
    @WithMockUser
    @DisplayName("HMAC_SHA256 — Should accept secret_alias as environment reference ${MY_SECRET}")
    void security_hmac_env_reference_201() throws Exception {
      var config = """
          { "header_name": "X-Sig", "secret_alias": "${MY_SECRET}" }
          """;
      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content(
                  buildSecurityPayload("sec-hmac-env-ref", "HMAC Env Ref", "HMAC_SHA256", config)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.security.type").value("HMAC_SHA256"));
    }

    // -----------------------------------------------------------------------
    // STATIC_TOKEN
    // -----------------------------------------------------------------------

    @Test
    @WithMockUser
    @DisplayName("STATIC_TOKEN — Should create with valid header_name and secret_alias")
    void security_static_token_valid_201() throws Exception {
      var config = """
          { "header_name": "X-Auth-Token", "secret_alias": "TOKEN_SECRET" }
          """;
      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content(
                  buildSecurityPayload("sec-token-valid", "Token Valid", "STATIC_TOKEN", config)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.security.type").value("STATIC_TOKEN"))
          .andExpect(jsonPath("$.security.config.header_name").value("X-Auth-Token"));
    }

    @Test
    @WithMockUser
    @DisplayName("STATIC_TOKEN — Should return 400 when header_name is missing")
    void security_static_token_missing_header_400() throws Exception {
      var config = """
          { "secret_alias": "TOKEN_SECRET" }
          """;
      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf())
              .content(buildSecurityPayload("sec-token-no-header", "Token No Header",
                  "STATIC_TOKEN", config)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error_description").value(containsString("header_name")));
    }

    @Test
    @WithMockUser
    @DisplayName("STATIC_TOKEN — Should return 400 when secret_alias is missing")
    void security_static_token_missing_secret_400() throws Exception {
      var config = """
          { "header_name": "X-Auth-Token" }
          """;
      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf())
              .content(buildSecurityPayload("sec-token-no-secret", "Token No Secret",
                  "STATIC_TOKEN", config)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error_description").value(containsString("secret_alias")));
    }

    // -----------------------------------------------------------------------
    // BASIC_AUTH
    // -----------------------------------------------------------------------

    @Test
    @WithMockUser
    @DisplayName("BASIC_AUTH — Should create with valid username and secret_alias")
    void security_basic_auth_valid_201() throws Exception {
      var config = """
          { "username": "admin", "secret_alias": "BASIC_SECRET" }
          """;
      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content(
                  buildSecurityPayload("sec-basic-valid", "Basic Valid", "BASIC_AUTH", config)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.security.type").value("BASIC_AUTH"));
    }

    @Test
    @WithMockUser
    @DisplayName("BASIC_AUTH — Should return 400 when username is missing")
    void security_basic_auth_missing_username_400() throws Exception {
      var config = """
          { "secret_alias": "BASIC_SECRET" }
          """;
      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content(
                  buildSecurityPayload("sec-basic-no-user", "Basic No User", "BASIC_AUTH", config)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error_description").value(containsString("username")));
    }

    @Test
    @WithMockUser
    @DisplayName("BASIC_AUTH — Should return 400 when secret_alias is missing")
    void security_basic_auth_missing_secret_400() throws Exception {
      var config = """
          { "username": "admin" }
          """;
      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf())
              .content(buildSecurityPayload("sec-basic-no-secret", "Basic No Secret", "BASIC_AUTH",
                  config)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error_description").value(containsString("secret_alias")));
    }

    // -----------------------------------------------------------------------
    // JWT_BEARER
    // -----------------------------------------------------------------------

    @Test
    @WithMockUser
    @DisplayName("JWT_BEARER — Should create with valid jwks_uri")
    void security_jwt_bearer_valid_201() throws Exception {
      var config = """
          { "jwks_uri": "https://auth.example.com/.well-known/jwks.json" }
          """;
      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf())
              .content(buildSecurityPayload("sec-jwt-valid", "JWT Valid", "JWT_BEARER", config)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.security.type").value("JWT_BEARER"));
    }

    @Test
    @WithMockUser
    @DisplayName("JWT_BEARER — Should create with jwks_uri as environment reference")
    void security_jwt_bearer_env_reference_201() throws Exception {
      var config = """
          { "jwks_uri": "${JWKS_URI}" }
          """;
      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf())
              .content(buildSecurityPayload("sec-jwt-env", "JWT Env", "JWT_BEARER", config)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.security.type").value("JWT_BEARER"));
    }

    @Test
    @WithMockUser
    @DisplayName("JWT_BEARER — Should return 400 when jwks_uri is missing")
    void security_jwt_bearer_missing_jwks_uri_400() throws Exception {
      var config = """
          {}
          """;
      mockMvc
          .perform(MockMvcRequestBuilders.post(WEBHOOK_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf())
              .content(buildSecurityPayload("sec-jwt-no-uri", "JWT No URI", "JWT_BEARER", config)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error_description").value(containsString("jwks_uri")));
    }
  }

  /// Builds a minimal POST payload with a given security type and config JSON
  /// string.
  private String buildSecurityPayload(String identifier, String title, String securityType,
      String configJson) {
    return """
        {
          "identifier": "%s",
          "title": "%s",
          "enabled": false,
          "mapping_identifiers": [],
          "security": {
            "type": "%s",
            "config": %s
          }
        }
        """.formatted(identifier, title, securityType, configJson);
  }
}
