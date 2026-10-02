package com.decathlon.idp_core.domain.model.authorization;

import java.util.Objects;
import java.util.Optional;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/// Identifies the resource and optional parent context for an authorization request.
public record AuthorizationResource(@NotBlank String type, @NotNull Optional<String> identifier,
    @NotNull Optional<String> parentIdentifier) {

  public static final String INBOUND_WEBHOOK_CONFIGURATION = "inbound_webhook_configuration";

  public AuthorizationResource {
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(identifier, "identifier must not be null");
    Objects.requireNonNull(parentIdentifier, "parentIdentifier must not be null");
  }
}
