package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

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
import com.decathlon.idp_core.infrastructure.adapters.persistence.mapper.EntityPersistenceMapper;
import com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity.EntityJpaEntity;
import com.decathlon.idp_core.infrastructure.adapters.persistence.repository.JpaEntityRepository;

import io.opentelemetry.instrumentation.annotations.WithSpan;

/// Verifies application spans have stable names around database and processing work.
class EntityTracingAnnotationTest {

  @Test
  void databaseAndProcessingMethodsHaveExplicitSpanNames() throws NoSuchMethodException {
    assertSpan(
        PrincipalProvisioningService.class.getMethod("provisionPrincipal", PrincipalInfo.class),
        "PrincipalProvisioningService.provisionPrincipal");
    assertSpan(PrincipalProvisioningService.class.getMethod("provisionGlobalAdministrator",
        PrincipalInfo.class), "PrincipalProvisioningService.provisionGlobalAdministrator");
    assertSpan(PrincipalProvisioningService.class.getMethod("findPrincipalWithSameKind",
        PrincipalInfo.class), "PrincipalProvisioningService.findPrincipalWithSameKind");
    assertSpan(PostgresEntityAdapter.class.getMethod("findByTemplateIdentifierAndIdentifier",
        String.class, String.class), "PostgresEntityAdapter.findByTemplateIdentifierAndIdentifier");
    assertSpan(JpaEntityRepository.class.getMethod("findByTemplateIdentifierAndIdentifier",
        String.class, String.class), "JpaEntityRepository.findByTemplateIdentifierAndIdentifier");
    assertSpan(EntityPersistenceMapper.class.getMethod("toDomain", EntityJpaEntity.class),
        "EntityPersistenceMapper.toDomain");
    assertSpan(PostgresEntityAdapter.class.getMethod("findSummariesByCompositeKeys", List.class),
        "PostgresEntityAdapter.findSummariesByCompositeKeys");
    assertSpan(
        PostgresEntityGraphAdapter.class.getMethod("findEntityGraph", Collection.class, int.class,
            boolean.class, boolean.class, EntityGraphTraversalMode.class),
        "PostgresEntityGraphAdapter.findEntityGraph");
    assertSpan(EntityGraphService.class.getMethod("getEntityGraph", String.class, String.class,
        EntityGraphQueryOptions.class), "EntityGraphService.getEntityGraph");
    assertSpan(
        EntityGraphHelper.class.getMethod("buildGraphNodesForEntityIds", Map.class, Set.class,
            Set.class, EntityGraphTraversalMode.class, int.class),
        "EntityGraphHelper.buildGraphNodesForEntityIds");
    assertSpan(EntityTemplateService.class.getMethod("getEntityTemplateByIdentifier", String.class),
        "EntityTemplateService.getEntityTemplateByIdentifier");
    assertSpan(EntityDtoOutFromEntityNodeMapper.class.getMethod("toDto", EntityGraphNode.class,
        String.class, int.class), "EntityDtoOutFromEntityNodeMapper.toDto");
    assertSpan(EntityGraphFlatDtoOutMapper.class.getMethod("toFlatDto", EntityGraphNode.class),
        "EntityGraphFlatDtoOutMapper.toFlatDto");
  }

  private static void assertSpan(java.lang.reflect.Method method, String expectedName) {
    WithSpan annotation = method.getAnnotation(WithSpan.class);
    assertNotNull(annotation);
    assertEquals(expectedName, annotation.value());
  }
}
