package com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies;

import static com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies.PrincipalStrategiesConstants.CLIENT_ID;
import static com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies.PrincipalStrategiesConstants.EMAIL;
import static com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies.PrincipalStrategiesConstants.GROUPS;
import static com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies.PrincipalStrategiesConstants.NAME;
import static com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies.PrincipalStrategiesConstants.ORIGIN;
import static com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies.PrincipalStrategiesConstants.PREFERRED_USERNAME;
import static com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies.PrincipalStrategiesConstants.SERVICE_NAME;

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
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.PrincipalExtractionProperties;
import com.decathlon.idp_core.infrastructure.adapters.api.exception.MissingPrincipalIdentifierException;
import com.decathlon.idp_core.infrastructure.adapters.api.principal.PrincipalExtractionStrategy;

@Component
public class JwtPrincipalExtractionStrategy implements PrincipalExtractionStrategy {

  private final AuthenticationProperties authProperties;
  private final PrincipalExtractionProperties extractionProperties;

  public JwtPrincipalExtractionStrategy(AuthenticationProperties authProperties,
      PrincipalExtractionProperties extractionProperties) {
    this.authProperties = authProperties;
    this.extractionProperties = extractionProperties;
  }

  @Override
  public boolean supports(Authentication authentication) {
    return authentication instanceof JwtAuthenticationToken;
  }

  @Override
  public PrincipalInfo extract(Authentication authentication) {
    JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
    Map<String, Object> claims = new HashMap<>(jwtToken.getToken().getClaims());
    Optional.ofNullable(jwtToken.getToken().getSubject())
        .ifPresent(subject -> claims.putIfAbsent("sub", subject));
    boolean isHuman = extractionProperties.humanIndicatorClaims().stream().map(claims::get)
        .anyMatch(JwtPrincipalExtractionStrategy::hasNonblankValue);

    return isHuman ? extractHumanFromJwt(claims) : extractMachineFromJwt(claims);
  }

  private static boolean hasNonblankValue(Object value) {
    return value != null && !value.toString().isBlank();
  }

  private PrincipalInfo extractHumanFromJwt(Map<String, Object> claims) {
    String identifierClaim = extractionProperties.human().idClaim();
    String identifier = requiredClaim(claims, identifierClaim);
    Map<String, String> claimMappings = authProperties.userClaimMappings();

    Optional<String> preferredUsername = mappedClaim(claims, claimMappings, PREFERRED_USERNAME);
    String name = mappedClaim(claims, claimMappings, NAME).or(() -> preferredUsername)
        .orElse(identifier);

    Map<String, String> attributes = new HashMap<>();
    mappedClaim(claims, claimMappings, EMAIL).ifPresent(value -> attributes.put(EMAIL, value));

    return new PrincipalInfo(identifier, PrincipalKind.HUMAN, name, attributes,
        extractGroups(claims));
  }

  private PrincipalInfo extractMachineFromJwt(Map<String, Object> claims) {
    String identifierClaim = extractionProperties.machine().idClaim();
    String machineId = requiredClaim(claims, identifierClaim);
    String identifier = extractionProperties.machine().idPrefix() + machineId;
    Map<String, String> claimMappings = authProperties.userClaimMappings();

    String clientId = mappedClaim(claims, claimMappings, CLIENT_ID).orElse(machineId);
    String serviceName = mappedClaim(claims, claimMappings, SERVICE_NAME)
        .or(() -> mappedClaim(claims, claimMappings, NAME)).orElse(clientId);

    Map<String, String> attributes = new HashMap<>();
    attributes.put(CLIENT_ID, clientId);
    mappedClaim(claims, Map.of(ORIGIN, ORIGIN), ORIGIN)
        .ifPresent(value -> attributes.put(ORIGIN, value));

    return new PrincipalInfo(identifier, PrincipalKind.SERVICE_ACCOUNT, serviceName, attributes,
        extractGroups(claims));
  }

  private String requiredClaim(Map<String, Object> claims, String claimName) {
    return Optional.ofNullable(claims.get(claimName)).map(String::valueOf)
        .filter(value -> !value.isBlank())
        .orElseThrow(() -> new MissingPrincipalIdentifierException(claimName));
  }

  private Optional<String> mappedClaim(Map<String, Object> claims,
      Map<String, String> claimMappings, String mappingName) {
    return Optional.ofNullable(claimMappings.get(mappingName)).map(claims::get)
        .filter(JwtPrincipalExtractionStrategy::hasNonblankValue).map(String::valueOf);
  }

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
