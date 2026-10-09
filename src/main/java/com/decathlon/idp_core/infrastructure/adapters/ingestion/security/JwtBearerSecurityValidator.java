package com.decathlon.idp_core.infrastructure.adapters.ingestion.security;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.port.WebhookSecurityStrategy;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthForbiddenException;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthUnauthorizedException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtBearerSecurityValidator
    implements
      WebhookSecurityStrategy,
      WebhookRequestAuthenticator {

  private static final String KEY_JWKS_URI_SNAKE_CASE = "jwks_uri";
  private static final String KEY_JWKS_URI_CAMEL_CASE = "jwksUri";
  private static final String KEY_CLIENT_ID_FIELD_SNAKE_CASE = "client_id_field";
  private static final String KEY_CLIENT_ID_FIELD_CAMEL_CASE = "clientIdField";
  private static final String KEY_CLIENT_ID_VALUES_SNAKE_CASE = "client_id_values";
  private static final String KEY_CLIENT_ID_VALUES_CAMEL_CASE = "clientIdValues";
  private static final String KEY_EXPECTED_AUDIENCE_SNAKE_CASE = "expected_audience";
  private static final String KEY_EXPECTED_AUDIENCE_CAMEL_CASE = "expectedAudience";

  private static final String BEARER_PREFIX = "Bearer ";
  private static final String CALLER_RESULT_ACCEPTED = "ACCEPTED";
  private static final String CALLER_RESULT_REJECTED = "REJECTED";
  private static final String CLAIM_EMAIL = "email";
  private static final int MAX_LOGGED_CALLER_LENGTH = 128;

  private final WebhookJwtDecoderProvider jwtDecoderProvider;
  private final Set<String> allowedJwksHosts;

  public JwtBearerSecurityValidator(WebhookJwtDecoderProvider jwtDecoderProvider) {
    this.jwtDecoderProvider = jwtDecoderProvider;
    this.allowedJwksHosts = Set.of();
  }

  @Autowired
  public JwtBearerSecurityValidator(WebhookJwtDecoderProvider jwtDecoderProvider,
      WebhookSecurityProperties webhookSecurityProperties) {
    this.jwtDecoderProvider = jwtDecoderProvider;
    this.allowedJwksHosts = Set.copyOf(webhookSecurityProperties.allowedJwksHosts());
  }

  @Override
  public boolean supports(WebhookSecurityType securityType) {
    return WebhookSecurityType.JWT_BEARER == securityType;
  }

  @Override
  public void validateConfiguration(Map<String, String> config) {
    String jwksUriValue = WebhookSecurityConfigurationUtils.required(config,
        KEY_JWKS_URI_SNAKE_CASE, KEY_JWKS_URI_CAMEL_CASE);
    if (jwksUriValue.isBlank()) {
      throw new WebhookSecurityConfigurationException("Invalid jwks_uri for JWT_BEARER security");
    }

    if (WebhookSecurityConfigurationUtils.isEnvironmentReference(jwksUriValue)) {
      throw new WebhookSecurityConfigurationException(
          "Invalid jwks_uri for JWT_BEARER security: runtime environment references are not supported");
    }

    validateJwksUri(jwksUriValue);

    String clientIdField = resolveRequiredClientIdField(config);
    if (WebhookSecurityConfigurationUtils.isEnvironmentReference(clientIdField)) {
      throw new WebhookSecurityConfigurationException(
          "Invalid client_id_field for JWT_BEARER security: runtime environment references are not supported");
    }

    if (!StringUtils.hasText(clientIdField)) {
      throw new WebhookSecurityConfigurationException(
          "Invalid client_id_field for JWT_BEARER security: must be a non-empty claim name");
    }

    String clientIdValues = WebhookSecurityConfigurationUtils.required(config,
        KEY_CLIENT_ID_VALUES_SNAKE_CASE, KEY_CLIENT_ID_VALUES_CAMEL_CASE);
    if (clientIdValues.isBlank()) {
      throw new WebhookSecurityConfigurationException(
          "Invalid client_id_values for JWT_BEARER security");
    }
    if (WebhookSecurityConfigurationUtils.isEnvironmentReference(clientIdValues)) {
      throw new WebhookSecurityConfigurationException(
          "Invalid client_id_values for JWT_BEARER security: runtime environment references are not supported");
    }

    String optionalExpectedAudience = resolveOptionalExpectedAudience(config);
    if (optionalExpectedAudience != null) {
      if (optionalExpectedAudience.isBlank()) {
        throw new WebhookSecurityConfigurationException(
            "Invalid expected_audience for JWT_BEARER security");
      }
      if (WebhookSecurityConfigurationUtils.isEnvironmentReference(optionalExpectedAudience)) {
        throw new WebhookSecurityConfigurationException(
            "Invalid expected_audience for JWT_BEARER security: runtime environment references are not supported");
      }
      parseAllowedAudienceValues(optionalExpectedAudience);
    }

    parseAllowedClientIdValues(clientIdValues);
  }

  @Override
  public void validateRequest(Map<String, Object> headers, byte[] rawPayload,
      Map<String, String> config) {
    String jwksUriValue = WebhookSecurityConfigurationUtils.required(config,
        KEY_JWKS_URI_SNAKE_CASE, KEY_JWKS_URI_CAMEL_CASE);

    String clientIdField = resolveRequiredClientIdField(config);
    String clientIdValues = WebhookSecurityConfigurationUtils.required(config,
        KEY_CLIENT_ID_VALUES_SNAKE_CASE, KEY_CLIENT_ID_VALUES_CAMEL_CASE);
    String optionalExpectedAudience = resolveOptionalExpectedAudience(config);

    String authorization = WebhookSecurityConfigurationUtils.requiredHeader(headers,
        "Authorization");
    if (!hasBearerPrefix(authorization)
        || authorization.substring(BEARER_PREFIX.length()).isBlank()) {
      throw new WebhookAuthUnauthorizedException(
          "Authorization header must use Bearer token format");
    }

    String token = authorization.substring(BEARER_PREFIX.length()).trim();
    Jwt jwt = decodeAndValidateJwt(token, jwksUriValue);

    String actualCallerIdentity = jwt.getClaimAsString(clientIdField);
    if (!StringUtils.hasText(actualCallerIdentity)) {
      logCallerOutcome(clientIdField, null, CALLER_RESULT_REJECTED);
      throw new WebhookAuthUnauthorizedException("JWT missing required claim: " + clientIdField);
    }

    Set<String> allowedIdentities = parseAllowedClientIdValues(clientIdValues);
    if (!allowedIdentities.contains(actualCallerIdentity)) {
      logCallerOutcome(clientIdField, actualCallerIdentity, CALLER_RESULT_REJECTED);
      throw new WebhookAuthForbiddenException(
          "JWT client or service account identifier was rejected");
    }

    if (StringUtils.hasText(optionalExpectedAudience)) {
      try {
        validateAudienceClaim(jwt, optionalExpectedAudience);
      } catch (WebhookAuthForbiddenException exception) {
        logCallerOutcome(clientIdField, actualCallerIdentity, CALLER_RESULT_REJECTED);
        throw exception;
      }
    }

    logCallerOutcome(clientIdField, actualCallerIdentity, CALLER_RESULT_ACCEPTED);
  }

  /// Logs the caller identity and the validation outcome. The token itself is
  /// never logged. The caller value is omitted when the identifying claim is
  /// `email`, because an email address is personal data.
  private void logCallerOutcome(String claimName, String caller, String result) {
    String loggedCaller = resolveLoggedCaller(claimName, caller);
    if (CALLER_RESULT_ACCEPTED.equals(result)) {
      log.info("webhook_jwt_caller claim={} caller={} result={}", claimName, loggedCaller, result);
    } else {
      log.warn("webhook_jwt_caller claim={} caller={} result={}", claimName, loggedCaller, result);
    }
  }

  private String resolveLoggedCaller(String claimName, String caller) {
    if (caller == null) {
      return "<missing>";
    }
    if (CLAIM_EMAIL.equalsIgnoreCase(claimName)) {
      return "<redacted>";
    }
    String sanitized = caller.replaceAll("\\p{Cntrl}", "_");
    return sanitized.length() > MAX_LOGGED_CALLER_LENGTH
        ? sanitized.substring(0, MAX_LOGGED_CALLER_LENGTH)
        : sanitized;
  }

  /// Validates the jwks_uri to prevent SSRF attacks.
  /// Enforces HTTPS-only and blocks internal, private, link-local, and loopback
  /// IP destinations.
  private void validateJwksUri(String jwksUri) {
    URI uri;
    try {
      uri = URI.create(jwksUri.trim());
    } catch (IllegalArgumentException _) {
      throw new WebhookSecurityConfigurationException(
          "Invalid jwks_uri for JWT_BEARER security: URI is malformed");
    }

    if (!"https".equalsIgnoreCase(uri.getScheme())) {
      throw new WebhookSecurityConfigurationException(
          "Invalid jwks_uri for JWT_BEARER security: only HTTPS URIs are allowed");
    }

    if (!StringUtils.hasText(uri.getHost())) {
      throw new WebhookSecurityConfigurationException(
          "Invalid jwks_uri for JWT_BEARER security: host is missing");
    }

    if (!allowedJwksHosts.isEmpty()
        && !JwtBearerJwksUriPolicy.isAllowedHost(uri.getHost(), allowedJwksHosts)) {
      throw new WebhookSecurityConfigurationException(
          "Invalid jwks_uri for JWT_BEARER security: host is not allow-listed");
    }

    if (JwtBearerJwksUriPolicy.isAllowedHost(uri.getHost(), allowedJwksHosts)) {
      return;
    }

    if (isPrivateOrLoopbackHost(uri.getHost())) {
      throw new WebhookSecurityConfigurationException(
          "Invalid jwks_uri for JWT_BEARER security: localhost, loopback, private and link-local hosts are not allowed");
    }
  }

  private boolean isPrivateOrLoopbackHost(String host) {
    String normalizedHost = host.trim();

    try {
      for (InetAddress address : resolveHostAddresses(normalizedHost)) {
        byte[] rawAddress = address.getAddress();
        boolean isIpv6UniqueLocal = rawAddress.length == 16 && (rawAddress[0] & 0xfe) == 0xfc;
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()
            || address.isLinkLocalAddress() || address.isSiteLocalAddress() || isIpv6UniqueLocal
            || address.isMulticastAddress()) {
          return true;
        }
      }
      return false;
    } catch (UnknownHostException _) {
      throw new WebhookSecurityConfigurationException(
          "Invalid jwks_uri for JWT_BEARER security: host cannot be resolved");
    }
  }

  protected InetAddress[] resolveHostAddresses(String host) throws UnknownHostException {
    return InetAddress.getAllByName(host);
  }

  private String resolveRequiredClientIdField(Map<String, String> config) {
    try {
      return WebhookSecurityConfigurationUtils.required(config, KEY_CLIENT_ID_FIELD_SNAKE_CASE,
          KEY_CLIENT_ID_FIELD_CAMEL_CASE);
    } catch (WebhookSecurityConfigurationException _) {
      throw new WebhookSecurityConfigurationException(
          "Missing required JWT_BEARER config key. Expected one of: client_id_field, clientIdField. "
              + "Value must be a non-empty JWT claim name (for example 'client_id', 'azp', 'email'). "
              + "Example: \"client_id_field\": \"client_id\"");
    }
  }

  private String resolveOptionalExpectedAudience(Map<String, String> config) {
    String value = config.get(KEY_EXPECTED_AUDIENCE_SNAKE_CASE);
    if (value != null) {
      return value;
    }
    return config.get(KEY_EXPECTED_AUDIENCE_CAMEL_CASE);
  }

  private Jwt decodeAndValidateJwt(String token, String jwksUri) {
    try {
      return jwtDecoderProvider.get(jwksUri).decode(token);
    } catch (JwtException | RestClientException exception) {
      throw new WebhookAuthUnauthorizedException("JWT token validation failed", exception);
    }
  }

  private boolean hasBearerPrefix(String authorization) {
    return authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length());
  }

  private Set<String> parseAllowedClientIdValues(String clientIdValues) {
    Set<String> values = Stream.of(clientIdValues.split(",")).map(String::trim)
        .filter(value -> !value.isBlank()).collect(Collectors.toUnmodifiableSet());

    if (values.isEmpty()) {
      throw new WebhookSecurityConfigurationException(
          "Invalid client_id_values for JWT_BEARER security");
    }
    return values;
  }

  private Set<String> parseAllowedAudienceValues(String audienceValues) {
    Set<String> values = Stream.of(audienceValues.split(",")).map(String::trim)
        .filter(value -> !value.isBlank()).collect(Collectors.toUnmodifiableSet());

    if (values.isEmpty()) {
      throw new WebhookSecurityConfigurationException(
          "Invalid expected_audience for JWT_BEARER security");
    }
    return values;
  }

  private void validateAudienceClaim(Jwt jwt, String configuredAudienceValues) {
    Set<String> allowedAudiences = parseAllowedAudienceValues(configuredAudienceValues);
    var jwtAudiences = jwt.getAudience();
    if (jwtAudiences == null || jwtAudiences.isEmpty()) {
      throw new WebhookAuthForbiddenException("JWT missing required claim: aud");
    }

    Set<String> tokenAudiences = jwtAudiences.stream().collect(Collectors.toUnmodifiableSet());

    boolean matched = tokenAudiences.stream().anyMatch(allowedAudiences::contains);
    if (!matched) {
      throw new WebhookAuthForbiddenException("JWT audience was rejected");
    }
  }
}
