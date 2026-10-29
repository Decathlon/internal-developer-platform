package com.decathlon.idp_core.domain.service.authorization;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

  @ParameterizedTest(name = "{1} {2} allowed={4}")
  @MethodSource("authorizationMatrix")
  void shouldApplyGlobalAuthorizationMatrix(PrincipalInfo principal, AuthorizationAction action,
      AuthorizationResource resource, Optional<Entity> principalEntity, boolean allowed) {
    var request = request(principal, action, resource, principalEntity);

    if (allowed) {
      assertThatCode(() -> authorizationService.authorize(request, policy))
          .doesNotThrowAnyException();
    } else {
      assertThatThrownBy(() -> authorizationService.authorize(request, policy))
          .isInstanceOf(PrincipalNotAuthorizedException.class);
    }
  }

  private static Stream<Arguments> authorizationMatrix() {
    AuthorizationResource entity = new AuthorizationResource("entity", Optional.of("item"),
        Optional.of("service"));
    AuthorizationResource template = new AuthorizationResource("entity_template",
        Optional.of("service"), Optional.empty());
    AuthorizationResource entitySearch = new AuthorizationResource("entity_search",
        Optional.empty(), Optional.empty());
    AuthorizationResource webhook = new AuthorizationResource(
        AuthorizationResource.INBOUND_WEBHOOK_CONFIGURATION, Optional.of("hook"), Optional.empty());
    AuthorizationResource mapping = new AuthorizationResource("entity_dynamic_mapping",
        Optional.of("mapping"), Optional.empty());
    AuthorizationResource audit = new AuthorizationResource("audit", Optional.empty(),
        Optional.empty());
    AuthorizationResource principalResource = new AuthorizationResource("principal",
        Optional.of("user"), Optional.empty());
    AuthorizationResource principalSelf = new AuthorizationResource(
        AuthorizationResource.PRINCIPAL_SELF, Optional.empty(), Optional.empty());
    List<AuthorizationResource> readOnlyResources = List.of(entity, entitySearch, template,
        principalSelf);
    List<AuthorizationResource> deniedResources = List.of(webhook, mapping, audit,
        principalResource);
    List<AuthorizationAction> actions = List.of(AuthorizationAction.READ,
        AuthorizationAction.CREATE, AuthorizationAction.UPDATE, AuthorizationAction.DELETE);

    PrincipalInfo human = principal("regular-human", PrincipalKind.HUMAN);
    PrincipalInfo serviceAccount = principal("service-client", PrincipalKind.SERVICE_ACCOUNT);
    PrincipalInfo admin = principal("catalog-admin", PrincipalKind.HUMAN);
    Entity activeHuman = principalEntity(false, "true", "true");
    Entity inactiveHuman = principalEntity(false, "false", "false");
    Entity servicePrincipal = new Entity(UUID.randomUUID(), "principal", "Service",
        "service-client", List.of(new Property(null, "kind", "SERVICE_ACCOUNT"),
            new Property(null, "is_admin", "false")),
        List.of());
    Entity adminPrincipal = new Entity(UUID.randomUUID(), "principal", "Admin", "catalog-admin",
        List.of(new Property(null, "kind", "HUMAN"), new Property(null, "is_admin", "true")),
        List.of());

    Stream.Builder<Arguments> cases = Stream.builder();
    for (AuthorizationAction action : actions) {
      for (AuthorizationResource resource : readOnlyResources) {
        cases.add(Arguments.of(human, action, resource, Optional.of(activeHuman),
            action == AuthorizationAction.READ));
        cases.add(Arguments.of(serviceAccount, action, resource, Optional.of(servicePrincipal),
            action == AuthorizationAction.READ));
        cases.add(Arguments.of(admin, action, resource, Optional.of(adminPrincipal), true));
      }
      for (AuthorizationResource resource : deniedResources) {
        cases.add(Arguments.of(human, action, resource, Optional.of(activeHuman), false));
        cases.add(
            Arguments.of(serviceAccount, action, resource, Optional.of(servicePrincipal), false));
        cases.add(Arguments.of(admin, action, resource, Optional.of(adminPrincipal), true));
      }
    }
    cases.add(
        Arguments.of(human, AuthorizationAction.READ, entity, Optional.of(inactiveHuman), false));
    cases.add(Arguments.of(human, AuthorizationAction.READ, entity, Optional.empty(), false));
    return cases.build();
  }

  @Test
  void shouldAllowBreakGlassPrincipalBeforeCheckingCatalogAdmin() {
    var request = request(principal("break-glass-user", PrincipalKind.HUMAN),
        AuthorizationAction.DELETE, CATALOG, Optional.empty());

    assertThatCode(() -> authorizationService.authorize(request, policy))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAllowPrincipalWithIsAdminSetToTrueForCrudExceptAuditWrites() {
    for (AuthorizationAction action : crudActions()) {
      var request = request(humanPrincipal, action, WEBHOOK_CONFIGURATION,
          Optional.of(principalEntity("true")));

      assertThatCode(() -> authorizationService.authorize(request, policy))
          .doesNotThrowAnyException();
    }
  }

  @Test
  void shouldAllowCatalogAdministratorForAllAuditActions() {
    AuthorizationResource audit = new AuthorizationResource("audit", Optional.empty(),
        Optional.empty());
    for (AuthorizationAction action : crudActions()) {
      var request = request(humanPrincipal, action, audit, Optional.of(principalEntity("true")));

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
  void shouldAllowServiceAccountReadsOnlyOnEntityAndTemplateResources() {
    AuthorizationResource entityTemplate = new AuthorizationResource("entity_template",
        Optional.of("api"), Optional.empty());
    for (AuthorizationResource resource : List.of(CATALOG, entityTemplate)) {
      var request = request(serviceAccount(), AuthorizationAction.READ, resource, Optional.empty());
      assertThatCode(() -> authorizationService.authorize(request, policy))
          .doesNotThrowAnyException();
    }

    for (AuthorizationAction action : crudActions()) {
      var request = request(serviceAccount(), action, WEBHOOK_CONFIGURATION, Optional.empty());
      assertThatThrownBy(() -> authorizationService.authorize(request, policy))
          .isInstanceOf(PrincipalNotAuthorizedException.class);
    }
  }

  @Test
  void shouldRejectServiceAccountWritesToPrincipalRecordsAndTemplate() {
    List<AuthorizationResource> protectedResources = List.of(
        new AuthorizationResource("entity", Optional.of("subject-123"),
            Optional.of(AuthorizationResource.PRINCIPAL_TEMPLATE_IDENTIFIER)),
        new AuthorizationResource("principal", Optional.of("subject-123"), Optional.empty()),
        new AuthorizationResource("entity_template",
            Optional.of(AuthorizationResource.PRINCIPAL_TEMPLATE_IDENTIFIER), Optional.empty()),
        new AuthorizationResource("entity_dynamic_mapping", Optional.of("principal-mapping"),
            Optional.of(AuthorizationResource.PRINCIPAL_TEMPLATE_IDENTIFIER)),
        new AuthorizationResource(AuthorizationResource.INBOUND_WEBHOOK_CONFIGURATION,
            Optional.of("webhook"),
            Optional.of(AuthorizationResource.PRINCIPAL_TEMPLATE_IDENTIFIER)));

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
  void shouldRejectHumanAndServiceAccountAccessToNonReadOnlyResources() {
    for (AuthorizationResource resource : List.of(WEBHOOK_CONFIGURATION,
        new AuthorizationResource("entity_dynamic_mapping", Optional.of("mapping"),
            Optional.empty()),
        new AuthorizationResource("audit", Optional.empty(), Optional.empty()))) {
      for (PrincipalInfo principal : List.of(humanPrincipal, serviceAccount())) {
        var request = request(principal, AuthorizationAction.READ, resource,
            Optional.of(principalEntity("false", "true")));
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

  private static PrincipalInfo principal(String identifier, PrincipalKind kind) {
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

  private static Entity principalEntity(boolean isAdmin, String isDigitalTeammate, String kind) {
    return new Entity(UUID.randomUUID(), "principal", "Platform User", "regular-human",
        List.of(new Property(null, "kind", kind),
            new Property(null, "is_admin", Boolean.toString(isAdmin)),
            new Property(null, REQUIRED_PRINCIPAL_PROPERTY, isDigitalTeammate)),
        List.of());
  }
}
