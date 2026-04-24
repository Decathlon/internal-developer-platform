package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.decathlon.idp_core.AbstractIntegrationTest;

/// Integration tests for the EntityController REST API endpoints.
/// These tests verify the behavior of entity retrieval endpoints, including
/// pagination, authentication, and lookup by template identifier and entity
/// identifier.
public class EntityControllerTest extends AbstractIntegrationTest {

    private static final String TEMPLATE_IDENTIFIER = "web-service";
    private static final String ENTITY_IDENTIFIER = "web-api-2";
    private static final String ENTITIES_BY_IDENTIFIER_PATH = "/api/v1/entities/{template-identifier}/identifier/{identifier}";
    private static final String ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH = "/api/v1/entities/{template-identifier}";
    private static final String ENTITY_JSON_FILES_TEST_PATH = "integration_test/json/entity/v1/";
    @Autowired
    private MockMvc mockMvc;

    /// Tests for GET /api/v1/entities/{template-identifier} endpoint (paginated
    /// retrieval).
    @Nested
    @DisplayName("GET /api/v1/entities/{template-identifier} - Get Templates Paginated")
    class GetEntitiesByTemplateIdentifierTests {

        @Test
        @DisplayName("Should return paginated entities with default pagination")
        @WithMockUser
        void getEntities_paginated_200() throws Exception {
            mockMvc.perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
                            .param("page", "0")
                            .param("size", "15")
                            .accept(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.page.total_elements").value(2))
                    .andExpect(jsonPath("$.page.total_pages").value(1))
                    .andExpect(jsonPath("$.page.size").value(15))
                    .andExpect(jsonPath("$.page.number").value(0))
                    .andExpect(jsonPath("$.content[0].template_identifier").value(TEMPLATE_IDENTIFIER));
        }

        @Test
        @DisplayName("Should return paginated entities with default pagination")
        @WithMockUser
        void getEntities_paginated_404_when_non_existent_template() throws Exception {
            mockMvc.perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, "non-existent-template-identifier")
                            .accept(APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void getTemplates_paginated_401_without_user_token() throws Exception {
            mockMvc.perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
                            .accept(APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return paginated entities with custom pagination")
        @WithMockUser
        void getEntities_paginated_200_custom() throws Exception {

            mockMvc.perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, "monitoring-service")
                            .param("page", "1")
                            .param("size", "5")
                            .param("sort", "template_identifier,asc")
                            .accept(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("Monitoring Service 6"))
                    .andExpect(jsonPath("$.page.total_elements").value(6))
                    .andExpect(jsonPath("$.page.total_pages").value(2))
                    .andExpect(jsonPath("$.page.size").value(5))
                    .andExpect(jsonPath("$.page.number").value(1));
        }

        @Test
        @DisplayName("Should return paginated entities with default pagination")
        @WithMockUser
        void getEntities_invalid_pagination_200() throws Exception {
            mockMvc.perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
                            .accept(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.page.total_elements").value(2))
                    .andExpect(jsonPath("$.page.total_pages").value(1))
                    .andExpect(jsonPath("$.page.size").value(20))
                    .andExpect(jsonPath("$.page.number").value(0))
                    .andExpect(jsonPath("$.content[0].template_identifier").value(TEMPLATE_IDENTIFIER));
        }
    }

    /// Tests for GET /api/v1/entities/{template-identifier}/identifier/{identifier}
    /// endpoint (lookup by template and identifier).
    @Nested
    @DisplayName("GET /api/v1/entities/{template-identifier}/identifier/{identifier} - Get Entities by template identifier and entity identifier")
    class GetEntitiesByTemplateAndEntityIdentifierTests {

        @Test
        @DisplayName("Should return entity by template identifier and identifier")
        @WithMockUser
        void getEntityByTemplateAndIdentifier_200() throws Exception {
            mockMvc.perform(get(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, ENTITY_IDENTIFIER)
                            .accept(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.identifier").value(ENTITY_IDENTIFIER))
                    .andExpect(jsonPath("$.template_identifier").value(TEMPLATE_IDENTIFIER));
        }

        @Test
        @DisplayName("Should return 404 for non-existent entity")
        @WithMockUser
        void getEntityByTemplateAndIdentifier_404_non_existent_entity() throws Exception {
            mockMvc.perform(get(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, "non-existent-identifier")
                            .accept(APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 for non-existent entity template")
        @WithMockUser
        void getEntityByTemplateAndIdentifier_404_non_existent_template() throws Exception {
            mockMvc.perform(get(ENTITIES_BY_IDENTIFIER_PATH, "non-existent-template", "non-existent-identifier")
                            .accept(APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/entities/{template-identifier} - Get Entities by template identifier and entity identifier")
    class PostEntitiesTests {

        @Test
        @WithMockUser()
        @DisplayName("Should create entity and return 201")
        void postEntity_201() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
                            .contentType(APPLICATION_JSON)
                            .accept(APPLICATION_JSON)
                            .with(csrf())
                            .content(getJsonTestFileContent(ENTITY_JSON_FILES_TEST_PATH + "postEntity_201.json")))
                    .andExpect(status().isCreated())
                    .andReturn();
        }

        @Test
        @WithMockUser()
        @DisplayName("Should return 400 when required template properties are missing")
        void postEntity_400_when_required_properties_missing() throws Exception {
            var payload = """
                    {
                      "name": "web-service-missing-required",
                      "identifier": "web-service-missing-required",
                      "properties": {
                        "port": "8080"
                      }
                    }
                    """;

            mockMvc.perform(MockMvcRequestBuilders.post(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
                            .contentType(APPLICATION_JSON)
                            .accept(APPLICATION_JSON)
                            .with(csrf())
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.error_description").value(org.hamcrest.Matchers.containsString("Property 'applicationName' is required")));
        }

        @Test
        @WithMockUser()
        @DisplayName("Should return 400 when property type does not match template")
        void postEntity_400_when_property_type_mismatch() throws Exception {
            var payload = """
                    {
                      "name": "web-service-invalid-type",
                      "identifier": "web-service-invalid-type",
                      "properties": {
                        "applicationName": "catalog-api",
                        "ownerEmail": "owner@example.com",
                        "port": "not-a-number",
                        "environment": "DEV",
                        "version": "1.2.3",
                        "teamName": "platform-team",
                        "baseUrl": "https://catalog.example.com",
                        "protocol": "HTTP",
                        "programmingLanguage": "JAVA"
                      }
                    }
                    """;

            mockMvc.perform(MockMvcRequestBuilders.post(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
                            .contentType(APPLICATION_JSON)
                            .accept(APPLICATION_JSON)
                            .with(csrf())
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.error_description").value(org.hamcrest.Matchers.containsString("Property 'port' must be of type NUMBER")));
        }

        @Test
        @WithMockUser()
        @DisplayName("Should return 400 when property rules are not respected")
        void postEntity_400_when_property_rules_not_respected() throws Exception {
            var payload = """
                    {
                      "name": "web-service-invalid-rules",
                      "identifier": "web-service-invalid-rules",
                      "properties": {
                        "applicationName": "catalog-api",
                        "ownerEmail": "invalid-email",
                        "port": "80",
                        "environment": "DEV",
                        "version": "1.2.3",
                        "teamName": "platform-team",
                        "baseUrl": "invalid-url",
                        "protocol": "HTTP",
                        "programmingLanguage": "JAVA"
                      }
                    }
                    """;

            mockMvc.perform(MockMvcRequestBuilders.post(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
                            .contentType(APPLICATION_JSON)
                            .accept(APPLICATION_JSON)
                            .with(csrf())
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.error_description").value(org.hamcrest.Matchers.allOf(
                            org.hamcrest.Matchers.containsString("Property 'ownerEmail' does not match expected format"),
                            org.hamcrest.Matchers.containsString("Property 'ownerEmail' does not match required format EMAIL"),
                            org.hamcrest.Matchers.containsString("Property 'baseUrl' does not match expected format"),
                            org.hamcrest.Matchers.containsString("Property 'baseUrl' does not match required format URL"),
                            org.hamcrest.Matchers.containsString("Property 'port' value must be greater than or equal to 1024")
                    )));
        }

    }

}
