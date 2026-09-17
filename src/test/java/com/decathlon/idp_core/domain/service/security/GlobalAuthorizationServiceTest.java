package com.decathlon.idp_core.domain.service.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;
import com.decathlon.idp_core.domain.model.principal.PrincipalKind;
import com.decathlon.idp_core.domain.model.security.Action;
import com.decathlon.idp_core.domain.port.AuthorizationPolicyPort;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;

@ExtendWith(MockitoExtension.class)
class GlobalAuthorizationServiceTest {

  @Mock
  private AuthorizationPolicyPort authorizationPolicyPort;

  @Mock
  private EntityRepositoryPort entityRepositoryPort;

  private GlobalAuthorizationService service;

  @BeforeEach
  void setUp() {
    when(authorizationPolicyPort.globalPrincipalIdentifiers()).thenReturn(Set.of("break-glass"));
    service = new GlobalAuthorizationService(authorizationPolicyPort, entityRepositoryPort);
  }

  @Test
  void grantsFullAccessToBreakGlassPrincipal() {
    PrincipalInfo principal = principal("break-glass", PrincipalKind.HUMAN);

    assertThat(service.isAuthorizedInStep1(principal, Action.DELETE)).isTrue();
  }

  @Test
  void grantsFullAccessToBooleanAdmin() {
    when(entityRepositoryPort.findByTemplateIdentifierAndIdentifier("principal", "admin"))
        .thenReturn(java.util.Optional.of(new Entity(null, "principal", "Admin", "admin",
            List.of(new Property(null, "is_admin", "true")), List.of())));

    assertThat(service.isAuthorizedInStep1(principal("admin", PrincipalKind.HUMAN), Action.UPDATE))
        .isTrue();
  }

  @Test
  void grantsReadOnlyAccessToServiceAccountByDefault() {
    assertThat(service.isAuthorizedInStep1(principal("service", PrincipalKind.SERVICE_ACCOUNT),
        Action.CREATE)).isFalse();
    assertThat(service.isAuthorizedInStep1(principal("service", PrincipalKind.SERVICE_ACCOUNT),
        Action.READ)).isTrue();
  }

  @Test
  void grantsReadOnlyAccessToHuman() {
    PrincipalInfo principal = principal("human", PrincipalKind.HUMAN);

    assertThat(service.isAuthorizedInStep1(principal, Action.READ)).isTrue();
    assertThat(service.isAuthorizedInStep1(principal, Action.DELETE)).isFalse();
  }

  private PrincipalInfo principal(String identifier, PrincipalKind kind) {
    return new PrincipalInfo(identifier, kind, identifier, Map.of(), List.of());
  }
}
