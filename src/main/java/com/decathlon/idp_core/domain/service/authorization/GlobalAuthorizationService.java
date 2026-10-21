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
  private static final String PRINCIPAL_TEMPLATE_IDENTIFIER = AuthorizationResource.PRINCIPAL_TEMPLATE_IDENTIFIER;
  private static final String ENTITY_RESOURCE = "entity";
  private static final String ENTITY_SEARCH_RESOURCE = "entity_search";
  private static final String ENTITY_TEMPLATE_RESOURCE = AuthorizationResource.ENTITY_TEMPLATE;
  private static final String PRINCIPAL_SELF_RESOURCE = AuthorizationResource.PRINCIPAL_SELF;

  /// Applies the global authorization matrix, including the break-glass bypass.
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

    boolean catalogAdministrator = request.principalEntity().filter(this::isCatalogAdministrator)
        .isPresent();
    if (catalogAdministrator) {
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

    if (PRINCIPAL_SELF_RESOURCE.equals(request.resource().type())
        && request.action() == AuthorizationAction.READ) {
      return;
    }

    boolean readOnlyResource = ENTITY_RESOURCE.equals(request.resource().type())
        || ENTITY_SEARCH_RESOURCE.equals(request.resource().type())
        || ENTITY_TEMPLATE_RESOURCE.equals(request.resource().type());
    if (readOnlyResource && request.action() == AuthorizationAction.READ) {
      return;
    }

    throw new PrincipalNotAuthorizedException(identifier);
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
