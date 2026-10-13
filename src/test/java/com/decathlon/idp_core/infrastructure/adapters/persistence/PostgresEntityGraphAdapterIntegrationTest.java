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
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphTraversalMode;

@DisplayName("PostgresEntityGraphAdapter integration tests")
class PostgresEntityGraphAdapterIntegrationTest extends AbstractIntegrationTest {

  private static final String TEMPLATE = "graph-adapter-test";

  @Autowired
  private PostgresEntityGraphAdapter adapter;

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
    UUID id = UUID.randomUUID();
    jdbcTemplate.update("""
        INSERT INTO idp_core.entity (id, template_identifier, name, identifier)
        VALUES (?, ?, ?, ?)
        """, id, TEMPLATE, identifier, identifier);
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
    UUID relationId = UUID.randomUUID();
    jdbcTemplate.update("""
        INSERT INTO idp_core.relation (id, name, target_template_identifier)
        VALUES (?, ?, ?)
        """, relationId, relationName, TEMPLATE);
    jdbcTemplate.update("""
        INSERT INTO idp_core.relation_target_entities
          (relation_id, target_entity_uuid, target_entity_identifier)
        VALUES (?, ?, ?)
        """, relationId, targetId, targetIdentifier);
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

}
