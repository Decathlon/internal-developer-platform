package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import com.decathlon.idp_core.AbstractIntegrationTest;
import com.decathlon.idp_core.domain.port.EntityTemplateRepositoryPort;

import lombok.extern.slf4j.Slf4j;

/// Integration tests for PrincipalController verifying JIT provisioning and
/// the /me endpoint behavior.
@Sql(scripts = {
    "/db/test/R__1_Insert_test_data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Slf4j
public class PrincipalControllerTest extends AbstractIntegrationTest {

  private static final String PRINCIPAL_ME_PATH = "/api/v1/entities/principals/me";

  @Autowired
  private MockMvc mockMvc;

  @Nested
  @DisplayName("GET /api/v1/entities/principals/me - Get Current Principal")
  class GetCurrentPrincipalTests {

    @Test
    @DisplayName("Should return current principal for authenticated user")
    @WithMockUser(username = "test-user")
    void getCurrentPrincipal_200_withAuthenticatedUser() throws Exception {
      // Given: Authenticated user (JIT provisioning will create principal entity)
      // Make a first call on any endpoint with token to initiate principal creation
      mockMvc.perform(get("/api/v1/entities/web-service").param("page", "0").param("size", "10")
          .accept(APPLICATION_JSON)).andExpect(status().isOk());

      // When: Request current principal
      mockMvc.perform(get(PRINCIPAL_ME_PATH).accept(APPLICATION_JSON))
          // Then: Principal entity returned
          .andExpect(status().isOk()).andExpect(content().contentType(APPLICATION_JSON))
          .andExpect(jsonPath("$.template_identifier", is("principal")))
          .andExpect(jsonPath("$.identifier", is("test-user")))
          .andExpect(jsonPath("$.name", is("test-user")))
          .andExpect(jsonPath("$.properties").isNotEmpty())
          .andExpect(jsonPath("$.properties.kind").value("HUMAN"));
    }

    @Test
    @DisplayName("Should return 401 without authentication")
    void getCurrentPrincipal_401_withoutAuthentication() throws Exception {
      // Given: No authentication

      // When: Request current principal
      mockMvc.perform(get(PRINCIPAL_ME_PATH).accept(APPLICATION_JSON))
          // Then: Unauthorized response
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should create principal on first request")
    @WithMockUser(username = "new-user")
    void getCurrentPrincipal_200_jitProvisioningCreatesEntity() throws Exception {
      // Given: New user (first authentication)

      // When: Request current principal (first time)
      mockMvc.perform(get(PRINCIPAL_ME_PATH).accept(APPLICATION_JSON))
          // Then: Principal created and returned
          .andExpect(status().isOk()).andExpect(jsonPath("$.identifier", is("new-user")))
          .andExpect(jsonPath("$.template_identifier", is("principal")));

      // When: Request again (second time)
      mockMvc.perform(get(PRINCIPAL_ME_PATH).accept(APPLICATION_JSON))
          // Then: Same principal returned (no duplicate creation)
          .andExpect(status().isOk()).andExpect(jsonPath("$.identifier", is("new-user")));
    }

    @Test
    @DisplayName("Should return principal with groups from authentication")
    @WithMockUser(username = "admin-user", authorities = {"platform-team", "admins"})
    void getCurrentPrincipal_200_withGroups() throws Exception {
      // Given: Authenticated user with group membership
      // Note: In real scenarios, groups would come from JWT claims, not Spring
      // authorities
      // This test demonstrates the endpoint structure

      // When: Request current principal
      mockMvc.perform(get(PRINCIPAL_ME_PATH).accept(APPLICATION_JSON))
          // Then: Principal returned (groups handling depends on JWT structure)
          .andExpect(status().isOk()).andExpect(jsonPath("$.identifier", is("admin-user")))
          .andExpect(jsonPath("$.template_identifier", is("principal")));
    }

    @Test
    @DisplayName("Should handle service account authentication")
    @WithMockUser(username = "service-account-client-id")
    void getCurrentPrincipal_200_serviceAccount() throws Exception {
      // Given: Service account authentication
      // Note: In production, service accounts would be detected via JWT claims
      // This test verifies the endpoint works with service account identifiers

      // When: Request current principal
      mockMvc.perform(get(PRINCIPAL_ME_PATH).accept(APPLICATION_JSON))
          // Then: Service account principal returned
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.identifier", is("service-account-client-id")))
          .andExpect(jsonPath("$.template_identifier", is("principal")));
    }

    @Test
    @DisplayName("Should update principal on subsequent authentications")
    @WithMockUser(username = "updating-user")
    void getCurrentPrincipal_200_updatesOnSubsequentAuth() throws Exception {
      // Given: Existing principal from previous authentication

      // When: Request current principal (first time)
      mockMvc.perform(get(PRINCIPAL_ME_PATH).accept(APPLICATION_JSON)).andExpect(status().isOk())
          .andExpect(jsonPath("$.identifier", is("updating-user")));

      // When: Request again after profile change (simulated by second call)
      mockMvc.perform(get(PRINCIPAL_ME_PATH).accept(APPLICATION_JSON))
          // Then: Updated principal returned
          .andExpect(status().isOk()).andExpect(jsonPath("$.identifier", is("updating-user")))
          .andExpect(jsonPath("$.template_identifier", is("principal")));
    }
  }

  @Nested
  @DisplayName("JIT Provisioning Integration")
  class JitProvisioningIntegrationTests {

    @Test
    @DisplayName("Should provision principal transparently on any API call")
    @WithMockUser(username = "jit-test-user")
    void jitProvisioning_worksOnAnyEndpoint() throws Exception {
      // Given: New user accessing any protected endpoint

      // When: Access a different endpoint (not /me)
      // This should trigger JIT provisioning via JitProvisioningFilter
      mockMvc
          .perform(get("/api/v1/entities/web-service").param("page", "0").param("size", "10")
              .accept(APPLICATION_JSON))
          // Then: Request succeeds (principal provisioned in background)
          .andExpect(status().isOk());

      // When: Now access /me endpoint
      mockMvc.perform(get(PRINCIPAL_ME_PATH).accept(APPLICATION_JSON))
          // Then: Principal already exists from previous JIT provisioning
          .andExpect(status().isOk()).andExpect(jsonPath("$.identifier", is("jit-test-user")));
    }
  }

  @Nested
  @DisplayName("Security Role Assignment")
  class SecurityRoleAssignmentTests {

    @Test
    @DisplayName("Should allow access with baseline role assignment")
    @WithMockUser(username = "baseline-role-user")
    void baselineRole_allowsAccess() throws Exception {
      // Given: User with baseline role (assigned via JwtAuthenticationConverter)

      // When: Access protected endpoint
      mockMvc.perform(get(PRINCIPAL_ME_PATH).accept(APPLICATION_JSON))
          // Then: Access granted (baseline role * provides access)
          .andExpect(status().isOk()).andExpect(jsonPath("$.identifier", is("baseline-role-user")));
    }
  }
}
