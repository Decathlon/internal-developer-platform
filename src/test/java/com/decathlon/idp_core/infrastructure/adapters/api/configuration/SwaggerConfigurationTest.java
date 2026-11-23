package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;

class SwaggerConfigurationTest {

  @Test
  void shouldConfigureOpenApiWithoutOptionalSecurity() {
    OpenAPI openApi = new SwaggerConfiguration("", "https://idp.example.test", false).openAPI();

    assertEquals("Idp core API", openApi.getInfo().getTitle());
    assertEquals("API dedicated to idp core functionalities", openApi.getInfo().getDescription());
    assertEquals("v1", openApi.getInfo().getVersion());
    assertEquals("https://idp.example.test", openApi.getServers().getFirst().getUrl());
    assertTrue(openApi.getSecurity() == null || openApi.getSecurity().isEmpty());
  }

  @Test
  void shouldConfigureOAuthAndBearerSecurityWhenEnabled() {
    OpenAPI openApi = new SwaggerConfiguration("https://auth.example.test/token",
        "https://idp.example.test", true).openAPI();

    assertNotNull(openApi.getComponents());
    assertEquals("https://auth.example.test/token", openApi.getComponents().getSecuritySchemes()
        .get(SwaggerConfiguration.CLIENT_ID).getFlows().getClientCredentials().getTokenUrl());
    assertEquals("JWT", openApi.getComponents().getSecuritySchemes()
        .get(SwaggerConfiguration.BEARER).getBearerFormat());
    assertEquals(2, openApi.getSecurity().size());
  }

  @Test
  void shouldAddLegacyFieldsToProblemDetailSchema() {
    GroupedOpenApi groupedOpenApi = new SwaggerConfiguration("", "http://localhost:8080", false)
        .allApis();
    Schema<?> problemDetailSchema = new Schema<>();
    problemDetailSchema.addProperty("properties", new ObjectSchema());
    problemDetailSchema.addProperty("status", new IntegerSchema());
    OpenAPI openApi = new OpenAPI()
        .components(new Components().addSchemas("ProblemDetail", problemDetailSchema));

    groupedOpenApi.getOpenApiCustomizers().forEach(customizer -> customizer.customise(openApi));

    Map<String, Schema> properties = problemDetailSchema.getProperties();
    assertFalse(properties.containsKey("properties"));
    assertTrue(properties.containsKey("error"));
    assertTrue(properties.containsKey("error_description"));
    assertTrue(properties.containsKey("timestamp"));
    assertEquals(List.of("error", "error_description", "timestamp"),
        problemDetailSchema.getRequired());
    assertEquals(Boolean.TRUE, problemDetailSchema.getAdditionalProperties());
    assertEquals(List.of("/**"), groupedOpenApi.getPathsToMatch());
  }

  @Test
  void shouldRejectOpenApiWithoutProblemDetailSchema() {
    GroupedOpenApi groupedOpenApi = new SwaggerConfiguration("", "http://localhost:8080", false)
        .allApis();
    OpenAPI openApi = new OpenAPI()
        .components(new Components().addSchemas("Other", new Schema<>()));

    assertThrows(IllegalStateException.class, () -> groupedOpenApi.getOpenApiCustomizers()
        .forEach(customizer -> customizer.customise(openApi)));
  }

  @Test
  void shouldRejectOpenApiWithoutComponents() {
    GroupedOpenApi groupedOpenApi = new SwaggerConfiguration("", "http://localhost:8080", false)
        .allApis();
    OpenAPI openApi = new OpenAPI();

    assertThrows(IllegalStateException.class, () -> groupedOpenApi.getOpenApiCustomizers()
        .forEach(customizer -> customizer.customise(openApi)));
  }

  @Test
  void shouldRejectOpenApiWithoutComponentSchemas() {
    GroupedOpenApi groupedOpenApi = new SwaggerConfiguration("", "http://localhost:8080", false)
        .allApis();
    OpenAPI openApi = new OpenAPI().components(new Components());

    assertThrows(IllegalStateException.class, () -> groupedOpenApi.getOpenApiCustomizers()
        .forEach(customizer -> customizer.customise(openApi)));
  }
}
