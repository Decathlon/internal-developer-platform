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
}
