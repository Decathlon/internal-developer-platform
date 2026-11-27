package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.decathlon.idp_core.AbstractIntegrationTest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.threads.virtual.enabled=true",
    "camel.servlet.mapping.servlet-name=RelatedByCamelServlet"})
@Sql(statements = {
    """
        INSERT INTO entity_template (id, identifier, name, description) VALUES
          ('dd000001-0000-0000-0000-000000000001', 'rel-product', 'Related Product', 'Product template'),
          ('dd000001-0000-0000-0000-000000000002', 'rel-component', 'Related Component', 'Component template'),
          ('dd000001-0000-0000-0000-000000000003', 'rel-asset', 'Related Asset', 'Asset template')
        ON CONFLICT DO NOTHING;
        """,
    """
        INSERT INTO relation_definition (id, name, target_template_identifier, required, to_many) VALUES
          ('dd000002-0000-0000-0000-000000000001', 'component_owned_by_product', 'rel-product', false, false),
          ('dd000002-0000-0000-0000-000000000002', 'asset_owned_by_component', 'rel-component', false, false),
          ('dd000002-0000-0000-0000-000000000003', 'unused_defined_relation', 'rel-asset', false, true)
        ON CONFLICT DO NOTHING;
        """,
    """
        INSERT INTO entity_template_relations_definitions (entity_template_id, relations_definitions_id) VALUES
          ('dd000001-0000-0000-0000-000000000002', 'dd000002-0000-0000-0000-000000000001'),
          ('dd000001-0000-0000-0000-000000000003', 'dd000002-0000-0000-0000-000000000002'),
          ('dd000001-0000-0000-0000-000000000001', 'dd000002-0000-0000-0000-000000000003')
        ON CONFLICT DO NOTHING;
        """,
    """
        INSERT INTO entity (id, identifier, name, template_identifier) VALUES
          ('dd000003-0000-0000-0000-000000000001', 'prod-1', 'Product One', 'rel-product'),
          ('dd000003-0000-0000-0000-000000000002', 'comp-1', 'Component One', 'rel-component'),
          ('dd000003-0000-0000-0000-000000000003', 'comp-2', 'Component Two', 'rel-component'),
          ('dd000003-0000-0000-0000-000000000004', 'asset-a', 'Asset Zeta', 'rel-asset'),
          ('dd000003-0000-0000-0000-000000000005', 'asset-b', 'Asset Alpha', 'rel-asset'),
          ('dd000003-0000-0000-0000-000000000006', 'asset-c', 'Asset Beta', 'rel-asset')
        ON CONFLICT DO NOTHING;
        """, """
        INSERT INTO relation (id, name, target_template_identifier) VALUES
          ('dd000004-0000-0000-0000-000000000001', 'component_owned_by_product', 'rel-product'),
          ('dd000004-0000-0000-0000-000000000002', 'component_owned_by_product', 'rel-product'),
          ('dd000004-0000-0000-0000-000000000003', 'asset_owned_by_component', 'rel-component'),
          ('dd000004-0000-0000-0000-000000000004', 'asset_owned_by_component', 'rel-component'),
          ('dd000004-0000-0000-0000-000000000005', 'asset_owned_by_component', 'rel-component')
        ON CONFLICT DO NOTHING;
        """,
    """
        INSERT INTO relation_target_entities (relation_id, target_entity_identifier, target_entity_uuid) VALUES
          ('dd000004-0000-0000-0000-000000000001', 'prod-1', 'dd000003-0000-0000-0000-000000000001'),
          ('dd000004-0000-0000-0000-000000000002', 'prod-1', 'dd000003-0000-0000-0000-000000000001'),
          ('dd000004-0000-0000-0000-000000000003', 'comp-1', 'dd000003-0000-0000-0000-000000000002'),
          ('dd000004-0000-0000-0000-000000000004', 'comp-1', 'dd000003-0000-0000-0000-000000000002'),
          ('dd000004-0000-0000-0000-000000000005', 'comp-2', 'dd000003-0000-0000-0000-000000000003')
        ON CONFLICT DO NOTHING;
        """,
    """
        INSERT INTO entity_relations (entity_id, relation_id) VALUES
          ('dd000003-0000-0000-0000-000000000002', 'dd000004-0000-0000-0000-000000000001'),
          ('dd000003-0000-0000-0000-000000000003', 'dd000004-0000-0000-0000-000000000002'),
          ('dd000003-0000-0000-0000-000000000004', 'dd000004-0000-0000-0000-000000000003'),
          ('dd000003-0000-0000-0000-000000000005', 'dd000004-0000-0000-0000-000000000004'),
          ('dd000003-0000-0000-0000-000000000006', 'dd000004-0000-0000-0000-000000000005')
        ON CONFLICT DO NOTHING;
        """}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(statements = {"""
    DELETE FROM entity_relations WHERE entity_id IN (
      'dd000003-0000-0000-0000-000000000002',
      'dd000003-0000-0000-0000-000000000003',
      'dd000003-0000-0000-0000-000000000004',
      'dd000003-0000-0000-0000-000000000005',
      'dd000003-0000-0000-0000-000000000006'
    );
    """, """
    DELETE FROM relation_target_entities WHERE relation_id IN (
      'dd000004-0000-0000-0000-000000000001',
      'dd000004-0000-0000-0000-000000000002',
      'dd000004-0000-0000-0000-000000000003',
      'dd000004-0000-0000-0000-000000000004',
      'dd000004-0000-0000-0000-000000000005'
    );
    """, """
    DELETE FROM relation WHERE id IN (
      'dd000004-0000-0000-0000-000000000001',
      'dd000004-0000-0000-0000-000000000002',
      'dd000004-0000-0000-0000-000000000003',
      'dd000004-0000-0000-0000-000000000004',
      'dd000004-0000-0000-0000-000000000005'
    );
    """, """
    DELETE FROM entity WHERE id IN (
      'dd000003-0000-0000-0000-000000000001',
      'dd000003-0000-0000-0000-000000000002',
      'dd000003-0000-0000-0000-000000000003',
      'dd000003-0000-0000-0000-000000000004',
      'dd000003-0000-0000-0000-000000000005',
      'dd000003-0000-0000-0000-000000000006'
    );
    """, """
    DELETE FROM entity_template_relations_definitions WHERE entity_template_id IN (
      'dd000001-0000-0000-0000-000000000001',
      'dd000001-0000-0000-0000-000000000002',
      'dd000001-0000-0000-0000-000000000003'
    );
    """, """
    DELETE FROM entity_template WHERE id IN (
      'dd000001-0000-0000-0000-000000000001',
      'dd000001-0000-0000-0000-000000000002',
      'dd000001-0000-0000-0000-000000000003'
    );
    """, """
    DELETE FROM relation_definition WHERE id IN (
      'dd000002-0000-0000-0000-000000000001',
      'dd000002-0000-0000-0000-000000000002',
      'dd000002-0000-0000-0000-000000000003'
    );
    """}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
@DisplayName("GET /api/v1/entities/{templateIdentifier}/{entityIdentifier}/related-by/{relationName}")
class EntityRelatedByControllerIntegrationTest extends AbstractIntegrationTest {

  private static final String BEARER_TOKEN = "valid-related-by-reader-token";
  private static final String BASE_PATH = "/api/v1/entities/{template}/{id}/related-by/{relationName}";

  @LocalServerPort
  private int port;

  @Autowired
  private JwtDecoder jwtDecoder;

  private RestTestClient client;

  private record SummaryItemRecord(String identifier, String name,
      @JsonProperty("template_identifier") String templateIdentifier, Object properties) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record PageMetadataRecord(int size, int number,
      @JsonProperty("total_elements") long totalElements,
      @JsonProperty("total_pages") int totalPages) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record SummaryPageResponseRecord(List<SummaryItemRecord> content,
      PageMetadataRecord page) {
  }

  private record ApiErrorRecord(String error,
      @JsonProperty("error_description") String errorDescription) {
  }

  @BeforeEach
  void setUpClient() {
    client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    Instant now = Instant.now();
    Jwt jwt = Jwt.withTokenValue(BEARER_TOKEN).header("alg", "RS256")
        .claim("sub", "related-by-reader").issuedAt(now).expiresAt(now.plusSeconds(3600)).build();
    when(jwtDecoder.decode(BEARER_TOKEN)).thenReturn(jwt);
  }

  @Test
  @DisplayName("Should return paginated assets linked to product via depth=2 terminal relation with sorting")
  void shouldReturnPaginatedAssetsLinkedToProduct() {
    client.get()
        .uri(BASE_PATH + "?depth=2&page=0&size=2&sort=name,asc", "rel-product", "prod-1",
            "asset_owned_by_component")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + BEARER_TOKEN).exchange().expectStatus()
        .isOk().expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody(SummaryPageResponseRecord.class).value(res -> {
          assertThat(res.page().totalElements()).isEqualTo(3L);
          assertThat(res.page().totalPages()).isEqualTo(2);
          assertThat(res.page().size()).isEqualTo(2);
          assertThat(res.page().number()).isZero();
          assertThat(res.content()).extracting("identifier").containsExactly("asset-b", "asset-c");
          assertThat(res.content()).extracting("templateIdentifier").containsOnly("rel-asset");
          assertThat(res.content()).allSatisfy(dto -> assertThat(dto.properties()).isNull());
        });

    client.get()
        .uri(BASE_PATH + "?depth=2&page=1&size=2&sort=name,asc", "rel-product", "prod-1",
            "asset_owned_by_component")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + BEARER_TOKEN).exchange().expectStatus()
        .isOk().expectBody(SummaryPageResponseRecord.class).value(res -> {
          assertThat(res.page().totalElements()).isEqualTo(3L);
          assertThat(res.page().number()).isEqualTo(1);
          assertThat(res.content()).extracting("identifier").containsExactly("asset-a");
        });
  }

  @Test
  @DisplayName("Should default depth=1 and return 200 with empty page when valid relation has no matches at depth=1")
  void shouldDefaultDepthToOneAndReturnEmptyPageWhenNoMatches() {
    client.get().uri(BASE_PATH, "rel-product", "prod-1", "asset_owned_by_component")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + BEARER_TOKEN).exchange().expectStatus()
        .isOk().expectBody(SummaryPageResponseRecord.class).value(res -> {
          assertThat(res.content()).isEmpty();
          assertThat(res.page().totalElements()).isZero();
        });

    client.get().uri(BASE_PATH + "?depth=1", "rel-product", "prod-1", "component_owned_by_product")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + BEARER_TOKEN).exchange().expectStatus()
        .isOk().expectBody(SummaryPageResponseRecord.class).value(res -> {
          assertThat(res.content()).extracting("identifier").containsExactly("comp-1", "comp-2");
        });
  }

  @Test
  @DisplayName("Should return 404 when root entity or relation definition does not exist")
  void shouldReturn404ForMissingRootOrUndefinedRelation() {
    client.get().uri(BASE_PATH, "rel-product", "missing-product", "asset_owned_by_component")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + BEARER_TOKEN).exchange().expectStatus()
        .isNotFound().expectBody(ApiErrorRecord.class)
        .value(err -> assertThat(err.error()).isEqualTo("NOT_FOUND"));

    client.get().uri(BASE_PATH, "rel-product", "prod-1", "non_existent_relation_def")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + BEARER_TOKEN).exchange().expectStatus()
        .isNotFound().expectBody(ApiErrorRecord.class).value(err -> {
          assertThat(err.error()).isEqualTo("NOT_FOUND");
          assertThat(err.errorDescription()).contains("non_existent_relation_def");
        });
  }

  @Test
  @DisplayName("Should return 400 for invalid depth, page, size, or sort parameters and 401 without token")
  void shouldValidateQueryParametersAndAuthentication() {
    client.get().uri(BASE_PATH + "?depth=0", "rel-product", "prod-1", "asset_owned_by_component")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + BEARER_TOKEN).exchange().expectStatus()
        .isBadRequest();

    client.get().uri(BASE_PATH + "?depth=7", "rel-product", "prod-1", "asset_owned_by_component")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + BEARER_TOKEN).exchange().expectStatus()
        .isBadRequest();

    client.get().uri(BASE_PATH + "?size=0", "rel-product", "prod-1", "asset_owned_by_component")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + BEARER_TOKEN).exchange().expectStatus()
        .isBadRequest();

    client.get()
        .uri(BASE_PATH + "?sort=unknownField,asc", "rel-product", "prod-1",
            "asset_owned_by_component")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + BEARER_TOKEN).exchange().expectStatus()
        .isBadRequest();

    client.get().uri(BASE_PATH, "rel-product", "prod-1", "asset_owned_by_component").exchange()
        .expectStatus().isUnauthorized();
  }
}
