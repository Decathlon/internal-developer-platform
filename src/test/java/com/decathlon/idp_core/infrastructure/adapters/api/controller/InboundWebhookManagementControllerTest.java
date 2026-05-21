package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import com.decathlon.idp_core.AbstractIntegrationTest;
import com.decathlon.idp_core.domain.constant.ValidationMessages;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/// Integration tests for {@link InboundWebhookManagementController}.
///
/// Covers the full HTTP contract (status codes, response shape, validation errors)
/// for all CRUD operations on inbound webhook connectors.
@DisplayName("InboundWebhookManagementController Integration Tests")
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@Sql(statements = {
        "DELETE FROM webhook_template_mapping",
        "DELETE FROM webhook_connector"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/db/test/R__1_Insert_test_data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Slf4j
class InboundWebhookManagementControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String WEBHOOK_PATH = "/api/v1/inbound-webhooks";
    private static final String JSON_PATH = "integration_test/json/webhook/v1/";

    private void createWebhookConnector(String identifier, String title) throws Exception {
        var payload = """
                {
                  "identifier": "%s",
                  "title": "%s",
                  "description": "test connector",
                  "enabled": true,
                  "mappings": [
                    {
                      "template": "microservice",
                      "filter": "true",
                      "entity": {
                        "identifier": ".id",
                        "title": ".name",
                        "properties": {
                          "applicationName": ".repository.name",
                          "ownerEmail": ".sender.login",
                          "environment": ".deployment.environment",
                          "version": ".deployment.sha",
                          "port": "8080",
                          "programmingLanguage": ".language"
                        },
                        "relations": {}
                      }
                    }
                  ],
                  "security": {
                    "type": "NONE",
                    "config": {}
                  }
                }
                """.formatted(identifier, title);

        mockMvc.perform(MockMvcRequestBuilders.post(WEBHOOK_PATH)
                        .contentType(APPLICATION_JSON)
                        .accept(APPLICATION_JSON)
                        .with(csrf())
                        .content(payload))
                .andExpect(status().isCreated());
    }

    private String buildPutPayload(String title, boolean enabled) {
        return """
                {
                  "identifier": "ignored-by-put",
                  "title": "%s",
                  "description": "updated description",
                  "enabled": %s,
                  "mappings": [
                    {
                      "template": "microservice",
                      "filter": "true",
                      "entity": {
                        "identifier": ".id",
                        "title": ".name",
                        "properties": {
                          "applicationName": ".repository.name",
                          "ownerEmail": ".sender.login",
                          "environment": ".deployment.environment",
                          "version": ".deployment.sha",
                          "port": "8080",
                          "programmingLanguage": ".language"
                        },
                        "relations": {}
                      }
                    }
                  ],
                  "security": {
                    "type": "STATIC_TOKEN",
                    "config": {
                      "header_name": "X-Token",
                      "secret_alias": "MY_TOKEN"
                    }
                  }
                }
                """.formatted(title, enabled);
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
        @DisplayName("Should return 200 with empty page when no connectors exist")
        void getWebhooks_200_empty_page() throws Exception {
            mockMvc.perform(get(WEBHOOK_PATH).accept(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/inbound-webhooks - Create connector")
    @Order(2)
    class PostWebhookTests {

        @Test
        @DisplayName("Should return 401 without authentication")
        void postWebhook_401_without_user_token() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(WEBHOOK_PATH)
                            .contentType(APPLICATION_JSON)
                            .accept(APPLICATION_JSON)
                            .with(csrf())
                            .content(getJsonTestFileContent(JSON_PATH + "postWebhook_201.json")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser
        @DisplayName("Should create connector and return 201")
        void postWebhook_201() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(WEBHOOK_PATH)
                            .contentType(APPLICATION_JSON)
                            .accept(APPLICATION_JSON)
                            .with(csrf())
                            .content(getJsonTestFileContent(JSON_PATH + "postWebhook_201.json")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.identifier").value("github-dora-connector"))
                    .andExpect(jsonPath("$.title").value("GitHub DORA Connector"))
                    .andExpect(jsonPath("$.security.type").value("HMAC_SHA256"));

            assertWebhookTemplateMapping("github-dora-connector", "microservice", ".action == \"deployment\"");
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 409 when identifier already exists")
        void postWebhook_409_identifier_already_exists() throws Exception {
            createWebhookConnector("github-dora-connector", "GitHub DORA Connector");

            mockMvc.perform(MockMvcRequestBuilders.post(WEBHOOK_PATH)
                            .contentType(APPLICATION_JSON)
                            .accept(APPLICATION_JSON)
                            .with(csrf())
                            .content(getJsonTestFileContent(JSON_PATH + "postWebhook_409_identifier_already_exists.json")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("CONFLICT"))
                    .andExpect(jsonPath("$.error_description").value(
                            containsString(ValidationMessages.WEBHOOK_CONNECTOR_ALREADY_EXIST)));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when identifier is missing")
        void postWebhook_400_identifier_missing() throws Exception {
            var result = postBadRequestAndAssertEquals(
                    WEBHOOK_PATH,
                    JSON_PATH + "postWebhook_400_identifier_missing.json",
                    ValidationMessages.WEBHOOK_CONNECTOR_IDENTIFIER_MANDATORY);
            assertNotNull(result);
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when identifier is blank")
        void postWebhook_400_identifier_blank() throws Exception {
            var result = postBadRequestAndAssertEquals(
                    WEBHOOK_PATH,
                    JSON_PATH + "postWebhook_400_identifier_blank.json",
                    ValidationMessages.WEBHOOK_CONNECTOR_IDENTIFIER_MANDATORY);
            assertNotNull(result);
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when mappings array is empty")
        void postWebhook_400_mappings_empty() throws Exception {
            var result = postBadRequestAndAssertEquals(
                    WEBHOOK_PATH,
                    JSON_PATH + "postWebhook_400_mappings_empty.json",
                    ValidationMessages.WEBHOOK_CONNECTOR_MAPPINGS_MANDATORY);
            assertNotNull(result);
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when security type is unknown")
        void postWebhook_400_invalid_security_type() throws Exception {
            var result = postBadRequestAndAssertContains(
                    WEBHOOK_PATH,
                    JSON_PATH + "postWebhook_400_invalid_security_type.json",
                    "UNKNOWN_TYPE");
            assertNotNull(result);
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when JSLT expression is invalid")
        void postWebhook_400_invalid_jslt() throws Exception {
            var result = postBadRequestAndAssertContains(
                    WEBHOOK_PATH,
                    JSON_PATH + "postWebhook_400_invalid_jslt.json",
                    "Invalid webhook mapping configuration");
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/inbound-webhooks/{identifier} - Get by identifier")
    @Order(3)
    class GetWebhookByIdentifierTests {

        @Test
        @WithMockUser
        @DisplayName("Should return 200 with connector details")
        void getWebhook_200() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(WEBHOOK_PATH)
                            .contentType(APPLICATION_JSON)
                            .accept(APPLICATION_JSON)
                            .with(csrf())
                            .content(getJsonTestFileContent(JSON_PATH + "postWebhook_201.json")))
                    .andExpect(status().isCreated());

            mockMvc.perform(get(WEBHOOK_PATH + "/github-dora-connector").accept(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.identifier").value("github-dora-connector"))
                    .andExpect(jsonPath("$.security.type").value("HMAC_SHA256"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when connector does not exist")
        void getWebhook_404_not_found() throws Exception {
            mockMvc.perform(get(WEBHOOK_PATH + "/non-existent-connector").accept(APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"))
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
            mockMvc.perform(MockMvcRequestBuilders.put(WEBHOOK_PATH + "/github-dora-connector")
                            .contentType(APPLICATION_JSON)
                            .accept(APPLICATION_JSON)
                            .with(csrf())
                            .content(getJsonTestFileContent(JSON_PATH + "putWebhook_200.json")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser
        @DisplayName("Should update connector and return 200")
        void putWebhook_200() throws Exception {
            createWebhookConnector("connector-put-200", "Connector Put Initial Title");

            mockMvc.perform(MockMvcRequestBuilders.put(WEBHOOK_PATH + "/connector-put-200")
                            .contentType(APPLICATION_JSON)
                            .accept(APPLICATION_JSON)
                            .with(csrf())
                            .content(buildPutPayload("Connector Put Updated Title", false)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.identifier").value("connector-put-200"))
                    .andExpect(jsonPath("$.title").value("Connector Put Updated Title"))
                    .andExpect(jsonPath("$.enabled").value(false))
                    .andExpect(jsonPath("$.security.type").value("STATIC_TOKEN"));

            mockMvc.perform(get(WEBHOOK_PATH + "/connector-put-200").accept(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Connector Put Updated Title"))
                    .andExpect(jsonPath("$.enabled").value(false));

            assertWebhookTemplateMapping("connector-put-200", "microservice", "true");
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 409 when updating title to an existing one")
        void putWebhook_409_title_already_exists() throws Exception {
            createWebhookConnector("connector-put-409-a", "Connector A Title");
            createWebhookConnector("connector-put-409-b", "Connector B Title");

            mockMvc.perform(MockMvcRequestBuilders.put(WEBHOOK_PATH + "/connector-put-409-b")
                            .contentType(APPLICATION_JSON)
                            .accept(APPLICATION_JSON)
                            .with(csrf())
                            .content(buildPutPayload("Connector A Title", true)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("CONFLICT"))
                    .andExpect(jsonPath("$.error_description").value(containsString("already exists")));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when connector does not exist")
        void putWebhook_404_not_found() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.put(WEBHOOK_PATH + "/non-existent-connector")
                            .contentType(APPLICATION_JSON)
                            .accept(APPLICATION_JSON)
                            .with(csrf())
                            .content(buildPutPayload("Updated Title", false)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"))
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
                            .accept(APPLICATION_JSON)
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser
        @DisplayName("Should delete connector and return 204")
        void deleteWebhook_204() throws Exception {
            createWebhookConnector("connector-delete-204", "Connector To Delete");

            mockMvc.perform(MockMvcRequestBuilders.delete(WEBHOOK_PATH + "/connector-delete-204")
                            .accept(APPLICATION_JSON)
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(WEBHOOK_PATH + "/connector-delete-204").accept(APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));

            assertWebhookTemplateMappingCount("connector-delete-204", 0L);
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when connector does not exist")
        void deleteWebhook_404_not_found() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.delete(WEBHOOK_PATH + "/non-existent-connector")
                            .accept(APPLICATION_JSON)
                            .with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.error_description").exists());
        }
    }

    private void assertWebhookTemplateMappingCount(String identifier, long expectedCount) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM webhook_template_mapping wtm
                        JOIN webhook_connector wc ON wc.id = wtm.webhook_id
                        WHERE wc.identifier = ?
                        """,
                Long.class,
                identifier
        );

        org.assertj.core.api.Assertions.assertThat(count).isEqualTo(expectedCount);
    }

    private void assertWebhookTemplateMapping(String identifier, String templateIdentifier, String filter) {
        assertWebhookTemplateMappingCount(identifier, 1L);

        var row = jdbcTemplate.queryForMap(
                """
                        SELECT et.identifier AS template_identifier, wtm.jslt_filter AS jslt_filter
                        FROM idp_core.webhook_template_mapping wtm
                        JOIN idp_core.webhook_connector wc ON wc.id = wtm.webhook_id
                        JOIN idp_core.entity_template et ON et.id = wtm.template_id
                        WHERE wc.identifier = ?
                        """,
                identifier
        );

        org.assertj.core.api.Assertions.assertThat(row.get("template_identifier")).isEqualTo(templateIdentifier);
        org.assertj.core.api.Assertions.assertThat(row.get("jslt_filter")).isEqualTo(filter);
    }
}
