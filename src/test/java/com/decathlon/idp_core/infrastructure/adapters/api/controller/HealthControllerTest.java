package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.decathlon.idp_core.AbstractIntegrationTest;

class HealthControllerTest extends AbstractIntegrationTest {

    @Test
    void getHealthWithoutAuth() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health").accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
    }

}
