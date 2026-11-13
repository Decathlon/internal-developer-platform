package com.decathlon.idp_core.infrastructure.adapters.api.auth.http_message_signature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.decathlon.idp_core.infrastructure.adapters.api.auth.GlobalAuthorizationFilter;
import com.decathlon.idp_core.infrastructure.adapters.api.auth.JitProvisioningFilter;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.AuthenticationProperties;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.HttpMessageSignatureAlgorithm;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.HttpMessageSignatureConfiguration;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.HttpMessageSignatureProperties;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.security.chains.ApiKeyFilterChainConfig;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.security.chains.JwtFilterChainConfig;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.security.chains.MockFilterChainConfig;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.security.chains.OAuth2LoginFilterChainConfig;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.security.chains.PublicFilterChainConfig;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiErrorResponseWriter;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionProperties;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionSecurityConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;

class HttpMessageSignatureConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(HttpMessageSignatureConfiguration.class)
      .withBean(ApiErrorResponseWriter.class, () -> new ApiErrorResponseWriter(new ObjectMapper()));

  @Test
  void defaultEnablementRequiresSecretAndFailsWithoutExposingIt() {
    contextRunner.run(context -> assertThat(context).hasFailed());
    contextRunner.withPropertyValues("app.security.http-message-signature-verification.secret= ")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void defaultsBindAndServletAutoRegistrationIsDisabled() {
    contextRunner
        .withPropertyValues("app.security.http-message-signature-verification.secret=dummy-secret")
        .run(context -> {
          assertThat(context).hasNotFailed().hasSingleBean(HttpMessageSignatureFilter.class);
          var properties = context.getBean(HttpMessageSignatureProperties.class);
          assertThat(properties.enabled()).isTrue();
          assertThat(properties.identifierHeaderName()).isEqualTo("x-request-id");
          assertThat(properties.algorithm()).isEqualTo(HttpMessageSignatureAlgorithm.HMAC_SHA256);
          assertThat(properties.validityDurationSeconds()).isEqualTo(30);
          assertThat(properties.clockSkewSeconds()).isZero();
          assertThat(properties.excludedPaths()).contains("/actuator/health", "/swagger-ui/**");
          assertThat(properties.toString()).contains("[REDACTED]").doesNotContain("dummy-secret");
          assertThat(context.getBean(FilterRegistrationBean.class).isEnabled()).isFalse();
        });
  }

  @Test
  void disabledFeatureNeedsNoSecretAndRegistersNoFilter() {
    contextRunner
        .withPropertyValues("app.security.http-message-signature-verification.enabled=false")
        .run(context -> {
          assertThat(context).hasNotFailed().doesNotHaveBean(HttpMessageSignatureFilter.class)
              .doesNotHaveBean(FilterRegistrationBean.class);
          assertThat(
              context.getBean(HttpMessageSignatureConfiguration.ChainConfigurer.class).filter())
                  .isEmpty();
        });
  }

  @Test
  void verificationConfigurationBindsCurrentPolicy() {
    contextRunner
        .withPropertyValues("app.security.http-message-signature-verification.secret=dummy-secret",
            "app.security.http-message-signature-verification.identifier-header-name=x-client-id")
        .run(context -> {
          assertThat(context).hasNotFailed().hasSingleBean(HttpMessageSignatureFilter.class);
          var properties = context.getBean(HttpMessageSignatureProperties.class);
          assertThat(properties.enabled()).isTrue();
          assertThat(properties.identifierHeaderName()).isEqualTo("x-client-id");
          assertThat(properties.algorithm()).isEqualTo(HttpMessageSignatureAlgorithm.HMAC_SHA256);
          assertThat(properties.validityDurationSeconds()).isEqualTo(30);
          assertThat(properties.secret()).isEqualTo("dummy-secret");
        });
  }

  @ParameterizedTest
  @ValueSource(strings = {"identifier-header-name=Authorization",
      "identifier-header-name=Signature", "identifier-header-name=bad header",
      "clock-skew-seconds=-1", "validity-duration-seconds=0", "validity-duration-seconds=-1",
      "algorithm=HMAC_MD5", "excluded-paths=relative"})
  void invalidPolicyFailsStartup(String setting) {
    contextRunner
        .withPropertyValues("app.security.http-message-signature-verification.secret=dummy-secret",
            "app.security.http-message-signature-verification." + setting)
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void exclusionsAreReplacedAndCanBeEmpty() {
    contextRunner
        .withPropertyValues("app.security.http-message-signature-verification.enabled=false",
            "app.security.http-message-signature-verification.excluded-paths=/custom/**")
        .run(context -> {
          assertThat(context.getBean(HttpMessageSignatureProperties.class).excludedPaths())
              .containsExactly("/custom/**");
        });
    contextRunner
        .withPropertyValues("app.security.http-message-signature-verification.enabled=false",
            "app.security.http-message-signature-verification.excluded-paths=")
        .run(context -> {
          assertThat(context.getBean(HttpMessageSignatureProperties.class).excludedPaths())
              .isEmpty();
        });
    var paths = new ArrayList<>(List.of("/custom/**"));
    var properties = new HttpMessageSignatureProperties(false, null, "request-id",
        HttpMessageSignatureAlgorithm.HMAC_SHA256, 30, 0, paths);
    paths.clear();
    assertThat(properties.excludedPaths()).containsExactly("/custom/**");
    var excludedPaths = properties.excludedPaths();
    assertThatThrownBy(excludedPaths::clear).isInstanceOf(UnsupportedOperationException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"jwt", "api-key", "mock", "oauth2-login"})
  void everyRealChainContainsExactlyOneSignatureFilterAfterCorsAndBeforeAuthentication(
      String mode) {
    chainRunner(mode).run(context -> {
      assertThat(context).hasNotFailed();
      assertThat(context.getBeansOfType(SecurityFilterChain.class)).hasSize(3);
      context.getBeansOfType(SecurityFilterChain.class).values().forEach(chain -> {
        var filters = chain.getFilters();
        assertThat(filters.stream().filter(HttpMessageSignatureFilter.class::isInstance))
            .hasSize(1);
        int signatureIndex = filters.indexOf(context.getBean(HttpMessageSignatureFilter.class));
        for (int i = 0; i < filters.size(); i++) {
          if (filters.get(i) instanceof CorsFilter) {
            assertThat(signatureIndex).isEqualTo(i + 1);
          }
          if (filters.get(i) instanceof CsrfFilter
              || filters.get(i) instanceof BearerTokenAuthenticationFilter
              || filters.get(i) instanceof AbstractAuthenticationProcessingFilter
              || filters.get(i) instanceof OAuth2AuthorizationRequestRedirectFilter
              || filters.get(i) instanceof JitProvisioningFilter
              || filters.get(i) instanceof GlobalAuthorizationFilter
              || filters.get(i) instanceof AnonymousAuthenticationFilter) {
            assertThat(signatureIndex).isLessThan(i);
          }
        }
      });
    });
  }

  @Test
  void disabledFeatureIsAbsentFromAllRealChains() {
    chainRunner("jwt")
        .withPropertyValues("app.security.http-message-signature-verification.enabled=false")
        .run(context -> {
          assertThat(context).hasNotFailed();
          context.getBeansOfType(SecurityFilterChain.class).values()
              .forEach(chain -> assertThat(chain.getFilters())
                  .noneMatch(HttpMessageSignatureFilter.class::isInstance));
        });
  }

  private WebApplicationContextRunner chainRunner(String mode) {
    var client = ClientRegistration.withRegistrationId("test").clientId("client")
        .clientSecret("dummy").authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
        .authorizationUri("https://example.com/authorize").tokenUri("https://example.com/token")
        .userInfoUri("https://example.com/user").userNameAttributeName("id").build();
    return new WebApplicationContextRunner().withUserConfiguration(Chains.class)
        .withBean("corsConfigurationSource", CorsConfigurationSource.class,
            UrlBasedCorsConfigurationSource::new)
        .withPropertyValues("app.security.http-message-signature-verification.secret=dummy-secret",
            "app.security.authentication." + mode + ".enabled=true")
        .withBean(ApiErrorResponseWriter.class,
            () -> new ApiErrorResponseWriter(new ObjectMapper()))
        .withBean(AuthenticationProperties.class,
            () -> new AuthenticationProperties(Map.of(),
                List.of("/", "/actuator/**", "/swagger-ui/**", "/v3/api-docs/**")))
        .withBean(IngestionProperties.class, () -> new IngestionProperties("/webhooks", "/{id}"))
        .withBean(JitProvisioningFilter.class, () -> mock(JitProvisioningFilter.class))
        .withBean(GlobalAuthorizationFilter.class, () -> mock(GlobalAuthorizationFilter.class))
        .withBean(JwtAuthenticationConverter.class, JwtAuthenticationConverter::new)
        .withBean(JwtDecoder.class, () -> mock(JwtDecoder.class))
        .withBean(ClientRegistrationRepository.class,
            () -> new InMemoryClientRegistrationRepository(client));
  }

  @Configuration(proxyBeanMethods = false)
  @EnableWebSecurity
  @Import({HttpMessageSignatureConfiguration.class, PublicFilterChainConfig.class,
      JwtFilterChainConfig.class, ApiKeyFilterChainConfig.class, MockFilterChainConfig.class,
      OAuth2LoginFilterChainConfig.class, IngestionSecurityConfiguration.class})
  static class Chains {
  }
}
