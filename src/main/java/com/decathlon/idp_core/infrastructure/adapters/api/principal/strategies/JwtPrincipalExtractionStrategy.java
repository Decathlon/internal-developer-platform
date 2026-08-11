package com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies;

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
import com.decathlon.idp_core.infrastructure.adapters.api.principal.PrincipalExtractionStrategy;

@Component
public class JwtPrincipalExtractionStrategy implements PrincipalExtractionStrategy {

  public static final String CLIENT_ID = "client_id";
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

    boolean isServiceAccount = detectServiceAccount(claims, sub);

    if (isServiceAccount) {
      return extractServiceAccountFromJwt(sub, claims);
    }
    return extractHumanFromJwt(sub, claims);
  }

  /// Detects if the JWT token belongs to a service account (M2M) based on
  /// configured strategy.
  ///
  /// **Strict Mode (Recommended):**
  /// Checks for a single, definitive claim configured in
  /// `app.security.authentication.service-account-detection.definitive-claim-name`.
  /// Example: If configured to check `token_type=m2m`, returns true only if that
  /// exact claim is present and has that value.
  /// Benefits: Prevents false positives and security-critical misclassifications.
  /// Risks: Requires IdP or API gateway to inject the definitive claim.
  ///
  /// **Legacy Mode (Backwards Compatibility):**
  /// Falls back to loose OR conditions checking multiple optional claims.
  /// Checks: `grant_type=client_credentials` OR `service_name` exists OR
  /// `sub==client_id`
  /// Benefits: Works without reconfiguration for existing deployments.
  /// Risks: Prone to false positives (e.g., human user classified as M2M).
  ///
  /// @param claims the JWT claims map
  /// @param sub the subject claim value
  /// @return true if the token is identified as a service account, false
  /// otherwise
  private boolean detectServiceAccount(Map<String, Object> claims, String sub) {
    AuthenticationProperties.ServiceAccountDetection config = authProperties
        .serviceAccountDetection();

    if (!config.enabled()) {
      return false;
    }

    if ("strict".equalsIgnoreCase(config.mode())) {
      return detectServiceAccountStrict(claims, config);
    }

    // Legacy mode (default for backwards compatibility)
    return detectServiceAccountLegacy(claims, sub);
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

  /// Legacy mode service account detection: checks multiple claims with OR logic.
  /// Fallback behavior for backwards compatibility.
  ///
  /// Checks:
  /// 1. grant_type or gty = "client_credentials" (OAuth2 standard)
  /// 2. service_name claim exists (Auth0 / custom IdP)
  /// 3. sub equals client_id or azp (subject is the OAuth2 client itself)
  ///
  /// **Warning:** This logic is prone to false positives. Strongly recommend
  /// upgrading
  /// to strict mode for production deployments.
  ///
  /// @param claims the JWT claims map
  /// @param sub the subject claim value
  /// @return true if any legacy condition indicates M2M
  private boolean detectServiceAccountLegacy(Map<String, Object> claims, String sub) {
    Map<String, String> claimMappings = authProperties.claimMappings();

    // Check grant_type = client_credentials (most definitive in legacy mode)
    String grantTypeClaim = claimMappings.get("grant_type");
    String gtyClaim = claimMappings.get("gty");

    String grantType = Optional.ofNullable(claims.get(grantTypeClaim))
        .or(() -> Optional.ofNullable(gtyClaim).flatMap(ct -> Optional.ofNullable(claims.get(ct))))
        .map(Object::toString).orElse(null);

    if ("client_credentials".equals(grantType)) {
      return true;
    }

    // Check for explicit service_name claim (custom M2M indicator)
    String serviceNameClaim = claimMappings.get("service_name");
    if (serviceNameClaim != null && claims.containsKey(serviceNameClaim)) {
      return true;
    }

    // Fallback: sub equals client_id (M2M flow where subject is the OAuth2 client)
    String clientIdClaim = claimMappings.get(CLIENT_ID);
    String azpClaim = claimMappings.get("azp");

    String clientId = Optional.ofNullable(clientIdClaim)
        .flatMap(c -> Optional.ofNullable(claims.get(c)))
        .or(() -> Optional.ofNullable(azpClaim).flatMap(c -> Optional.ofNullable(claims.get(c))))
        .map(Object::toString).orElse(null);

    return clientId != null && sub.equals(clientId);
  }

  private PrincipalInfo extractHumanFromJwt(String sub, Map<String, Object> claims) {
    Map<String, String> claimMappings = authProperties.claimMappings();

    // Try to extract preferred username, fallback to sub
    String preferredUsernameClaim = claimMappings.get("preferred_username");
    String identifier = Optional.ofNullable(preferredUsernameClaim)
        .flatMap(c -> Optional.ofNullable(claims.get(c))).map(Object::toString).orElse(sub);

    // Try to extract name, fallback to identifier
    String nameClaim = claimMappings.get("name");
    String name = Optional.ofNullable(nameClaim).flatMap(c -> Optional.ofNullable(claims.get(c)))
        .map(Object::toString).orElse(identifier);

    Map<String, String> attributes = new HashMap<>();

    // Extract email if present
    String emailClaim = claimMappings.get("email");
    if (emailClaim != null) {
      Optional.ofNullable(claims.get(emailClaim)).map(Object::toString)
          .ifPresent(email -> attributes.put("email", email));
    }

    List<String> groups = extractGroups(claims);

    return new PrincipalInfo(identifier, PrincipalKind.HUMAN, name, attributes, groups);
  }

  private PrincipalInfo extractServiceAccountFromJwt(String sub, Map<String, Object> claims) {
    Map<String, String> claimMappings = authProperties.claimMappings();

    // Extract client_id or azp, fallback to sub
    String clientIdClaim = claimMappings.get(CLIENT_ID);
    String azpClaim = claimMappings.get("azp");

    String clientId = Optional.ofNullable(clientIdClaim)
        .flatMap(c -> Optional.ofNullable(claims.get(c)))
        .or(() -> Optional.ofNullable(azpClaim).flatMap(c -> Optional.ofNullable(claims.get(c))))
        .map(Object::toString).orElse(sub);

    // Try to extract service_name or name, fallback to clientId
    String serviceNameClaim = claimMappings.get("service_name");
    String nameClaim = claimMappings.get("name");

    String serviceName = Optional.ofNullable(serviceNameClaim)
        .flatMap(c -> Optional.ofNullable(claims.get(c)))
        .or(() -> Optional.ofNullable(nameClaim).flatMap(c -> Optional.ofNullable(claims.get(c))))
        .map(Object::toString).orElse(clientId);

    Map<String, String> attributes = new HashMap<>();
    attributes.put(CLIENT_ID, clientId);

    // Extract origin if present (non-standard, IdP-specific)
    String originValue = Optional.ofNullable(claims.get("origin")).map(Object::toString)
        .orElse(null);
    if (originValue != null) {
      attributes.put("origin", originValue);
    }

    List<String> groups = extractGroups(claims);

    return new PrincipalInfo(clientId, PrincipalKind.SERVICE_ACCOUNT, serviceName, attributes,
        groups);
  }

  @SuppressWarnings("unchecked")
  private List<String> extractGroups(Map<String, Object> claims) {
    Map<String, String> claimMappings = authProperties.claimMappings();
    String groupsClaim = claimMappings.get("groups");

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
