package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import com.decathlon.idp_core.AbstractIntegrationTest;
import com.decathlon.idp_core.domain.service.entity_graph.EntityGraphService;
import com.decathlon.idp_core.infrastructure.adapters.persistence.PostgresEntityAdapter;
import com.decathlon.idp_core.infrastructure.adapters.persistence.PostgresEntityGraphAdapter;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaEntityRepository;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.instrumentation.annotations.WithSpan;

/// Verifies annotated service and persistence spans keep the incoming trace
/// context when requests reach repository calls.
@Sql(scripts = "/db/test/R__3_Insert_graph_entities_test_data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class EntityTracingIntegrationTest extends AbstractIntegrationTest {

  private static final String GRAPH_PATH = "/api/v1/entities/{templateId}/{entityId}/graph";
  private static final String ENTITY_PATH = "/api/v1/entities/{templateId}/{entityId}";
  private static final String TRACE_ID = "0123456789abcdef0123456789abcdef";
  private static final String PARENT_SPAN_ID = "0123456789abcdef";

  @Autowired
  private MockMvc mockMvc;

  @MockitoSpyBean
  private JpaEntityRepository jpaEntityRepository;

  @Test
  @WithMockUser
  void getEntityKeepsTraceContextThroughServiceAndRepositoryAnnotations() throws Exception {
    assertWithSpanAnnotation(EntityGraphService.class
        .getMethod("getEntityGraph", String.class, String.class,
            com.decathlon.idp_core.domain.model.entity_graph.EntityGraphQueryOptions.class)
        .getAnnotation(WithSpan.class), "EntityGraphService.getEntityGraph");
    assertWithSpanAnnotation(
        PostgresEntityAdapter.class.getMethod("findSummariesByCompositeKeys", java.util.List.class)
            .getAnnotation(WithSpan.class),
        "PostgresEntityAdapter.findSummariesByCompositeKeys");
    assertWithSpanAnnotation(PostgresEntityGraphAdapter.class
        .getMethod("findEntityGraph", java.util.Collection.class, int.class, boolean.class,
            boolean.class,
            com.decathlon.idp_core.domain.model.entity_graph.EntityGraphTraversalMode.class)
        .getAnnotation(WithSpan.class), "PostgresEntityGraphAdapter.findEntityGraph");

    SpanContext repositoryContext = captureRepositoryContexts(
        () -> mockMvc
            .perform(get(ENTITY_PATH, "web-service", "graph-svc-a")
                .header("traceparent", traceparent()).accept(APPLICATION_JSON))
            .andExpect(status().isOk()));

    assertTrue(repositoryContext.isValid());
    assertEquals(TRACE_ID, repositoryContext.getTraceId());
  }

  @Test
  @WithMockUser
  void graphKeepsTraceContextThroughServiceAndRepositoryAnnotations() throws Exception {
    SpanContext repositoryContext = captureRepositoryContexts(
        () -> mockMvc
            .perform(get(GRAPH_PATH, "web-service", "graph-svc-a")
                .header("traceparent", traceparent()).accept(APPLICATION_JSON))
            .andExpect(status().isOk()));

    assertTrue(repositoryContext.isValid());
    assertEquals(TRACE_ID, repositoryContext.getTraceId());
  }

  @Test
  @WithMockUser
  void missingEntityStillReturnsNotFoundThroughAnnotatedService() throws Exception {
    mockMvc
        .perform(get(GRAPH_PATH, "web-service", "missing-entity")
            .header("traceparent", traceparent()).accept(APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  private SpanContext captureRepositoryContexts(ThrowingRequest request) throws Exception {
    AtomicReference<SpanContext> summaryContext = new AtomicReference<>();
    AtomicReference<SpanContext> graphContext = new AtomicReference<>();

    doAnswer(invocation -> {
      summaryContext.set(Span.current().getSpanContext());
      return invocation.callRealMethod();
    }).when(jpaEntityRepository).findByCompositeKeys(any(String[].class), any(String[].class));
    doAnswer(invocation -> {
      graphContext.set(Span.current().getSpanContext());
      return invocation.callRealMethod();
    }).when(jpaEntityRepository).findEntityIdsInGraph(anyCollection(), anyInt(), anyString());

    request.perform();

    assertTrue(summaryContext.get().isValid());
    assertTrue(graphContext.get().isValid());
    assertEquals(summaryContext.get().getTraceId(), graphContext.get().getTraceId());
    return graphContext.get();
  }

  private static void assertWithSpanAnnotation(WithSpan annotation, String expectedName) {
    assertTrue(annotation != null);
    assertEquals(expectedName, annotation.value());
  }

  private static String traceparent() {
    return "00-" + TRACE_ID + "-" + PARENT_SPAN_ID + "-01";
  }

  @FunctionalInterface
  private interface ThrowingRequest {
    void perform() throws Exception;
  }
}
