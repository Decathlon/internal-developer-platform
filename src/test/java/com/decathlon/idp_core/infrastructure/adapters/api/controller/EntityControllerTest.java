package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.decathlon.idp_core.AbstractIntegrationTest;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity.EntitySearchDomainMapper;

/// Integration tests for the EntityController REST API endpoints.
/// These tests verify the behavior of entity retrieval endpoints, including
/// pagination, authentication, and lookup by template identifier and entity
/// identifier.
public class EntityControllerTest extends AbstractIntegrationTest {

    private static final String TEMPLATE_IDENTIFIER = "web-service";
    private static final String ENTITY_IDENTIFIER = "web-api-2";
    private static final String ENTITIES_BY_IDENTIFIER_PATH = "/api/v1/entities/{template-identifier}/{identifier}";
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

    /// Tests for GET /api/v1/entities/{template-identifier}/{identifier}
    /// endpoint (lookup by template and identifier).
    @Nested
    @DisplayName("GET /api/v1/entities/{template-identifier}/{identifier} - Get Entities by template identifier and entity identifier")
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

    @Nested
    @DisplayName("PUT /api/v1/entities/{template-identifier}/identifier/{identifier} - Update entity")
    class PutEntitiesTests {

        @Test
        @WithMockUser
        @DisplayName("Should update entity and return 200")
        void putEntity_200() throws Exception {
            var payload = """
                    {
                      "name": "Web API 2 Updated",
                      "identifier": "web-api-2",
                      "properties": {
                        "applicationName": "catalog-api",
                        "ownerEmail": "owner@example.com",
                        "port": "9090",
                        "environment": "DEV",
                        "version": "1.2.3",
                        "teamName": "platform-team",
                        "baseUrl": "https://catalog.example.com",
                        "protocol": "HTTP",
                        "programmingLanguage": "PYTHON"
                      },
                      "relations": [
                        {
                          "name": "database",
                          "target_entity_identifiers": ["cache-service-1"]
                        }
                      ]
                    }
                    """;

            mockMvc.perform(put(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, ENTITY_IDENTIFIER)
                            .contentType(APPLICATION_JSON)
                            .accept(APPLICATION_JSON)
                            .with(csrf())
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.identifier").value(ENTITY_IDENTIFIER))
                    .andExpect(jsonPath("$.template_identifier").value(TEMPLATE_IDENTIFIER))
                    .andExpect(jsonPath("$.name").value("Web API 2 Updated"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when updating non-existent entity")
        void putEntity_404_non_existent_entity() throws Exception {
            var payload = """
                    {
                      "name": "Unknown",
                      "identifier": "unknown-entity"
                    }
                    """;

            mockMvc.perform(put(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, "unknown-entity")
                            .contentType(APPLICATION_JSON)
                            .accept(APPLICATION_JSON)
                            .with(csrf())
                            .content(payload))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when path identifier and body identifier do not match")
        void putEntity_400_identifier_mismatch() throws Exception {
            var payload = """
                    {
                      "name": "Web API 2 Updated",
                      "identifier": "different-id",
                      "properties": {
                        "applicationName": "catalog-api",
                        "ownerEmail": "owner@example.com",
                        "port": "8080",
                        "environment": "DEV",
                        "version": "1.2.3",
                        "teamName": "platform-team",
                        "baseUrl": "https://catalog.example.com",
                        "protocol": "HTTP",
                        "programmingLanguage": "JAVA"
                      }
                    }
                    """;

            mockMvc.perform(put(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, ENTITY_IDENTIFIER)
                            .contentType(APPLICATION_JSON)
                            .accept(APPLICATION_JSON)
                            .with(csrf())
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.error_description").value(org.hamcrest.Matchers.containsString("Entity identifier in body must match path identifier")));
        }

        @Test
        @DisplayName("Should return 401 when updating without authentication")
        void putEntity_401_without_user_token() throws Exception {
            var payload = """
                    {
                      "name": "Web API 2 Updated",
                      "identifier": "web-api-2"
                    }
                    """;

            mockMvc.perform(put(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, ENTITY_IDENTIFIER)
                            .contentType(APPLICATION_JSON)
                            .accept(APPLICATION_JSON)
                            .with(csrf())
                            .content(payload))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 403 when updating without CSRF token")
        void putEntity_403_without_csrf() throws Exception {
            var payload = """
                    {
                      "name": "Web API 2 Updated",
                      "identifier": "web-api-2"
                    }
                    """;

            mockMvc.perform(put(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, ENTITY_IDENTIFIER)
                            .contentType(APPLICATION_JSON)
                            .accept(APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/entities/search")
    class SearchEntitiesTests {

        private static final String SEARCH_PATH = "/api/v1/entities/search";
        private static final String SEARCH_JSON_PATH = ENTITY_JSON_FILES_TEST_PATH + "search/";

        @Test
        @DisplayName("Should return 401 without authentication")
        void search_401_withoutAuth() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(SEARCH_JSON_PATH + "search_request_template_and_property.json")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should search entities by template AND property (EQ)")
        @WithMockUser
        void search_200_byTemplateAndProperty() throws Exception {
            MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(SEARCH_JSON_PATH + "search_request_template_and_property.json")))
                    .andExpect(status().isOk())
                    .andReturn();
            JSONAssert.assertEquals(
                    getJsonTestFileContent(ENTITY_JSON_FILES_TEST_PATH + "searchEntities_200_byTemplateAndProperty.json"),
                    result.getResponse().getContentAsString(),
                    JSONCompareMode.STRICT);
        }

        @Test
        @DisplayName("Should search entities using OR connector across templates")
        @WithMockUser
        void search_200_orTemplates() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(SEARCH_JSON_PATH + "search_request_or_templates.json")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.page.total_elements").value(2));
        }

        @Test
        @DisplayName("Should search entities using OR connector on multiple templates")
        @WithMockUser
        void search_200_inTemplates() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(SEARCH_JSON_PATH + "search_request_in_templates.json")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.page.total_elements").value(2));
        }

        @Test
        @DisplayName("Should search entities by relations_as_target identifier")
        @WithMockUser
        void search_200_byRelationsAsTarget() throws Exception {
            MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(SEARCH_JSON_PATH + "search_request_relations_as_target.json")))
                    .andExpect(status().isOk())
                    .andReturn();
            JSONAssert.assertEquals(
                    getJsonTestFileContent(ENTITY_JSON_FILES_TEST_PATH + "searchEntities_200_byRelationsAsTarget.json"),
                    result.getResponse().getContentAsString(),
                    JSONCompareMode.STRICT);
        }

        @Test
        @DisplayName("Should search entities using STARTS_WITH operator")
        @WithMockUser
        void search_200_startsWith() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(SEARCH_JSON_PATH + "search_request_starts_with.json")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].identifier").value("web-api-1"));
        }

        @Test
        @DisplayName("Should search entities using NEQ operator")
        @WithMockUser
        void search_200_neq() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(SEARCH_JSON_PATH + "search_request_neq.json")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].identifier").value("web-api-2"));
        }

        @Test
        @DisplayName("Should return empty content when no entities match")
        @WithMockUser
        void search_200_noMatch() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            {
                              "filter": {
                                "field": "identifier",
                                "operation": "EQ",
                                "value": "non-existent-entity-xyz"
                              },
                              "page": 0,
                              "size": 20
                            }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.page.total_elements").value(0));
        }

        @Test
        @DisplayName("Should return all entities when filter is null")
        @WithMockUser
        void search_200_nullFilter() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            {
                              "page": 0,
                              "size": 5
                            }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(5));
        }

        @Test
        @DisplayName("Should return paginated results respecting size parameter")
        @WithMockUser
        void search_200_paginated() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            {
                              "filter": {
                                "field": "template",
                                "operation": "EQ",
                                "value": "monitoring-service"
                              },
                              "page": 0,
                              "size": 3
                            }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(3))
                    .andExpect(jsonPath("$.page.size").value(3))
                    .andExpect(jsonPath("$.page.total_elements").value(6))
                    .andExpect(jsonPath("$.page.total_pages").value(2));
        }

        @Test
        @DisplayName("Should return 400 for invalid connector value")
        @WithMockUser
        void search_400_invalidConnector() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            {
                              "filter": {
                                "connector": "INVALID_CONNECTOR",
                                "criteria": [
                                  { "field": "template", "operation": "EQ", "value": "microservice" }
                                ]
                              },
                              "page": 0,
                              "size": 20
                            }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_description").value("Invalid connector 'INVALID_CONNECTOR'. Supported values: AND, OR"));
        }

        @Test
        @DisplayName("Should return 400 for invalid operation value")
        @WithMockUser
        void search_400_invalidOperation() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            {
                              "filter": {
                                "field": "identifier",
                                "operation": "LIKE",
                                "value": "api"
                              },
                              "page": 0,
                              "size": 20
                            }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_description").value("Invalid operation 'LIKE'. Supported values: EQ, NEQ, CONTAINS, NOT_CONTAINS, STARTS_WITH, ENDS_WITH, GT, GTE, LT, LTE"));
        }

        @Test
        @DisplayName("Should return 400 for invalid field name")
        @WithMockUser
        void search_400_invalidField() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            {
                              "filter": {
                                "field": "unknownField",
                                "operation": "EQ",
                                "value": "value"
                              },
                              "page": 0,
                              "size": 20
                            }
                            """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when criterion is missing field")
        @WithMockUser
        void search_400_missingField() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            {
                              "filter": {
                                "operation": "EQ",
                                "value": "microservice"
                              },
                              "page": 0,
                              "size": 20
                            }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_description").value("A criterion node must have a non-blank 'field'"));
        }

        @Test
        @DisplayName("Should return 400 when group is missing criteria")
        @WithMockUser
        void search_400_groupMissingCriteria() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            {
                              "filter": {
                                "connector": "AND"
                              },
                              "page": 0,
                              "size": 20
                            }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_description").value("A group node must have a non-empty 'criteria' list"));
        }

        @Test
        @DisplayName("Should support sort parameter")
        @WithMockUser
        void search_200_withSort() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            {
                              "filter": {
                                "field": "template",
                                "operation": "EQ",
                                "value": "monitoring-service"
                              },
                              "page": 0,
                              "size": 6,
                              "sort": "name:desc"
                            }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(6))
                    .andExpect(jsonPath("$.content[0].name").value("Monitoring Service 6"));
        }

        @Test
        @DisplayName("Should support nested AND/OR filter composition")
        @WithMockUser
        void search_200_nestedFilter() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            {
                              "filter": {
                                "connector": "AND",
                                "criteria": [
                                  {
                                    "connector": "OR",
                                    "criteria": [
                                      { "field": "template", "operation": "EQ", "value": "microservice" },
                                      { "field": "template", "operation": "EQ", "value": "batch-job" }
                                    ]
                                  },
                                  { "field": "identifier", "operation": "EQ", "value": "microservice-1" }
                                ]
                              },
                              "page": 0,
                              "size": 20
                            }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].identifier").value("microservice-1"));
        }

        @Test
        @DisplayName("Should find entities by query matching identifier")
        @WithMockUser
        void search_200_queryByIdentifier() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            { "query": "web-api", "page": 0, "size": 20 }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.page.total_elements").value(2));
        }

        @Test
        @DisplayName("Should find entities by query matching name (case-insensitive)")
        @WithMockUser
        void search_200_queryByName() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            { "query": "Web API", "page": 0, "size": 20 }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.page.total_elements").value(2));
        }

        @Test
        @DisplayName("Should find entities by query matching a property value")
        @WithMockUser
        void search_200_queryByPropertyValue() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            { "query": "JAVA", "page": 0, "size": 20 }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].identifier").value("web-api-1"));
        }

        @Test
        @DisplayName("Should combine query and filter with AND semantics")
        @WithMockUser
        void search_200_queryAndFilter() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            {
                              "query": "JAVA",
                              "filter": {
                                "field": "template",
                                "operation": "EQ",
                                "value": "web-service"
                              },
                              "page": 0,
                              "size": 20
                            }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].identifier").value("web-api-1"));
        }

        @Test
        @DisplayName("Should treat blank query as no-op and return all entities")
        @WithMockUser
        void search_200_blankQueryIsNoOp() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            { "query": "   ", "page": 0, "size": 5 }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(5));
        }

        @Test
        @DisplayName("Should return 400 when query exceeds maximum length")
        @WithMockUser
        void search_400_queryTooLong() throws Exception {
            String tooLong = "x".repeat(256);
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            { "query": "%s", "page": 0, "size": 20 }
                            """.formatted(tooLong)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_description").value("Search query must not exceed 255 characters"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when GT operator is used on a non-property field")
        void search_400_numericOperator_onNonPropertyField() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            {
                              "filter": {
                                "connector": "AND",
                                "criteria": [
                                  { "field": "template", "operation": "GT", "value": "5" }
                                ]
                              },
                              "page": 0, "size": 20
                            }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_description").value(
                            org.hamcrest.Matchers.containsString("GT")));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when GT operator is used with a non-numeric value")
        void search_400_numericOperator_nonNumericValue() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            {
                              "filter": {
                                "connector": "AND",
                                "criteria": [
                                  { "field": "property.port", "operation": "GT", "value": "not-a-number" }
                                ]
                              },
                              "page": 0, "size": 20
                            }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_description").value(
                            org.hamcrest.Matchers.containsString("not-a-number")));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when GTE is used on a STRING-typed property with a known template")
        void search_400_numericOperator_onStringProperty() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            {
                              "filter": {
                                "connector": "AND",
                                "criteria": [
                                  { "field": "template", "operation": "EQ", "value": "web-service" },
                                  { "field": "property.programmingLanguage", "operation": "GTE", "value": "5" }
                                ]
                              },
                              "page": 0, "size": 20
                            }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_description").value(
                            org.hamcrest.Matchers.allOf(
                                    org.hamcrest.Matchers.containsString("programmingLanguage"),
                                    org.hamcrest.Matchers.containsString("STRING"))));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 200 and match correct entities when GT used on a NUMBER property")
        void search_200_numericGt_onNumberProperty() throws Exception {
            // web-api-1 has port=8080, web-api-2 has port=9090; GT 8085 should return only web-api-2
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            {
                              "filter": {
                                "connector": "AND",
                                "criteria": [
                                  { "field": "template", "operation": "EQ", "value": "web-service" },
                                  { "field": "property.port", "operation": "GT", "value": "8085" }
                                ]
                              },
                              "page": 0, "size": 20
                            }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.total_elements").value(1))
                    .andExpect(jsonPath("$.content[0].identifier").value("web-api-2"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 200 and match all seeded entities when LTE used with upper bound covering all")
        void search_200_numericLte_onNumberProperty_allMatch() throws Exception {
            // Both web-api-1 (port=8080) and web-api-2 (port=9090) are <= 9999.
            // Other test methods (e.g. postEntity_201) may create additional web-service entities
            // in the same shared DB, so we only assert at-least-2 rather than an exact count.
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            {
                              "filter": {
                                "connector": "AND",
                                "criteria": [
                                  { "field": "template", "operation": "EQ", "value": "web-service" },
                                  { "field": "property.port", "operation": "LTE", "value": "9999" }
                                ]
                              },
                              "page": 0, "size": 20
                            }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.total_elements",
                            org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 200 when page and size are omitted from the request body")
        void search_200_noPageOrSize_usesDefaults() throws Exception {
            // Omitting page and size must not cause a 400 JSON parse error (primitive int vs null).
            // The record defaults should kick in: page=0, size=20.
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            {
                              "filter": {
                                "connector": "AND",
                                "criteria": [
                                  { "field": "template", "operation": "EQ", "value": "web-service" }
                                ]
                              }
                            }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.size").value(20))
                    .andExpect(jsonPath("$.page.number").value(0));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when size exceeds the maximum allowed value")
        void search_400_pageSizeTooLarge() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            { "page": 0, "size": 501 }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_description")
                            .value("Page size must not exceed %d".formatted(EntitySearchDomainMapper.MAX_PAGE_SIZE)));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when sort field is not in the allowed list")
        void search_400_invalidSortField() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            { "page": 0, "size": 20, "sort": "badField:asc" }
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_description").value("Invalid sort field 'badField'. Supported fields: identifier, name, templateIdentifier"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return entities that have a relation with an exact name match")
        void search_200_byRelationNameEq() throws Exception {
            // web-api-1 has relation "api-link"; web-api-2 does not
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(SEARCH_JSON_PATH + "search_request_relation_name_eq.json")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].identifier").value("web-api-1"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return entities that have a relation whose name contains the given value")
        void search_200_byRelationNameContains() throws Exception {
            // both web-api-1 and web-api-2 have a relation named "database"
            mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(SEARCH_JSON_PATH + "search_request_relation_name_contains.json")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2));
        }
    }
}
