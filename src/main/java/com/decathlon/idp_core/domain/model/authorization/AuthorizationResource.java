package com.decathlon.idp_core.domain.model.authorization;

import java.util.Objects;
import java.util.Optional;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.decathlon.idp_core.domain.constant.ValidationMessages;

/// Identifies the resource and optional parent context for an authorization request.
public record AuthorizationResource(@NotBlank String type, @NotNull Optional<String> identifier,
    @NotNull Optional<String> parentIdentifier) {

  public static final String INBOUND_WEBHOOK_CONFIGURATION = "inbound_webhook_configuration";
  public static final String ENTITY_DYNAMIC_MAPPING = "entity_dynamic_mapping";
  public static final String ENTITY_TEMPLATE = "entity_template";
  public static final String PRINCIPAL_TEMPLATE_IDENTIFIER = "principal";
  public static final String PRINCIPAL_SELF = "principal_self";

  public AuthorizationResource {
    Objects.requireNonNull(type, ValidationMessages.authorizationValueMustNotBeNull("type"));
    Objects.requireNonNull(identifier,
        ValidationMessages.authorizationValueMustNotBeNull("identifier"));
    Objects.requireNonNull(parentIdentifier,
        ValidationMessages.authorizationValueMustNotBeNull("parentIdentifier"));
  }
}
