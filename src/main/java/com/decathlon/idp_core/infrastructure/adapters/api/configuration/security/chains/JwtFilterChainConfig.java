package com.decathlon.idp_core.infrastructure.adapters.api.configuration.security.chains;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import com.decathlon.idp_core.infrastructure.adapters.api.auth.GlobalAuthorizationFilter;
import com.decathlon.idp_core.infrastructure.adapters.api.auth.JitProvisioningFilter;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.HttpMessageSignatureConfiguration.ChainConfigurer;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiErrorResponseWriter;

@Configuration
@ConditionalOnProperty(prefix = "app.security.authentication.jwt", name = "enabled", havingValue = "true", matchIfMissing = false)
public class JwtFilterChainConfig {

  private final JitProvisioningFilter jitProvisioningFilter;
  private final GlobalAuthorizationFilter globalAuthorizationFilter;
  private final JwtAuthenticationConverter jwtAuthenticationConverter;
  private final ApiErrorResponseWriter errorResponseWriter;

  public JwtFilterChainConfig(JitProvisioningFilter jitProvisioningFilter,
      GlobalAuthorizationFilter globalAuthorizationFilter,
      JwtAuthenticationConverter jwtAuthenticationConverter,
      ApiErrorResponseWriter errorResponseWriter) {
    this.jitProvisioningFilter = jitProvisioningFilter;
    this.globalAuthorizationFilter = globalAuthorizationFilter;
    this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    this.errorResponseWriter = errorResponseWriter;
  }

  @Bean
  @Order(2)
  public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http,
      ChainConfigurer httpMessageSignature) {
    var bearerTokenAuthenticationEntryPoint = new BearerTokenAuthenticationEntryPoint();
    var bearerTokenAccessDeniedHandler = new BearerTokenAccessDeniedHandler();
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/**").fullyAuthenticated().anyRequest().authenticated())
        .cors(withDefaults())
        .oauth2ResourceServer(
            oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                .authenticationEntryPoint((request, response, exception) -> {
                  bearerTokenAuthenticationEntryPoint.commence(request, response, exception);
                  errorResponseWriter.write(request, response,
                      HttpStatus.valueOf(response.getStatus()), "A valid bearer token is required");
                }).accessDeniedHandler((request, response, exception) -> {
                  bearerTokenAccessDeniedHandler.handle(request, response, exception);
                  errorResponseWriter.write(request, response, HttpStatus.FORBIDDEN,
                      "Access is denied");
                }))
        .addFilterAfter(jitProvisioningFilter, BearerTokenAuthenticationFilter.class)
        .addFilterAfter(globalAuthorizationFilter, JitProvisioningFilter.class);

    httpMessageSignature.configure(http);
    return http.build();
  }
}
