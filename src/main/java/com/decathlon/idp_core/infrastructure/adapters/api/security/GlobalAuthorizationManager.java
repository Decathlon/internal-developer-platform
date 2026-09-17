package com.decathlon.idp_core.infrastructure.adapters.api.security;

import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/// Maps HTTP methods to the domain actions used by the Phase 1 policy.
@Component
@RequiredArgsConstructor
public class GlobalAuthorizationManager
    implements
      AuthorizationManager<RequestAuthorizationContext> {

  private final PermissionEvaluator permissionEvaluator;

  @Override
  public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication,
      RequestAuthorizationContext context) {
    return new AuthorizationDecision(permissionEvaluator.hasPermission(authentication.get(), null,
        actionFor(context.getRequest())));
  }

  private String actionFor(HttpServletRequest request) {
    return switch (request.getMethod()) {
      case "GET", "HEAD", "OPTIONS" -> "READ";
      case "POST" -> "CREATE";
      case "PUT", "PATCH" -> "UPDATE";
      case "DELETE" -> "DELETE";
      default -> "READ";
    };
  }
}
