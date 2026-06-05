package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.decathlon.idp_core.AbstractIntegrationTest;
import com.decathlon.idp_core.domain.constant.SearchConstraints;

/// Integration tests for the EntityController REST API endpoints. These tests
/// verify the behavior of entity retrieval endpoints, including pagination,
/// authentication, and lookup by template identifier and entity identifier.
public class EntityControllerTest extends AbstractIntegrationTest {

  private static final String TEMPLATE_IDENTIFIER = "web-service";
  private static final String ENTITY_IDENTIFIER = "web-api-2";
  private static final String ENTITIES_BY_IDENTIFIER_PATH = "/api/v1/entities/{template-identifier}/{identifier}";
  private static final String ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH = "/api/v1/entities/{template-identifier}";
  private static final String ENTITY_JSON_FILES_TEST_PATH = "integration_test/json/entity/v1/";
  @Autowired
  private MockMvc mockMvc;

  /// Tests for GET /api/v1/entities/{template-identifier} endpoint
  /// (paginated retrieval).
  @Nested
  @DisplayName("GET /api/v1/entities/{template-identifier} - Get Templates Paginated")
  class GetEntitiesByTemplateIdentifierTests {

    @Test
    @DisplayName("Should return paginated entities with default pagination")
    @WithMockUser
    void getEntities_paginated_200() throws Exception {
      mockMvc
          .perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER).param("page", "0")
              .param("size", "15").accept(APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(content().contentType(APPLICATION_JSON))
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content.length()").value(2))
          .andExpect(jsonPath("$.page.total_elements").value(2))
          .andExpect(jsonPath("$.page.total_pages").value(1))
          .andExpect(jsonPath("$.page.size").value(15))
          .andExpect(jsonPath("$.page.number").value(0))
          .andExpect(jsonPath("$.content[0].template_identifier").value(TEMPLATE_IDENTIFIER));
    }

    @Test
    @DisplayName("Should return 404 when template does not exist")
    @WithMockUser
    void getEntities_paginated_404_when_non_existent_template() throws Exception {
      mockMvc.perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, "non-existent-template-identifier")
          .accept(APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 for non-existent template even when q is provided")
    @WithMockUser
    void getEntities_404_nonExistentTemplate_withFilter() throws Exception {
      mockMvc.perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, "non-existent-template-identifier")
          .param("q", "name=foo").accept(APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 401 without authentication")
    void getTemplates_paginated_401_without_user_token() throws Exception {
      mockMvc.perform(
          get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER).accept(APPLICATION_JSON))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return paginated entities with custom pagination")
    @WithMockUser
    void getEntities_paginated_200_custom() throws Exception {

      mockMvc
          .perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, "monitoring-service")
              .param("page", "1").param("size", "5").param("sort", "template_identifier,asc")
              .accept(APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(content().contentType(APPLICATION_JSON))
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
      mockMvc
          .perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
              .accept(APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(content().contentType(APPLICATION_JSON))
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
    @CsvSource({"identifier=web-api-1", "name:Web API 1", "property.programmingLanguage=JAVA",
        "relation=api-link", "relation.api-link.name:microservice",
        "relation=api-link;relation.api-link.name:microservice"})
    @DisplayName("Should filter entities by various criteria")
    @WithMockUser
    void getEntities_200_withFilter(String query) throws Exception {
      MvcResult mvcResult = mockMvc
          .perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER).param("q", query)
              .accept(APPLICATION_JSON).with(csrf()))
          .andExpect(status().isOk()).andReturn();
      JSONAssert.assertEquals(
          getJsonTestFileContent(
              ENTITY_JSON_FILES_TEST_PATH + "getEntities_200_identifierEquals.json"),
          mvcResult.getResponse().getContentAsString(), JSONCompareMode.STRICT);
    }

    @Test
    @DisplayName("Should return empty page when no entity matches filter")
    @WithMockUser
    void getEntities_200_noMatch() throws Exception {
      mockMvc
          .perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
              .param("q", "name=nonexistent-entity").accept(APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @DisplayName("Should filter microservices by relations_as_target identifier")
    @WithMockUser
    void getEntities_200_relationsAsTargetIdentifier() throws Exception {
      MvcResult mvcResult = mockMvc
          .perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, "microservice")
              .param("q", "relations_as_target.api-link.identifier=web-api-1")
              .accept(APPLICATION_JSON).with(csrf()))
          .andExpect(status().isOk()).andReturn();
      JSONAssert.assertEquals(
          getJsonTestFileContent(
              ENTITY_JSON_FILES_TEST_PATH + "getEntities_200_relationsAsTargetIdentifier.json"),
          mvcResult.getResponse().getContentAsString(), JSONCompareMode.STRICT);
    }

    @Test
    @DisplayName("Should filter microservices by relations_as_target name contains")
    @WithMockUser
    void getEntities_200_relationsAsTargetNameContains() throws Exception {
      MvcResult mvcResult = mockMvc
          .perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, "microservice")
              .param("q", "relations_as_target.api-link.name:Web API").accept(APPLICATION_JSON)
              .with(csrf()))
          .andExpect(status().isOk()).andReturn();
      JSONAssert.assertEquals(
          getJsonTestFileContent(
              ENTITY_JSON_FILES_TEST_PATH + "getEntities_200_relationsAsTargetIdentifier.json"),
          mvcResult.getResponse().getContentAsString(), JSONCompareMode.STRICT);
    }

    @Test
    @DisplayName("Should return 400 for malformed query without operator")
    @WithMockUser
    void getEntities_400_malformedQuery() throws Exception {
      mockMvc
          .perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
              .param("q", "noOperator").accept(APPLICATION_JSON))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error_description")
              .value("Invalid query format, expected field:operator:value"));
    }

    @Test
    @DisplayName("Should return 400 for duplicate criterion on the same field")
    @WithMockUser
    void getEntities_400_duplicateCriterion() throws Exception {
      mockMvc
          .perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
              .param("q", "name=A;name=B").accept(APPLICATION_JSON))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error_description")
              .value("Multiple filters for the same property are not supported"));
    }

    @Test
    @DisplayName("Should return 400 when criteria count exceeds maximum of 10")
    @WithMockUser
    void getEntities_400_tooManyCriteria() throws Exception {
      var query = "property.a=1;property.b=2;property.c=3;property.d=4;property.e=5;"
          + "property.f=6;property.g=7;property.h=8;property.i=9;property.j=10;property.k=11";
      mockMvc
          .perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER).param("q", query)
              .accept(APPLICATION_JSON))
          .andExpect(status().isBadRequest()).andExpect(
              jsonPath("$.error_description").value("Filter query exceeds maximum of 10 criteria"));
    }

    @ParameterizedTest(name = "comparison filter ''{0}'' returns 400")
    @CsvSource({"name<Web API 2", "name>Web API 1"})
    @DisplayName("Should return 400 when < or > is used on attribute fields")
    @WithMockUser
    void getEntities_400_comparisonOnAttribute(String query) throws Exception {
      mockMvc
          .perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
              .param("q", query.trim()).accept(APPLICATION_JSON))
          .andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "comparison filter ''{0}'' returns 400")
    @CsvSource({"property.programmingLanguage<PYTHON", "property.programmingLanguage>JAVA"})
    @DisplayName("Should return 400 when < or > is used on a STRING property")
    @WithMockUser
    void getEntities_400_comparisonOnStringProperty(String query) throws Exception {
      mockMvc
          .perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
              .param("q", query.trim()).accept(APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error_description").value(
              "Operation '%s' is not applicable for property 'programmingLanguage': only NUMBER properties support comparison operators."
                  .formatted(query.trim().contains("<") ? "<" : ">")));
    }

    @ParameterizedTest(name = "comparison filter ''{0}'' returns {1} result(s)")
    @CsvSource({"property.port<9090, 1", "property.port>8080, 1"})
    @DisplayName("Should filter entities using < and > comparison operators on a NUMBER property")
    @WithMockUser
    void getEntities_200_comparisonOnNumberProperty(String query, int expectedCount)
        throws Exception {
      mockMvc
          .perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
              .param("q", query.trim()).accept(APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content.length()").value(expectedCount));
    }

    @ParameterizedTest(name = "blank q ''{0}'' behaves like no filter")
    @ValueSource(strings = {"", "   "})
    @DisplayName("Should return all entities when q is empty or blank")
    @WithMockUser
    void getEntities_200_emptyOrBlankQ_returnsAllEntities(String q) throws Exception {
      mockMvc
          .perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER).param("q", q)
              .accept(APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2))
          .andExpect(jsonPath("$.page.total_elements").value(2));
    }

    @Test
    @DisplayName("Should filter and paginate when q and page/size are combined")
    @WithMockUser
    void getEntities_200_paginationWithFilter() throws Exception {
      mockMvc
          .perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, "monitoring-service")
              .param("q", "name:Monitoring").param("page", "1").param("size", "3")
              .accept(APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(3))
          .andExpect(jsonPath("$.page.total_elements").value(6))
          .andExpect(jsonPath("$.page.total_pages").value(2))
          .andExpect(jsonPath("$.page.number").value(1));
    }

    @Test
    @DisplayName("Should use raw (case-sensitive) comparison for < and >, unlike = and : which normalise to lowercase")
    @WithMockUser
    void getEntities_200_comparisonOperators_areCaseSensitive() throws Exception {
      // EQUALS normalises both sides to lowercase → case-insensitive match
      mockMvc
          .perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
              .param("q", "property.programmingLanguage=python").accept(APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));

      // LESS_THAN and GREATER_THAN pass values to the DB without lowercasing.
      // port is a NUMBER property (8080 for web-api-1, 9090 for web-api-2).
      // These assertions verify correct boundary semantics using raw numeric string
      // comparison.
      mockMvc
          .perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
              .param("q", "property.port>8080").accept(APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));

      mockMvc
          .perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
              .param("q", "property.port<9090").accept(APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("Should return 400 for operator mismatch on criterion type")
    @WithMockUser
    void getEntities_400_typeMismatch() throws Exception {
      mockMvc
          .perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
              .param("q", "relation<api-link").accept(APPLICATION_JSON))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error_description")
              .value("Operation '<' is not applicable for field 'relation'."));
    }

    @Test
    @DisplayName("Should return 400 for unsupported property in relations_as_target")
    @WithMockUser
    void getEntities_400_relationsAsTargetInvalidProperty() throws Exception {
      mockMvc
          .perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, "microservice")
              .param("q", "relations_as_target.api-link.language=JAVA").accept(APPLICATION_JSON))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error_description").value(
              "Invalid property 'language' in criterion 'relations_as_target.api-link.language=JAVA': only 'identifier' and 'name' are supported for relations_as_target"));
    }

    @Test
    @DisplayName("Should return 400 for unsupported property in relation")
    @WithMockUser
    void getEntities_400_relationInvalidProperty() throws Exception {
      mockMvc
          .perform(get(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, "microservice")
              .param("q", "relation.api-link.template=foo").accept(APPLICATION_JSON))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error_description").value(
              "Invalid property 'template' in criterion 'relation.api-link.template=foo': only 'identifier' and 'name' are supported for relation"));
    }
  }

  /// Tests for GET /api/v1/entities/{template-identifier}/{identifier} endpoint
  /// (lookup by template and identifier).
  @Nested
  @DisplayName("GET /api/v1/entities/{template-identifier}/{identifier} - Get Entities by template identifier and entity identifier")
  class GetEntitiesByTemplateAndEntityIdentifierTests {

    @Test
    @DisplayName("Should return entity by template identifier and identifier")
    @WithMockUser
    void getEntityByTemplateAndIdentifier_200() throws Exception {
      mockMvc
          .perform(get(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, ENTITY_IDENTIFIER)
              .accept(APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(content().contentType(APPLICATION_JSON))
          .andExpect(jsonPath("$.identifier").value(ENTITY_IDENTIFIER))
          .andExpect(jsonPath("$.template_identifier").value(TEMPLATE_IDENTIFIER));
    }

    @Test
    @DisplayName("Should return 404 for non-existent entity")
    @WithMockUser
    void getEntityByTemplateAndIdentifier_404_non_existent_entity() throws Exception {
      mockMvc
          .perform(get(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, "non-existent-identifier")
              .accept(APPLICATION_JSON))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 for non-existent entity template")
    @WithMockUser
    void getEntityByTemplateAndIdentifier_404_non_existent_template() throws Exception {
      mockMvc.perform(
          get(ENTITIES_BY_IDENTIFIER_PATH, "non-existent-template", "non-existent-identifier")
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
      mockMvc
          .perform(
              MockMvcRequestBuilders.post(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
                  .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf()).content(
                      getJsonTestFileContent(ENTITY_JSON_FILES_TEST_PATH + "postEntity_201.json")))
          .andExpect(status().isCreated()).andReturn();
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

      mockMvc
          .perform(MockMvcRequestBuilders
              .post(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
              .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"))
          .andExpect(jsonPath("$.error_description").value(
              org.hamcrest.Matchers.containsString("Property 'applicationName' is required")));
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

      mockMvc
          .perform(MockMvcRequestBuilders
              .post(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
              .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"))
          .andExpect(jsonPath("$.error_description").value(
              org.hamcrest.Matchers.containsString("Property 'port' must be of type NUMBER")));
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

      mockMvc
          .perform(MockMvcRequestBuilders
              .post(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
              .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"))
          .andExpect(jsonPath("$.error_description").value(org.hamcrest.Matchers.allOf(
              org.hamcrest.Matchers
                  .containsString("Property 'ownerEmail' does not match expected format"),
              org.hamcrest.Matchers
                  .containsString("Property 'ownerEmail' does not match required format EMAIL"),
              org.hamcrest.Matchers
                  .containsString("Property 'baseUrl' does not match expected format"),
              org.hamcrest.Matchers
                  .containsString("Property 'baseUrl' does not match required format URL"),
              org.hamcrest.Matchers
                  .containsString("Property 'port' value must be greater than or equal to 1024"))));
    }

    @Test
    @WithMockUser()
    @DisplayName("Should return 400 when payload contains a property not defined in template")
    void postEntity_400_when_property_not_defined_in_template() throws Exception {
      var payload = """
          {
            "name": "web-service-extra-property",
            "identifier": "web-service-extra-property",
            "properties": {
              "applicationName": "catalog-api",
              "ownerEmail": "owner@example.com",
              "port": "8080",
              "environment": "DEV",
              "version": "1.2.3",
              "teamName": "platform-team",
              "baseUrl": "https://catalog.example.com",
              "protocol": "HTTP",
              "programmingLanguage": "JAVA",
              "status": "deprecated"
            }
          }
          """;

      mockMvc
          .perform(MockMvcRequestBuilders
              .post(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
              .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"))
          .andExpect(jsonPath("$.error_description").value(org.hamcrest.Matchers
              .containsString("Property 'status' is not defined in template 'web-service'")));
    }

    @Test
    @WithMockUser()
    @DisplayName("Should return 400 when relation target entity does not exist")
    void postEntity_400_when_relation_target_entity_does_not_exist() throws Exception {
      var payload = """
          {
            "name": "web-service-missing-relation-target",
            "identifier": "web-service-missing-relation-target",
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
            },
            "relations": [
              {
                "name": "database",
                "target_entity_identifiers": ["missing-database"]
              }
            ]
          }
          """;

      mockMvc
          .perform(MockMvcRequestBuilders
              .post(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
              .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"))
          .andExpect(jsonPath("$.error_description").value(org.hamcrest.Matchers.containsString(
              "Relation 'database': target entity 'missing-database' does not exist")));
    }

  }

  @Nested
  @DisplayName("PUT /api/v1/entities/{template-identifier}/{identifier} - Update entity")
  class PutEntitiesTests {

    private static final String VALID_UPDATE_PAYLOAD = """
        {
          "name": "Web API 2 Updated",
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

    @Test
    @WithMockUser
    @DisplayName("Should update entity and return 200")
    void putEntity_200() throws Exception {
      mockMvc
          .perform(put(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, ENTITY_IDENTIFIER)
              .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf())
              .content(VALID_UPDATE_PAYLOAD))
          .andExpect(status().isOk()).andExpect(content().contentType(APPLICATION_JSON))
          .andExpect(jsonPath("$.identifier").value(ENTITY_IDENTIFIER))
          .andExpect(jsonPath("$.template_identifier").value(TEMPLATE_IDENTIFIER))
          .andExpect(jsonPath("$.name").value("Web API 2 Updated"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when updating non-existent entity")
    void putEntity_404_non_existent_entity() throws Exception {
      mockMvc.perform(put(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, "unknown-entity")
          .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf())
          .content(VALID_UPDATE_PAYLOAD)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when updating entity in non-existent template")
    void putEntity_404_non_existent_template() throws Exception {
      mockMvc.perform(put(ENTITIES_BY_IDENTIFIER_PATH, "non-existent-template", ENTITY_IDENTIFIER)
          .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf())
          .content(VALID_UPDATE_PAYLOAD)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when required template properties are missing on update")
    void putEntity_400_when_required_properties_missing() throws Exception {
      var payload = """
          {
            "name": "Web API 2 Updated",
            "properties": {
              "port": "8080"
            }
          }
          """;

      mockMvc
          .perform(put(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, ENTITY_IDENTIFIER)
              .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"))
          .andExpect(jsonPath("$.error_description").value(
              org.hamcrest.Matchers.containsString("Property 'applicationName' is required")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when property type does not match template on update")
    void putEntity_400_when_property_type_mismatch() throws Exception {
      var payload = """
          {
            "name": "Web API 2 Updated",
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

      mockMvc
          .perform(put(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, ENTITY_IDENTIFIER)
              .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"))
          .andExpect(jsonPath("$.error_description").value(
              org.hamcrest.Matchers.containsString("Property 'port' must be of type NUMBER")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when property rules are not respected on update")
    void putEntity_400_when_property_rules_not_respected() throws Exception {
      var payload = """
          {
            "name": "Web API 2 Updated",
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

      mockMvc
          .perform(put(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, ENTITY_IDENTIFIER)
              .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"))
          .andExpect(jsonPath("$.error_description").value(org.hamcrest.Matchers.allOf(
              org.hamcrest.Matchers
                  .containsString("Property 'ownerEmail' does not match expected format"),
              org.hamcrest.Matchers
                  .containsString("Property 'ownerEmail' does not match required format EMAIL"),
              org.hamcrest.Matchers
                  .containsString("Property 'baseUrl' does not match expected format"),
              org.hamcrest.Matchers
                  .containsString("Property 'baseUrl' does not match required format URL"),
              org.hamcrest.Matchers
                  .containsString("Property 'port' value must be greater than or equal to 1024"))));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when update contains a property not defined in template and a missing relation target")
    void putEntity_400_when_property_not_defined_and_relation_target_missing() throws Exception {
      var payload = """
          {
            "name": "Web API 2 Updated",
            "properties": {
              "applicationName": "catalog-api",
              "ownerEmail": "owner@example.com",
              "port": "8080",
              "environment": "DEV",
              "version": "1.2.3",
              "teamName": "platform-team",
              "baseUrl": "https://catalog.example.com",
              "protocol": "HTTP",
              "programmingLanguage": "JAVA",
              "status": "deprecated"
            },
            "relations": [
              {
                "name": "database",
                "target_entity_identifiers": ["missing-database"]
              }
            ]
          }
          """;

      mockMvc
          .perform(put(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, ENTITY_IDENTIFIER)
              .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf()).content(payload))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"))
          .andExpect(jsonPath("$.error_description").value(org.hamcrest.Matchers.allOf(
              org.hamcrest.Matchers
                  .containsString("Property 'status' is not defined in template 'web-service'"),
              org.hamcrest.Matchers.containsString(
                  "Relation 'database': target entity 'missing-database' does not exist"))));
    }

    @Test
    @DisplayName("Should return 401 when updating without authentication")
    void putEntity_401_without_user_token() throws Exception {
      mockMvc.perform(put(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, ENTITY_IDENTIFIER)
          .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf())
          .content(VALID_UPDATE_PAYLOAD)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 403 when updating without CSRF token")
    void putEntity_403_without_csrf() throws Exception {
      mockMvc
          .perform(put(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, ENTITY_IDENTIFIER)
              .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).content(VALID_UPDATE_PAYLOAD))
          .andExpect(status().isForbidden());
    }

  }

  @Nested
  @DisplayName("POST /api/v1/entities/search")
  class SearchEntitiesTests {

    private static final String SEARCH_PATH = "/api/v1/entities/search";

    @Test
    @DisplayName("Should return 401 without authentication")
    void search_401_withoutAuth() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
          .accept(APPLICATION_JSON).with(csrf()).content("""
              { "page": 0, "size": 20 }
              """)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should search entities by template AND property (EQ)")
    @WithMockUser
    void search_200_byTemplateAndProperty() throws Exception {
      var result = mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
          .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf()).content("""
              {
                "filter": {
                  "connector": "AND",
                  "criteria": [
                    { "field": "template", "operation": "EQ", "value": "web-service" },
                    { "field": "property.programmingLanguage", "operation": "EQ", "value": "JAVA" }
                  ]
                },
                "page": 0, "size": 20, "sort": "identifier:asc"
              }
              """)).andExpect(status().isOk()).andReturn();
      JSONAssert.assertEquals(
          getJsonTestFileContent(
              ENTITY_JSON_FILES_TEST_PATH + "searchEntities_200_byTemplateAndProperty.json"),
          result.getResponse().getContentAsString(), JSONCompareMode.STRICT);
    }

    @Test
    @DisplayName("Should search entities using OR connector across templates")
    @WithMockUser
    void search_200_orTemplates() throws Exception {
      var result = mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
          .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf()).content("""
              {
                "filter": {
                  "connector": "OR",
                  "criteria": [
                    { "field": "template", "operation": "EQ", "value": "microservice" },
                    { "field": "template", "operation": "EQ", "value": "batch-job" }
                  ]
                },
                "page": 0, "size": 20, "sort": "identifier:asc"
              }
              """)).andExpect(status().isOk()).andReturn();
      JSONAssert.assertEquals(
          getJsonTestFileContent(
              ENTITY_JSON_FILES_TEST_PATH + "searchEntities_200_orTemplates.json"),
          result.getResponse().getContentAsString(), JSONCompareMode.STRICT);
    }

    @Test
    @DisplayName("Should search entities by relations_as_target identifier")
    @WithMockUser
    void search_200_byRelationsAsTarget() throws Exception {
      var result = mockMvc
          .perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content(
                  """
                      {
                        "filter": {
                          "connector": "AND",
                          "criteria": [
                            { "field": "template", "operation": "EQ", "value": "microservice" },
                            { "field": "relations_as_target.api-link.identifier", "operation": "EQ", "value": "web-api-1" }
                          ]
                        },
                        "page": 0, "size": 20
                      }
                      """))
          .andExpect(status().isOk()).andReturn();
      JSONAssert.assertEquals(
          getJsonTestFileContent(
              ENTITY_JSON_FILES_TEST_PATH + "searchEntities_200_byRelationsAsTarget.json"),
          result.getResponse().getContentAsString(), JSONCompareMode.STRICT);
    }

    @Test
    @DisplayName("Should search entities by bare relations_as_target presence (EQ)")
    @WithMockUser
    void search_200_byRelationsAsTargetPresence() throws Exception {
      var result = mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
          .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf()).content("""
              {
                "filter": {
                  "connector": "AND",
                  "criteria": [
                    { "field": "template", "operation": "EQ", "value": "microservice" },
                    { "field": "relations_as_target", "operation": "EQ", "value": "api-link" }
                  ]
                },
                "page": 0, "size": 20
              }
              """)).andExpect(status().isOk()).andReturn();
      JSONAssert.assertEquals(
          getJsonTestFileContent(
              ENTITY_JSON_FILES_TEST_PATH + "searchEntities_200_byRelationsAsTargetPresence.json"),
          result.getResponse().getContentAsString(), JSONCompareMode.STRICT);
    }

    @Test
    @DisplayName("Should search entities by bare relations_as_target absence (NOT_CONTAINS)")
    @WithMockUser
    void search_200_byRelationsAsTargetAbsence() throws Exception {
      // web-api-1 and web-api-2 are web-service entities not targeted by any 'uses'
      // relation,
      // so NOT_CONTAINS must include them in the results.
      mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
          .accept(APPLICATION_JSON).with(csrf()).content("""
              {
                "filter": {
                  "connector": "AND",
                  "criteria": [
                    { "field": "template", "operation": "EQ", "value": "web-service" },
                    { "field": "relations_as_target", "operation": "NOT_CONTAINS", "value": "uses" }
                  ]
                },
                "page": 0, "size": 20
              }
              """)).andExpect(status().isOk())
          .andExpect(jsonPath("$.content[*].identifier", hasItem("web-api-1")))
          .andExpect(jsonPath("$.content[*].identifier", hasItem("web-api-2")));
    }

    @Test
    @DisplayName("Should search entities using STARTS_WITH operator")
    @WithMockUser
    void search_200_startsWith() throws Exception {
      var result = mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
          .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf()).content("""
              {
                "filter": {
                  "connector": "AND",
                  "criteria": [
                    { "field": "template", "operation": "EQ", "value": "web-service" },
                    { "field": "name", "operation": "STARTS_WITH", "value": "Web API 1" }
                  ]
                },
                "page": 0, "size": 20
              }
              """)).andExpect(status().isOk()).andReturn();
      JSONAssert.assertEquals(
          getJsonTestFileContent(
              ENTITY_JSON_FILES_TEST_PATH + "searchEntities_200_startsWith.json"),
          result.getResponse().getContentAsString(), JSONCompareMode.STRICT);
    }

    @Test
    @DisplayName("Should search entities using NEQ operator")
    @WithMockUser
    void search_200_neq() throws Exception {
      var result = mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
          .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf()).content("""
              {
                "filter": {
                  "connector": "AND",
                  "criteria": [
                    { "field": "template", "operation": "EQ", "value": "web-service" },
                    { "field": "identifier", "operation": "STARTS_WITH", "value": "web-api" },
                    { "field": "identifier", "operation": "NEQ", "value": "web-api-1" }
                  ]
                },
                "page": 0, "size": 20
              }
              """)).andExpect(status().isOk()).andReturn();
      JSONAssert.assertEquals(
          getJsonTestFileContent(ENTITY_JSON_FILES_TEST_PATH + "searchEntities_200_neq.json"),
          result.getResponse().getContentAsString(), JSONCompareMode.STRICT);
    }

    @Test
    @DisplayName("Should return empty content when no entities match")
    @WithMockUser
    void search_200_noMatch() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content("""
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
          .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0))
          .andExpect(jsonPath("$.page.total_elements").value(0));
    }

    @Test
    @DisplayName("Should return all entities when no filter is applied")
    @WithMockUser
    void search_200_nullFilter() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content("""
                  {
                    "page": 0,
                    "size": 5
                  }
                  """))
          .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content.length()").value(5));
    }

    @Test
    @DisplayName("Should return paginated results respecting size parameter")
    @WithMockUser
    void search_200_paginated() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content("""
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
          .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(3))
          .andExpect(jsonPath("$.page.size").value(3))
          .andExpect(jsonPath("$.page.total_elements").value(6))
          .andExpect(jsonPath("$.page.total_pages").value(2));
    }

    @Test
    @DisplayName("Should return 400 for invalid connector value")
    @WithMockUser
    void search_400_invalidConnector() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content("""
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
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error_description")
              .value("Invalid connector 'INVALID_CONNECTOR'. Supported values: AND, OR"));
    }

    @Test
    @DisplayName("Should return 400 for invalid operation value")
    @WithMockUser
    void search_400_invalidOperation() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content("""
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
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error_description").value(
              "Invalid operation 'LIKE'. Supported values: EQ, NEQ, CONTAINS, NOT_CONTAINS, STARTS_WITH, ENDS_WITH, GT, GTE, LT, LTE"));
    }

    @Test
    @DisplayName("Should return 400 for invalid field name")
    @WithMockUser
    void search_400_invalidField() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
          .accept(APPLICATION_JSON).with(csrf()).content("""
              {
                "filter": {
                  "field": "unknownField",
                  "operation": "EQ",
                  "value": "value"
                },
                "page": 0,
                "size": 20
              }
              """)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when criterion is missing field")
    @WithMockUser
    void search_400_missingField() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content("""
                  {
                    "filter": {
                      "operation": "EQ",
                      "value": "microservice"
                    },
                    "page": 0,
                    "size": 20
                  }
                  """))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error_description")
              .value("A criterion node must have a non-blank 'field'"));
    }

    @Test
    @DisplayName("Should return 400 when group is missing criteria")
    @WithMockUser
    void search_400_groupMissingCriteria() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content("""
                  {
                    "filter": {
                      "connector": "AND"
                    },
                    "page": 0,
                    "size": 20
                  }
                  """))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error_description")
              .value("A group node must have a non-empty 'criteria' list"));
    }

    @Test
    @DisplayName("Should support sort parameter")
    @WithMockUser
    void search_200_withSort() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content("""
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
          .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(6))
          .andExpect(jsonPath("$.content[0].name").value("Monitoring Service 6"));
    }

    @Test
    @DisplayName("Should support nested AND/OR filter composition")
    @WithMockUser
    void search_200_nestedFilter() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content("""
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
          .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
          .andExpect(jsonPath("$.content[0].identifier").value("microservice-1"));
    }

    @Test
    @DisplayName("Should find entities by query matching identifier")
    @WithMockUser
    void search_200_queryByIdentifier() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content("""
                  { "query": "web-api", "page": 0, "size": 20 }
                  """))
          .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2))
          .andExpect(jsonPath("$.page.total_elements").value(2));
    }

    @Test
    @DisplayName("Should find entities by query matching name (case-insensitive)")
    @WithMockUser
    void search_200_queryByName() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content("""
                  { "query": "Web API", "page": 0, "size": 20 }
                  """))
          .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2))
          .andExpect(jsonPath("$.page.total_elements").value(2));
    }

    @Test
    @DisplayName("Should find entities by query matching a property value")
    @WithMockUser
    void search_200_queryByPropertyValue() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content("""
                  { "query": "JAVA", "page": 0, "size": 20 }
                  """))
          .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(3))
          .andExpect(jsonPath("$.content[*].identifier", hasItem("web-api-1")))
          .andExpect(jsonPath("$.content[*].identifier", hasItem("web-api-2")))
          .andExpect(jsonPath("$.content[*].identifier", hasItem("web-service-valid-1")));
    }

    @Test
    @DisplayName("Should combine query and filter with AND semantics")
    @WithMockUser
    void search_200_queryAndFilter() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content("""
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
          .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(3))
          .andExpect(jsonPath("$.content[*].identifier", hasItem("web-api-1")))
          .andExpect(jsonPath("$.content[*].identifier", hasItem("web-api-2")))
          .andExpect(jsonPath("$.content[*].identifier", hasItem("web-service-valid-1")));
    }

    @Test
    @DisplayName("Should treat blank query as no-op and return all entities")
    @WithMockUser
    void search_200_blankQueryIsNoOp() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content("""
                  { "query": "   ", "page": 0, "size": 5 }
                  """))
          .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(5));
    }

    @Test
    @DisplayName("Should return 400 when query exceeds maximum length")
    @WithMockUser
    void search_400_queryTooLong() throws Exception {
      var tooLong = "x".repeat(256);
      mockMvc
          .perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content("""
                  { "query": "%s", "page": 0, "size": 20 }
                  """.formatted(tooLong)))
          .andExpect(status().isBadRequest()).andExpect(
              jsonPath("$.error_description").value("Search query must not exceed 255 characters"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when GT operator is used on a non-property field")
    void search_400_numericOperator_onNonPropertyField() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content("""
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
          .andExpect(status().isBadRequest()).andExpect(
              jsonPath("$.error_description").value(org.hamcrest.Matchers.containsString("GT")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when GT operator is used with a non-numeric value")
    void search_400_numericOperator_nonNumericValue() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content("""
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
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error_description")
              .value(org.hamcrest.Matchers.containsString("not-a-number")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when GTE is used on a STRING-typed property with a known template")
    void search_400_numericOperator_onStringProperty() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content(
                  """
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
          .andExpect(jsonPath("$.error_description").value(org.hamcrest.Matchers.allOf(
              org.hamcrest.Matchers.containsString("programmingLanguage"),
              org.hamcrest.Matchers.containsString("STRING"))));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 200 and match correct entities when GT used on a NUMBER property")
    void search_200_numericGt_onNumberProperty() throws Exception {
      // web-api-1 has port=8080, web-api-2 has port=9090; GT 8085 should return only
      // web-api-2
      mockMvc
          .perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content("""
                  {
                    "filter": {
                      "connector": "AND",
                      "criteria": [
                        { "field": "template", "operation": "EQ", "value": "web-service" },
                        { "field": "property.port", "operation": "GT", "value": "8079" }
                      ]
                    },
                    "page": 0, "size": 20
                  }
                  """))
          .andExpect(status().isOk()).andExpect(jsonPath("$.page.total_elements").value(3))
          .andExpect(jsonPath("$.content[0].identifier").value("web-api-1"))
          .andExpect(jsonPath("$.content[1].identifier").value("web-service-valid-1"))
          .andExpect(jsonPath("$.content[2].identifier").value("web-api-2"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 200 and match all seeded entities when LTE used with upper bound covering all")
    void search_200_numericLte_onNumberProperty_allMatch() throws Exception {
      // Both web-api-1 (port=8080) and web-api-2 (port=9090) are <= 9999.
      // Other test methods (e.g. postEntity_201) may create additional web-service
      // entities
      // in the same shared DB, so we only assert at-least-2 rather than an exact
      // count.
      mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
          .accept(APPLICATION_JSON).with(csrf()).content("""
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
              """)).andExpect(status().isOk()).andExpect(
              jsonPath("$.page.total_elements", org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 200 when page and size are omitted from the request body")
    void search_200_noPageOrSize_usesDefaults() throws Exception {
      // Omitting page and size must not cause a 400 JSON parse error (primitive int
      // vs null).
      // The record defaults should kick in: page=0, size=20.
      mockMvc
          .perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content("""
                  {
                    "filter": {
                      "connector": "AND",
                      "criteria": [
                        { "field": "template", "operation": "EQ", "value": "web-service" }
                      ]
                    }
                  }
                  """))
          .andExpect(status().isOk()).andExpect(jsonPath("$.page.size").value(20))
          .andExpect(jsonPath("$.page.number").value(0));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when size exceeds the maximum allowed value")
    void search_400_pageSizeTooLarge() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content("""
                  { "page": 0, "size": 501 }
                  """))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error_description")
              .value("Page size must not exceed %d".formatted(SearchConstraints.MAX_PAGE_SIZE)));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when sort field is not in the allowed list")
    void search_400_invalidSortField() throws Exception {
      mockMvc
          .perform(MockMvcRequestBuilders.post(SEARCH_PATH).contentType(APPLICATION_JSON)
              .accept(APPLICATION_JSON).with(csrf()).content("""
                  { "page": 0, "size": 20, "sort": "badField:asc" }
                  """))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error_description").value(
              "Invalid sort field 'badField'. Supported fields: identifier, name, templateIdentifier"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return entities that have a relation with an exact name match")
    void search_200_byRelationNameEq() throws Exception {
      // web-api-1 has relation "api-link"; web-api-2 does not
      var result = mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
          .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf()).content("""
              {
                "filter": { "field": "relation", "operation": "EQ", "value": "api-link" },
                "page": 0, "size": 20
              }
              """)).andExpect(status().isOk()).andReturn();
      JSONAssert.assertEquals(
          getJsonTestFileContent(
              ENTITY_JSON_FILES_TEST_PATH + "searchEntities_200_byRelationNameEq.json"),
          result.getResponse().getContentAsString(), JSONCompareMode.STRICT);
    }

    @Test
    @WithMockUser
    @DisplayName("Should return entities that have a relation whose name contains the given value")
    void search_200_byRelationNameContains() throws Exception {
      // both web-api-1 and web-api-2 have a relation named "database"
      var result = mockMvc.perform(MockMvcRequestBuilders.post(SEARCH_PATH)
          .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf()).content("""
              {
                "filter": { "field": "relation", "operation": "CONTAINS", "value": "database" },
                "page": 0, "size": 20, "sort": "identifier:asc"
              }
              """)).andExpect(status().isOk()).andReturn();
      JSONAssert.assertEquals(
          getJsonTestFileContent(
              ENTITY_JSON_FILES_TEST_PATH + "searchEntities_200_byRelationNameContains.json"),
          result.getResponse().getContentAsString(), JSONCompareMode.STRICT);
    }
  }

  @Nested
  @DisplayName("DELETE /api/v1/entities/{template-identifier}/{entity-identifier} - Delete Entity")
  class DeleteEntityTests {

    private String createWebServicePayload(String name, String identifier) {
      return """
          {
            "name": "%s",
            "identifier": "%s",
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
          """.formatted(name, identifier);
    }

    private String createWebServicePayloadWithRelation(String name, String identifier,
        String targetEntityIdentifier) {
      return """
          {
            "name": "%s",
            "identifier": "%s",
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
            },
            "relations": [
              {
                "name": "%s",
                "target_entity_identifiers": ["%s"]
              }
            ]
          }
          """.formatted(name, identifier, "database", targetEntityIdentifier);
    }

    @Test
    @WithMockUser()
    @DisplayName("Should delete entity and return 204 No Content")
    void deleteEntity_204_success() throws Exception {
      var entityIdentifier = "delete-success-entity";
      mockMvc
          .perform(
              MockMvcRequestBuilders.post(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
                  .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf())
                  .content(createWebServicePayload("Delete Success Entity", entityIdentifier)))
          .andExpect(status().isCreated());

      mockMvc.perform(delete(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, entityIdentifier)
          .accept(APPLICATION_JSON).with(csrf())).andExpect(status().isNoContent());

      mockMvc.perform(get(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, entityIdentifier)
          .accept(APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser()
    @DisplayName("Should return 404 when deleting non-existent entity")
    void deleteEntity_404_non_existent_entity() throws Exception {
      mockMvc
          .perform(delete(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, "non-existent-entity")
              .accept(APPLICATION_JSON).with(csrf()))
          .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser()
    @DisplayName("Should return 404 when template does not exist")
    void deleteEntity_404_non_existent_template() throws Exception {
      mockMvc
          .perform(delete(ENTITIES_BY_IDENTIFIER_PATH, "non-existent-template", ENTITY_IDENTIFIER)
              .accept(APPLICATION_JSON).with(csrf()))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 401 when deleting without authentication")
    void deleteEntity_401_without_user_token() throws Exception {
      mockMvc.perform(delete(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, ENTITY_IDENTIFIER)
          .accept(APPLICATION_JSON).with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 403 when deleting without CSRF token")
    void deleteEntity_403_without_csrf() throws Exception {
      mockMvc.perform(delete(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, ENTITY_IDENTIFIER)
          .accept(APPLICATION_JSON)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser()
    @DisplayName("Should clean up relations from parent entities when entity is deleted")
    void deleteEntity_204_with_relation_cleanup() throws Exception {
      var targetEntityIdentifier = "relation-target-entity";
      var sourceEntityIdentifier = "relation-source-entity";

      mockMvc
          .perform(
              MockMvcRequestBuilders.post(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
                  .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf()).content(
                      createWebServicePayload("Relation Target Entity", targetEntityIdentifier)))
          .andExpect(status().isCreated());

      mockMvc
          .perform(
              MockMvcRequestBuilders.post(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
                  .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf())
                  .content(createWebServicePayloadWithRelation("Relation Source Entity",
                      sourceEntityIdentifier, targetEntityIdentifier)))
          .andExpect(status().isCreated());

      mockMvc.perform(get(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, targetEntityIdentifier)
          .accept(APPLICATION_JSON)).andExpect(status().isOk());

      mockMvc
          .perform(delete(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, targetEntityIdentifier)
              .accept(APPLICATION_JSON).with(csrf()))
          .andExpect(status().isNoContent());

      mockMvc.perform(get(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, targetEntityIdentifier)
          .accept(APPLICATION_JSON)).andExpect(status().isNotFound());

      mockMvc
          .perform(get(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, sourceEntityIdentifier)
              .accept(APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers
              .containsString("\"identifier\":\"" + targetEntityIdentifier + "\""))));

      mockMvc
          .perform(delete(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, sourceEntityIdentifier)
              .accept(APPLICATION_JSON).with(csrf()))
          .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser()
    @DisplayName("Should handle deletion of entity with no incoming relations")
    void deleteEntity_204_no_incoming_relations() throws Exception {
      var entityIdentifier = "isolated-entity";

      mockMvc
          .perform(
              MockMvcRequestBuilders.post(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
                  .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf())
                  .content(createWebServicePayload("Isolated Entity", entityIdentifier)))
          .andExpect(status().isCreated());

      mockMvc.perform(delete(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, entityIdentifier)
          .accept(APPLICATION_JSON).with(csrf())).andExpect(status().isNoContent());

      mockMvc.perform(get(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, entityIdentifier)
          .accept(APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser()
    @DisplayName("Should return 404 when entity identifier path segment is blank")
    void deleteEntity_404_with_blank_entity_identifier() throws Exception {
      mockMvc
          .perform(delete("/api/v1/entities/{template-identifier}/{entity-identifier}",
              TEMPLATE_IDENTIFIER, "").accept(APPLICATION_JSON).with(csrf()))
          .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser()
    @DisplayName("Should return 400 when template identifier path segment is blank")
    void deleteEntity_400_with_blank_template_identifier() throws Exception {
      mockMvc
          .perform(delete("/api/v1/entities/{template-identifier}/{entity-identifier}", "",
              ENTITY_IDENTIFIER).accept(APPLICATION_JSON).with(csrf()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser()
    @DisplayName("Should delete entity with properties and relations")
    void deleteEntity_204_with_properties_and_relations() throws Exception {
      var entityIdentifier = "test-entity-with-relations";

      mockMvc.perform(
          MockMvcRequestBuilders.post(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
              .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf())
              .content(createWebServicePayloadWithRelation("Test Entity With Relations",
                  entityIdentifier, "database-service-1")))
          .andExpect(status().isCreated());

      mockMvc.perform(delete(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, entityIdentifier)
          .accept(APPLICATION_JSON).with(csrf())).andExpect(status().isNoContent());

      mockMvc.perform(get(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, entityIdentifier)
          .accept(APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser()
    @DisplayName("Should return 404 on repeated deletion attempts (first 204, second 404)")
    void deleteEntity_repeated_deletion() throws Exception {
      var entityIdentifier = "idempotent-test";

      mockMvc
          .perform(
              MockMvcRequestBuilders.post(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER)
                  .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf())
                  .content(createWebServicePayload("Idempotent Test", entityIdentifier)))
          .andExpect(status().isCreated());

      mockMvc.perform(delete(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, entityIdentifier)
          .accept(APPLICATION_JSON).with(csrf())).andExpect(status().isNoContent());

      mockMvc.perform(delete(ENTITIES_BY_IDENTIFIER_PATH, TEMPLATE_IDENTIFIER, entityIdentifier)
          .accept(APPLICATION_JSON).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser()
    @DisplayName("Should return 409 Conflict when entity deletion is blocked by required relations")
    void deleteEntity_409_blocked_by_required_relations() throws Exception {
      var teamIdentifier = "test-team-required-delete";
      var supportIdentifier = "test-support-with-required-team-delete";

      // Create team entity first
      var teamPayload = """
          {
            "name": "Test Team Required",
            "identifier": "%s",
            "properties": {
              "applicationName": "test-app",
              "ownerEmail": "owner@example.com",
              "environment": "DEV"
            }
          }
          """.formatted(teamIdentifier);

      mockMvc.perform(MockMvcRequestBuilders.post(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, "team")
          .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf()).content(teamPayload))
          .andExpect(status().isCreated());

      // Create support entity with required relation to team
      var supportPayload = """
          {
            "name": "Test Support With Required Team",
            "identifier": "%s",
            "properties": {
              "applicationName": "support-app",
              "ownerEmail": "support@example.com",
              "environment": "PROD",
              "version": "1.0.0",
              "teamName": "support-team"
            },
            "relations": [
              {
                "name": "required_team",
                "target_entity_identifiers": ["%s"]
              }
            ]
          }
          """.formatted(supportIdentifier, teamIdentifier);

      mockMvc.perform(MockMvcRequestBuilders.post(ENTITIES_BY_TEMPLATE_IDENTIFIER_PATH, "support")
          .contentType(APPLICATION_JSON).accept(APPLICATION_JSON).with(csrf())
          .content(supportPayload)).andExpect(status().isCreated());

      // Verify team entity was created
      mockMvc
          .perform(
              get(ENTITIES_BY_IDENTIFIER_PATH, "team", teamIdentifier).accept(APPLICATION_JSON))
          .andExpect(status().isOk());

      // Attempt to delete team entity (should fail with 409)
      mockMvc
          .perform(delete(ENTITIES_BY_IDENTIFIER_PATH, "team", teamIdentifier)
              .accept(APPLICATION_JSON).with(csrf()))
          .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("CONFLICT"))
          .andExpect(jsonPath("$.error_description").value(org.hamcrest.Matchers.allOf(
              org.hamcrest.Matchers.containsString("Cannot delete entity"),
              org.hamcrest.Matchers.containsString(teamIdentifier),
              org.hamcrest.Matchers.containsString("required relations"),
              org.hamcrest.Matchers.containsString(supportIdentifier))));

      // Verify team entity still exists after failed deletion
      mockMvc
          .perform(
              get(ENTITIES_BY_IDENTIFIER_PATH, "team", teamIdentifier).accept(APPLICATION_JSON))
          .andExpect(status().isOk());

      // Clean up: delete support first, then team should be deletable
      mockMvc.perform(delete(ENTITIES_BY_IDENTIFIER_PATH, "support", supportIdentifier)
          .accept(APPLICATION_JSON).with(csrf())).andExpect(status().isNoContent());

      mockMvc.perform(delete(ENTITIES_BY_IDENTIFIER_PATH, "team", teamIdentifier)
          .accept(APPLICATION_JSON).with(csrf())).andExpect(status().isNoContent());
    }
  }
}
