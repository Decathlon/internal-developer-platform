package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.port.WebhookSecurityStrategy;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthForbiddenException;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthUnauthorizedException;

@Component
public class JwtBearerSecurityValidator implements WebhookSecurityStrategy {

  private static final String KEY_JWKS_URI_SNAKE_CASE = "jwks_uri";
  private static final String KEY_JWKS_URI_CAMEL_CASE = "jwksUri";
  private static final String KEY_CLIENT_ID_FIELD_SNAKE_CASE = "client_id_field";
  private static final String KEY_CLIENT_ID_FIELD_CAMEL_CASE = "clientIdField";
  private static final String KEY_CLIENT_ID_VALUES_SNAKE_CASE = "client_id_values";
  private static final String KEY_CLIENT_ID_VALUES_CAMEL_CASE = "clientIdValues";
  private static final String KEY_EXPECTED_AUDIENCE_SNAKE_CASE = "expected_audience";

  private static final String BEARER_PREFIX = "Bearer ";
  private static final String CLIENT_CLAIM_AZP = "azp";
  private static final String CLIENT_CLAIM_EMAIL = "email";

  private final WebhookJwtDecoderProvider jwtDecoderProvider;

  public JwtBearerSecurityValidator(WebhookJwtDecoderProvider jwtDecoderProvider) {
    this.jwtDecoderProvider = jwtDecoderProvider;
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

    if (!CLIENT_CLAIM_AZP.equals(clientIdField) && !CLIENT_CLAIM_EMAIL.equals(clientIdField)) {
      throw new WebhookSecurityConfigurationException(
          "Invalid client_id_field for JWT_BEARER security: must be 'azp' (Client ID) or 'email' (Service Account)");
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
    if (!authorization.startsWith(BEARER_PREFIX)
        || authorization.substring(BEARER_PREFIX.length()).isBlank()) {
      throw new WebhookAuthUnauthorizedException(
          "Authorization header must use Bearer token format");
    }

    String token = authorization.substring(BEARER_PREFIX.length()).trim();
    Jwt jwt = decodeAndValidateJwt(token, jwksUriValue);

    String actualCallerIdentity = jwt.getClaimAsString(clientIdField);
    if (!StringUtils.hasText(actualCallerIdentity)) {
      throw new WebhookAuthForbiddenException("JWT missing required claim: " + clientIdField);
    }

    Set<String> allowedIdentities = parseAllowedClientIdValues(clientIdValues);
    if (!allowedIdentities.contains(actualCallerIdentity)) {
      throw new WebhookAuthForbiddenException(
          "JWT client or service account identifier was rejected");
    }

    if (StringUtils.hasText(optionalExpectedAudience)) {
      validateAudienceClaim(jwt, optionalExpectedAudience);
    }
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

    if (isPrivateOrLoopbackHost(uri.getHost())) {
      throw new WebhookSecurityConfigurationException(
          "Invalid jwks_uri for JWT_BEARER security: localhost, loopback, private and link-local hosts are not allowed");
    }
  }

  private boolean isPrivateOrLoopbackHost(String host) {
    String normalizedHost = host.trim();

    try {
      for (InetAddress address : resolveHostAddresses(normalizedHost)) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()
            || address.isLinkLocalAddress() || address.isSiteLocalAddress()
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
              + "Allowed values: 'azp' (Client ID) or 'email' (Service Account). "
              + "Example: \"client_id_field\": \"email\"");
    }
  }

  private String resolveOptionalExpectedAudience(Map<String, String> config) {
    return config.get(KEY_EXPECTED_AUDIENCE_SNAKE_CASE);
  }

  private Jwt decodeAndValidateJwt(String token, String jwksUri) {
    try {
      return jwtDecoderProvider.get(jwksUri).decode(token);
    } catch (JwtException exception) {
      throw new WebhookAuthUnauthorizedException("JWT token validation failed", exception);
    }
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
