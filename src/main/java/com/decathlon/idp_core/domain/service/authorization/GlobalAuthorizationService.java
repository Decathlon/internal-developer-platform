package com.decathlon.idp_core.domain.service.authorization;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.exception.authorization.PrincipalNotAuthorizedException;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationAction;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationMode;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationPolicy;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationRequest;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationResource;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.principal.PrincipalKind;

/// Applies the global authorization policy to a principal request.
@Service
public class GlobalAuthorizationService {

  private static final String IS_ADMIN_PROPERTY = "is_admin";
  private static final String PRINCIPAL_TEMPLATE_IDENTIFIER = "principal";
  private static final String ENTITY_RESOURCE = "entity";
  private static final String PRINCIPAL_RESOURCE = "principal";
  private static final String ENTITY_TEMPLATE_RESOURCE = "entity_template";

  /// Authorizes break-glass principals, catalog administrators, eligible
  /// service-account operations, and reads.
  ///
  /// @param request the principal and operation being authorized
  /// @param policy the configured global authorization policy
  /// @throws PrincipalNotAuthorizedException when the request is not permitted
  public void authorize(AuthorizationRequest request, AuthorizationPolicy policy) {
    String identifier = request.principal().identifier();
    if (policy.mode() != AuthorizationMode.GLOBAL
        || request.action() == AuthorizationAction.UNSUPPORTED) {
      throw new PrincipalNotAuthorizedException(identifier);
    }

    if (policy.globalPrincipalIdentifiers().contains(identifier)) {
      return;
    }

    if (request.principalEntity().filter(this::isCatalogAdministrator).isPresent()) {
      return;
    }

    boolean missingConfiguredAccessProperty = request.principal().kind() == PrincipalKind.HUMAN
        && policy.requiredPrincipalProperty()
            .map(propertyName -> request.principalEntity()
                .filter(entity -> hasRequiredPrincipalProperty(entity, propertyName)).isEmpty())
            .orElse(false);
    if (missingConfiguredAccessProperty) {
      throw new PrincipalNotAuthorizedException(identifier);
    }

    if (request.principal().kind() == PrincipalKind.SERVICE_ACCOUNT) {
      if (isPrincipalManagementWrite(request)) {
        throw new PrincipalNotAuthorizedException(identifier);
      }
      if (!isPlatformAdminOnlyCreation(request)) {
        return;
      }
    }

    if (request.action() == AuthorizationAction.READ) {
      return;
    }

    throw new PrincipalNotAuthorizedException(identifier);
  }

  private boolean isPlatformAdminOnlyCreation(AuthorizationRequest request) {
    return request.action() == AuthorizationAction.CREATE
        && AuthorizationResource.INBOUND_WEBHOOK_CONFIGURATION.equals(request.resource().type());
  }

  private boolean isPrincipalManagementWrite(AuthorizationRequest request) {
    if (request.action() == AuthorizationAction.READ) {
      return false;
    }

    AuthorizationResource resource = request.resource();
    return PRINCIPAL_RESOURCE.equals(resource.type())
        || (ENTITY_RESOURCE.equals(resource.type()) && resource.parentIdentifier()
            .filter(PRINCIPAL_TEMPLATE_IDENTIFIER::equals).isPresent())
        || (ENTITY_TEMPLATE_RESOURCE.equals(resource.type())
            && resource.identifier().filter(PRINCIPAL_TEMPLATE_IDENTIFIER::equals).isPresent());
  }

  private boolean isCatalogAdministrator(Entity principalEntity) {
    return PRINCIPAL_TEMPLATE_IDENTIFIER.equals(principalEntity.templateIdentifier())
        && principalEntity.properties().stream()
            .anyMatch(property -> IS_ADMIN_PROPERTY.equals(property.name())
                && Boolean.parseBoolean(property.value()));
  }

  private boolean hasRequiredPrincipalProperty(Entity principalEntity, String propertyName) {
    return PRINCIPAL_TEMPLATE_IDENTIFIER.equals(principalEntity.templateIdentifier())
        && principalEntity.properties().stream()
            .anyMatch(property -> propertyName.equals(property.name())
                && Boolean.parseBoolean(property.value()));
  }
}
