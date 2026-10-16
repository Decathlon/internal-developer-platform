package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityScheme;

class SwaggerConfigurationTest {

  private final SwaggerConfiguration configuration = new SwaggerConfiguration(
      "https://identity.example/token", "https://idp.example", true);

  @Test
  void shouldDocumentProblemDetailExtensionsAtRootAndPreserveStandardFields() {
    Schema<?> schema = new ObjectSchema().addProperty("detail", new StringSchema())
        .addProperty("instance", new StringSchema().format("uri"))
        .addProperty("properties", new ObjectSchema());
    OpenAPI openApi = new OpenAPI()
        .components(new Components().addSchemas("ProblemDetail", schema));

    configuration.problemDetailSchemaCustomizer().customise(openApi);

    assertFalse(schema.getProperties().containsKey("properties"));
    assertEquals("string", schema.getProperties().get("detail").getType());
    assertEquals("uri", schema.getProperties().get("instance").getFormat());
    assertEquals("string", schema.getProperties().get("error").getType());
    assertEquals("string", schema.getProperties().get("error_description").getType());
    assertEquals("string", schema.getProperties().get("timestamp").getType());
    assertEquals("date-time", schema.getProperties().get("timestamp").getFormat());
    assertEquals(5, schema.getProperties().size());
  }

  @Test
  void shouldAddExtensionsWhenProblemDetailHasNoProperties() {
    Schema<?> schema = new ObjectSchema();
    OpenAPI openApi = new OpenAPI()
        .components(new Components().addSchemas("ProblemDetail", schema));

    configuration.problemDetailSchemaCustomizer().customise(openApi);

    assertEquals(3, schema.getProperties().size());
    assertTrue(schema.getProperties().keySet()
        .containsAll(List.of("error", "error_description", "timestamp")));
  }

  @Test
  void shouldLeaveDocumentWithoutComponentsUnchanged() {
    OpenAPI openApi = new OpenAPI();

    configuration.problemDetailSchemaCustomizer().customise(openApi);

    assertNull(openApi.getComponents());
  }

  @Test
  void shouldLeaveComponentsWithoutSchemasUnchanged() {
    OpenAPI openApi = new OpenAPI().components(new Components());

    configuration.problemDetailSchemaCustomizer().customise(openApi);

    assertNull(openApi.getComponents().getSchemas());
  }

  @Test
  void shouldLeaveOtherSchemasUnchangedWhenProblemDetailIsAbsent() {
    Schema<?> schema = new ObjectSchema().addProperty("name", new StringSchema());
    OpenAPI openApi = new OpenAPI().components(new Components().addSchemas("Entity", schema));

    configuration.problemDetailSchemaCustomizer().customise(openApi);

    assertEquals(1, openApi.getComponents().getSchemas().size());
    assertSame(schema, openApi.getComponents().getSchemas().get("Entity"));
    assertEquals(1, schema.getProperties().size());
  }

  @Test
  void shouldHandleEmptySchemas() {
    OpenAPI openApi = new OpenAPI().components(new Components().schemas(new LinkedHashMap<>()));

    configuration.problemDetailSchemaCustomizer().customise(openApi);

    assertTrue(openApi.getComponents().getSchemas().isEmpty());
  }

  @Test
  void shouldKeepCustomizationIdempotent() {
    Schema<?> schema = new ObjectSchema();
    OpenAPI openApi = new OpenAPI()
        .components(new Components().addSchemas("ProblemDetail", schema));

    configuration.problemDetailSchemaCustomizer().customise(openApi);
    configuration.problemDetailSchemaCustomizer().customise(openApi);

    assertEquals(3, schema.getProperties().size());
    assertEquals("date-time", schema.getProperties().get("timestamp").getFormat());
  }

  @Test
  void shouldConfigureApiMetadataAndBothAuthenticationSchemes() {
    OpenAPI openApi = configuration.openAPI();

    assertEquals("Idp core API", openApi.getInfo().getTitle());
    assertEquals("v1", openApi.getInfo().getVersion());
    assertEquals("https://idp.example", openApi.getServers().getFirst().getUrl());
    var schemes = openApi.getComponents().getSecuritySchemes();
    assertEquals(SecurityScheme.Type.OAUTH2, schemes.get("clientId").getType());
    assertEquals("https://identity.example/token",
        schemes.get("clientId").getFlows().getClientCredentials().getTokenUrl());
    assertEquals(SecurityScheme.Type.HTTP, schemes.get("bearer").getType());
    assertEquals("bearer", schemes.get("bearer").getScheme());
    assertEquals("JWT", schemes.get("bearer").getBearerFormat());
    assertEquals(2, openApi.getSecurity().size());
    assertTrue(openApi.getSecurity().get(0).containsKey("clientId"));
    assertTrue(openApi.getSecurity().get(1).containsKey("bearer"));
  }

  @Test
  void shouldOmitSecurityWhenAuthenticationIsDisabled() {
    OpenAPI openApi = new SwaggerConfiguration(" ", "http://localhost:8080", false).openAPI();

    assertNull(openApi.getComponents());
    assertNull(openApi.getSecurity());
  }

  @Test
  void shouldConfigureInternalApiGroup() {
    var group = configuration.allApis();

    assertEquals("internal", group.getGroup());
    assertEquals(List.of("/**"), group.getPathsToMatch());
  }

  @Test
  void shouldConstructPaginatedResponseModels() {
    var pageable = PageRequest.of(0, 20);
    var pages = List.of(new SwaggerConfiguration.TemplatePageResponse(List.of(), pageable, 0),
        new SwaggerConfiguration.EntityPageResponse(List.of(), pageable, 0),
        new SwaggerConfiguration.WebhookConnectorPageResponse(List.of(), pageable, 0),
        new SwaggerConfiguration.EntityDynamicMappingPageResponse(List.of(), pageable, 0));

    for (var page : pages) {
      assertTrue(page.getContent().isEmpty());
      assertEquals(pageable, page.getPageable());
      assertEquals(0, page.getTotalElements());
    }
  }
}
