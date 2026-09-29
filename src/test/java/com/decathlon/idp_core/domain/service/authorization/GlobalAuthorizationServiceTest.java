package com.decathlon.idp_core.domain.service.authorization;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.exception.authorization.PrincipalNotAuthorizedException;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationMode;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationPolicy;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationRequest;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;
import com.decathlon.idp_core.domain.model.principal.PrincipalKind;

class GlobalAuthorizationServiceTest {

  private static final String PRINCIPAL_IDENTIFIER = "platform-user";

  private final GlobalAuthorizationService authorizationService = new GlobalAuthorizationService();
  private AuthorizationPolicy policy;
  private PrincipalInfo humanPrincipal;

  @BeforeEach
  void setUp() {
    policy = new AuthorizationPolicy(AuthorizationMode.GLOBAL, Set.of("break-glass-user"));
    humanPrincipal = principal(PRINCIPAL_IDENTIFIER, PrincipalKind.HUMAN);
  }

  @Test
  void shouldAllowBreakGlassPrincipalBeforeCheckingCatalogAdmin() {
    var request = new AuthorizationRequest(principal("break-glass-user", PrincipalKind.HUMAN),
        Optional.empty(), false);

    assertThatCode(() -> authorizationService.authorize(request, policy))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAllowPrincipalWithIsAdminSetToTrue() {
    var request = new AuthorizationRequest(humanPrincipal, Optional.of(principalEntity("true")),
        false);

    assertThatCode(() -> authorizationService.authorize(request, policy))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAllowServiceAccountForWriteOperations() {
    var request = new AuthorizationRequest(
        principal(PRINCIPAL_IDENTIFIER, PrincipalKind.SERVICE_ACCOUNT), Optional.empty(), false);

    assertThatCode(() -> authorizationService.authorize(request, policy))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAllowReadOperationsForNonAdminHumans() {
    var request = new AuthorizationRequest(humanPrincipal, Optional.empty(), true);

    assertThatCode(() -> authorizationService.authorize(request, policy))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRejectWriteOperationsForNonAdminHumans() {
    var request = new AuthorizationRequest(humanPrincipal, Optional.of(principalEntity("false")),
        false);

    assertThatThrownBy(() -> authorizationService.authorize(request, policy))
        .isInstanceOf(PrincipalNotAuthorizedException.class)
        .hasMessageContaining(PRINCIPAL_IDENTIFIER);
  }

  @Test
  void shouldRejectModesThatAreNotImplementedYet() {
    var futureModePolicy = new AuthorizationPolicy(AuthorizationMode.RBAC, Set.of());
    var request = new AuthorizationRequest(humanPrincipal, Optional.empty(), true);

    assertThatThrownBy(() -> authorizationService.authorize(request, futureModePolicy))
        .isInstanceOf(PrincipalNotAuthorizedException.class);
  }

  private PrincipalInfo principal(String identifier, PrincipalKind kind) {
    return new PrincipalInfo(identifier, kind, "Platform User", null, null);
  }

  private Entity principalEntity(String isAdmin) {
    return new Entity(UUID.randomUUID(), "principal", "Platform User", PRINCIPAL_IDENTIFIER,
        List.of(new Property(null, "is_admin", isAdmin)), List.of());
  }
}
