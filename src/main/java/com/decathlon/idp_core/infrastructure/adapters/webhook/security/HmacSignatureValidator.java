package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;

@Component
public class HmacSignatureValidator {

  public String computeHexSha256(byte[] payload, String secret) {
    try {
      if (secret == null || secret.isBlank()) {
        throw new WebhookAuthenticationException("Unable to compute HMAC signature");
      }
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] digest = mac.doFinal(payload);
      return toHex(digest);
    } catch (Exception e) {
      throw new WebhookAuthenticationException("Unable to compute HMAC signature", e);
    }
  }

  private String toHex(byte[] input) {
    StringBuilder sb = new StringBuilder(input.length * 2);
    for (byte value : input) {
      sb.append(String.format("%02x", value));
    }
    return sb.toString();
  }
}
