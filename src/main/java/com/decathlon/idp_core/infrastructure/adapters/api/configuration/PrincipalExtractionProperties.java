package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Provider-specific JWT principal classification and identifier configuration.
 *
 * @param humanIndicatorClaims
 *          claims whose nonblank values identify human tokens
 * @param human
 *          human principal identifier configuration
 * @param machine
 *          machine principal identifier and namespace configuration
 */
@ConfigurationProperties(prefix = "app.security.authentication.principal-extraction")
public record PrincipalExtractionProperties(List<String> humanIndicatorClaims, Human human,
    Machine machine) {

  public PrincipalExtractionProperties {
    humanIndicatorClaims = humanIndicatorClaims == null
        ? List.of()
        : List.copyOf(humanIndicatorClaims);
    human = human == null ? new Human("sub") : human;
    machine = machine == null ? new Machine("client_id", "") : machine;
  }

  /** Human principal identifier claim. */
  public record Human(String idClaim) {
    public Human {
      idClaim = idClaim == null || idClaim.isBlank() ? "sub" : idClaim.trim();
    }
  }

  /** Machine principal identifier claim and optional identifier prefix. */
  public record Machine(String idClaim, String idPrefix) {
    public Machine {
      idClaim = idClaim == null || idClaim.isBlank() ? "client_id" : idClaim.trim();
      idPrefix = idPrefix == null ? "" : idPrefix;
    }
  }
}
