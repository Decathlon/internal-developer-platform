package com.decathlon.idp_core.infrastructure.adapters.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.decathlon.idp_core.domain.model.authorization.AuthorizationAction;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationResource;
import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;
import com.decathlon.idp_core.domain.model.principal.PrincipalKind;

class AuthorizationRequestFactoryTest {

  private final AuthorizationRequestFactory factory = new AuthorizationRequestFactory(
      TestHandlerMappings.controllers());
  private final PrincipalInfo principal = new PrincipalInfo("subject-123", PrincipalKind.HUMAN,
      "User", Map.of(), List.of());

  @Test
  void shouldMapPostSearchToRead() {
    var authorizationRequest = create("POST", "/api/v1/entities/search");

    assertThat(authorizationRequest.action()).isEqualTo(AuthorizationAction.READ);
    assertThat(authorizationRequest.resource().type()).isEqualTo("entity_search");
  }

  @Test
  void shouldMapDynamicMappingDryRunToRead() {
    var authorizationRequest = create("POST", "/api/v1/entity_dynamic_mappings/dry-run");

    assertThat(authorizationRequest.action()).isEqualTo(AuthorizationAction.READ);
    assertThat(authorizationRequest.resource().type()).isEqualTo("entity_dynamic_mapping_dry_run");
  }

  @Test
  void shouldMapWebhookConfigurationCreationSeparately() {
    var authorizationRequest = create("POST", "/api/v1/inbound_webhooks");

    assertThat(authorizationRequest.action()).isEqualTo(AuthorizationAction.CREATE);
    assertThat(authorizationRequest.resource().type())
        .isEqualTo(AuthorizationResource.INBOUND_WEBHOOK_CONFIGURATION);
  }

  @Test
  void shouldMapCatalogEntityPathContext() {
    var authorizationRequest = create("DELETE", "/api/v1/entities/principal/user-123");

    assertThat(authorizationRequest.action()).isEqualTo(AuthorizationAction.DELETE);
    assertThat(authorizationRequest.resource().type()).isEqualTo("entity");
    assertThat(authorizationRequest.resource().identifier()).contains("user-123");
    assertThat(authorizationRequest.resource().parentIdentifier()).contains("principal");
  }

  @Test
  void shouldMapAuditPathVariablesByName() {
    var authorizationRequest = create("GET", "/api/v1/audit/entities/principal/user-123");

    assertThat(authorizationRequest.resource().type()).isEqualTo("audit");
    assertThat(authorizationRequest.resource().identifier()).contains("user-123");
    assertThat(authorizationRequest.resource().parentIdentifier()).contains("principal");
  }

  @Test
  void shouldMapTemplateIdentifier() {
    var authorizationRequest = create("PUT", "/api/v1/entity-templates/service");

    assertThat(authorizationRequest.action()).isEqualTo(AuthorizationAction.UPDATE);
    assertThat(authorizationRequest.resource().type()).isEqualTo("entity_template");
    assertThat(authorizationRequest.resource().identifier()).contains("service");
  }

  @Test
  void shouldUseUnknownResourceWhenNoHandlerMatches() {
    var authorizationRequest = create("GET", "/actuator/health");

    assertThat(authorizationRequest.action()).isEqualTo(AuthorizationAction.READ);
    assertThat(authorizationRequest.resource().type()).isEqualTo("unknown");
  }

  @Test
  void shouldRejectUnknownHttpMethods() {
    var authorizationRequest = create("TRACE", "/api/v1/entities");

    assertThat(authorizationRequest.action()).isEqualTo(AuthorizationAction.UNSUPPORTED);
  }

  private com.decathlon.idp_core.domain.model.authorization.AuthorizationRequest create(
      String method, String path) {
    return factory.create(new MockHttpServletRequest(method, path), principal, Optional.empty());
  }
}
