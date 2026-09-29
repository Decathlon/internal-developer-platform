package com.decathlon.idp_core.infrastructure.adapters.api.auth;

import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

import com.decathlon.idp_core.domain.model.authorization.AuthorizationAction;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationRequest;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationResource;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;

import lombok.extern.slf4j.Slf4j;

/// Maps servlet requests to the framework-independent authorization context.
///
/// The resource is read from [AuthorizedResource] on the handler resolved by Spring MVC and
/// its identifiers from the matched URI template variables, so no URL is parsed manually.
@Slf4j
@Component
public class AuthorizationRequestFactory {

  private static final String UNKNOWN_RESOURCE = "unknown";
  private static final String TEMPLATE_IDENTIFIER = "templateIdentifier";
  private static final String ENTITY_IDENTIFIER = "entityIdentifier";
  private static final String IDENTIFIER = "identifier";

  private final HandlerMapping handlerMapping;

  /// Creates a factory backed by the MVC handler mapping.
  ///
  /// @param handlerMapping the mapping used to resolve the controller method of a
  /// request
  public AuthorizationRequestFactory(
      @Lazy @Qualifier("requestMappingHandlerMapping") HandlerMapping handlerMapping) {
    this.handlerMapping = handlerMapping;
  }

  /// Creates an authorization request from the principal and HTTP request.
  public AuthorizationRequest create(HttpServletRequest request, PrincipalInfo principal,
      Optional<Entity> principalEntity) {
    Optional<HandlerMethod> handler = resolveHandler(request);
    boolean readOnly = handler.map(this::isReadOnly).orElse(false);
    return new AuthorizationRequest(principal, principalEntity,
        resolveAction(request.getMethod(), readOnly), resolveResource(request, handler));
  }

  private Optional<HandlerMethod> resolveHandler(HttpServletRequest request) {
    try {
      return Optional.ofNullable(handlerMapping.getHandler(request))
          .map(HandlerExecutionChain::getHandler).filter(HandlerMethod.class::isInstance)
          .map(HandlerMethod.class::cast);
    } catch (Exception exception) {
      log.warn("Unable to resolve handler for {} {}", request.getMethod(), request.getRequestURI(),
          exception);
      return Optional.empty();
    }
  }

  private boolean isReadOnly(HandlerMethod handler) {
    AuthorizedResource annotation = handler.getMethodAnnotation(AuthorizedResource.class);
    return annotation != null && annotation.readOnly();
  }

  private AuthorizationAction resolveAction(String method, boolean readOnly) {
    if ("POST".equals(method) && readOnly) {
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

  private AuthorizationResource resolveResource(HttpServletRequest request,
      Optional<HandlerMethod> handler) {
    String type = handler.flatMap(this::resourceType).orElse(UNKNOWN_RESOURCE);
    Map<String, String> variables = pathVariables(request);
    Optional<String> identifier = Optional.ofNullable(variables.get(ENTITY_IDENTIFIER))
        .or(() -> Optional.ofNullable(variables.get(IDENTIFIER)));
    return new AuthorizationResource(type, identifier,
        Optional.ofNullable(variables.get(TEMPLATE_IDENTIFIER)));
  }

  private Optional<String> resourceType(HandlerMethod handler) {
    AuthorizedResource onMethod = handler.getMethodAnnotation(AuthorizedResource.class);
    if (onMethod != null && !onMethod.value().isBlank()) {
      return Optional.of(onMethod.value());
    }
    return Optional
        .ofNullable(AnnotatedElementUtils.findMergedAnnotation(handler.getBeanType(),
            AuthorizedResource.class))
        .map(AuthorizedResource::value).filter(value -> !value.isBlank());
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> pathVariables(HttpServletRequest request) {
    Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    return variables instanceof Map<?, ?> map ? (Map<String, String>) map : Map.of();
  }
}
