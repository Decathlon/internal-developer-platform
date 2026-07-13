package com.decathlon.idp_core.infrastructure.adapters.api.auth.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Objects;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import com.decathlon.idp_core.domain.exception.mock.MockSecurityConfigurationException;

/// Unit tests for MockSecurityConfiguration and MockJwtAuthenticationFilter.
/// Covers mock security setup and JWT token generation for local development.
@DisplayName("MockSecurityConfiguration Tests")
class MockSecurityConfigurationTest {

  @Test
  void shouldInjectMockJwtAuthenticationAndClearAfterwards() throws ServletException, IOException {

    // Given
    MockSecurityConfiguration.MockJwtAuthenticationFilter filter = new MockSecurityConfiguration.MockJwtAuthenticationFilter();
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain filterChain = new FilterChain() {
      @Override
      public void doFilter(ServletRequest request, ServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        assertThat(auth).isNotNull();
        assertThat(auth).isInstanceOf(JwtAuthenticationToken.class);

        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) auth;

        assertThat(jwtAuth.getToken().getClaimAsString("sub")).isEqualTo("local-developer");
        assertThat(jwtAuth.getToken().getClaimAsString("email")).isEqualTo("developer@local.dev");
        assertThat(jwtAuth.getToken().getClaimAsString("client_id")).isEqualTo("client-id");

        assertThat(jwtAuth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("ROLE_USER", "ROLE_API_CLIENT");
      }
    };
    SecurityContextHolder.clearContext();

    // When
    filter.doFilter(request, response, filterChain);

    // Then
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(MockSecurityConfiguration.class));

  @Test
  void shouldLoadConfigurationWhenMockIsEnabled() {
    contextRunner.withPropertyValues("app.security.mock-enabled=true").run(context -> {
      assertThat(context).hasSingleBean(MockSecurityConfiguration.class)
          .hasSingleBean(SecurityFilterChain.class);
      assertThat(context.getBean("securityFilterChainMock")).isNotNull();
    });
  }

  @Test
  void shouldNotLoadConfigurationWhenMockIsDisabled() {
    contextRunner.withPropertyValues("app.security.mock-enabled=false")
        .run(context -> assertThat(context).doesNotHaveBean(MockSecurityConfiguration.class)
            .doesNotHaveBean("securityFilterChainMock"));
  }

  @Test
  void shouldNotLoadConfigurationWhenPropertyIsMissing() {
    contextRunner
        .run(context -> assertThat(context).doesNotHaveBean(MockSecurityConfiguration.class)
            .doesNotHaveBean("securityFilterChainMock"));
  }

  @Test
  void shouldThrowMockSecurityConfigurationExceptionWhenHttpConfigFails() {
    // Given
    MockSecurityConfiguration configuration = new MockSecurityConfiguration();
    HttpSecurity httpSecurityMock = mock(HttpSecurity.class);
    RuntimeException simulatedError = new RuntimeException("Internal simulated Error");
    when(httpSecurityMock.sessionManagement(any())).thenThrow(simulatedError);

    // When & Then
    assertThatThrownBy(() -> configuration.securityFilterChainMock(httpSecurityMock))
        .isInstanceOf(MockSecurityConfigurationException.class)
        .hasMessage("Failed to configure mock security filter chain").hasCause(simulatedError);
  }

  @Nested
  @DisplayName("JWT Token Creation Tests")
  class JwtTokenCreationTests {

    @Test
    @DisplayName("Should create mock JWT with correct subject")
    void shouldCreateMockJwtWithCorrectSubject() throws ServletException, IOException {
      // Given
      MockSecurityConfiguration.MockJwtAuthenticationFilter filter = new MockSecurityConfiguration.MockJwtAuthenticationFilter();
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();

      FilterChain filterChain = new FilterChain() {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
          Authentication auth = SecurityContextHolder.getContext().getAuthentication();
          Jwt jwt = ((JwtAuthenticationToken) auth).getToken();
          assertThat(jwt.getSubject()).isEqualTo("local-developer");
        }
      };

      // When
      filter.doFilter(request, response, filterChain);

      // Then
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should create mock JWT with client_id claim")
    void shouldCreateMockJwtWithClientId() throws ServletException, IOException {
      // Given
      MockSecurityConfiguration.MockJwtAuthenticationFilter filter = new MockSecurityConfiguration.MockJwtAuthenticationFilter();
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();

      FilterChain filterChain = new FilterChain() {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
          Authentication auth = SecurityContextHolder.getContext().getAuthentication();
          Jwt jwt = ((JwtAuthenticationToken) auth).getToken();
          assertThat(jwt.getClaimAsString("client_id")).isEqualTo("client-id");
        }
      };

      // When
      filter.doFilter(request, response, filterChain);

      // Then
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should create mock JWT with scope claim")
    void shouldCreateMockJwtWithScope() throws ServletException, IOException {
      // Given
      MockSecurityConfiguration.MockJwtAuthenticationFilter filter = new MockSecurityConfiguration.MockJwtAuthenticationFilter();
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();

      FilterChain filterChain = new FilterChain() {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
          Authentication auth = SecurityContextHolder.getContext().getAuthentication();
          Jwt jwt = ((JwtAuthenticationToken) auth).getToken();
          assertThat(jwt.getClaimAsString("scope")).isEqualTo("auth read write");
        }
      };

      // When
      filter.doFilter(request, response, filterChain);

      // Then
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should create mock JWT with valid timestamps")
    void shouldCreateMockJwtWithValidTimestamps() throws ServletException, IOException {
      // Given
      MockSecurityConfiguration.MockJwtAuthenticationFilter filter = new MockSecurityConfiguration.MockJwtAuthenticationFilter();
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();

      FilterChain filterChain = new FilterChain() {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
          Authentication auth = SecurityContextHolder.getContext().getAuthentication();
          Jwt jwt = ((JwtAuthenticationToken) auth).getToken();
          assertThat(jwt.getIssuedAt()).isNotNull();
          assertThat(jwt.getExpiresAt()).isNotNull().isAfter(jwt.getIssuedAt());
        }
      };

      // When
      filter.doFilter(request, response, filterChain);

      // Then
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should create mock JWT with 1-hour expiration")
    void shouldCreateMockJwtWith1HourExpiration() throws ServletException, IOException {
      // Given
      MockSecurityConfiguration.MockJwtAuthenticationFilter filter = new MockSecurityConfiguration.MockJwtAuthenticationFilter();
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();

      FilterChain filterChain = new FilterChain() {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
          Authentication auth = SecurityContextHolder.getContext().getAuthentication();
          Jwt jwt = ((JwtAuthenticationToken) Objects.requireNonNull(auth)).getToken();
          long expirationDurationSeconds = Objects.requireNonNull(jwt.getExpiresAt())
              .getEpochSecond() - Objects.requireNonNull(jwt.getIssuedAt()).getEpochSecond();
          assertThat(expirationDurationSeconds).isEqualTo(3600);
        }
      };

      // When
      filter.doFilter(request, response, filterChain);
    }

    @Test
    @DisplayName("Should create mock JWT with additional user claims")
    void shouldCreateMockJwtWithAdditionalUserClaims() throws ServletException, IOException {
      // Given
      MockSecurityConfiguration.MockJwtAuthenticationFilter filter = new MockSecurityConfiguration.MockJwtAuthenticationFilter();
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();

      FilterChain filterChain = new FilterChain() {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
          Authentication auth = SecurityContextHolder.getContext().getAuthentication();
          Jwt jwt = ((JwtAuthenticationToken) Objects.requireNonNull(auth)).getToken();
          assertThat(jwt.getClaimAsString("user_id")).isEqualTo("dev-user-001");
          assertThat(jwt.getClaimAsString("email")).isEqualTo("developer@local.dev");
        }
      };

      // When
      filter.doFilter(request, response, filterChain);
    }

    @Test
    @DisplayName("Should create mock JWT with correct header")
    void shouldCreateMockJwtWithCorrectHeader() throws ServletException, IOException {
      // Given
      MockSecurityConfiguration.MockJwtAuthenticationFilter filter = new MockSecurityConfiguration.MockJwtAuthenticationFilter();
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();

      FilterChain filterChain = new FilterChain() {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
          Authentication auth = SecurityContextHolder.getContext().getAuthentication();
          Jwt jwt = ((JwtAuthenticationToken) Objects.requireNonNull(auth)).getToken();
          assertThat(jwt.getHeaders()).containsEntry("alg", "RS256");
          assertThat(jwt.getHeaders()).containsEntry("typ", "JWT");
        }
      };
      // When
      filter.doFilter(request, response, filterChain);
    }
  }

  @Nested
  @DisplayName("Filter Chain Exception Handling Tests")
  class FilterChainExceptionHandlingTests {

    @Test
    @DisplayName("Should clear SecurityContext even if filter chain throws ServletException")
    void shouldClearSecurityContextOnServletException() {
      // Given
      MockSecurityConfiguration.MockJwtAuthenticationFilter filter = new MockSecurityConfiguration.MockJwtAuthenticationFilter();
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();

      FilterChain filterChain = new FilterChain() {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response)
            throws ServletException {
          throw new ServletException("Test exception");
        }
      };

      // When & Then
      assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
          .isInstanceOf(ServletException.class);

      // Context should be cleared
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should clear SecurityContext even if filter chain throws IOException")
    void shouldClearSecurityContextOnIOException() {
      // Given
      MockSecurityConfiguration.MockJwtAuthenticationFilter filter = new MockSecurityConfiguration.MockJwtAuthenticationFilter();
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();

      FilterChain filterChain = new FilterChain() {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException {
          throw new IOException("Test IO exception");
        }
      };

      // When & Then
      assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
          .isInstanceOf(IOException.class);

      // Context should be cleared
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
  }

  @Nested
  @DisplayName("Authority Tests")
  class AuthorityTests {

    @Test
    @DisplayName("Should include both ROLE_USER and ROLE_API_CLIENT authorities")
    void shouldIncludeBothRoles() throws ServletException, IOException {
      // Given
      MockSecurityConfiguration.MockJwtAuthenticationFilter filter = new MockSecurityConfiguration.MockJwtAuthenticationFilter();
      MockHttpServletRequest request = new MockHttpServletRequest();
      MockHttpServletResponse response = new MockHttpServletResponse();

      FilterChain filterChain = new FilterChain() {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
          Authentication auth = SecurityContextHolder.getContext().getAuthentication();
          assertThat(Objects.requireNonNull(auth).getAuthorities())
              .extracting(GrantedAuthority::getAuthority)
              .containsExactlyInAnyOrder("ROLE_USER", "ROLE_API_CLIENT");
        }
      };

      // When
      filter.doFilter(request, response, filterChain);
    }
  }
}
