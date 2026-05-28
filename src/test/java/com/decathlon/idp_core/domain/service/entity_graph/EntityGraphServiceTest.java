package com.decathlon.idp_core.domain.service.entity_graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntityCompositeKey;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphNode;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphRelation;
import com.decathlon.idp_core.domain.port.EntityGraphRepositoryPort;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("EntityGraphService Tests")
class EntityGraphServiceTest {

  @Mock
  private EntityRepositoryPort entityRepositoryPort;

  @Mock
  private EntityGraphRepositoryPort entityGraphRepositoryPort;

  @InjectMocks
  private EntityGraphService entityGraphService;

  // --- Fixtures ---

  private Entity entity(String templateIdentifier, String identifier, String name) {
    return new Entity(UUID.randomUUID(), templateIdentifier, name, identifier, List.of(),
        List.of());
  }

  private Entity entityWithRelations(String templateIdentifier, String identifier, String name,
      List<Relation> relations) {
    return new Entity(UUID.randomUUID(), templateIdentifier, name, identifier, List.of(),
        relations);
  }

  private Relation relation(String name, String targetTemplateIdentifier, String... targetIds) {
    return new Relation(UUID.randomUUID(), name, targetTemplateIdentifier, List.of(targetIds));
  }

  private EntityCompositeKey key(String templateIdentifier, String identifier) {
    return new EntityCompositeKey(templateIdentifier, identifier);
  }

  private static final String TEMPLATE = "web-service";

  // --- Helper to stub both ports ---

  private void stubGraph(Map<EntityCompositeKey, Entity> entityMap) {
    when(
        entityGraphRepositoryPort.findEntityGraph(anyString(), anyString(), anyInt(), anyBoolean()))
            .thenReturn(entityMap);
  }

  // ========================
  @Nested
  @DisplayName("Root Entity Not Found")
  class RootEntityNotFound {

    @Test
    @DisplayName("Should throw EntityNotFoundException when root entity does not exist")
    void shouldThrowWhenRootEntityNotFound() {
      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "missing"))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> entityGraphService.getEntityGraph(TEMPLATE, "missing", 1, false))
          .isInstanceOf(EntityNotFoundException.class);

      verify(entityGraphRepositoryPort, never()).findEntityGraph(anyString(), anyString(), anyInt(),
          anyBoolean());
    }
  }

  // ========================
  @Nested
  @DisplayName("Single Root — No Relations")
  class SingleRootNoRelations {

    @Test
    @DisplayName("Should return leaf node when entity has no relations")
    void shouldReturnLeafNodeWhenNoRelations() {
      Entity api = entity(TEMPLATE, "api", "API Service");
      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(Map.of(key(TEMPLATE, "api"), api));

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", 1, false);

      assertThat(result.identifier()).isEqualTo("api");
      assertThat(result.name()).isEqualTo("API Service");
      assertThat(result.relations()).isEmpty();
      assertThat(result.relationsAsTarget()).isEmpty();
    }
  }

  // ========================
  @Nested
  @DisplayName("Outbound Relations")
  class OutboundRelations {

    @Test
    @DisplayName("Should resolve outbound relation targets at depth 1")
    void shouldResolveOutboundRelations() {
      Entity api = entityWithRelations(TEMPLATE, "api", "API Service",
          List.of(relation("uses-db", "database", "postgres")));
      Entity postgres = entity("database", "postgres", "Postgres DB");

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(Map.of(key(TEMPLATE, "api"), api, key("database", "postgres"), postgres));

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", 1, false);

      assertThat(result.relations()).hasSize(1);
      assertThat(result.relations().get(0).name()).isEqualTo("uses-db");
      assertThat(result.relations().get(0).targets()).hasSize(1);
      assertThat(result.relations().get(0).targets().get(0).identifier()).isEqualTo("postgres");
    }

    @Test
    @DisplayName("Should return fallback node when target is not in the pre-loaded entity map")
    void shouldReturnFallbackNodeWhenTargetNotInMap() {
      Entity api = entityWithRelations(TEMPLATE, "api", "API Service",
          List.of(relation("uses-db", "database", "missing-db")));

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(Map.of(key(TEMPLATE, "api"), api));

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", 1, false);

      assertThat(result.relations()).hasSize(1);
      EntityGraphNode fallback = result.relations().get(0).targets().get(0);
      assertThat(fallback.identifier()).isEqualTo("missing-db");
    }
  }

  // ========================
  @Nested
  @DisplayName("Inbound Relations (relationsAsTarget)")
  class InboundRelations {

    @Test
    @DisplayName("Should resolve inbound relations when another entity points to root")
    void shouldResolveInboundRelations() {
      Entity api = entity(TEMPLATE, "api", "API Service");
      Entity consumer = entityWithRelations(TEMPLATE, "consumer", "Consumer",
          List.of(relation("depends-on", TEMPLATE, "api")));

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(Map.of(key(TEMPLATE, "api"), api, key(TEMPLATE, "consumer"), consumer));

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", 1, false);

      assertThat(result.relationsAsTarget()).hasSize(1);
      assertThat(result.relationsAsTarget().get(0).name()).isEqualTo("depends-on");
      assertThat(result.relationsAsTarget().get(0).targets().get(0).identifier())
          .isEqualTo("consumer");
    }
  }

  // ========================
  @Nested
  @DisplayName("Depth Clamping")
  class DepthClamping {

    @Test
    @DisplayName("Should clamp depth below 1 to 1")
    void shouldClampDepthBelowOne() {
      Entity api = entity(TEMPLATE, "api", "API Service");
      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(Map.of(key(TEMPLATE, "api"), api));

      entityGraphService.getEntityGraph(TEMPLATE, "api", 0, false);

      verify(entityGraphRepositoryPort).findEntityGraph(TEMPLATE, "api", 1, false);
    }

    @Test
    @DisplayName("Should clamp depth above MAX_DEPTH to MAX_DEPTH")
    void shouldClampDepthAboveTen() {
      Entity api = entity(TEMPLATE, "api", "API Service");
      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(Map.of(key(TEMPLATE, "api"), api));

      entityGraphService.getEntityGraph(TEMPLATE, "api", 99, false);

      verify(entityGraphRepositoryPort).findEntityGraph(TEMPLATE, "api", 10, false);
    }
  }

  // ========================
  @Nested
  @DisplayName("Depth Limit — Leaf Nodes at Boundary")
  class DepthLimit {

    @Test
    @DisplayName("Should return target as leaf node when depth limit is reached")
    void shouldReturnLeafNodeAtDepthBoundary() {
      Entity api = entityWithRelations(TEMPLATE, "api", "API Service",
          List.of(relation("uses-db", "database", "postgres")));
      Entity postgres = entityWithRelations("database", "postgres", "Postgres DB",
          List.of(relation("runs-on", "infra", "server-1")));
      Entity server = entity("infra", "server-1", "Server 1");

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(Map.of(key(TEMPLATE, "api"), api, key("database", "postgres"), postgres,
          key("infra", "server-1"), server));

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", 1, false);

      EntityGraphNode postgresNode = result.relations().get(0).targets().get(0);
      assertThat(postgresNode.identifier()).isEqualTo("postgres");
      // At depth=1, postgres is a leaf — no further relations resolved
      assertThat(postgresNode.relations()).isEmpty();
      assertThat(postgresNode.relationsAsTarget()).isEmpty();
    }
  }

  // ========================
  @Nested
  @DisplayName("Multiple Named Relations")
  class MultipleRelations {

    @Test
    @DisplayName("Should resolve multiple distinct relation types")
    void shouldResolveMultipleNamedRelations() {
      Entity api = entityWithRelations(TEMPLATE, "api", "API Service", List.of(
          relation("uses-db", "database", "postgres"), relation("depends-on", TEMPLATE, "auth")));
      Entity postgres = entity("database", "postgres", "Postgres DB");
      Entity auth = entity(TEMPLATE, "auth", "Auth Service");

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
          .thenReturn(Optional.of(api));
      stubGraph(Map.of(key(TEMPLATE, "api"), api, key("database", "postgres"), postgres,
          key(TEMPLATE, "auth"), auth));

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", 1, false);

      assertThat(result.relations()).hasSize(2);
      assertThat(result.relations().stream().map(EntityGraphRelation::name))
          .containsExactlyInAnyOrder("uses-db", "depends-on");
    }
  }

  // ========================
  @Nested
  @DisplayName("Full Graph Returned — Filtering Is a Mapper Concern")
  class FullGraphReturned {

    @Test
    @DisplayName("Should return all edges regardless of relation type (no filtering in service)")
    void shouldReturnAllEdgesWithoutFiltering() {
      // A --(depends-on)--> B --(owns)--> C
      // The service must return both edges — the mapper will filter them.
      Entity a = entityWithRelations(TEMPLATE, "a", "A",
          List.of(relation("depends-on", TEMPLATE, "b")));
      Entity b = entityWithRelations(TEMPLATE, "b", "B", List.of(relation("owns", TEMPLATE, "c")));
      Entity c = entity(TEMPLATE, "c", "C");

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "a"))
          .thenReturn(Optional.of(a));
      stubGraph(Map.of(key(TEMPLATE, "a"), a, key(TEMPLATE, "b"), b, key(TEMPLATE, "c"), c));

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "a", 2, false);

      // Root A has one outbound "depends-on" edge → B
      assertThat(result.relations()).hasSize(1);
      assertThat(result.relations().get(0).name()).isEqualTo("depends-on");

      // B (at depth 1) has one outbound "owns" edge → C
      EntityGraphNode nodeB = result.relations().get(0).targets().get(0);
      assertThat(nodeB.identifier()).isEqualTo("b");
      assertThat(nodeB.relations()).hasSize(1);
      assertThat(nodeB.relations().get(0).name()).isEqualTo("owns");
      assertThat(nodeB.relations().get(0).targets().get(0).identifier()).isEqualTo("c");

      verify(entityGraphRepositoryPort).findEntityGraph(TEMPLATE, "a", 2, false);
    }
  }

  // ========================
  @Nested
  @DisplayName("Visited Node Guard — OOM Prevention")
  class VisitedNodeGuard {

    @Test
    @DisplayName("Should complete at depth=10 without exponential recursion for a small graph")
    void shouldNotExplodeAtMaxDepthWithSmallGraph() {
      // A --(uses)--> B --(uses)--> C; B also has inbound from A and C has inbound
      // from B.
      // Without the visited-node guard this produces O(2^depth) calls at depth=10.
      Entity a = entityWithRelations(TEMPLATE, "a", "A", List.of(relation("uses", TEMPLATE, "b")));
      Entity b = entityWithRelations(TEMPLATE, "b", "B", List.of(relation("uses", TEMPLATE, "c")));
      Entity c = entity(TEMPLATE, "c", "C");

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "a"))
          .thenReturn(Optional.of(a));
      stubGraph(Map.of(key(TEMPLATE, "a"), a, key(TEMPLATE, "b"), b, key(TEMPLATE, "c"), c));

      // Must complete instantly — any OOM or StackOverflow here means the guard is
      // missing.
      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "a", 10, false);

      assertThat(result.identifier()).isEqualTo("a");
      assertThat(result.relations()).hasSize(1);
    }

    @Test
    @DisplayName("Should return stub leaf for already-visited node instead of re-expanding it")
    void shouldReturnStubLeafForRevisitedNode() {
      // A --(uses)--> B; B also points back to A (cycle: A→B→A)
      Entity a = entityWithRelations(TEMPLATE, "a", "A", List.of(relation("uses", TEMPLATE, "b")));
      Entity b = entityWithRelations(TEMPLATE, "b", "B", List.of(relation("uses", TEMPLATE, "a")));

      when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "a"))
          .thenReturn(Optional.of(a));
      stubGraph(Map.of(key(TEMPLATE, "a"), a, key(TEMPLATE, "b"), b));

      EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "a", 5, false);

      // A → B is resolved
      assertThat(result.relations()).hasSize(1);
      EntityGraphNode nodeB = result.relations().get(0).targets().get(0);
      assertThat(nodeB.identifier()).isEqualTo("b");

      // B → A is a revisit: A was already marked visited, so it returns a stub leaf
      // with no further outbound or inbound relations (no infinite loop).
      EntityGraphNode stubA = nodeB.relations().get(0).targets().get(0);
      assertThat(stubA.identifier()).isEqualTo("a");
      assertThat(stubA.relations()).isEmpty();
      assertThat(stubA.relationsAsTarget()).isEmpty();
    }
  }
}
