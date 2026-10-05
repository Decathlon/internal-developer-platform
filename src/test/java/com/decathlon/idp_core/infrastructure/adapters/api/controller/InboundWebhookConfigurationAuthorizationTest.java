package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookSecurity;
import com.decathlon.idp_core.domain.service.webhook.WebhookConnectorService;
import com.decathlon.idp_core.infrastructure.adapters.api.auth.RequestAuthorizer;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.InboundWebhookSecurityContractDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.InboundWebhookUpdateDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.connector.webhook.InboundWebhookMapper;

@ExtendWith(MockitoExtension.class)
class InboundWebhookConfigurationAuthorizationTest {

  @Mock
  private WebhookConnectorService webhookConnectorService;

  @Mock
  private InboundWebhookMapper inboundWebhookMapper;

  @Mock
  private RequestAuthorizer requestAuthorizer;

  @Mock
  private HttpServletRequest request;

  @InjectMocks
  private InboundWebhookConfigurationController controller;

  @Test
  void shouldAuthorizeConnectorUpdatesThatAttachPrincipalMappingsBeforePersistence() {
    var mapping = new EntityDynamicMapping(UUID.randomUUID(), "principal-mapping",
        AuthorizationResource.PRINCIPAL_TEMPLATE_IDENTIFIER, ".action == 'update'",
        MappingAction.UPDATE_ENTITY, "Principal mapping", null, ".user.id", ".user.name",
        Map.of("is_admin", "true"), List.of());
    when(webhookConnectorService.resolveAndValidateMappings(List.of("principal-mapping")))
        .thenReturn(List.of(mapping));
    doThrow(new PrincipalNotAuthorizedException("service-account")).when(requestAuthorizer)
        .authorize(eq(request), eq(AuthorizationAction.UPDATE), any(AuthorizationResource.class));

    var update = new InboundWebhookUpdateDtoIn("connector", "update", true,
        List.of("principal-mapping"), new InboundWebhookSecurityContractDtoIn("NONE", Map.of()));

    assertThatThrownBy(() -> controller.putWebhookConnector("connector", update, request))
        .isInstanceOf(PrincipalNotAuthorizedException.class);

    verify(requestAuthorizer).authorize(eq(request), eq(AuthorizationAction.UPDATE),
        org.mockito.ArgumentMatchers.argThat(
            resource -> resource.type().equals(AuthorizationResource.INBOUND_WEBHOOK_CONFIGURATION)
                && resource.identifier().filter("connector"::equals).isPresent()
                && resource.parentIdentifier()
                    .filter(AuthorizationResource.PRINCIPAL_TEMPLATE_IDENTIFIER::equals)
                    .isPresent()));
    verify(webhookConnectorService, never()).updateWebhookConnector(any(), any());
  }

  @Test
  void shouldAuthorizeResolvedPrincipalMappingsBeforeDeletingConnector() {
    var mapping = mapping("principal-mapping", AuthorizationResource.PRINCIPAL_TEMPLATE_IDENTIFIER);
    when(webhookConnectorService.getWebhookConnector("connector"))
        .thenReturn(connector("connector", List.of(mapping)));
    doThrow(new PrincipalNotAuthorizedException("service-account")).when(requestAuthorizer)
        .authorize(eq(request), eq(AuthorizationAction.DELETE), any(AuthorizationResource.class));

    assertThatThrownBy(() -> controller.deleteWebhookConnector("connector", request))
        .isInstanceOf(PrincipalNotAuthorizedException.class);

    verify(requestAuthorizer).authorize(eq(request), eq(AuthorizationAction.DELETE),
        org.mockito.ArgumentMatchers.argThat(
            resource -> resource.type().equals(AuthorizationResource.INBOUND_WEBHOOK_CONFIGURATION)
                && resource.identifier().filter("connector"::equals).isPresent()
                && resource.parentIdentifier()
                    .filter(AuthorizationResource.PRINCIPAL_TEMPLATE_IDENTIFIER::equals)
                    .isPresent()));
    verify(webhookConnectorService, never()).deleteWebhookConnector(any());
  }

  @Test
  void shouldDeleteConnectorWithoutPrincipalAuthorizationWhenMappingsAreNotPrincipalTargeted() {
    when(webhookConnectorService.getWebhookConnector("connector"))
        .thenReturn(connector("connector", List.of(mapping("regular-mapping", "microservice"))));

    controller.deleteWebhookConnector("connector", request);

    verify(requestAuthorizer, never()).authorize(any(), any(), any());
    verify(webhookConnectorService).deleteWebhookConnector("connector");
  }

  private EntityDynamicMapping mapping(String identifier, String targetTemplate) {
    return new EntityDynamicMapping(UUID.randomUUID(), identifier, targetTemplate,
        ".action == 'update'", MappingAction.UPDATE_ENTITY, "Mapping", null, ".entity.id",
        ".entity.name", Map.of(), List.of());
  }

  private WebhookConnector connector(String identifier, List<EntityDynamicMapping> mappings) {
    return new WebhookConnector(UUID.randomUUID(), identifier, "Connector", null, true, mappings,
        new WebhookSecurity(WebhookSecurityType.NONE, Map.of()));
  }
}
