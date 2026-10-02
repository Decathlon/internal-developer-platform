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
import com.decathlon.idp_core.domain.model.authorization.AuthorizationAction;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationMode;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationPolicy;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationRequest;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationResource;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;
import com.decathlon.idp_core.domain.model.principal.PrincipalKind;

class GlobalAuthorizationServiceTest {

  private static final String PRINCIPAL_IDENTIFIER = "platform-user";
  private static final String REQUIRED_PRINCIPAL_PROPERTY = "is_idp_user";
  private static final AuthorizationResource CATALOG = new AuthorizationResource("entity",
      Optional.of("api-one"), Optional.of("api"));
  private static final AuthorizationResource WEBHOOK_CONFIGURATION = new AuthorizationResource(
      AuthorizationResource.INBOUND_WEBHOOK_CONFIGURATION, Optional.of("connector"),
      Optional.empty());

  private final GlobalAuthorizationService authorizationService = new GlobalAuthorizationService();
  private AuthorizationPolicy policy;
  private PrincipalInfo humanPrincipal;

  @BeforeEach
  void setUp() {
    policy = new AuthorizationPolicy(AuthorizationMode.GLOBAL, Set.of("break-glass-user"),
        Optional.of(REQUIRED_PRINCIPAL_PROPERTY));
    humanPrincipal = principal(PRINCIPAL_IDENTIFIER, PrincipalKind.HUMAN);
  }

  @Test
  void shouldAllowBreakGlassPrincipalBeforeCheckingCatalogAdmin() {
    var request = request(principal("break-glass-user", PrincipalKind.HUMAN),
        AuthorizationAction.DELETE, CATALOG, Optional.empty());

    assertThatCode(() -> authorizationService.authorize(request, policy))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAllowPrincipalWithIsAdminSetToTrueForAllCrud() {
    for (AuthorizationAction action : crudActions()) {
      var request = request(humanPrincipal, action, WEBHOOK_CONFIGURATION,
          Optional.of(principalEntity("true")));

      assertThatCode(() -> authorizationService.authorize(request, policy))
          .doesNotThrowAnyException();
    }
  }

  @Test
  void shouldAllowPlatformAdministratorToDeletePrincipalEntities() {
    AuthorizationResource principalResource = new AuthorizationResource("entity",
        Optional.of("subject-123"), Optional.of("principal"));
    var request = request(humanPrincipal, AuthorizationAction.DELETE, principalResource,
        Optional.of(principalEntity("true")));

    assertThatCode(() -> authorizationService.authorize(request, policy))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAllowServiceAccountCrudOutsideWebhookConfigurationCreation() {
    for (AuthorizationAction action : crudActions()) {
      if (action == AuthorizationAction.CREATE) {
        var webhookRequest = request(serviceAccount(), action, WEBHOOK_CONFIGURATION,
            Optional.empty());
        assertThatThrownBy(() -> authorizationService.authorize(webhookRequest, policy))
            .isInstanceOf(PrincipalNotAuthorizedException.class);
      } else {
        var request = request(serviceAccount(), action, WEBHOOK_CONFIGURATION, Optional.empty());
        assertThatCode(() -> authorizationService.authorize(request, policy))
            .doesNotThrowAnyException();
      }
    }
  }

  @Test
  void shouldAllowServiceAccountToCreateOtherCatalogResources() {
    var request = request(serviceAccount(), AuthorizationAction.CREATE, CATALOG, Optional.empty());

    assertThatCode(() -> authorizationService.authorize(request, policy))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRejectServiceAccountWritesToPrincipalRecordsAndTemplate() {
    List<AuthorizationResource> protectedResources = List.of(
        new AuthorizationResource("entity", Optional.of("subject-123"), Optional.of("principal")),
        new AuthorizationResource("principal", Optional.of("subject-123"), Optional.empty()),
        new AuthorizationResource("entity_template", Optional.of("principal"), Optional.empty()));

    for (AuthorizationResource resource : protectedResources) {
      for (AuthorizationAction action : List.of(AuthorizationAction.CREATE,
          AuthorizationAction.UPDATE, AuthorizationAction.DELETE)) {
        var request = request(serviceAccount(), action, resource, Optional.empty());
        assertThatThrownBy(() -> authorizationService.authorize(request, policy))
            .isInstanceOf(PrincipalNotAuthorizedException.class);
      }
    }
  }

  @Test
  void shouldAllowBreakGlassPrincipalToManagePrincipalRecordsWithoutCatalogEntity() {
    AuthorizationResource principalResource = new AuthorizationResource("entity",
        Optional.of("subject-123"), Optional.of("principal"));
    var request = request(principal("break-glass-user", PrincipalKind.HUMAN),
        AuthorizationAction.UPDATE, principalResource, Optional.empty());

    assertThatCode(() -> authorizationService.authorize(request, policy))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAllowReadOperationsForNonAdminHumans() {
    var request = request(humanPrincipal, AuthorizationAction.READ, CATALOG,
        Optional.of(principalEntity("false", "true")));

    assertThatCode(() -> authorizationService.authorize(request, policy))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldUseConfiguredPrincipalPropertyForHumanAccess() {
    var configuredPolicy = new AuthorizationPolicy(AuthorizationMode.GLOBAL, Set.of(),
        Optional.of("employee_enabled"));
    Entity eligiblePrincipal = new Entity(UUID.randomUUID(), "principal", "Platform User",
        PRINCIPAL_IDENTIFIER, List.of(new Property(null, "is_admin", "false"),
            new Property(null, "employee_enabled", "true")),
        List.of());
    var request = request(humanPrincipal, AuthorizationAction.READ, CATALOG,
        Optional.of(eligiblePrincipal));

    assertThatCode(() -> authorizationService.authorize(request, configuredPolicy))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldNotRequirePrincipalPropertyWhenGateIsNotConfigured() {
    var ungatedPolicy = new AuthorizationPolicy(AuthorizationMode.GLOBAL, Set.of(),
        Optional.empty());
    var request = request(humanPrincipal, AuthorizationAction.READ, CATALOG, Optional.empty());

    assertThatCode(() -> authorizationService.authorize(request, ungatedPolicy))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldKeepHumanWritesDeniedWhenGateIsNotConfigured() {
    var ungatedPolicy = new AuthorizationPolicy(AuthorizationMode.GLOBAL, Set.of(),
        Optional.empty());
    var request = request(humanPrincipal, AuthorizationAction.UPDATE, CATALOG, Optional.empty());

    assertThatThrownBy(() -> authorizationService.authorize(request, ungatedPolicy))
        .isInstanceOf(PrincipalNotAuthorizedException.class);
  }

  @Test
  void shouldRejectHumanWithoutRequiredPropertyEvenForReadOperations() {
    for (Optional<Entity> principalEntity : List.<Optional<Entity>>of(Optional.empty(),
        Optional.of(principalEntity("false", "false")))) {
      var request = request(humanPrincipal, AuthorizationAction.READ, CATALOG, principalEntity);

      assertThatThrownBy(() -> authorizationService.authorize(request, policy))
          .isInstanceOf(PrincipalNotAuthorizedException.class);
    }
  }

  @Test
  void shouldRejectWriteOperationsForNonAdminHumans() {
    var request = request(humanPrincipal, AuthorizationAction.UPDATE, CATALOG,
        Optional.of(principalEntity("false", "true")));

    assertThatThrownBy(() -> authorizationService.authorize(request, policy))
        .isInstanceOf(PrincipalNotAuthorizedException.class)
        .hasMessageContaining(PRINCIPAL_IDENTIFIER);
  }

  @Test
  void shouldRejectAdminFlagOnNonPrincipalEntity() {
    Entity nonPrincipalEntity = new Entity(UUID.randomUUID(), "team", "Platform User",
        PRINCIPAL_IDENTIFIER, List.of(new Property(null, "is_admin", "true"),
            new Property(null, REQUIRED_PRINCIPAL_PROPERTY, "true")),
        List.of());
    var request = request(humanPrincipal, AuthorizationAction.UPDATE, CATALOG,
        Optional.of(nonPrincipalEntity));

    assertThatThrownBy(() -> authorizationService.authorize(request, policy))
        .isInstanceOf(PrincipalNotAuthorizedException.class);
  }

  @Test
  void shouldRejectUnsupportedActionsEvenForBreakGlassPrincipal() {
    var request = request(principal("break-glass-user", PrincipalKind.HUMAN),
        AuthorizationAction.UNSUPPORTED, CATALOG, Optional.empty());

    assertThatThrownBy(() -> authorizationService.authorize(request, policy))
        .isInstanceOf(PrincipalNotAuthorizedException.class);
  }

  @Test
  void shouldRejectModesThatAreNotImplementedYet() {
    var futureModePolicy = new AuthorizationPolicy(AuthorizationMode.RBAC, Set.of(),
        Optional.of(REQUIRED_PRINCIPAL_PROPERTY));
    var request = request(humanPrincipal, AuthorizationAction.READ, CATALOG, Optional.empty());

    assertThatThrownBy(() -> authorizationService.authorize(request, futureModePolicy))
        .isInstanceOf(PrincipalNotAuthorizedException.class);
  }

  private AuthorizationRequest request(PrincipalInfo principal, AuthorizationAction action,
      AuthorizationResource resource, Optional<Entity> principalEntity) {
    return new AuthorizationRequest(principal, principalEntity, action, resource);
  }

  private List<AuthorizationAction> crudActions() {
    return List.of(AuthorizationAction.CREATE, AuthorizationAction.READ, AuthorizationAction.UPDATE,
        AuthorizationAction.DELETE);
  }

  private PrincipalInfo principal(String identifier, PrincipalKind kind) {
    return new PrincipalInfo(identifier, kind, "Platform User", null, null);
  }

  private PrincipalInfo serviceAccount() {
    return principal(PRINCIPAL_IDENTIFIER, PrincipalKind.SERVICE_ACCOUNT);
  }

  private Entity principalEntity(String isAdmin) {
    return principalEntity(isAdmin, "false");
  }

  private Entity principalEntity(String isAdmin, String isDigitalTeammate) {
    return new Entity(UUID.randomUUID(), "principal", "Platform User", PRINCIPAL_IDENTIFIER,
        List.of(new Property(null, "is_admin", isAdmin),
            new Property(null, REQUIRED_PRINCIPAL_PROPERTY, isDigitalTeammate)),
        List.of());
  }
}
