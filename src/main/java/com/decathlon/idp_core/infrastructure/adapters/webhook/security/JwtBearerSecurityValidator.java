package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

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

  private static final String BEARER_PREFIX = "Bearer ";
  private static final String CLIENT_CLAIM_AZP = "azp";
  private static final String CLIENT_CLAIM_EMAIL = "email";
  private static final String DEFAULT_CLIENT_ID_FIELD = CLIENT_CLAIM_AZP;

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

    String clientIdField = resolveOptionalClientIdField(config);
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

    parseAllowedClientIdValues(clientIdValues);
  }

  @Override
  public void validateRequest(Map<String, Object> headers, byte[] rawPayload,
      Map<String, String> config) {
    String jwksUriValue = WebhookSecurityConfigurationUtils.required(config,
        KEY_JWKS_URI_SNAKE_CASE, KEY_JWKS_URI_CAMEL_CASE);

    if (WebhookSecurityConfigurationUtils.isEnvironmentReference(jwksUriValue)) {
      jwksUriValue = WebhookSecurityConfigurationUtils.resolveRuntimeSecret(jwksUriValue);
    }

    String clientIdField = resolveOptionalClientIdField(config);
    String clientIdValues = WebhookSecurityConfigurationUtils.required(config,
        KEY_CLIENT_ID_VALUES_SNAKE_CASE, KEY_CLIENT_ID_VALUES_CAMEL_CASE);

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
  }

  private String resolveOptionalClientIdField(Map<String, String> config) {
    String snakeCaseValue = config.get(KEY_CLIENT_ID_FIELD_SNAKE_CASE);
    if (snakeCaseValue != null && !snakeCaseValue.isBlank()) {
      return snakeCaseValue;
    }

    String camelCaseValue = config.get(KEY_CLIENT_ID_FIELD_CAMEL_CASE);
    if (camelCaseValue != null && !camelCaseValue.isBlank()) {
      return camelCaseValue;
    }

    return DEFAULT_CLIENT_ID_FIELD;
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
}
