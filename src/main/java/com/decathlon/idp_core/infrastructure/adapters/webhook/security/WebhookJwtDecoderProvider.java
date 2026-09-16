package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/// Builds and caches JwtDecoder instances keyed by jwks_uri.
/// Uses RestTemplate only as a thin adapter for NimbusJwtDecoder (which requires RestOperations).
/// The underlying HTTP client is the native JDK HttpClient for Virtual Thread compatibility.
@Component
public class WebhookJwtDecoderProvider {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

  private final ConcurrentMap<String, JwtDecoder> decodersByJwksUri = new ConcurrentHashMap<>();
  private final RestTemplate restTemplate = buildSecureRestTemplate();

  public JwtDecoder get(String jwksUri) {
    JwtBearerJwksUriPolicy.validateRuntimeJwksUri(URI.create(jwksUri));

    return decodersByJwksUri.computeIfAbsent(jwksUri, this::createDecoder);
  }

  private JwtDecoder createDecoder(String jwksUri) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).restOperations(restTemplate)
        .build();
    decoder.setJwtValidator(JwtValidators.createDefault());
    return decoder;
  }

  private RestTemplate buildSecureRestTemplate() {
    HttpClient jdkHttpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NEVER).build();

    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdkHttpClient);
    factory.setReadTimeout(READ_TIMEOUT);

    RestTemplate template = new RestTemplate(factory);
    template.setInterceptors(List.of((request, body, execution) -> {
      JwtBearerJwksUriPolicy.validateRuntimeJwksUri(request.getURI());
      return execution.execute(request, body);
    }));

    return template;
  }
}
