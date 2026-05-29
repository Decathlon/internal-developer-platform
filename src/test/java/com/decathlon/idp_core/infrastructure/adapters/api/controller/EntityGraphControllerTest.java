package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
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

import com.decathlon.idp_core.AbstractIntegrationTest;

/// Integration tests for the EntityGraphController REST API endpoint.
///
/// Tests are based on the three-node chain seeded in R__2_Insert_entities_test_data.sql:
///
///     graph-svc-a --[uses]-->     graph-svc-b --[uses]--> graph-svc-c
///     graph-svc-a --[monitors]--> graph-svc-b
///
/// Key scenarios verified:
///
/// - No filter: all nodes and edges are returned
/// - Filter "uses": full chain traversed (a→b→c), "monitors" edge excluded at every depth
/// - Filter "monitors": only a→b returned; c is unreachable via "monitors" edges
/// - 404 for unknown entity
/// - 401 without authentication
@DisplayName("GET /api/v1/entities/{templateIdentifier}/{entityIdentifier}/graph")
public class EntityGraphControllerTest extends AbstractIntegrationTest {

  private static final String GRAPH_PATH = "/api/v1/entities/{templateId}/{entityId}/graph";
  private static final String TEMPLATE = "web-service";
  private static final String ENTITY_A = "graph-svc-a";
  private static final String ENTITY_B = "graph-svc-b";
  private static final String ENTITY_C = "graph-svc-c";

  @Autowired
  private MockMvc mockMvc;

  @Nested
  @DisplayName("Without relation filter")
  class NoFilter {

    @Test
    @WithMockUser
    @DisplayName("Should return all nodes and edges when no filter is applied (depth=3)")
    void shouldReturnAllNodesAndEdgesWithNoFilter() throws Exception {
      mockMvc
          .perform(get(GRAPH_PATH, TEMPLATE, ENTITY_A).param("depth", "3").accept(APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(content().contentType(APPLICATION_JSON))
          // All three nodes must be present
          .andExpect(
              jsonPath("$.nodes[*].identifier", containsInAnyOrder(ENTITY_A, ENTITY_B, ENTITY_C)))
          // Three edges: a-[uses]->b, a-[monitors]->b, b-[uses]->c
          .andExpect(jsonPath("$.edges", hasSize(3)));
    }
  }

  @Nested
  @DisplayName("With 'uses' relation filter")
  class UsesFilter {

    @Test
    @WithMockUser
    @DisplayName("Should traverse full chain via 'uses' edges and exclude 'monitors' edge (depth=3)")
    void shouldTraverseFullChainWithUsesFilter() throws Exception {
      mockMvc
          .perform(get(GRAPH_PATH, TEMPLATE, ENTITY_A).param("depth", "3")
              .param("relations", "uses").accept(APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(content().contentType(APPLICATION_JSON))
          // All three nodes are reachable via "uses" chain: a→b→c
          .andExpect(
              jsonPath("$.nodes[*].identifier", containsInAnyOrder(ENTITY_A, ENTITY_B, ENTITY_C)))
          // Only the two "uses" edges: a-[uses]->b and b-[uses]->c
          .andExpect(jsonPath("$.edges", hasSize(2)))
          .andExpect(jsonPath("$.edges[*].type", containsInAnyOrder("uses", "uses")));
    }

    @Test
    @WithMockUser
    @DisplayName("Should still reach graph-svc-c at depth 2 when filtering by 'uses'")
    void shouldReachNodeCAtDepthTwoWithUsesFilter() throws Exception {
      // This specifically verifies that the filter applies recursively:
      // at depth=2, a→b (level 1) and b→c (level 2) must both be traversed.
      mockMvc
          .perform(get(GRAPH_PATH, TEMPLATE, ENTITY_A).param("depth", "2")
              .param("relations", "uses").accept(APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(content().contentType(APPLICATION_JSON))
          .andExpect(
              jsonPath("$.nodes[*].identifier", containsInAnyOrder(ENTITY_A, ENTITY_B, ENTITY_C)))
          .andExpect(jsonPath("$.edges", hasSize(2)));
    }
  }

  @Nested
  @DisplayName("With 'monitors' relation filter")
  class MonitorsFilter {

    @Test
    @WithMockUser
    @DisplayName("Should return only graph-svc-a and graph-svc-b when filtering by 'monitors' (depth=3)")
    void shouldReturnOnlyRootAndDirectTargetWithMonitorsFilter() throws Exception {
      // "monitors" only exists at level 1 (a→b). Since b has no "monitors" edges,
      // graph-svc-c must NOT appear in the result.
      mockMvc
          .perform(get(GRAPH_PATH, TEMPLATE, ENTITY_A).param("depth", "3")
              .param("relations", "monitors").accept(APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(content().contentType(APPLICATION_JSON))
          // Only a and b — c is unreachable via "monitors"
          .andExpect(jsonPath("$.nodes", hasSize(2)))
          .andExpect(jsonPath("$.nodes[*].identifier", containsInAnyOrder(ENTITY_A, ENTITY_B)))
          // One edge only: a-[monitors]->b
          .andExpect(jsonPath("$.edges", hasSize(1)))
          .andExpect(jsonPath("$.edges[0].type").value("monitors"));
    }
  }

  @Nested
  @DisplayName("Error cases")
  class ErrorCases {

    @Test
    @WithMockUser
    @DisplayName("Should return 404 when entity does not exist")
    void shouldReturn404ForUnknownEntity() throws Exception {
      mockMvc.perform(get(GRAPH_PATH, TEMPLATE, "non-existent-entity").accept(APPLICATION_JSON))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 401 without authentication")
    void shouldReturn401WithoutAuthentication() throws Exception {
      mockMvc.perform(get(GRAPH_PATH, TEMPLATE, ENTITY_A).accept(APPLICATION_JSON))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("With 'properties' filter (include_data=true)")
  class PropertyFilter {

    @Test
    @WithMockUser
    @DisplayName("Should include only requested property in each node's data when one property is requested")
    void shouldIncludeOnlyRequestedProperty() throws Exception {
      mockMvc
          .perform(get(GRAPH_PATH, TEMPLATE, ENTITY_A).param("depth", "3")
              .param("include_data", "true").param("properties", "tier").accept(APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(content().contentType(APPLICATION_JSON))
          // All three nodes are still returned
          .andExpect(
              jsonPath("$.nodes[*].identifier", containsInAnyOrder(ENTITY_A, ENTITY_B, ENTITY_C)))
          // Each node's data must contain "tier" …
          .andExpect(jsonPath("$.nodes[0].data.tier").exists())
          // … but must NOT contain "version"
          .andExpect(jsonPath("$.nodes[0].data.version").doesNotExist());
    }

    @Test
    @WithMockUser
    @DisplayName("Should include multiple requested properties in each node's data")
    void shouldIncludeMultipleRequestedProperties() throws Exception {
      mockMvc
          .perform(get(GRAPH_PATH, TEMPLATE, ENTITY_A).param("depth", "3")
              .param("include_data", "true").param("properties", "tier")
              .param("properties", "version").accept(APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(content().contentType(APPLICATION_JSON))
          .andExpect(jsonPath("$.nodes[0].data.tier").exists())
          .andExpect(jsonPath("$.nodes[0].data.version").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return empty data when requested property does not exist on entity")
    void shouldReturnEmptyDataForUnknownProperty() throws Exception {
      mockMvc
          .perform(
              get(GRAPH_PATH, TEMPLATE, ENTITY_A).param("depth", "3").param("include_data", "true")
                  .param("properties", "non-existent-prop").accept(APPLICATION_JSON))
          .andExpect(status().isOk())
          // data field is omitted from JSON when empty (@JsonInclude NON_EMPTY)
          .andExpect(jsonPath("$.nodes[0].data").doesNotExist());
    }

    @Test
    @WithMockUser
    @DisplayName("Should include all properties when no property filter is supplied")
    void shouldIncludeAllPropertiesWithoutFilter() throws Exception {
      mockMvc
          .perform(get(GRAPH_PATH, TEMPLATE, ENTITY_A).param("depth", "3")
              .param("include_data", "true").accept(APPLICATION_JSON))
          .andExpect(status().isOk()).andExpect(jsonPath("$.nodes[0].data.tier").exists())
          .andExpect(jsonPath("$.nodes[0].data.version").exists());
    }
  }
}
