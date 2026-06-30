package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.decathlon.idp_core.AbstractIntegrationTest;

@DisplayName("Audit Controller Integration Tests")
class AuditControllerTest extends AbstractIntegrationTest {

  private static final String AUDIT_JSON_FILES_TEST_PATH = "integration_test/json/audit/v1/";

  @Autowired
  private MockMvc mockMvc;

  private static final String AUDIT_BASE_PATH = "/api/v1/audit/entities";
  private static final String ENTITY_BASE_PATH = "/api/v1/entities";

  @Test
  @WithMockUser
  @DisplayName("Should return audit history for existing entity")
  void getAuditHistory_shouldReturnEmptyAuditHistory_whenEntityExistsBeforeAudit()
      throws Exception {
    String templateIdentifier = "web-service";
    String entityIdentifier = "web-api-1";

    // When requesting audit history
    mockMvc
        .perform(get(AUDIT_BASE_PATH + "/{templateIdentifier}/{entityIdentifier}",
            templateIdentifier, entityIdentifier).with(csrf()))
        .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  @WithMockUser
  @DisplayName("Should return 404 when entity does not exist")
  void getAuditHistory_shouldReturn404_whenEntityDoesNotExist() throws Exception {
    String templateIdentifier = "non-existing-template";
    String entityIdentifier = "non-existing-entity";

    mockMvc.perform(get(AUDIT_BASE_PATH + "/{templateIdentifier}/{entityIdentifier}",
        templateIdentifier, entityIdentifier).with(csrf())).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 401 without authentication")
  void getAuditHistory_shouldReturn401_withoutAuthentication() throws Exception {
    String templateIdentifier = "web-audited";
    String entityIdentifier = "web-api-1";

    mockMvc.perform(get(AUDIT_BASE_PATH + "/{templateIdentifier}/{entityIdentifier}",
        templateIdentifier, entityIdentifier)).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "test-user")
  @DisplayName("Should track complete lifecycle (Create, Update, Delete) in audit history")
  void auditHistory_shouldTrackFullLifecycle() throws Exception {
    String templateIdentifier = "web-audited";
    String entityIdentifier = "audit-lifecycle-test";

    generateAuditHistory(templateIdentifier, entityIdentifier, true);
    // 4. VERIFY FULL AUDIT HISTORY
    // Envers sorts by revision number descending, so index 0 is DELETED, 1 is
    // UPDATED, 2 is CREATED.
    mockMvc
        .perform(get(AUDIT_BASE_PATH + "/{templateIdentifier}/{entityIdentifier}",
            templateIdentifier, entityIdentifier).with(csrf()))
        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))

        // Latest action (DELETED)
        .andExpect(jsonPath("$[0].revision_type").value("DELETED"))
        .andExpect(jsonPath("$[0].modified_by").value("test-user"))

        // Middle action (UPDATED)
        .andExpect(jsonPath("$[1].revision_type").value("UPDATED"))
        .andExpect(jsonPath("$[1].modified_by").value("test-user"))
        .andExpect(jsonPath("$[1].snapshot.name").value("Audit Test Entity Updated"))
        // Verify modification flags in snapshot (only true flags are included)
        .andExpect(jsonPath("$[1].snapshot.modified_flags.name_mod").value(true))
        .andExpect(jsonPath("$[1].snapshot.modified_flags.identifier_mod").doesNotExist())

        // First action (CREATED)
        .andExpect(jsonPath("$[2].revision_type").value("CREATED"))
        .andExpect(jsonPath("$[2].modified_by").value("test-user"))
        .andExpect(jsonPath("$[2].snapshot.name").value("Audit Test Entity"));
  }

  @Test
  @WithMockUser(username = "latest-tester")
  @DisplayName("Should return the latest modification for an entity at first position")
  void latestAudit_shouldReturnLatestChange() throws Exception {

    String templateIdentifier = "web-audited";
    String entityIdentifier = "audit-latest-test";

    generateAuditHistory(templateIdentifier, entityIdentifier, false);

    mockMvc
        .perform(get(AUDIT_BASE_PATH + "/{templateIdentifier}/{entityIdentifier}",
            templateIdentifier, entityIdentifier).with(csrf()))
        .andExpect(status().isOk()).andExpect(jsonPath("$[0].revision_type").value("UPDATED"))
        .andExpect(jsonPath("$[0].modified_by").value("latest-tester"))
        .andExpect(jsonPath("$[0].snapshot.name").value("Audit Test Entity Updated"));
  }

  private void generateAuditHistory(final String templateIdentifier, final String entityIdentifier,
      final Boolean deleted) throws Exception {

    String createPayload = getJsonTestFileContent(
        AUDIT_JSON_FILES_TEST_PATH + "getAudit_200_history_create.json")
            .formatted(entityIdentifier);

    mockMvc
        .perform(post(ENTITY_BASE_PATH + "/{templateIdentifier}", templateIdentifier)
            .contentType(APPLICATION_JSON).with(csrf()).content(createPayload))
        .andExpect(status().isCreated());

    mockMvc
        .perform(put(ENTITY_BASE_PATH + "/{templateIdentifier}/{entityIdentifier}",
            templateIdentifier, entityIdentifier)
                .contentType(APPLICATION_JSON).with(csrf())
                .content(getJsonTestFileContent(
                    AUDIT_JSON_FILES_TEST_PATH + "getAudit_200_history_update.json")))
        .andExpect(status().isOk());

    if (deleted) {
      mockMvc.perform(delete(ENTITY_BASE_PATH + "/{templateIdentifier}/{entityIdentifier}",
          templateIdentifier, entityIdentifier).with(csrf())).andExpect(status().isNoContent());
    }
  }
}
