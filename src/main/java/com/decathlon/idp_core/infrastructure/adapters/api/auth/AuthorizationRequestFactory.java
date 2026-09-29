package com.decathlon.idp_core.infrastructure.adapters.api.auth;

import java.util.Arrays;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.authorization.AuthorizationAction;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationRequest;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationResource;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;

/// Maps servlet requests to the framework-independent authorization context.
@Component
public class AuthorizationRequestFactory {

  private static final String ENTITY_SEARCH_PATH = "/api/v1/entities/search";
  private static final String ENTITY_DYNAMIC_MAPPING_DRY_RUN_PATH = "/api/v1/entity_dynamic_mappings/dry-run";

  /// Creates an authorization request from the principal and HTTP request.
  public AuthorizationRequest create(HttpServletRequest request, PrincipalInfo principal,
      Optional<Entity> principalEntity) {
    String path = normalizedPath(request);
    return new AuthorizationRequest(principal, principalEntity,
        resolveAction(request.getMethod(), path), resolveResource(path));
  }

  private AuthorizationAction resolveAction(String method, String path) {
    if ("POST".equals(method)
        && (ENTITY_SEARCH_PATH.equals(path) || ENTITY_DYNAMIC_MAPPING_DRY_RUN_PATH.equals(path))) {
      return AuthorizationAction.READ;
    }

    return switch (method) {
      case "GET", "HEAD", "OPTIONS" -> AuthorizationAction.READ;
      case "POST" -> AuthorizationAction.CREATE;
      case "PUT", "PATCH" -> AuthorizationAction.UPDATE;
      case "DELETE" -> AuthorizationAction.DELETE;
      default -> AuthorizationAction.UNSUPPORTED;
    };
  }

  private AuthorizationResource resolveResource(String path) {
    if (ENTITY_SEARCH_PATH.equals(path)) {
      return new AuthorizationResource("entity_search", Optional.empty(), Optional.empty());
    }
    if (ENTITY_DYNAMIC_MAPPING_DRY_RUN_PATH.equals(path)) {
      return new AuthorizationResource("entity_dynamic_mapping_dry_run", Optional.empty(),
          Optional.empty());
    }

    String[] segments = Arrays.stream(path.split("/")).filter(segment -> !segment.isBlank())
        .toArray(String[]::new);
    if (segments.length < 3 || !"api".equals(segments[0]) || !"v1".equals(segments[1])) {
      return new AuthorizationResource("unknown", Optional.empty(), Optional.empty());
    }

    String resourceRoot = segments[2];
    if ("entities".equals(resourceRoot)) {
      return entityResource(segments);
    }

    if ("inbound_webhooks".equals(resourceRoot)) {
      return new AuthorizationResource(AuthorizationResource.INBOUND_WEBHOOK_CONFIGURATION,
          identifierAt(segments, 3), Optional.empty());
    }

    String type = switch (resourceRoot) {
      case "entity-templates" -> "entity_template";
      case "entity_dynamic_mappings" -> "entity_dynamic_mapping";
      case "audit" -> "audit";
      default -> "catalog_resource";
    };
    return new AuthorizationResource(type, identifierAt(segments, 3), Optional.empty());
  }

  private AuthorizationResource entityResource(String[] segments) {
    if (segments.length >= 5) {
      return new AuthorizationResource("entity", Optional.of(segments[4]),
          Optional.of(segments[3]));
    }
    return new AuthorizationResource("entity", Optional.empty(), identifierAt(segments, 3));
  }

  private Optional<String> identifierAt(String[] segments, int index) {
    return segments.length > index ? Optional.of(segments[index]) : Optional.empty();
  }

  private String normalizedPath(HttpServletRequest request) {
    String requestUri = request.getRequestURI();
    String contextPath = request.getContextPath();
    String path = requestUri.substring(contextPath.length());
    return path.endsWith("/") && path.length() > 1 ? path.substring(0, path.length() - 1) : path;
  }
}
