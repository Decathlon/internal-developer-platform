package com.decathlon.idp_core.domain.model.principal;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/// Domain model representing the identity and attributes of an authenticated principal.
///
/// **Business invariants:**
/// - [identifier] must be unique and non-blank (subject for humans, client_id for services)
/// - [kind] determines how claims are interpreted
/// - [name] provides a human-readable label for both humans and service accounts
///
/// **Ubiquitous language:** A Principal is any authenticated actor (human or machine)
/// that can interact with the IDP-Core API. This model carries the essential identity
/// information extracted from the authentication context.
public record PrincipalInfo(@NotBlank String identifier, @NotNull PrincipalKind kind,
    @NotBlank String name, Map<String, String> attributes, List<String> groups) {

  public PrincipalInfo {
    attributes = attributes != null ? Map.copyOf(attributes) : Map.of();
    groups = groups != null ? List.copyOf(groups) : List.of();
  }
}
