package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/// Builds and caches JwtDecoder instances keyed by jwks_uri.
@Component
public class WebhookJwtDecoderProvider {

  private final ConcurrentMap<String, JwtDecoder> decodersByJwksUri = new ConcurrentHashMap<>();

  public JwtDecoder get(String jwksUri) {
    return decodersByJwksUri.computeIfAbsent(jwksUri, this::createDecoder);
  }

  private JwtDecoder createDecoder(String jwksUri) {
    var decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
    decoder.setJwtValidator(JwtValidators.createDefault());
    return decoder;
  }
}
