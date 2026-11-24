package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import java.time.Clock;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.filter.CorsFilter;

import com.decathlon.idp_core.infrastructure.adapters.api.auth.http_message_signature.HttpMessageSignatureFilter;
import com.decathlon.idp_core.infrastructure.adapters.api.auth.http_message_signature.HttpMessageSignatureVerifier;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiErrorResponseWriter;

/// Shared HTTP message signature enforcement wiring for every HTTP security chain.
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HttpMessageSignatureProperties.class)
public class HttpMessageSignatureConfiguration {

  /// Adds HTTP message signature verification only when enabled, without
  /// registering a servlet
  /// filter.
  @Bean
  public ChainConfigurer httpMessageSignatureChainConfigurer(
      Optional<HttpMessageSignatureFilter> filter) {
    return new ChainConfigurer(filter);
  }

  /// Creates the enabled HTTP message signature verification filter with the
  /// system UTC clock.
  @Bean
  @ConditionalOnProperty(prefix = "app.security.http-message-signature-verification", name = "enabled", havingValue = "true", matchIfMissing = true)
  public HttpMessageSignatureFilter httpMessageSignatureFilter(
      HttpMessageSignatureProperties properties, ApiErrorResponseWriter errorWriter) {
    return new HttpMessageSignatureFilter(properties,
        new HttpMessageSignatureVerifier(properties, Clock.systemUTC()), errorWriter);
  }

  /// Prevents Spring Boot from also running the filter outside Spring Security.
  @Bean
  @ConditionalOnProperty(prefix = "app.security.http-message-signature-verification", name = "enabled", havingValue = "true", matchIfMissing = true)
  public FilterRegistrationBean<HttpMessageSignatureFilter> httpMessageSignatureRegistration(
      HttpMessageSignatureFilter filter) {
    var registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  /// Applies the same optional filter to each independently matched security
  /// chain.
  public record ChainConfigurer(Optional<HttpMessageSignatureFilter> filter) {
    /// Lets CORS handle preflights before verification, CSRF, and authentication.
    public void configure(HttpSecurity http) {
      filter.ifPresent(value -> http.addFilterAfter(value, CorsFilter.class));
    }
  }
}
