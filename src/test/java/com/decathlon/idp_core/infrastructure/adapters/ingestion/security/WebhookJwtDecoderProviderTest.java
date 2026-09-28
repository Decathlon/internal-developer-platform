package com.decathlon.idp_core.infrastructure.adapters.ingestion.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookJwksHostForbiddenException;

@DisplayName("WebhookJwtDecoderProvider Tests")
class WebhookJwtDecoderProviderTest {

  @Test
  @DisplayName("uses Apache HttpClient 5 with custom DnsResolver to prevent DNS rebinding")
  void buildSecureRestTemplate_usesApacheHttpClientWithSecureDnsResolver() throws Exception {
    WebhookJwtDecoderProvider provider = new WebhookJwtDecoderProvider();
    RestTemplate restTemplate = extractRestTemplate(provider);

    assertThat(restTemplate.getRequestFactory())
        .isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
  }

  @Test
  @DisplayName("accepts an allow-listed localhost JWKS host at runtime")
  void get_acceptsAllowListedLocalhost() {
    WebhookJwtDecoderProvider provider = new WebhookJwtDecoderProvider(
        new WebhookSecurityProperties(Set.of("localhost")));

    assertThatCode(() -> provider.get("https://localhost/.well-known/jwks.json"))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("rejects a runtime JWKS host that is not allow-listed")
  void get_rejectsNonAllowListedHost() {
    WebhookJwtDecoderProvider provider = new WebhookJwtDecoderProvider(
        new WebhookSecurityProperties(Set.of("auth.decathlon.com")));

    assertThatThrownBy(() -> provider.get("https://github.com/.well-known/jwks.json"))
        .isInstanceOf(WebhookJwksHostForbiddenException.class);
  }

  private RestTemplate extractRestTemplate(WebhookJwtDecoderProvider provider) throws Exception {
    Field field = WebhookJwtDecoderProvider.class.getDeclaredField("restTemplate");
    field.setAccessible(true);
    return (RestTemplate) field.get(provider);
  }
}
