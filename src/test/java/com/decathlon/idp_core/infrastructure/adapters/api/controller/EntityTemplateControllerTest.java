package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static com.decathlon.idp_core.domain.constant.ValidationsMessages.TEMPLATE_ALREADY_EXISTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.decathlon.idp_core.AbstractIntegrationTest;
import com.decathlon.idp_core.domain.constant.ValidationsMessages;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.entity_template.PropertyRules;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.domain.ports.EntityTemplateRepositoryPort;

import lombok.extern.slf4j.Slf4j;

@DisplayName("EntityTemplate Controller Integration Tests")
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@Slf4j
class EntityTemplateControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityTemplateRepositoryPort entityTemplateRepository;
    private static final String ENTITY_TEMPLATE_PATH = "/api/v1/entity-templates";

    /**
     * Test suite for the GET /api/v1/entity-templates endpoint, covering paginated
     * retrieval of entity templates.
     *
     * <p>
     * This nested class contains tests that verify:
     * </p>
     * <ul>
     * <li>Default pagination behavior and response structure</li>
     * <li>Authentication requirements for accessing the endpoint</li>
     * <li>Custom pagination and sorting parameters</li>
     * <li>Retrieval of a specific template by identifier</li>
     * <li>Filtering templates by identifier using query parameters</li>
     * </ul>
     *
     * <p>
     * Each test ensures the API returns the expected HTTP status codes, response
     * content, and pagination metadata.
     * </p>
     */
    @Nested
    @DisplayName("GET /api/v1/entity-templates - Get Templates Paginated")
    @Order(1)
    class GetTemplatesPaginatedTests {

        /**
         * Tests the GET /api/v1/entity-templates/ endpoint with default pagination
         * parameters.
         *
         * <p>
         * This test verifies that:
         * </p>
         * <ul>
         * <li>The endpoint returns HTTP 200 OK status</li>
         * <li>Response content type is application/json</li>
         * <li>All 10 templates are returned in the content array</li>
         * <li>Default pagination settings are applied (page 0, size 20)</li>
         * <li>Template ordering is consistent (batch-job at index 1)</li>
         * <li>Pagination metadata is correctly populated</li>
         * </ul>
         *
         * @throws Exception if the MockMvc request fails
         */
        @Test
        @DisplayName("Should return paginated templates with default pagination")
        @WithMockUser
        void getTemplates_paginated_200() throws Exception {

            mockMvc.perform(get("/api/v1/entity-templates")
                    .accept(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(10))
                    .andExpect(jsonPath("$.content[1].identifier").value("batch-job"))
                    .andExpect(jsonPath("$.page.total_elements").value(10))
                    .andExpect(jsonPath("$.page.total_pages").value(1))
                    .andExpect(jsonPath("$.page.size").value(20))
                    .andExpect(jsonPath("$.page.number").value(0));
        }

        /**
         * Tests that accessing the /api/v1/entity-templates/ endpoint without
         * authentication
         * returns a 401 Unauthorized status.
         *
         * @throws Exception if an error occurs during the request
         */
        @Test
        @DisplayName("Should return 401 without authentication")
        void getTemplates_paginated_401_without_user_token() throws Exception {
            mockMvc.perform(get("/api/v1/entity-templates/")
                    .accept(APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        /**
         * Tests the GET /api/v1/entity-templates/ endpoint with custom pagination
         * parameters.
         *
         * <p>
         * This test verifies that:
         * </p>
         * <ul>
         * <li>Custom pagination parameters are correctly applied (page=1, size=5,
         * sort=identifier,asc)</li>
         * <li>Only 5 templates are returned on the second page</li>
         * <li>Templates are sorted alphabetically by identifier</li>
         * <li>Pagination metadata reflects the custom settings</li>
         * <li>First template in the sorted result is "frontend-app"</li>
         * </ul>
         *
         * @throws Exception if the MockMvc request fails
         */
        @Test
        @DisplayName("Should return paginated templates with custom pagination")
        @WithMockUser
        void getTemplates_paginated_200_custom() throws Exception {

            mockMvc.perform(get("/api/v1/entity-templates")
                    .param("page", "1")
                    .param("size", "5")
                    .param("sort", "identifier,asc")
                    .accept(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.content.length()").value(5))
                    .andExpect(jsonPath("$.content[0].identifier").value("frontend-app"))
                    .andExpect(jsonPath("$.page.total_elements").value(10))
                    .andExpect(jsonPath("$.page.total_pages").value(2))
                    .andExpect(jsonPath("$.page.size").value(5))
                    .andExpect(jsonPath("$.page.number").value(1));
        }

        /**
         * Tests the GET /api/v1/entity-templates/identifier/{identifier} endpoint for
         * retrieving a specific template.
         *
         * <p>
         * This test verifies that:
         * </p>
         * <ul>
         * <li>The endpoint returns HTTP 200 OK status for valid identifier</li>
         * <li>The response contains the specific template data for "frontend-app"</li>
         * <li>The identifier field in response matches the requested identifier</li>
         * </ul>
         *
         * @throws Exception if the MockMvc request fails
         */
        @Test
        @DisplayName("Should return 200 even with invalid pagination parameters")
        @WithMockUser
        void getTemplates_paginated_200_invalid_pagination() throws Exception {

            mockMvc.perform(get("/api/v1/entity-templates/identifier/frontend-app")
                    .accept(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.identifier").value("frontend-app"));
        }

        /**
         * Tests the GET /api/v1/entity-templates/ endpoint with identifier query
         * parameter.
         *
         * <p>
         * This test verifies that:
         * </p>
         * <ul>
         * <li>The endpoint returns HTTP 200 OK status when identifier parameter is
         * provided</li>
         * <li>The API can handle identifier-based filtering via query parameter</li>
         * <li>Valid identifier "web-service" is processed successfully</li>
         * </ul>
         *
         * @throws Exception if the MockMvc request fails
         */
        @Test
        @DisplayName("Should return 200 with valid identifier")
        @WithMockUser
        void getTemplates_paginated_200_with_valid_identifier() throws Exception {
            mockMvc.perform(get("/api/v1/entity-templates")
                    .param("identifier", "web-service")
                    .accept(APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

    }

    @Nested
    @DisplayName("POST /api/v1/entity-templates - Create Template")
    @Order(2)
    class PostTemplateTests {

        private static final String ENTITY_TEMPLATE_JSON_TEST_PATH = "integration_test/json/entity-template/v1/";

        /**
         * Tests the POST /api/v1/entity-templates endpoint for successful template
         * creation.
         *
         * <p>
         * This test verifies that:
         * </p>
         * <ul>
         * <li>Valid template data results in HTTP 201 Created status</li>
         * <li>The request includes proper CSRF token for security</li>
         * <li>Content type is properly set to application/json</li>
         * <li>Template is created using valid JSON from test file</li>
         * </ul>
         *
         * @throws Exception if the MockMvc request fails
         */
        @Test
        @WithMockUser()
        @DisplayName("Should create template and return 201")
        void postTemplate_201() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(ENTITY_TEMPLATE_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(ENTITY_TEMPLATE_JSON_TEST_PATH + "postEntityTemplate_201.json")))
                    .andExpect(status().isCreated())
                    .andReturn();
        }

        /**
         * Tests the POST /api/v1/entity-templates endpoint without authentication.
         *
         * <p>
         * This test verifies that:
         * </p>
         * <ul>
         * <li>Unauthenticated requests result in HTTP 401 Unauthorized status</li>
         * <li>CSRF token is included but authentication is missing</li>
         * <li>Valid template data does not bypass authentication requirements</li>
         * </ul>
         *
         * @throws Exception if the MockMvc request fails
         */
        @Test
        @DisplayName("Should create template and return 401")
        void postTemplate_401_without_user_token() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(ENTITY_TEMPLATE_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(ENTITY_TEMPLATE_JSON_TEST_PATH + "postEntityTemplate_201.json")))
                    .andExpect(status().isUnauthorized())
                    .andReturn();
        }

        /**
         * Tests the POST /api/v1/entity-templates endpoint when the identifier field is
         * missing.
         *
         * <p>
         * This test verifies that:
         * </p>
         * <ul>
         * <li>Missing identifier results in HTTP 400 Bad Request</li>
         * <li>Validation error message matches expected template identifier mandatory
         * message</li>
         * <li>Request is properly rejected with appropriate error response</li>
         * </ul>
         *
         * @throws Exception if the MockMvc request fails
         */
        @Test
        @WithMockUser()
        @DisplayName("Returns 400 when identifier is missing")
        void postTemplate_400_identifier_missing() throws Exception {
            MvcResult res = postAndValidateBadRequest(ENTITY_TEMPLATE_PATH,
                    ENTITY_TEMPLATE_JSON_TEST_PATH + "postEntityTemplate_400_identifier_missing.json",
                    ValidationsMessages.TEMPLATE_IDENTIFIER_MANDATORY);
            assertNotNull(res, "Test executed successfully");
        }

        /**
         * Tests the POST /api/v1/entity-templates endpoint when identifier field is
         * blank.
         *
         * <p>
         * This test verifies that:
         * </p>
         * <ul>
         * <li>Blank identifier results in HTTP 400 Bad Request</li>
         * <li>Validation error message matches expected template identifier mandatory
         * message</li>
         * <li>Request is properly rejected with appropriate error response</li>
         * </ul>
         *
         * @throws Exception if the MockMvc request fails
         */
        @Test
        @WithMockUser()
        @DisplayName("Returns 400 when identifier is blank")
        void postTemplate_400_identifier_blank() throws Exception {
            MvcResult res = postAndValidateBadRequest(ENTITY_TEMPLATE_PATH,
                    ENTITY_TEMPLATE_JSON_TEST_PATH + "postEntityTemplate_400_identifier_blank.json",
                    ValidationsMessages.TEMPLATE_IDENTIFIER_MANDATORY);
            assertNotNull(res, "Test executed successfully");
        }

        /**
         * Tests the POST /api/v1/entity-templates endpoint when properties array is
         * empty.
         *
         * <p>
         * This test verifies that:
         * </p>
         * <ul>
         * <li>Empty properties array results in HTTP 400 Bad Request</li>
         * <li>Validation error message indicates property definitions are
         * mandatory</li>
         * <li>At least one property definition is required for template creation</li>
         * </ul>
         *
         * @throws Exception if the MockMvc request fails
         */
        @Test
        @WithMockUser()
        @DisplayName("Returns 400 when properties array is empty")
        void postTemplate_400_properties_empty() throws Exception {
            MvcResult res = postAndValidateBadRequest(ENTITY_TEMPLATE_PATH,
                    ENTITY_TEMPLATE_JSON_TEST_PATH + "postEntityTemplate_400_properties_empty.json",
                    ValidationsMessages.PROPERTY_DEFINITIONS_MANDATORY);
            assertNotNull(res, "Test executed successfully");
        }

        /**
         * Tests the POST /api/v1/entity-templates endpoint when property name field is
         * missing.
         *
         * <p>
         * This test verifies that:
         * </p>
         * <ul>
         * <li>Missing property name results in HTTP 400 Bad Request</li>
         * <li>Validation error message indicates property name is mandatory</li>
         * <li>All property definitions must include a name field</li>
         * </ul>
         *
         * @throws Exception if the MockMvc request fails
         */
        @Test
        @WithMockUser()
        @DisplayName("Returns 400 when property name is missing")
        void postTemplate_400_property_name_missing() throws Exception {
            MvcResult res = postAndValidateBadRequest(ENTITY_TEMPLATE_PATH,
                    ENTITY_TEMPLATE_JSON_TEST_PATH + "postEntityTemplate_400_property_name_missing.json",
                    ValidationsMessages.PROPERTY_NAME_MANDATORY);
            assertNotNull(res, "Test executed successfully");
        }

        /**
         * Tests the POST /api/v1/entity-templates endpoint when property name field is
         * blank.
         *
         * <p>
         * This test verifies that:
         * </p>
         * <ul>
         * <li>Blank property name results in HTTP 400 Bad Request</li>
         * <li>Validation error message indicates property name is mandatory</li>
         * <li>Property names cannot be empty or whitespace-only</li>
         * </ul>
         *
         * @throws Exception if the MockMvc request fails
         */
        @Test
        @WithMockUser()
        @DisplayName("Returns 400 when property name is blank")
        void postTemplate_400_property_name_blank() throws Exception {
            MvcResult res = postAndValidateBadRequest(ENTITY_TEMPLATE_PATH,
                    ENTITY_TEMPLATE_JSON_TEST_PATH + "postEntityTemplate_400_property_name_blank.json",
                    ValidationsMessages.PROPERTY_NAME_MANDATORY);
            assertNotNull(res, "Test executed successfully");
        }

        /**
         * Tests the POST /api/v1/entity-templates endpoint when property description
         * field is missing.
         *
         * <p>
         * This test verifies that:
         * </p>
         * <ul>
         * <li>Missing property description results in HTTP 400 Bad Request</li>
         * <li>Validation error message indicates property description is mandatory</li>
         * <li>All property definitions must include a description field</li>
         * </ul>
         *
         * @throws Exception if the MockMvc request fails
         */
        @Test
        @WithMockUser()
        @DisplayName("Returns 400 when property description is missing")
        void postTemplate_400_property_description_missing() throws Exception {
            MvcResult res = postAndValidateBadRequest(ENTITY_TEMPLATE_PATH,
                    ENTITY_TEMPLATE_JSON_TEST_PATH + "postEntityTemplate_400_property_description_missing.json",
                    ValidationsMessages.PROPERTY_DESCRIPTION_MANDATORY);
            assertNotNull(res, "Test executed successfully");
        }

        /**
         * Tests the POST /api/v1/entity-templates endpoint when property description
         * field is blank.
         *
         * <p>
         * This test verifies that:
         * </p>
         * <ul>
         * <li>Blank property description results in HTTP 400 Bad Request</li>
         * <li>Validation error message indicates property description is mandatory</li>
         * <li>Property descriptions cannot be empty or whitespace-only</li>
         * </ul>
         *
         * @throws Exception if the MockMvc request fails
         */
        @Test
        @WithMockUser()
        @DisplayName("Returns 400 when property description is blank")
        void postTemplate_400_property_description_blank() throws Exception {
            MvcResult res = postAndValidateBadRequest(ENTITY_TEMPLATE_PATH,
                    ENTITY_TEMPLATE_JSON_TEST_PATH + "postEntityTemplate_400_property_description_blank.json",
                    ValidationsMessages.PROPERTY_DESCRIPTION_MANDATORY);
            assertNotNull(res, "Test executed successfully");
        }

        /**
         * Tests the POST /api/v1/entity-templates endpoint when property type field is
         * missing.
         *
         * <p>
         * This test verifies that:
         * </p>
         * <ul>
         * <li>Missing property type results in HTTP 400 Bad Request</li>
         * <li>Validation error message indicates property type is mandatory</li>
         * <li>All property definitions must include a type field</li>
         * </ul>
         *
         * @throws Exception if the MockMvc request fails
         */
        @Test
        @WithMockUser()
        @DisplayName("Returns 400 when property type is missing")
        void postTemplate_400_property_type_missing() throws Exception {
            MvcResult res = postAndValidateBadRequest(ENTITY_TEMPLATE_PATH,
                    ENTITY_TEMPLATE_JSON_TEST_PATH + "postEntityTemplate_400_property_type_missing.json",
                    ValidationsMessages.PROPERTY_TYPE_MANDATORY);
            assertNotNull(res, "Test executed successfully");
        }

        /**
         * Tests the POST /api/v1/entity-templates endpoint when trying to create a
         * template with duplicate identifier.
         *
         * <p>
         * This test verifies that:
         * </p>
         * <ul>
         * <li>Duplicate identifier results in HTTP 409 Conflict status</li>
         * <li>Error response indicates that template already exists</li>
         * <li>Business rule preventing duplicate identifiers is enforced</li>
         * <li>Specific error message includes the conflicting identifier</li>
         * </ul>
         *
         * @throws Exception if the MockMvc request fails
         */
        @Test
        @WithMockUser()
        @DisplayName("Returns 409 when identifier already exists")
        void postTemplate_409_identifier_already_exists() throws Exception {

            // Then, try to create template with the existing identifier in database
            mockMvc.perform(MockMvcRequestBuilders.post(ENTITY_TEMPLATE_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(
                            ENTITY_TEMPLATE_JSON_TEST_PATH + "postEntityTemplate_409_identifier_already_exists.json")))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.error_description").exists())
                    .andExpect(jsonPath("$.error_description").value(TEMPLATE_ALREADY_EXISTS + ":web-service"));
        }

        /**
         * Tests the POST /api/v1/entity-templates endpoint when property type contains
         * an invalid enum value.
         *
         * <p>
         * This test verifies that:
         * </p>
         * <ul>
         * <li>Invalid property type enum value results in HTTP 400 Bad Request</li>
         * <li>JSON deserialization error occurs for unsupported enum values</li>
         * <li>Error response indicates the invalid enum value cannot be processed</li>
         * <li>Proper validation of PropertyType enum constraints</li>
         * </ul>
         *
         * @throws Exception if the MockMvc request fails
         */
        @Test
        @WithMockUser()
        @DisplayName("Returns 400 when property type has invalid enum value")
        void postTemplate_400_property_type_invalid_enum() throws Exception {
            MvcResult res = postAndValidateBadRequest(ENTITY_TEMPLATE_PATH,
                    ENTITY_TEMPLATE_JSON_TEST_PATH + "postEntityTemplate_400_bad_property_type.json",
                    "Invalid value 'NOT IN ENUM' for property 'type'");
            assertNotNull(res, "Test executed successfully");
        }

        /**
         * Tests the POST /api/v1/entity-templates endpoint when property format
         * contains
         * an invalid enum value.
         *
         * <p>
         * This test verifies that:
         * </p>
         * <ul>
         * <li>Invalid property format enum value results in HTTP 400 Bad Request</li>
         * <li>JSON deserialization error occurs for unsupported enum values</li>
         * <li>Error response indicates the invalid enum value cannot be processed</li>
         * <li>Proper validation of PropertyFormat enum constraints</li>
         * </ul>
         *
         * @throws Exception if the MockMvc request fails
         */
        @Test
        @WithMockUser()
        @DisplayName("Returns 400 when property format has invalid enum value")
        void postTemplate_400_property_format_invalid_enum() throws Exception {
            MvcResult res = postAndValidateBadRequest(ENTITY_TEMPLATE_PATH,
                    ENTITY_TEMPLATE_JSON_TEST_PATH + "postEntityTemplate_400_bad_property_format.json",
                    "Invalid value 'NOT A VALID FORMAT' for property 'format'");
            assertNotNull(res, "Test executed successfully");
        }

    }

    @Nested
    @DisplayName("PUT /api/v1/entity-templates - Update Template")
    @Order(3)
    class PutTemplateTests {

        @Test
        void putTemplate_without_user_token_401() throws Exception {
            String identifier = "web-service";
            mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/entity-templates/identifier/" + identifier)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(
                            "integration_test/json/entity-template/v1/putEntityTemplate_200.json")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser
        @DisplayName("Should update existing property rules using PUT")
        void putTemplate_shouldMergePropertyRules() throws Exception {

            mockMvc.perform(MockMvcRequestBuilders.post(ENTITY_TEMPLATE_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(
                            PostTemplateTests.ENTITY_TEMPLATE_JSON_TEST_PATH
                                    + "postEntityTemplateWithoutRelationsDefinitions_201.json")))
                    .andExpect(status().isCreated());

            EntityTemplate initialTemplate = entityTemplateRepository
                    .findByIdentifier("temp-test-99")
                    .orElseThrow();

            PropertyDefinition initialProperty = initialTemplate.propertiesDefinitions().get(0);
            UUID initialRulesId = initialProperty.rules().id();

            mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/entity-templates/identifier/temp-test-99")
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(
                            PostTemplateTests.ENTITY_TEMPLATE_JSON_TEST_PATH
                                    + "putEntityTemplate_updateRules_200.json")))
                    .andExpect(status().isOk());

            EntityTemplate updatedTemplate = entityTemplateRepository
                    .findByIdentifier("temp-test-99")
                    .orElseThrow();

            assertThat(updatedTemplate.propertiesDefinitions()).hasSize(1);

            PropertyDefinition updatedProperty = updatedTemplate.propertiesDefinitions().get(0);

            assertThat(updatedProperty.name()).isEqualTo("property-test");

            PropertyRules updatedRules = updatedProperty.rules();
            assertThat(updatedRules.format()).isNull();
            assertThat(updatedRules.regex()).isEqualTo("^[a-zA-Z0-9]+$");
            assertThat(updatedRules.maxLength()).isEqualTo(255);
            assertThat(updatedRules.minLength()).isNull();

            assertThat(updatedRules.id()).isEqualTo(initialRulesId);

            assertThat(updatedTemplate.relationsDefinitions()).isEmpty();
        }

        @Test
        @WithMockUser
        @DisplayName("Should update template with relations and return 200")
        void putTemplate_updateRelations_200() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(ENTITY_TEMPLATE_PATH)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content("""
                            {
                              "identifier": "template-rel-test",
                              "description": "Initial template",
                              "properties_definitions": [
                                {
                                  "name": "property1",
                                  "description": "description",
                                  "required": true,
                                  "type": "STRING",
                                  "rules": {}
                                }
                              ],
                              "relations_definitions": [
                                {
                                  "name": "owns",
                                  "target_entity_identifier": "child-entity",
                                  "required": true,
                                  "to_many": true
                                }
                              ]
                            }
                            """))
                    .andExpect(status().isCreated());

            String updateJson = """
                    {
                      "identifier": "template-rel-test",
                      "description": "Updated template with new relation",
                      "properties_definitions": [
                        {
                          "name": "property1",
                          "description": "Updated description",
                          "type": "STRING",
                          "required": true,
                          "rules": {}
                        }
                      ],
                      "relations_definitions": [
                        {
                          "name": "owns",
                          "target_entity_identifier": "child-entity-updated",
                          "required": false,
                          "to_many": false
                        },
                        {
                          "name": "belongsTo",
                          "target_entity_identifier": "parent-entity",
                          "required": true,
                          "to_many": false
                        }
                      ]
                    }
                    """;

            mockMvc.perform(MockMvcRequestBuilders.put(ENTITY_TEMPLATE_PATH + "/identifier/template-rel-test")
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(updateJson))
                    .andExpect(status().isOk());

            Optional<EntityTemplate> updatedTemplateOpt = entityTemplateRepository
                    .findByIdentifier("template-rel-test");
            assertThat(updatedTemplateOpt).isPresent();

            EntityTemplate updatedTemplate = updatedTemplateOpt.get();

            // Vérifier description mise à jour
            assertThat(updatedTemplate.description()).isEqualTo("Updated template with new relation");

            // Vérifier properties
            assertThat(updatedTemplate.propertiesDefinitions()).hasSize(1);
            assertThat(updatedTemplate.propertiesDefinitions().get(0).description())
                    .isEqualTo("Updated description");

            // Vérifier relations
            assertThat(updatedTemplate.relationsDefinitions()).hasSize(2);

            Map<String, RelationDefinition> relationsMap = updatedTemplate.relationsDefinitions()
                    .stream()
                    .collect(Collectors.toMap(RelationDefinition::name, r -> r));

            assertThat(relationsMap.get("owns").targetEntityIdentifier()).isEqualTo("child-entity-updated");
            assertThat(relationsMap.get("owns").required()).isFalse();
            assertThat(relationsMap.get("owns").toMany()).isFalse();

            assertThat(relationsMap.get("belongsTo").targetEntityIdentifier()).isEqualTo("parent-entity");
            assertThat(relationsMap.get("belongsTo").required()).isTrue();
            assertThat(relationsMap.get("belongsTo").toMany()).isFalse();
        }

        @Test
        @WithMockUser()
        @DisplayName("Should update template and return 201")
        void putTemplate_200() throws Exception {
            String identifier = "web-service";
            mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/entity-templates/identifier/" + identifier)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(
                            "integration_test/json/entity-template/v1/putEntityTemplate_200.json")))
                    .andExpect(status().isOk());

            Optional<EntityTemplate> entityTemplateUpdated = entityTemplateRepository.findByIdentifier("web-service");
            assertThat(entityTemplateUpdated).isPresent();
            assertThat(entityTemplateUpdated.get().propertiesDefinitions()).hasSize(2);
            assertThat(entityTemplateUpdated.get().relationsDefinitions()).isEmpty();
        }

        @Test
        @WithMockUser
        void putTemplate_withUnknownIdentifier_404() throws Exception {
            String identifier = "unknown-identifier";
            mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/entity-templates/identifier/" + identifier)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(
                            "integration_test/json/entity-template/v1/putEntityTemplate_200.json")))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string(
                            "{\"error\":\"NOT_FOUND\",\"error_description\":\"Template not found with identifier: unknown-identifier\"}"));
        }

        @Test
        @WithMockUser()
        void putTemplate_400_properties_empty() throws Exception {
            String identifier = "unknown-identifier";
            mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/entity-templates/identifier/" + identifier)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(
                            "integration_test/json/entity-template/v1/putEntityTemplate_400_withoutPropertiesDefinitions.json")))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(
                            "{\"error\":\"BAD_REQUEST\",\"error_description\":\"Entity Template property definitions are mandatory and cannot be empty\"}"));
        }

        @Test
        @WithMockUser()
        void putTemplate_400_propertyNameIsMissing() throws Exception {
            String identifier = "web-service";
            mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/entity-templates/identifier/" + identifier)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(
                            "integration_test/json/entity-template/v1/putEntityTemplate_400_propertyNameIsMissing.json")))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(
                            "{\"error\":\"BAD_REQUEST\",\"error_description\":\"Property name is mandatory and cannot be blank\"}"));
        }

        @Test
        @WithMockUser()
        void putTemplate_400_propertyNameIsBlank() throws Exception {
            String identifier = "web-service";
            mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/entity-templates/identifier/" + identifier)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(
                            "integration_test/json/entity-template/v1/putEntityTemplate_400_propertyNameIsBlank.json")))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(
                            "{\"error\":\"BAD_REQUEST\",\"error_description\":\"Property name is mandatory and cannot be blank\"}"));
        }

        @Test
        @WithMockUser()
        void putTemplate_400_propertyDescriptionIsBlank() throws Exception {
            String identifier = "web-service";
            mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/entity-templates/identifier/" + identifier)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(
                            "integration_test/json/entity-template/v1/putEntityTemplate_400_propertyDescriptionIsBlank.json")))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(
                            "{\"error\":\"BAD_REQUEST\",\"error_description\":\"Property description is mandatory and cannot be blank\"}"));
        }

        @Test
        @WithMockUser()
        void putTemplate_400_propertyDescriptionIsMissing() throws Exception {
            String identifier = "web-service";
            mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/entity-templates/identifier/" + identifier)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(
                            "integration_test/json/entity-template/v1/putEntityTemplate_400_propertyDescriptionIsMissing.json")))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(
                            "{\"error\":\"BAD_REQUEST\",\"error_description\":\"Property description is mandatory and cannot be blank\"}"));
        }

        @Test
        @WithMockUser()
        void putTemplate_400_propertyTypeIsMissing() throws Exception {
            String identifier = "web-service";
            mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/entity-templates/identifier/" + identifier)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(
                            "integration_test/json/entity-template/v1/putEntityTemplate_400_propertyTypeIsMissing.json")))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(
                            "{\"error\":\"BAD_REQUEST\",\"error_description\":\"Property type is mandatory\"}"));
        }

        @Test
        @WithMockUser()
        void putTemplate_409_wheneIdentifierAlreadyExists() throws Exception {
            String identifier = "web-service";
            Optional<EntityTemplate> entityTemplateUpdated = entityTemplateRepository.findByIdentifier("microservice");
            assertThat(entityTemplateUpdated).isPresent();
            mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/entity-templates/identifier/" + identifier)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .with(csrf())
                    .content(getJsonTestFileContent(
                            "integration_test/json/entity-template/v1/putEntityTemplate_409_withIdentifierAlreadyExists.json")))
                    .andExpect(status().isConflict())
                    .andExpect(content().string(
                            "{\"error\":\"CONFLICT\",\"error_description\":\"An Entity Template already exists with the same identifier:microservice\"}"));
        }

    }

    @Nested
    @DisplayName("DELETE /api/v1/entity-templates/{id} - Delete Template")
    @Order(4)
    class DeleteTemplateTests {

        private static final String ENTITY_TEMPLATE_PATH = "/api/v1/entity-templates";

        /**
         * Tests the DELETE /api/v1/entity-templates/{id} endpoint for successful
         * template deletion.
         *
         * <p>
         * This test verifies that:
         * </p>
         * <ul>
         * <li>Existing template can be successfully deleted</li>
         * <li>HTTP 204 No Content status is returned for successful deletion</li>
         * <li>CSRF token is properly included for security</li>
         * <li>Uses valid template ID "monitoring-service" from test data</li>
         * </ul>
         *
         * @throws Exception if the MockMvc request fails
         */
        @Test
        @WithMockUser()
        @DisplayName("Should delete template and return 204")
        void deleteTemplate_204() throws Exception {
            // Use an existing template ID from test data
            String templateId = "monitoring-service";

            mockMvc.perform(MockMvcRequestBuilders.delete(ENTITY_TEMPLATE_PATH + "/identifier/" + templateId)
                    .accept(APPLICATION_JSON)
                    .with(csrf()))
                    .andExpect(status().isNoContent());

            assertNotNull(templateId, "Test executed successfully");
        }

        /**
         * Tests the DELETE /api/v1/entity-templates/{id} endpoint when template does
         * not
         * exist.
         *
         * <p>
         * This test verifies that:
         * </p>
         * <ul>
         * <li>Non-existent template ID results in HTTP 404 Not Found status</li>
         * <li>Error response contains proper error structure with NOT_FOUND status</li>
         * <li>Error description is included in the response</li>
         * <li>CSRF token is properly included for security</li>
         * </ul>
         *
         * @throws Exception if the MockMvc request fails
         */
        @Test
        @WithMockUser()
        @DisplayName("Should return 404 when template not found")
        void deleteTemplate_404_not_found() throws Exception {
            // Use a non-existent template ID
            String nonExistentId = "non-existing-identifier";

            mockMvc.perform(MockMvcRequestBuilders.delete(ENTITY_TEMPLATE_PATH + "/identifier/" + nonExistentId)
                    .accept(APPLICATION_JSON)
                    .with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.error_description").exists());

            assertNotNull(nonExistentId, "Test executed successfully");
        }

        /**
         * Tests the DELETE /api/v1/entity-templates/{id} endpoint when authentication is missing.
         *
         * <p>
         * This test verifies that:
         * </p>
         * <ul>
         * <li>Missing authentication results in HTTP 401 Unauthorized status</li>
         * <li>CSRF token is included but authentication is required</li>
         * <li>Security measures prevent unauthorized deletion attempts</li>
         * </ul>
         *
         * @throws Exception if the MockMvc request fails
         */
        @Test
        @DisplayName("Should return 401 when deleting without user token")
        void deleteTemplate_401_without_user_token() throws Exception {
            String templateId = "123e4567-e89b-12d3-a456-426614174001";
            mockMvc.perform(MockMvcRequestBuilders.delete(ENTITY_TEMPLATE_PATH + "/" + templateId)
                    .accept(APPLICATION_JSON)
                    .with(csrf()))
                    .andExpect(status().isUnauthorized());

        }
    }

}
