package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.decathlon.idp_core.AbstractIntegrationTest;

/// Integration test for health check endpoint accessibility.
///
/// **Test purpose:** Verifies that the actuator health endpoint is accessible
/// without authentication, ensuring system monitoring capabilities work correctly.
class HealthControllerTest extends AbstractIntegrationTest {

    @Test
    void getHealthWithoutAuth() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health").accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
    }

}
