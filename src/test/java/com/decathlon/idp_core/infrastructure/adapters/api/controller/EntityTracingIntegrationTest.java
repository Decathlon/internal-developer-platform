package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
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
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphNode;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphQueryOptions;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphTraversalMode;
import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;
import com.decathlon.idp_core.domain.service.entity_graph.EntityGraphHelper;
import com.decathlon.idp_core.domain.service.entity_graph.EntityGraphService;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateService;
import com.decathlon.idp_core.domain.service.principal.PrincipalProvisioningService;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity.EntityDtoOutFromEntityNodeMapper;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity_graph.EntityGraphFlatDtoOutMapper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.PostgresEntityAdapter;
import com.decathlon.idp_core.infrastructure.adapters.persistence.PostgresEntityGraphAdapter;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

/// Verifies service, graph-processing, persistence, and mapper spans keep the
/// incoming trace context for entity and graph requests.
@Sql(scripts = "/db/test/R__3_Insert_graph_entities_test_data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class EntityTracingIntegrationTest extends AbstractIntegrationTest {

  private static final String GRAPH_PATH = "/api/v1/entities/{templateId}/{entityId}/graph";
  private static final String ENTITY_PATH = "/api/v1/entities/{templateId}/{entityId}";
  private static final String TRACE_ID = "0123456789abcdef0123456789abcdef";
  private static final String PARENT_SPAN_ID = "0123456789abcdef";

  @Autowired
  private MockMvc mockMvc;

  @MockitoSpyBean
  private EntityGraphService entityGraphService;

  @MockitoSpyBean
  private PrincipalProvisioningService principalProvisioningService;

  @MockitoSpyBean
  private EntityTemplateService entityTemplateService;

  @MockitoSpyBean
  private EntityGraphHelper entityGraphHelper;

  @MockitoSpyBean
  private PostgresEntityAdapter postgresEntityAdapter;

  @MockitoSpyBean
  private PostgresEntityGraphAdapter postgresEntityGraphAdapter;

  @MockitoSpyBean
  private EntityDtoOutFromEntityNodeMapper entityDtoOutFromEntityNodeMapper;

  @MockitoSpyBean
  private EntityGraphFlatDtoOutMapper entityGraphFlatDtoOutMapper;

  @Test
  @WithMockUser
  void getEntityKeepsTraceContextAcrossProcessingPersistenceAndMappingSpans() throws Exception {
    TraceContexts traceContexts = captureTraceContexts(
        () -> mockMvc
            .perform(get(ENTITY_PATH, "web-service", "graph-svc-a")
                .header("traceparent", traceparent()).accept(APPLICATION_JSON))
            .andExpect(status().isOk()));

    assertContextsShareTrace(traceContexts);
  }

  @Test
  @WithMockUser
  void graphKeepsTraceContextAcrossProcessingPersistenceAndMappingSpans() throws Exception {
    TraceContexts traceContexts = captureTraceContexts(
        () -> mockMvc
            .perform(get(GRAPH_PATH, "web-service", "graph-svc-a")
                .header("traceparent", traceparent()).accept(APPLICATION_JSON))
            .andExpect(status().isOk()));

    assertContextsShareTrace(traceContexts);
  }

  @Test
  @WithMockUser
  void missingEntityStillReturnsNotFoundThroughAnnotatedService() throws Exception {
    AtomicReference<SpanContext> serviceContext = new AtomicReference<>();
    doAnswer(invocation -> {
      serviceContext.set(Span.current().getSpanContext());
      return invocation.callRealMethod();
    }).when(entityGraphService).getEntityGraph(anyString(), anyString(),
        any(EntityGraphQueryOptions.class));

    mockMvc
        .perform(get(GRAPH_PATH, "web-service", "missing-entity")
            .header("traceparent", traceparent()).accept(APPLICATION_JSON))
        .andExpect(status().isNotFound());

    assertTrue(serviceContext.get().isValid());
    assertEquals(TRACE_ID, serviceContext.get().getTraceId());
  }

  private TraceContexts captureTraceContexts(ThrowingRequest request) throws Exception {
    AtomicReference<SpanContext> principalProvisioningContext = new AtomicReference<>();
    AtomicReference<SpanContext> principalValidationContext = new AtomicReference<>();
    AtomicReference<SpanContext> principalEntityLookupContext = new AtomicReference<>();
    AtomicReference<SpanContext> serviceContext = new AtomicReference<>();
    AtomicReference<SpanContext> helperContext = new AtomicReference<>();
    AtomicReference<SpanContext> summaryContext = new AtomicReference<>();
    AtomicReference<SpanContext> graphContext = new AtomicReference<>();
    AtomicReference<SpanContext> mapperContext = new AtomicReference<>();
    AtomicReference<SpanContext> templateLookupContext = new AtomicReference<>();

    doAnswer(invocation -> {
      principalProvisioningContext.set(Span.current().getSpanContext());
      return invocation.callRealMethod();
    }).when(principalProvisioningService).provisionPrincipal(any(PrincipalInfo.class));
    doAnswer(invocation -> {
      principalValidationContext.set(Span.current().getSpanContext());
      return invocation.callRealMethod();
    }).when(principalProvisioningService).findPrincipalWithSameKind(any(PrincipalInfo.class));
    doAnswer(invocation -> {
      principalEntityLookupContext.set(Span.current().getSpanContext());
      return invocation.callRealMethod();
    }).when(postgresEntityAdapter).findByTemplateIdentifierAndIdentifier(anyString(), anyString());
    doAnswer(invocation -> {
      serviceContext.set(Span.current().getSpanContext());
      return invocation.callRealMethod();
    }).when(entityGraphService).getEntityGraph(anyString(), anyString(),
        any(EntityGraphQueryOptions.class));
    doAnswer(invocation -> {
      helperContext.set(Span.current().getSpanContext());
      return invocation.callRealMethod();
    }).when(entityGraphHelper).buildGraphNodesForEntityIds(anyMap(), anySet(), anySet(), any(),
        anyInt());
    doAnswer(invocation -> {
      summaryContext.set(Span.current().getSpanContext());
      return invocation.callRealMethod();
    }).when(postgresEntityAdapter).findSummariesByCompositeKeys(anyList());
    doAnswer(invocation -> {
      graphContext.set(Span.current().getSpanContext());
      return invocation.callRealMethod();
    }).when(postgresEntityGraphAdapter).findEntityGraph(anyCollection(), anyInt(), anyBoolean(),
        anyBoolean(), any());
    doAnswer(invocation -> {
      mapperContext.set(Span.current().getSpanContext());
      return invocation.callRealMethod();
    }).when(entityDtoOutFromEntityNodeMapper).toDto(any(EntityGraphNode.class), anyString(),
        anyInt());
    doAnswer(invocation -> {
      mapperContext.set(Span.current().getSpanContext());
      return invocation.callRealMethod();
    }).when(entityGraphFlatDtoOutMapper).toFlatDto(any(EntityGraphNode.class));
    doAnswer(invocation -> {
      templateLookupContext.set(Span.current().getSpanContext());
      return invocation.callRealMethod();
    }).when(entityTemplateService).getEntityTemplateByIdentifier(anyString());

    request.perform();

    return new TraceContexts(principalProvisioningContext.get(), principalValidationContext.get(),
        principalEntityLookupContext.get(), serviceContext.get(), helperContext.get(),
        summaryContext.get(), graphContext.get(), mapperContext.get(), templateLookupContext.get());
  }

  private static void assertContextsShareTrace(TraceContexts traceContexts) {
    SpanContext[] contexts = {traceContexts.principalProvisioning(),
        traceContexts.principalValidation(), traceContexts.principalEntityLookup(),
        traceContexts.service(), traceContexts.helper(), traceContexts.summaryRepository(),
        traceContexts.graphRepository(), traceContexts.mapper()};
    for (SpanContext context : contexts) {
      assertTrue(context.isValid());
      assertEquals(TRACE_ID, context.getTraceId());
    }
    if (traceContexts.templateLookup() != null) {
      assertTrue(traceContexts.templateLookup().isValid());
      assertEquals(TRACE_ID, traceContexts.templateLookup().getTraceId());
    }
    for (int first = 0; first < contexts.length; first++) {
      for (int second = first + 1; second < contexts.length; second++) {
        assertNotEquals(contexts[first].getSpanId(), contexts[second].getSpanId());
      }
    }
    if (traceContexts.templateLookup() != null) {
      for (SpanContext context : contexts) {
        assertNotEquals(context.getSpanId(), traceContexts.templateLookup().getSpanId());
      }
    }
  }

  private static String traceparent() {
    return "00-" + TRACE_ID + "-" + PARENT_SPAN_ID + "-01";
  }

  private record TraceContexts(SpanContext principalProvisioning, SpanContext principalValidation,
      SpanContext principalEntityLookup, SpanContext service, SpanContext helper,
      SpanContext summaryRepository, SpanContext graphRepository, SpanContext mapper,
      SpanContext templateLookup) {
  }

  @FunctionalInterface
  private interface ThrowingRequest {
    void perform() throws Exception;
  }
}
