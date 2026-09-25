package com.decathlon.idp_core.infrastructure.adapters.ingestion.security;

import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.port.WebhookSecurityStrategy;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthForbiddenException;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthUnauthorizedException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class HmacSha256SecurityValidator
    implements
      WebhookSecurityStrategy,
      WebhookRequestAuthenticator {

  private static final String DEFAULT_HMAC_PREFIX = "sha256=";

  private final HmacSignatureValidator hmacSignatureValidator;

  @Override
  public boolean supports(WebhookSecurityType securityType) {
    return WebhookSecurityType.HMAC_SHA256 == securityType;
  }

  @Override
  public void validateConfiguration(Map<String, String> config) {
    WebhookSecurityConfigurationUtils.required(config, "header_name", "headerName");
    String alias = WebhookSecurityConfigurationUtils.required(config, "secret_alias",
        "secretAlias");
    WebhookSecurityConfigurationUtils.validateSecretAliasFormat(alias);
    WebhookSecurityConfigurationUtils.validateSecretAliasExists(alias);
  }

  @Override
  public void validateRequest(Map<String, Object> headers, byte[] rawPayload,
      Map<String, String> config) {
    String headerName = WebhookSecurityConfigurationUtils.required(config, "header_name",
        "headerName");
    String prefix = WebhookSecurityConfigurationUtils.optional(config, "prefix",
        DEFAULT_HMAC_PREFIX);

    String expectedSecret = WebhookSecurityConfigurationUtils.resolveRequiredRuntimeValue(config,
        "secret_alias", "secretAlias");
    String receivedSignature = WebhookSecurityConfigurationUtils.requiredHeader(headers,
        headerName);

    if (!receivedSignature.startsWith(prefix)) {
      throw new WebhookAuthUnauthorizedException(
          "Header %s lacks required '%s' prefix".formatted(headerName, prefix));
    }

    String contentType = String.valueOf(headers.getOrDefault("Content-Type", "")).trim();

    String computedSignature = prefix
        + hmacSignatureValidator.computeHexSha256(rawPayload, expectedSecret);

    boolean matches = WebhookSecurityConfigurationUtils.constantTimeEquals(computedSignature,
        receivedSignature);

    if (!matches) {
      String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
      if (normalizedContentType.contains("application/x-www-form-urlencoded")
          || normalizedContentType.contains("multipart/form-data")) {
        throw new WebhookAuthForbiddenException(
            "Header %s signature does not match computed HMAC. The request body appears to have been transformed before validation (Content-Type: %s)."
                .formatted(headerName, contentType));
      }

      throw new WebhookAuthForbiddenException(
          "Header %s signature does not match computed HMAC".formatted(headerName));
    }
  }
}
