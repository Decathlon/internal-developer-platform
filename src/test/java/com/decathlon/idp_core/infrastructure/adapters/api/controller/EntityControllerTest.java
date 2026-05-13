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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.decathlon.idp_core.AbstractIntegrationTest;

    /// Integration tests for the EntityController REST API endpoints.
    /// These tests verify the behavior of entity retrieval endpoints, including
    /// pagination, authentication, and lookup by template identifier and entity
    /// identifier.
public class EntityControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String TEMPLATE_IDENTIFIER = "web-service";
    private static final String ENTITY_IDENTIFIER = "web-api-2";
    private static final String ENTITIES_BY_IDENTIFIER_PATH = "/api/v1/entities/{template-identifier}/identifier/{identifier}";
    private static final String ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH = "/api/v1/entities/{template-identifier}";
    private static final String ENTITY_JSON_FILES_TEST_PATH = "integration_test/json/entity/v1/";


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

    /// Tests for GET /api/v1/entities/{template-identifier}?q= endpoint
    @Nested
    @DisplayName("GET /api/v1/entities/{template-identifier}?q= - Filter by q parameter")
    class GetEntitiesByTemplateIdentifierWithFilterTests {

        @ParameterizedTest
        @CsvSource({
            "identifier=web-api-1",
            "name:Web API 1",
            "property.programmingLanguage=JAVA",
            "relation=api-link",
            "relation.api-link.name:microservice",
            "relation=api-link;relation.api-link.name:microservice"
        })
        @DisplayName("Should filter entities by various criteria")
        @WithMockUser
        void getEntities_200_withFilter(String query) throws Exception {
            MvcResult mvcResult = mockMvc.perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
                            .param("q", query)
                            .accept(APPLICATION_JSON)
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andReturn();
            JSONAssert.assertEquals(
                    getJsonTestFileContent(ENTITY_JSON_FILES_TEST_PATH + "getEntities_200_identifierEquals.json"),
                    mvcResult.getResponse().getContentAsString(),
                    JSONCompareMode.STRICT);
        }

        @Test
        @DisplayName("Should return empty page when no entity matches filter")
        @WithMockUser
        void getEntities_200_noMatch() throws Exception {
            mockMvc.perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
                            .param("q", "name=nonexistent-entity")
                            .accept(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0));
        }

        @Test
        @DisplayName("Should filter microservices by relations_as_target identifier")
        @WithMockUser
        void getEntities_200_relationsAsTargetIdentifier() throws Exception {
            MvcResult mvcResult = mockMvc.perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, "microservice")
                            .param("q", "relations_as_target.api-link.identifier=web-api-1")
                            .accept(APPLICATION_JSON)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andReturn();
            JSONAssert.assertEquals(
                    getJsonTestFileContent(ENTITY_JSON_FILES_TEST_PATH + "getEntities_200_relationsAsTargetIdentifier.json"),
                    mvcResult.getResponse().getContentAsString(),
                    JSONCompareMode.STRICT);
        }

        @Test
        @DisplayName("Should filter microservices by relations_as_target name contains")
        @WithMockUser
        void getEntities_200_relationsAsTargetNameContains() throws Exception {
            MvcResult mvcResult = mockMvc.perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, "microservice")
                            .param("q", "relations_as_target.api-link.name:Web API")
                            .accept(APPLICATION_JSON)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andReturn();
            JSONAssert.assertEquals(
                    getJsonTestFileContent(ENTITY_JSON_FILES_TEST_PATH + "getEntities_200_relationsAsTargetIdentifier.json"),
                    mvcResult.getResponse().getContentAsString(),
                    JSONCompareMode.STRICT);
        }

        @Test
        @DisplayName("Should return 400 for malformed query without operator")
        @WithMockUser
        void getEntities_400_malformedQuery() throws Exception {
            mockMvc.perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
                            .param("q", "noOperator")
                            .accept(APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_description").value("Invalid query format, expected field:operator:value"));
        }

        @Test
        @DisplayName("Should return 400 for duplicate criterion on the same field")
        @WithMockUser
        void getEntities_400_duplicateCriterion() throws Exception {
            mockMvc.perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
                            .param("q", "name=A;name=B")
                            .accept(APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_description").value("Multiple filters for the same property are not supported"));
        }

        @Test
        @DisplayName("Should return 400 when criteria count exceeds maximum of 10")
        @WithMockUser
        void getEntities_400_tooManyCriteria() throws Exception {
            var query = "property.a=1;property.b=2;property.c=3;property.d=4;property.e=5;"
                    + "property.f=6;property.g=7;property.h=8;property.i=9;property.j=10;property.k=11";
            mockMvc.perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
                            .param("q", query)
                            .accept(APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_description").value("Filter query exceeds maximum of 10 criteria"));
        }

        @Test
        @DisplayName("Should return 400 for operator mismatch on criterion type")
        @WithMockUser
        void getEntities_400_typeMismatch() throws Exception {
            mockMvc.perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
                            .param("q", "relation<api-link")
                            .accept(APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_description").value("Operation '<' is not applicable for field 'relation'."));
        }

        @Test
        @DisplayName("Should return 400 for unsupported property in relations_as_target")
        @WithMockUser
        void getEntities_400_relationsAsTargetInvalidProperty() throws Exception {
            mockMvc.perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, "microservice")
                            .param("q", "relations_as_target.api-link.language=JAVA")
                            .accept(APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_description").value("Invalid property 'language' in criterion 'relations_as_target.api-link.language=JAVA': only 'identifier' and 'name' are supported for relations_as_target"));
        }

        @Test
        @DisplayName("Should return 400 for unsupported property in relation")
        @WithMockUser
        void getEntities_400_relationInvalidProperty() throws Exception {
            mockMvc.perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, "microservice")
                            .param("q", "relation.api-link.template=foo")
                            .accept(APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_description").value("Invalid property 'template' in criterion 'relation.api-link.template=foo': only 'identifier' and 'name' are supported for relation"));
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

    }

}
