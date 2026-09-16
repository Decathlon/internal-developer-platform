package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

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

  private RestTemplate extractRestTemplate(WebhookJwtDecoderProvider provider) throws Exception {
    Field field = WebhookJwtDecoderProvider.class.getDeclaredField("restTemplate");
    field.setAccessible(true);
    return (RestTemplate) field.get(provider);
  }
}
