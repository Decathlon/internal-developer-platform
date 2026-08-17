package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.port.WebhookSecurityStrategy;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthForbiddenException;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthUnauthorizedException;

import lombok.RequiredArgsConstructor;

/// HMAC SHA256 security strategy for webhooks.
///
/// Validates HMAC SHA256 signature configuration at creation time and authenticates
/// incoming webhook requests by verifying the signature against a stored secret.
@Component
@RequiredArgsConstructor
public class HmacSha256SecurityValidator implements WebhookSecurityStrategy {

  private static final String DEFAULT_HMAC_PREFIX = "sha256=";

  private final HmacSignatureValidator hmacSignatureValidator;

  @Override
  public boolean supports(WebhookSecurityType securityType) {
    return WebhookSecurityType.HMAC_SHA256 == securityType;
  }

  @Override
  public void validateConfiguration(Map<String, String> config) {
    WebhookSecurityConfigurationUtils.required(config, "header_name", "headerName");
    String alias = WebhookSecurityConfigurationUtils.required(config, "secret_alias", "secretAlias");
    WebhookSecurityConfigurationUtils.validateSecretAliasFormat(alias);
  }

  @Override
  public void validateRequest(Map<String, Object> headers, byte[] rawPayload,
      Map<String, String> config) {
    String headerName = WebhookSecurityConfigurationUtils.required(config, "header_name",
        "headerName");
    String prefix = WebhookSecurityConfigurationUtils.optional(config, "prefix",
        DEFAULT_HMAC_PREFIX);

    String expectedSecret = WebhookSecurityConfigurationUtils.resolveRequiredRuntimeValue(config, "secret_alias", "secretAlias");
    String receivedSignature = WebhookSecurityConfigurationUtils.requiredHeader(headers,
        headerName);
    if (!receivedSignature.startsWith(prefix)) {
      throw new WebhookAuthUnauthorizedException("HMAC signature format is invalid");
    }

    String computedSignature = prefix
        + hmacSignatureValidator.computeHexSha256(rawPayload, expectedSecret);

    if (!WebhookSecurityConfigurationUtils.constantTimeEquals(computedSignature,
        receivedSignature)) {
      throw new WebhookAuthForbiddenException("HMAC signature was rejected");
    }
  }

}
