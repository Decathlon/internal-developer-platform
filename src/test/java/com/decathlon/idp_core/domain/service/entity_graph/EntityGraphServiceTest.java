package com.decathlon.idp_core.domain.service.entity_graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.decathlon.idp_core.domain.port.EntityGraphRepositoryPort;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("EntityGraphService Tests")
class EntityGraphServiceTest {

    private static final String TEMPLATE = "web-service";
    private static final String DB_TEMPLATE = "database";
    private static final String CACHE_TEMPLATE = "cache";
    private static final String INFRA_TEMPLATE = "infrastructure";
    private static final int DEFAULT_DEPTH = 3;

    @Mock
    private EntityRepositoryPort entityRepositoryPort;

    @Mock
    private EntityGraphRepositoryPort entityGraphRepositoryPort;

    @InjectMocks
    private EntityGraphService entityGraphService;

    // --- Fixtures ---

    private Entity entity(String templateIdentifier, String identifier, String name) {
        return new Entity(UUID.randomUUID(), templateIdentifier, name, identifier, List.of(), List.of());
    }

    private Entity entityWithRelations(String templateIdentifier, String identifier, String name,
            List<Relation> relations) {
        return new Entity(UUID.randomUUID(), templateIdentifier, name, identifier, List.of(), relations);
    }

    private Relation relation(String name, String targetTemplateIdentifier, List<String> targetIdentifiers) {
        return new Relation(UUID.randomUUID(), name, targetTemplateIdentifier, targetIdentifiers);
    }

    private EntityCompositeKey key(String templateIdentifier, String identifier) {
        return new EntityCompositeKey(templateIdentifier, identifier);
    }

    // --- Tests ---

    @Nested
    @DisplayName("getEntityGraph — root entity not found")
    class RootEntityNotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when root entity does not exist")
        void shouldThrowWhenRootEntityNotFound() {
            when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "missing"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> entityGraphService.getEntityGraph(TEMPLATE, "missing", DEFAULT_DEPTH))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(entityRepositoryPort).findByTemplateIdentifierAndIdentifier(TEMPLATE, "missing");
            verifyNoInteractions(entityGraphRepositoryPort);
        }
    }

    @Nested
    @DisplayName("getEntityGraph — single root, no relations")
    class SingleRootNoRelations {

        @Test
        @DisplayName("Should return a leaf node when entity has no relations")
        void shouldReturnLeafNodeWhenNoRelations() {
            var root = entity(TEMPLATE, "api", "API Service");

            when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
                    .thenReturn(Optional.of(root));
            when(entityGraphRepositoryPort.findEntityGraph(TEMPLATE, "api", DEFAULT_DEPTH))
                    .thenReturn(Map.of(key(TEMPLATE, "api"), root));

            EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", DEFAULT_DEPTH);

            assertThat(result.identifier()).isEqualTo("api");
            assertThat(result.name()).isEqualTo("API Service");
            assertThat(result.relations()).isEmpty();
            assertThat(result.relationsAsTarget()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getEntityGraph — outbound relations")
    class OutboundRelations {

        @Test
        @DisplayName("Should resolve outbound relations to graph nodes")
        void shouldResolveOutboundRelations() {
            var db = entity(DB_TEMPLATE, "postgres", "Postgres DB");
            var api = entityWithRelations(TEMPLATE, "api", "API Service",
                    List.of(relation("uses", DB_TEMPLATE, List.of("postgres"))));

            when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
                    .thenReturn(Optional.of(api));
            when(entityGraphRepositoryPort.findEntityGraph(TEMPLATE, "api", DEFAULT_DEPTH))
                    .thenReturn(Map.of(
                            key(TEMPLATE, "api"), api,
                            key(DB_TEMPLATE, "postgres"), db
                    ));

            EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", DEFAULT_DEPTH);

            assertThat(result.relations()).hasSize(1);
            assertThat(result.relations().getFirst().name()).isEqualTo("uses");
            assertThat(result.relations().getFirst().targets()).hasSize(1);
            assertThat(result.relations().getFirst().targets().getFirst().identifier()).isEqualTo("postgres");
        }

        @Test
        @DisplayName("Should create a fallback node when relation target is not in the graph map")
        void shouldReturnFallbackNodeWhenTargetNotInMap() {
            // Simulates a target entity outside the loaded depth — still produces a placeholder node
            var api = entityWithRelations(TEMPLATE, "api", "API Service",
                    List.of(relation("uses", DB_TEMPLATE, List.of("unknown-db"))));

            when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
                    .thenReturn(Optional.of(api));
            when(entityGraphRepositoryPort.findEntityGraph(TEMPLATE, "api", DEFAULT_DEPTH))
                    .thenReturn(Map.of(key(TEMPLATE, "api"), api));

            EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", DEFAULT_DEPTH);

            assertThat(result.relations()).hasSize(1);
            // Fallback node uses identifier as both id and name when entity is not in map
            assertThat(result.relations().getFirst().targets().getFirst().identifier()).isEqualTo("unknown-db");
        }
    }

    @Nested
    @DisplayName("getEntityGraph — inbound relations")
    class InboundRelations {

        @Test
        @DisplayName("Should resolve inbound relations for entities that are targeted by others")
        void shouldResolveInboundRelations() {
            var db = entity(DB_TEMPLATE, "postgres", "Postgres DB");
            var api = entityWithRelations(TEMPLATE, "api", "API Service",
                    List.of(relation("uses", DB_TEMPLATE, List.of("postgres"))));

            when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(DB_TEMPLATE, "postgres"))
                    .thenReturn(Optional.of(db));
            when(entityGraphRepositoryPort.findEntityGraph(DB_TEMPLATE, "postgres", DEFAULT_DEPTH))
                    .thenReturn(Map.of(
                            key(TEMPLATE, "api"), api,
                            key(DB_TEMPLATE, "postgres"), db
                    ));

            EntityGraphNode result = entityGraphService.getEntityGraph(DB_TEMPLATE, "postgres", DEFAULT_DEPTH);

            // postgres is targeted by api via "uses"
            assertThat(result.relationsAsTarget()).hasSize(1);
            assertThat(result.relationsAsTarget().getFirst().name()).isEqualTo("uses");
            assertThat(result.relationsAsTarget().getFirst().targets().getFirst().identifier()).isEqualTo("api");
        }
    }

    @Nested
    @DisplayName("getEntityGraph — depth clamping")
    class DepthClamping {

        @Test
        @DisplayName("Should clamp depth below 1 to 1")
        void shouldClampDepthBelowOne() {
            var root = entity(TEMPLATE, "api", "API Service");

            when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
                    .thenReturn(Optional.of(root));
            when(entityGraphRepositoryPort.findEntityGraph(TEMPLATE, "api", 1))
                    .thenReturn(Map.of(key(TEMPLATE, "api"), root));

            entityGraphService.getEntityGraph(TEMPLATE, "api", 0);

            verify(entityGraphRepositoryPort).findEntityGraph(TEMPLATE, "api", 1);
        }

        @Test
        @DisplayName("Should clamp depth above 10 to 10")
        void shouldClampDepthAboveTen() {
            var root = entity(TEMPLATE, "api", "API Service");

            when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
                    .thenReturn(Optional.of(root));
            when(entityGraphRepositoryPort.findEntityGraph(TEMPLATE, "api", 10))
                    .thenReturn(Map.of(key(TEMPLATE, "api"), root));

            entityGraphService.getEntityGraph(TEMPLATE, "api", 99);

            verify(entityGraphRepositoryPort).findEntityGraph(TEMPLATE, "api", 10);
        }
    }

    @Nested
    @DisplayName("getEntityGraph — depth limit stops recursion")
    class DepthLimit {

        @Test
        @DisplayName("Should return a leaf node for targets at the depth boundary")
        void shouldReturnLeafNodeAtDepthBoundary() {
            // api --uses--> postgres --runs-on--> server-1
            // At depth=1: postgres node is resolved but its own relations are NOT expanded
            var server = entity(INFRA_TEMPLATE, "server-1", "Server 1");
            var db = entityWithRelations(DB_TEMPLATE, "postgres", "Postgres DB",
                    List.of(relation("runs-on", INFRA_TEMPLATE, List.of("server-1"))));
            var api = entityWithRelations(TEMPLATE, "api", "API Service",
                    List.of(relation("uses", DB_TEMPLATE, List.of("postgres"))));

            when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
                    .thenReturn(Optional.of(api));
            when(entityGraphRepositoryPort.findEntityGraph(TEMPLATE, "api", 1))
                    .thenReturn(Map.of(
                            key(TEMPLATE, "api"), api,
                            key(DB_TEMPLATE, "postgres"), db,
                            key(INFRA_TEMPLATE, "server-1"), server
                    ));

            EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", 1);

            // postgres node is included but its child relations are empty (remaining depth = 0)
            var dbNode = result.relations().getFirst().targets().getFirst();
            assertThat(dbNode.identifier()).isEqualTo("postgres");
            assertThat(dbNode.relations()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getEntityGraph — multiple outbound relations")
    class MultipleRelations {

        @Test
        @DisplayName("Should resolve multiple named relation types correctly")
        void shouldResolveMultipleNamedRelations() {
            var db = entity(DB_TEMPLATE, "postgres", "Postgres DB");
            var cache = entity(CACHE_TEMPLATE, "redis", "Redis Cache");
            var api = entityWithRelations(TEMPLATE, "api", "API Service", List.of(
                    relation("uses-db", DB_TEMPLATE, List.of("postgres")),
                    relation("uses-cache", CACHE_TEMPLATE, List.of("redis"))
            ));

            when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier(TEMPLATE, "api"))
                    .thenReturn(Optional.of(api));
            when(entityGraphRepositoryPort.findEntityGraph(TEMPLATE, "api", DEFAULT_DEPTH))
                    .thenReturn(Map.of(
                            key(TEMPLATE, "api"), api,
                            key(DB_TEMPLATE, "postgres"), db,
                            key(CACHE_TEMPLATE, "redis"), cache
                    ));

            EntityGraphNode result = entityGraphService.getEntityGraph(TEMPLATE, "api", DEFAULT_DEPTH);

            assertThat(result.relations()).hasSize(2);
            var relationNames = result.relations().stream().map(r -> r.name()).toList();
            assertThat(relationNames).containsExactlyInAnyOrder("uses-db", "uses-cache");
        }
    }
}
