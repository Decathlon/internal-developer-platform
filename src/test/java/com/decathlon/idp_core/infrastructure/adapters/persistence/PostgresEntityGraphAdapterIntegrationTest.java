package com.decathlon.idp_core.infrastructure.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.decathlon.idp_core.AbstractIntegrationTest;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntitySummary;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphTraversalMode;
import com.decathlon.idp_core.domain.model.entity_graph.EntityRelatedByQuery;
import com.decathlon.idp_core.domain.model.search.PaginatedResult;
import com.decathlon.idp_core.domain.model.search.PaginationCriteria;
import com.decathlon.idp_core.domain.model.search.SortCriteria;

@DisplayName("PostgresEntityGraphAdapter integration tests")
class PostgresEntityGraphAdapterIntegrationTest extends AbstractIntegrationTest {

  private static final String TEMPLATE = "graph-adapter-test";

  @Autowired
  private PostgresEntityGraphAdapter adapter;

  @Autowired
  private PostgresEntityTemplateAdapter templateAdapter;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  @Transactional
  @DisplayName("Should map empty property and relation JSON arrays")
  void shouldMapEmptyProjectionArrays() {
    UUID rootId = insertEntity("empty-root");

    Map<UUID, Entity> graph = adapter.findEntityGraph(List.of(rootId), 1, true, false,
        EntityGraphTraversalMode.DIRECT_LINEAGE);

    assertThat(graph).containsOnlyKeys(rootId);
    assertThat(graph.get(rootId).properties()).isEmpty();
    assertThat(graph.get(rootId).relations()).isEmpty();
  }

  @Test
  @Transactional
  @DisplayName("Should deserialize populated projection arrays in stable order")
  void shouldDeserializePopulatedProjectionArraysInStableOrder() {
    UUID rootId = insertEntity("populated-root");
    UUID childId = insertEntity("populated-child");
    insertProperty(rootId, "zeta", "last");
    insertProperty(rootId, "alpha", "first");
    insertProperty(childId, "child-property", "child-value");
    insertRelation(rootId, "uses", childId, "populated-child");
    insertRelation(rootId, "monitors", childId, "populated-child");

    Map<UUID, Entity> graph = adapter.findEntityGraph(List.of(rootId), 1, true, false,
        EntityGraphTraversalMode.DIRECT_LINEAGE);

    assertThat(graph).containsOnlyKeys(rootId, childId);
    assertThat(graph.get(rootId).properties()).extracting("name").containsExactly("alpha", "zeta");
    assertThat(graph.get(rootId).properties()).extracting("value").containsExactly("first", "last");
    assertThat(graph.get(rootId).relations()).extracting("name").containsExactly("monitors",
        "uses");
    assertThat(graph.get(rootId).relations().get(0).targetEntityIdentifiers())
        .containsExactly("populated-child");
    assertThat(graph.get(childId).properties()).extracting("name")
        .containsExactly("child-property");
  }

  @Test
  @Transactional
  @DisplayName("Should include only root properties when includeProperties is false")
  void shouldOnlyIncludeRootPropertiesWhenIncludePropertiesIsFalse() {
    UUID rootId = insertEntity("root-only-properties-root");
    UUID childId = insertEntity("root-only-properties-child");
    insertProperty(rootId, "root-property", "root-value");
    insertProperty(childId, "child-property", "child-value");
    insertRelation(rootId, "uses", childId, "root-only-properties-child");

    Map<UUID, Entity> graph = adapter.findEntityGraph(List.of(rootId), 1, false, true,
        EntityGraphTraversalMode.DIRECT_LINEAGE);

    assertThat(graph).containsOnlyKeys(rootId, childId);
    assertThat(graph.get(rootId).properties()).extracting("name").containsExactly("root-property");
    assertThat(graph.get(childId).properties()).isEmpty();
  }

  private UUID insertEntity(String identifier) {
    return insertEntityWithTemplateAndName(TEMPLATE, identifier, identifier);
  }

  private UUID insertEntityWithTemplateAndName(String template, String identifier, String name) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update("""
        INSERT INTO idp_core.entity (id, template_identifier, name, identifier)
        VALUES (?, ?, ?, ?)
        """, id, template, name, identifier);
    return id;
  }

  private void insertProperty(UUID entityId, String name, String value) {
    UUID propertyId = UUID.randomUUID();
    jdbcTemplate.update("INSERT INTO idp_core.property (id, name, value) VALUES (?, ?, ?)",
        propertyId, name, value);
    jdbcTemplate.update(
        "INSERT INTO idp_core.entity_properties (entity_id, property_id) VALUES (?, ?)", entityId,
        propertyId);
  }

  private void insertRelation(UUID entityId, String relationName, UUID targetId,
      String targetIdentifier) {
    insertRelation(entityId, relationName, TEMPLATE, targetId, targetIdentifier);
  }

  private void insertRelation(UUID entityId, String relationName, String targetTemplate,
      UUID targetId, String targetIdentifier) {
    insertRelation(entityId, relationName, targetTemplate, targetId, targetIdentifier, false);
  }

  private void insertRelationWithoutTargetUuid(UUID entityId, String relationName,
      String targetTemplate, String targetIdentifier) {
    insertRelation(entityId, relationName, targetTemplate, null, targetIdentifier, true);
  }

  private void insertRelation(UUID entityId, String relationName, String targetTemplate,
      UUID targetId, String targetIdentifier, boolean omitTargetUuid) {
    UUID relationId = UUID.randomUUID();
    jdbcTemplate.update("""
        INSERT INTO idp_core.relation (id, name, target_template_identifier)
        VALUES (?, ?, ?)
        """, relationId, relationName, targetTemplate);
    jdbcTemplate.update("""
        INSERT INTO idp_core.relation_target_entities
          (relation_id, target_entity_uuid, target_entity_identifier)
        VALUES (?, ?, ?)
        """, relationId, omitTargetUuid ? null : targetId, targetIdentifier);
    jdbcTemplate.update(
        "INSERT INTO idp_core.entity_relations (entity_id, relation_id) VALUES (?, ?)", entityId,
        relationId);
  }

  @Test
  @Transactional
  @DisplayName("Should traverse inbound and outbound recursively in bidirectional mode")
  void shouldTraverseBothDirectionsRecursivelyInBidirectionalMode() {
    UUID upstream2Id = insertEntity("bidirectional-upstream-2");
    UUID upstreamId = insertEntity("bidirectional-upstream");
    UUID rootId = insertEntity("bidirectional-root");
    UUID downstreamId = insertEntity("bidirectional-downstream");
    UUID downstream2Id = insertEntity("bidirectional-downstream-2");

    insertRelation(upstream2Id, "uses", upstreamId, "bidirectional-upstream");
    insertRelation(upstreamId, "uses", rootId, "bidirectional-root");
    insertRelation(rootId, "uses", downstreamId, "bidirectional-downstream");
    insertRelation(downstreamId, "uses", downstream2Id, "bidirectional-downstream-2");

    Map<UUID, Entity> graph = adapter.findEntityGraph(List.of(rootId), 2, false, false,
        EntityGraphTraversalMode.BIDIRECTIONAL);

    assertThat(graph.values()).extracting(Entity::identifier).containsExactlyInAnyOrder(
        "bidirectional-upstream-2", "bidirectional-upstream", "bidirectional-root",
        "bidirectional-downstream", "bidirectional-downstream-2");
  }

  @Test
  @Transactional
  @DisplayName("Should follow outbound recursively without traversing inbound in outbound-only mode")
  void shouldOnlyTraverseOutboundRecursivelyInOutboundOnlyMode() {
    UUID upstreamId = insertEntity("outbound-upstream");
    UUID rootId = insertEntity("outbound-root");
    UUID downstreamId = insertEntity("outbound-downstream");
    UUID downstream2Id = insertEntity("outbound-downstream-2");

    insertRelation(upstreamId, "uses", rootId, "outbound-root");
    insertRelation(rootId, "uses", downstreamId, "outbound-downstream");
    insertRelation(downstreamId, "uses", downstream2Id, "outbound-downstream-2");

    Map<UUID, Entity> graph = adapter.findEntityGraph(List.of(rootId), 2, false, false,
        EntityGraphTraversalMode.OUTBOUND_ONLY);

    assertThat(graph.values()).extracting(Entity::identifier)
        .containsExactlyInAnyOrder("outbound-root", "outbound-downstream", "outbound-downstream-2");
  }

  @Test
  @Transactional
  @DisplayName("Should check whether relation definition exists by name")
  void shouldCheckRelationDefinitionExistsByName() {
    jdbcTemplate.update(
        """
            INSERT INTO idp_core.relation_definition (id, name, target_template_identifier, required, to_many)
            VALUES (?, ?, ?, false, true)
            """,
        UUID.randomUUID(), "asset_owned_by_component", "asset");

    assertThat(templateAdapter.existsRelationDefinitionByName("asset_owned_by_component")).isTrue();
    assertThat(templateAdapter.existsRelationDefinitionByName("completely_unknown_relation"))
        .isFalse();
  }

  @Test
  @Transactional
  @DisplayName("Should find inbound multi-hop assets linked to product skipping intermediary components")
  void shouldFindInboundMultiHopAssetsSkippingIntermediaryComponents() {
    UUID productId = insertEntityWithTemplateAndName("product", "payments", "Payments");
    insertEntityWithTemplateAndName("product", "logistics", "Logistics");
    UUID comp1Id = insertEntityWithTemplateAndName("component", "checkout-api", "Checkout API");
    UUID comp2Id = insertEntityWithTemplateAndName("component", "billing-worker", "Billing Worker");
    UUID asset1Id = insertEntityWithTemplateAndName("asset", "orders-db", "Orders Database");
    UUID asset2Id = insertEntityWithTemplateAndName("asset", "ledger-db", "Ledger Database");

    // Inbound lineage: asset -> component -> product
    insertRelationWithoutTargetUuid(comp1Id, "component_owned_by_product", "product", "payments");
    insertRelationWithoutTargetUuid(comp1Id, "component_owned_by_product", "product", "logistics");
    insertRelationWithoutTargetUuid(comp2Id, "component_owned_by_product", "product", "payments");
    insertRelationWithoutTargetUuid(asset1Id, "asset_owned_by_component", "component",
        "checkout-api");
    insertRelationWithoutTargetUuid(asset2Id, "asset_owned_by_component", "component",
        "billing-worker");

    // At depth=1, assets are 2 hops away, so nothing matches
    // asset_owned_by_component
    PaginatedResult<EntitySummary> depth1 = adapter.findEntitiesRelatedByRelation(
        new EntityRelatedByQuery(productId, "asset_owned_by_component", 1,
            EntityGraphTraversalMode.DIRECT_LINEAGE, new PaginationCriteria(0, 20, "identifier"),
            new SortCriteria("identifier", SortCriteria.Direction.ASC)));
    assertThat(depth1.content()).isEmpty();
    assertThat(depth1.totalElements()).isZero();

    // At depth=2, returns ONLY the assets (skips components and does not leak
    // otherProductId)
    PaginatedResult<EntitySummary> depth2 = adapter.findEntitiesRelatedByRelation(
        new EntityRelatedByQuery(productId, "asset_owned_by_component", 2,
            EntityGraphTraversalMode.DIRECT_LINEAGE, new PaginationCriteria(0, 20, "identifier"),
            new SortCriteria("identifier", SortCriteria.Direction.ASC)));

    assertThat(depth2.totalElements()).isEqualTo(2L);
    assertThat(depth2.totalPages()).isEqualTo(1);
    assertThat(depth2.content()).extracting("identifier").containsExactly("ledger-db", "orders-db");
    assertThat(depth2.content()).extracting("templateIdentifier").containsOnly("asset");
  }

  @Test
  @Transactional
  @DisplayName("Should support outbound chains, diamond convergence, cycles, sorting, and out-of-range pagination")
  void shouldSupportOutboundDiamondsCyclesSortingAndPagination() {
    UUID rootId = insertEntityWithTemplateAndName("product", "core-product", "Core Product");
    UUID compId = insertEntityWithTemplateAndName("component", "core-comp", "Core Component");
    UUID assetAId = insertEntityWithTemplateAndName("asset", "asset-alpha", "Zeta Name");
    UUID assetBId = insertEntityWithTemplateAndName("asset", "asset-beta", "Alpha Name");
    UUID assetCId = insertEntityWithTemplateAndName("archive", "asset-gamma", "Middle Name");

    // Outbound lineage from root through comp to three assets
    insertRelationWithoutTargetUuid(rootId, "has_component", "component", "core-comp");
    // Self-cycle on root via target relation (root itself must NOT be returned)
    insertRelation(rootId, "has_asset", "product", rootId, "core-product");
    // Downstream cycle between assetA and comp (must not loop infinitely or
    // duplicate)
    insertRelation(assetAId, "links_back", "component", compId, "core-comp");
    // Diamond: root also directly links to assetA via a different relation at
    // depth=1!
    insertRelation(rootId, "monitors_directly", "asset", assetAId, "asset-alpha");
    insertRelation(compId, "has_asset", "asset", assetAId, "asset-alpha");
    insertRelation(compId, "has_asset", "asset", assetBId, "asset-beta");
    insertRelation(compId, "has_asset", "archive", assetCId, "asset-gamma");

    // Page 0 (size 2) sorted by name ASC: Alpha Name (asset-beta), Middle Name
    // (asset-gamma)
    PaginatedResult<EntitySummary> page0 = adapter
        .findEntitiesRelatedByRelation(new EntityRelatedByQuery(rootId, "has_asset", 3,
            EntityGraphTraversalMode.DIRECT_LINEAGE, new PaginationCriteria(0, 2, "name,asc"),
            new SortCriteria("name", SortCriteria.Direction.ASC)));

    assertThat(page0.totalElements()).isEqualTo(3L);
    assertThat(page0.totalPages()).isEqualTo(2);
    assertThat(page0.content()).extracting("identifier").containsExactly("asset-beta",
        "asset-gamma");

    // Page 1 (size 2) sorted by name ASC: Zeta Name (asset-alpha, reached via
    // diamond!)
    PaginatedResult<EntitySummary> page1 = adapter
        .findEntitiesRelatedByRelation(new EntityRelatedByQuery(rootId, "has_asset", 3,
            EntityGraphTraversalMode.DIRECT_LINEAGE, new PaginationCriteria(1, 2, "name,asc"),
            new SortCriteria("name", SortCriteria.Direction.ASC)));

    assertThat(page1.totalElements()).isEqualTo(3L);
    assertThat(page1.content()).extracting("identifier").containsExactly("asset-alpha");

    // Out-of-range Page 5 (size 2): empty content, but totalElements=3 preserved
    // via count query
    PaginatedResult<EntitySummary> page5 = adapter.findEntitiesRelatedByRelation(
        new EntityRelatedByQuery(rootId, "has_asset", 3, EntityGraphTraversalMode.DIRECT_LINEAGE,
            new PaginationCriteria(5, 2, "templateIdentifier,asc"),
            new SortCriteria("templateIdentifier", SortCriteria.Direction.ASC)));

    assertThat(page5.content()).isEmpty();
    assertThat(page5.totalElements()).isEqualTo(3L);
    assertThat(page5.totalPages()).isEqualTo(2);
    assertThat(page5.currentPage()).isEqualTo(5);
  }

}
