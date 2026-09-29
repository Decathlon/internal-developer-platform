package com.decathlon.idp_core.domain.service.authorization;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.exception.authorization.PrincipalNotAuthorizedException;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationMode;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationPolicy;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationRequest;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.principal.PrincipalKind;

/// Applies the global authorization policy to a principal request.
@Service
public class GlobalAuthorizationService {

  private static final String IS_ADMIN_PROPERTY = "is_admin";
  private static final String PRINCIPAL_TEMPLATE_IDENTIFIER = "principal";

  /// Authorizes break-glass principals, catalog administrators, service accounts,
  /// and reads.
  ///
  /// @param request the principal and operation being authorized
  /// @param policy the configured global authorization policy
  /// @throws PrincipalNotAuthorizedException when the request is not permitted
  public void authorize(AuthorizationRequest request, AuthorizationPolicy policy) {
    String identifier = request.principal().identifier();
    if (policy.mode() != AuthorizationMode.GLOBAL) {
      throw new PrincipalNotAuthorizedException(identifier);
    }

    if (policy.globalPrincipalIdentifiers().contains(identifier)) {
      return;
    }

    if (request.principalEntity().filter(this::isCatalogAdministrator).isPresent()) {
      return;
    }

    if (request.principal().kind() == PrincipalKind.SERVICE_ACCOUNT) {
      return;
    }

    if (request.readOperation()) {
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
}
