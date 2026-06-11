package com.decathlon.idp_core.infrastructure.adapters.api.auth.mock;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

import com.decathlon.idp_core.domain.exception.mock.MockSecurityConfigurationException;

/// Local mock security configuration that mirrors OAuth2/JWT behavior for local development.
///
/// **Purpose:** Allows local development without a real OAuth2/JWT provider.
/// Enabled only when `app.security.mock-enabled=true`.
///
/// **Mock JWT Token Details:**
/// - Subject (sub): "local-developer"
/// - Client ID (client_id): "client-credentials"
/// - Scopes: auth, read, write
/// - Issued at: Current time
/// - Expires in: 1 hour
/// - Additional claims: Mock user information
///
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "app.security.mock-enabled", havingValue = "true")
public class MockSecurityConfiguration {

  /// Security filter chain for local mocking with JWT-like behavior.
  ///
  /// **Configuration:**
  /// - Session: Stateless (CSRF protection not needed for token-based
  /// authentication)
  /// - Authorization: All requests permitted (mock authentication injected by
  /// filter)
  /// - Custom filter: Adds MockJwtAuthenticationFilter before
  /// AnonymousAuthenticationFilter
  ///
  /// @param http HttpSecurity to configure
  /// @return Configured security filter chain
  @Bean
  public SecurityFilterChain securityFilterChainMock(HttpSecurity http) {
    try {
      http.sessionManagement(
          session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(authMocked -> authMocked.anyRequest().permitAll())
          .addFilterBefore(new MockJwtAuthenticationFilter(),
              org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class);
    } catch (Exception e) {
      throw new MockSecurityConfigurationException("Failed to configure mock security filter chain",
          e);
    }
    return http.build();
  }

  /// Filter that injects mock JWT authentication into SecurityContext.
  ///
  /// **Behavior:**
  /// - Creates a JwtAuthenticationToken from the mock JWT token
  /// - Sets it in the SecurityContextHolder for the current request
  /// - Allows downstream code to access authentication details normally
  ///
  /// **Why a filter:** Ensures authentication is set early in the request cycle,
  /// making it available to all downstream components (controllers, services,
  /// etc.)
  static class MockJwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request,
        @Nonnull HttpServletResponse response, @Nonnull FilterChain filterChain)
        throws ServletException, IOException {
      // Create mock JWT and authentication token
      Jwt mockJwt = createMockJwt();
      Collection<GrantedAuthority> authorities = createMockAuthorities();
      Authentication authentication = new JwtAuthenticationToken(mockJwt, authorities);

      // Set in SecurityContext for this request
      SecurityContextHolder.getContext().setAuthentication(authentication);

      try {
        filterChain.doFilter(request, response);
      } finally {
        // Clean up SecurityContext after request
        SecurityContextHolder.clearContext();
      }
    }

    /// Creates a mock JWT token with standard OAuth2 claims for local development.
    ///
    /// **Mock token details:**
    /// - sub: "local-developer" - The subject/principal
    /// - client_id: "client-credentials" - OAuth2 client identifier
    /// - scope: "auth read write" - Space-separated scopes
    /// - iat: current time - Issued at timestamp
    /// - exp: current time + 3600 - Expires in 1 hour
    ///
    /// @return Mock JWT token ready for authentication
    private Jwt createMockJwt() {
      Instant now = Instant.now();
      Instant expiresAt = now.plusSeconds(3600);

      Map<String, Object> headers = Map.of("alg", "RS256", "typ", "JWT");

      Map<String, Object> claims = Map.of("sub", "local-developer", "client_id", "client-id",
          "scope", "auth read write", "iat", now.getEpochSecond(), "exp",
          expiresAt.getEpochSecond(), "user_id", "dev-user-001", "email", "developer@local.dev");

      return new Jwt("mock-token-value", now, expiresAt, headers, claims);
    }

    /// Creates mock authorities for the authenticated principal.
    ///
    /// **Mock authorities:**
    /// - ROLE_USER: Standard user role
    /// - ROLE_API_CLIENT: API client role for programmatic access
    ///
    /// @return Collection of granted authorities
    private Collection<GrantedAuthority> createMockAuthorities() {
      return List.of(new SimpleGrantedAuthority("ROLE_USER"),
          new SimpleGrantedAuthority("ROLE_API_CLIENT"));
    }

  }
}
