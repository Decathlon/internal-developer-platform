package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.decathlon.idp_core.domain.exception.authorization.PrincipalNotAuthorizedException;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationAction;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationResource;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.model.entity_mapping.MappingAction;
import com.decathlon.idp_core.domain.service.entity_dynamic_mapping.EntityDynamicMappingDryRunService;
import com.decathlon.idp_core.domain.service.entity_dynamic_mapping.EntityDynamicMappingService;
import com.decathlon.idp_core.infrastructure.adapters.api.auth.RequestAuthorizer;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity_dynamic_mapping.EntityDynamicMappingDryRunDtoInMapper;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity_dynamic_mapping.EntityDynamicMappingDryRunDtoOutMapper;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity_dynamic_mapping.EntityDynamicMappingMapper;

@ExtendWith(MockitoExtension.class)
class EntityDynamicMappingDeletionAuthorizationTest {

  @Mock
  private EntityDynamicMappingMapper dynamicMappingMapper;

  @Mock
  private EntityDynamicMappingService dynamicMappingService;

  @Mock
  private EntityDynamicMappingDryRunService dynamicMappingDryRunService;

  @Mock
  private EntityDynamicMappingDryRunDtoOutMapper entityDynamicMappingDryRunDtoOutMapper;

  @Mock
  private EntityDynamicMappingDryRunDtoInMapper entityDynamicMappingDryRunDtoInMapper;

  @Mock
  private RequestAuthorizer requestAuthorizer;

  @Mock
  private HttpServletRequest request;

  @InjectMocks
  private EntityDynamicMappingController controller;

  @Test
  void shouldAuthorizeResolvedPrincipalTargetBeforeDeletingMapping() {
    when(dynamicMappingService.getEntityDynamicMapping("principal-mapping"))
        .thenReturn(mapping("principal-mapping", AuthorizationResource.PRINCIPAL_TEMPLATE_IDENTIFIER));
    doThrow(new PrincipalNotAuthorizedException("service-account")).when(requestAuthorizer)
        .authorize(eq(request), eq(AuthorizationAction.DELETE), any(AuthorizationResource.class));

    assertThatThrownBy(
        () -> controller.deleteEntityDynamicMapping("principal-mapping", request))
            .isInstanceOf(PrincipalNotAuthorizedException.class);

    verify(requestAuthorizer).authorize(eq(request), eq(AuthorizationAction.DELETE),
        argThat(resource -> resource.type().equals(AuthorizationResource.ENTITY_DYNAMIC_MAPPING)
            && resource.identifier().filter("principal-mapping"::equals).isPresent()
            && resource.parentIdentifier()
                .filter(AuthorizationResource.PRINCIPAL_TEMPLATE_IDENTIFIER::equals).isPresent()));
    verify(dynamicMappingService, never()).deleteEntityDynamicMapping(any());
  }

  @Test
  void shouldDeleteMappingWithoutPrincipalAuthorizationWhenTargetIsNotPrincipal() {
    when(dynamicMappingService.getEntityDynamicMapping("regular-mapping"))
        .thenReturn(mapping("regular-mapping", "microservice"));

    controller.deleteEntityDynamicMapping("regular-mapping", request);

    verify(requestAuthorizer, never()).authorize(any(), any(), any());
    verify(dynamicMappingService).deleteEntityDynamicMapping("regular-mapping");
  }

  private EntityDynamicMapping mapping(String identifier, String targetTemplate) {
    return new EntityDynamicMapping(UUID.randomUUID(), identifier, targetTemplate, ".action == 'update'",
        MappingAction.UPDATE_ENTITY, "Mapping", null, ".entity.id", ".entity.name", Map.of(),
        List.of());
  }
}
