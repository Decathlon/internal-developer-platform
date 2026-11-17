package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/// Custom JWT decoder configuration for OAuth2 resource server integration.
///
/// **Infrastructure rationale:** Overrides Spring Boot's strict auto-configuration to handle
/// identity providers with non-standard JWKS responses. Some providers omit explicit 'alg'
/// or 'use' attributes on keys, causing Spring's default decoder to fail validation.
///
/// **Technical solution:** Uses bare-bones NimbusJwtDecoder configuration that defaults to
/// RS256 and is more permissive with key validation. Successfully decodes tokens with valid
/// 'kid' matching, bypassing strict attribute validation that would drop keys.
///
/// **Security considerations:** Maintains security while providing flexibility for various
/// identity provider implementations that may not follow all JWKS specifications exactly.
///
/// **Token binding:** a valid signature only proves that the token comes from the identity
/// provider, not that it was issued for this service. When
/// `app.security.authentication.jwt.issuer-uri` and/or `...jwt.audiences` are set, the `iss`
/// and `aud` claims are validated in addition to the signature and timestamps. Both are
/// optional for backward compatibility; a warning is logged at startup when either is missing.
@Configuration
@ConditionalOnProperty(prefix = "app.security.authentication.jwt", name = "enabled", havingValue = "true", matchIfMissing = false)
public class JwtConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(JwtConfiguration.class);

  private final String jwkSetUri;
  private final String issuerUri;
  private final List<String> audiences;

  public JwtConfiguration(
      @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
      @Value("${app.security.authentication.jwt.issuer-uri:}") String issuerUri,
      @Value("${app.security.authentication.jwt.audiences:}") String audiences) {
    this.jwkSetUri = jwkSetUri;
    this.issuerUri = issuerUri == null ? "" : issuerUri.trim();
    this.audiences = audiences == null
        ? List.of()
        : Arrays.stream(audiences.split(",")).map(String::trim).filter(a -> !a.isEmpty()).toList();
  }

  @Bean
  @ConditionalOnMissingBean
  public JwtDecoder jwtDecoder() {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    decoder.setJwtValidator(jwtValidator());
    return decoder;
  }

  /// Builds the claim validators applied after signature verification.
  ///
  /// Timestamps are always validated. The issuer is validated when configured,
  /// and the token
  /// must carry at least one of the configured audiences when any is configured.
  OAuth2TokenValidator<Jwt> jwtValidator() {
    List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
    if (issuerUri.isEmpty()) {
      LOGGER.warn(
          "app.security.authentication.jwt.issuer-uri is not set: the JWT issuer (iss) is not validated.");
      validators.add(JwtValidators.createDefault());
    } else {
      validators.add(JwtValidators.createDefaultWithIssuer(issuerUri));
    }
    if (audiences.isEmpty()) {
      LOGGER.warn(
          "app.security.authentication.jwt.audiences is not set: the JWT audience (aud) is not validated, "
              + "tokens issued for other applications of the same identity provider are accepted.");
    } else {
      validators.add(new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
          aud -> aud != null && aud.stream().anyMatch(audiences::contains)));
    }
    return new DelegatingOAuth2TokenValidator<>(validators);
  }
}
