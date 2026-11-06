package com.decathlon.idp_core.infrastructure.adapters.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphTraversalMode;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.EntityGraphJsonProjection;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaEntityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;

@ExtendWith(MockitoExtension.class)
class PostgresEntityGraphAdapterTracingTest {

  @RegisterExtension
  private static final OpenTelemetryExtension OPEN_TELEMETRY_EXTENSION = OpenTelemetryExtension
      .create();

  @Mock
  private JpaEntityRepository jpaEntityRepository;

  private PostgresEntityGraphAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new PostgresEntityGraphAdapter(jpaEntityRepository, new ObjectMapper(),
        OPEN_TELEMETRY_EXTENSION.getOpenTelemetry());
  }

  @Test
  void createsNamedChildSpansAndKeepsRepositoryCallsInTheirContext() {
    UUID entityId = UUID.randomUUID();
    EntityGraphJsonProjection projection = projection(entityId);
    AtomicReference<SpanContext> idsQueryContext = new AtomicReference<>();
    AtomicReference<SpanContext> graphDataQueryContext = new AtomicReference<>();
    when(jpaEntityRepository.findEntityIdsInGraph(anyCollection(), anyInt(), anyString()))
        .thenAnswer(invocation -> {
          idsQueryContext.set(Span.current().getSpanContext());
          return List.of(entityId);
        });
    when(jpaEntityRepository.findEntityGraphDataByIds(anyCollection(), anyCollection(),
        anyBoolean(), anyBoolean())).thenAnswer(invocation -> {
          graphDataQueryContext.set(Span.current().getSpanContext());
          return List.of(projection);
        });

    Span parentSpan = OPEN_TELEMETRY_EXTENSION.getOpenTelemetry().getTracer("test")
        .spanBuilder("test.request").startSpan();
    SpanContext parentContext = parentSpan.getSpanContext();
    try (Scope scope = parentSpan.makeCurrent()) {
      adapter.findEntityGraph(List.of(entityId), 2, true, false,
          EntityGraphTraversalMode.OUTBOUND_ONLY);
    } finally {
      parentSpan.end();
    }

    List<SpanData> spans = OPEN_TELEMETRY_EXTENSION.getSpans();
    assertThat(spans).extracting(SpanData::getName).containsExactlyInAnyOrder("test.request",
        "entity_graph.database.find_entity_ids", "entity_graph.database.find_graph_data",
        "entity_graph.projection_mapping");
    assertRepositoryCallUsesSpan(spans, "entity_graph.database.find_entity_ids",
        idsQueryContext.get(), parentContext);
    assertRepositoryCallUsesSpan(spans, "entity_graph.database.find_graph_data",
        graphDataQueryContext.get(), parentContext);
    assertThat(spans).allSatisfy(span -> assertThat(span.getAttributes().size()).isZero());
  }

  @Test
  void marksDatabaseOperationFailedAndRecordsExceptionWithoutAddingAttributes() {
    IllegalStateException databaseFailure = new IllegalStateException("database failure");
    when(jpaEntityRepository.findEntityIdsInGraph(anyCollection(), anyInt(), anyString()))
        .thenThrow(databaseFailure);

    assertThatThrownBy(() -> adapter.findEntityGraph(List.of(UUID.randomUUID()), 1, true, false,
        EntityGraphTraversalMode.OUTBOUND_ONLY)).isSameAs(databaseFailure);

    SpanData failedSpan = OPEN_TELEMETRY_EXTENSION.getSpans().getFirst();
    assertThat(failedSpan.getName()).isEqualTo("entity_graph.database.find_entity_ids");
    assertThat(failedSpan.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
    assertThat(failedSpan.getEvents())
        .anySatisfy(event -> assertThat(event.getName()).isEqualTo("exception"));
    assertThat(failedSpan.getAttributes().size()).isZero();
  }

  private static EntityGraphJsonProjection projection(UUID id) {
    EntityGraphJsonProjection projection = mock(EntityGraphJsonProjection.class);
    when(projection.getId()).thenReturn(id);
    when(projection.getTemplateIdentifier()).thenReturn("test-template");
    when(projection.getName()).thenReturn("Test Entity");
    when(projection.getIdentifier()).thenReturn("test-entity");
    when(projection.getPropertiesJson()).thenReturn("[]");
    when(projection.getRelationsJson()).thenReturn("[]");
    return projection;
  }

  private static void assertRepositoryCallUsesSpan(List<SpanData> spans, String spanName,
      SpanContext repositoryCallContext, SpanContext parentContext) {
    assertThat(repositoryCallContext).isNotNull();
    assertThat(repositoryCallContext.getTraceId()).isEqualTo(parentContext.getTraceId());
    SpanData childSpan = spans.stream().filter(span -> span.getName().equals(spanName)).findFirst()
        .orElseThrow();
    assertThat(repositoryCallContext.getSpanId()).isEqualTo(childSpan.getSpanContext().getSpanId());
    assertThat(childSpan.getParentSpanId()).isEqualTo(parentContext.getSpanId());
  }
}
