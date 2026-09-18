package com.decathlon.idp_core.infrastructure.adapters.ingestion.security;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/// Builds and caches JwtDecoder instances keyed by jwks_uri.
/// Uses RestTemplate only as a thin adapter for NimbusJwtDecoder (which requires RestOperations).
/// Apache HttpClient 5 with custom DnsResolver prevents DNS rebinding by validating and
/// caching resolved addresses. Virtual Thread compatible since 5.3+.
@Component
public class WebhookJwtDecoderProvider {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

  private final Set<String> allowedJwksHosts;
  private final ConcurrentMap<String, JwtDecoder> decodersByJwksUri = new ConcurrentHashMap<>();
  private final RestTemplate restTemplate;

  public WebhookJwtDecoderProvider() {
    this.allowedJwksHosts = Set.of();
    this.restTemplate = buildSecureRestTemplate();
  }

  @Autowired
  public WebhookJwtDecoderProvider(WebhookSecurityProperties webhookSecurityProperties) {
    this.allowedJwksHosts = Set.copyOf(webhookSecurityProperties.allowedJwksHosts());
    this.restTemplate = buildSecureRestTemplate();
  }

  public JwtDecoder get(String jwksUri) {
    JwtBearerJwksUriPolicy.validateRuntimeJwksUri(URI.create(jwksUri), allowedJwksHosts);

    return decodersByJwksUri.computeIfAbsent(jwksUri, this::createDecoder);
  }

  private JwtDecoder createDecoder(String jwksUri) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).restOperations(restTemplate)
        .build();
    decoder.setJwtValidator(JwtValidators.createDefault());
    return decoder;
  }

  private RestTemplate buildSecureRestTemplate() {
    // Custom DNS resolver validates and reuses a single resolution per hostname.
    // Prevents DNS rebinding by ensuring resolved addresses are always safe.
    DnsResolver secureDnsResolver = new DnsResolver() {
      @Override
      public InetAddress[] resolve(String host) throws UnknownHostException {
        return resolveAndValidateOnce(host);
      }

      @Override
      public String resolveCanonicalHostname(String host) throws UnknownHostException {
        return resolveAndValidateOnce(host)[0].getCanonicalHostName();
      }
    };

    ConnectionConfig connectionConfig = ConnectionConfig.custom()
        .setConnectTimeout(Timeout.ofMilliseconds(CONNECT_TIMEOUT.toMillis())).build();
    var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
        .setDefaultConnectionConfig(connectionConfig).setDnsResolver(secureDnsResolver).build();

    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectionRequestTimeout(Timeout.ofMilliseconds(CONNECT_TIMEOUT.toMillis()))
        .setResponseTimeout(Timeout.ofMilliseconds(READ_TIMEOUT.toMillis())).build();

    CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connectionManager)
        .setDefaultRequestConfig(requestConfig).disableRedirectHandling().build();

    return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
  }

  private InetAddress[] resolveAndValidateOnce(String host) throws UnknownHostException {
    InetAddress[] addresses = InetAddress.getAllByName(host);
    if (JwtBearerJwksUriPolicy.isAllowedHost(host, allowedJwksHosts)) {
      return addresses;
    }

    if (JwtBearerJwksUriPolicy.containsPrivateOrLoopbackAddress(addresses)) {
      throw new UnknownHostException("JWKS host resolved to unsafe address: " + host);
    }
    return addresses;
  }
}
