package com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies;

import static com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies.PrincipalStrategiesConstants.CLIENT_ID;
import static com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies.PrincipalStrategiesConstants.EMAIL;
import static com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies.PrincipalStrategiesConstants.GROUPS;
import static com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies.PrincipalStrategiesConstants.NAME;
import static com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies.PrincipalStrategiesConstants.ORIGIN;
import static com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies.PrincipalStrategiesConstants.PREFERRED_USERNAME;
import static com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies.PrincipalStrategiesConstants.SERVICE_NAME;
import static com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies.PrincipalStrategiesConstants.SUB;
import static org.springframework.security.oauth2.core.oidc.IdTokenClaimNames.AZP;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;
import com.decathlon.idp_core.domain.model.principal.PrincipalKind;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.AuthenticationProperties;
import com.decathlon.idp_core.infrastructure.adapters.api.exception.MissingPrincipalIdentifierException;
import com.decathlon.idp_core.infrastructure.adapters.api.principal.PrincipalExtractionStrategy;

@Component
public class JwtPrincipalExtractionStrategy implements PrincipalExtractionStrategy {

  private final AuthenticationProperties authProperties;

  public JwtPrincipalExtractionStrategy(AuthenticationProperties authProperties) {
    this.authProperties = authProperties;
  }

  @Override
  public boolean supports(Authentication authentication) {
    return authentication instanceof JwtAuthenticationToken;
  }

  @Override
  public PrincipalInfo extract(Authentication authentication) {
    JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
    var claims = jwtToken.getToken().getClaims();
    String sub = jwtToken.getToken().getSubject();

    boolean isServiceAccount = detectServiceAccount(claims);

    if (isServiceAccount) {
      return extractServiceAccountFromJwt(sub, claims);
    }
    return extractHumanFromJwt(sub, claims);
  }

  /// Detects if the JWT token belongs to a service account (M2M) using the
  /// configured definitive claim. Fallback claims cannot establish a service
  /// account because this classification bypasses human authorization checks.
  ///
  /// @param claims the JWT claims map
  /// @return true if the token is identified as a service account, false
  /// otherwise
  private boolean detectServiceAccount(Map<String, Object> claims) {
    AuthenticationProperties.ServiceAccountDetection config = authProperties
        .serviceAccountDetection();

    if (!config.enabled()) {
      return false;
    }

    return detectServiceAccountStrict(claims, config);
  }

  /// Strict mode service account detection: checks for a single definitive claim.
  /// Example: `token_type=m2m` or `account_type=service`
  ///
  /// @param claims the JWT claims map
  /// @param config the service account detection configuration
  /// @return true only if the definitive claim is present and has the expected
  /// value
  private boolean detectServiceAccountStrict(Map<String, Object> claims,
      AuthenticationProperties.ServiceAccountDetection config) {
    Object claimValue = claims.get(config.definitiveClaimName());
    if (claimValue == null) {
      return false;
    }
    return config.definitiveClaimValue().equals(claimValue.toString());
  }

  private PrincipalInfo extractHumanFromJwt(String sub, Map<String, Object> claims) {
    Map<String, String> claimMappings = authProperties.userClaimMappings();

    String identifierClaim = authProperties.principalIdentifierClaim();
    Object identifierValue = claims.get(identifierClaim);
    if (identifierValue == null && SUB.equals(identifierClaim)) {
      identifierValue = sub;
    }
    String identifier = Optional.ofNullable(identifierValue).map(Object::toString)
        .filter(value -> !value.isBlank())
        .orElseThrow(() -> new MissingPrincipalIdentifierException(identifierClaim));

    String preferredUsernameClaim = claimMappings.get(PREFERRED_USERNAME);
    Optional<String> preferredUsername = Optional.ofNullable(preferredUsernameClaim)
        .flatMap(claim -> Optional.ofNullable(claims.get(claim))).map(Object::toString)
        .filter(value -> !value.isBlank());

    // Try to extract name, fallback to identifier
    String nameClaim = claimMappings.get(NAME);
    String name = Optional.ofNullable(nameClaim).flatMap(c -> Optional.ofNullable(claims.get(c)))
        .map(Object::toString).filter(value -> !value.isBlank()).or(() -> preferredUsername)
        .orElse(identifier);

    Map<String, String> attributes = new HashMap<>();

    // Extract email if present
    String emailClaim = claimMappings.get(EMAIL);
    if (emailClaim != null) {
      Optional.ofNullable(claims.get(emailClaim)).map(Object::toString)
          .ifPresent(email -> attributes.put(EMAIL, email));
    }

    List<String> groups = extractGroups(claims);

    return new PrincipalInfo(identifier, PrincipalKind.HUMAN, name, attributes, groups);
  }

  private PrincipalInfo extractServiceAccountFromJwt(String sub, Map<String, Object> claims) {
    Map<String, String> claimMappings = authProperties.userClaimMappings();

    // Extract client_id or azp, fallback to sub
    String clientIdClaim = claimMappings.get(CLIENT_ID);
    String azpClaim = claimMappings.get(AZP);

    String clientId = Optional.ofNullable(clientIdClaim)
        .flatMap(c -> Optional.ofNullable(claims.get(c)))
        .or(() -> Optional.ofNullable(azpClaim).flatMap(c -> Optional.ofNullable(claims.get(c))))
        .map(Object::toString).orElse(sub);

    // Try to extract service_name or name, fallback to clientId
    String serviceNameClaim = claimMappings.get(SERVICE_NAME);
    String nameClaim = claimMappings.get(NAME);

    String serviceName = Optional.ofNullable(serviceNameClaim)
        .flatMap(c -> Optional.ofNullable(claims.get(c)))
        .or(() -> Optional.ofNullable(nameClaim).flatMap(c -> Optional.ofNullable(claims.get(c))))
        .map(Object::toString).orElse(clientId);

    Map<String, String> attributes = new HashMap<>();
    attributes.put(CLIENT_ID, clientId);

    // Extract origin if present (non-standard, IdP-specific)
    String originValue = Optional.ofNullable(claims.get(ORIGIN)).map(Object::toString).orElse(null);
    if (originValue != null) {
      attributes.put(ORIGIN, originValue);
    }

    List<String> groups = extractGroups(claims);

    return new PrincipalInfo(clientId, PrincipalKind.SERVICE_ACCOUNT, serviceName, attributes,
        groups);
  }

  @SuppressWarnings("unchecked")
  private List<String> extractGroups(Map<String, Object> claims) {
    Map<String, String> claimMappings = authProperties.userClaimMappings();
    String groupsClaim = claimMappings.get(GROUPS);

    if (groupsClaim == null) {
      return List.of();
    }

    Object groupsClaimValue = claims.get(groupsClaim);
    if (groupsClaimValue instanceof List<?> list) {
      return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }
    return List.of();
  }
}
