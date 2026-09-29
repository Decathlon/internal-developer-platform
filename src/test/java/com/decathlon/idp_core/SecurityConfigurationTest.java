package com.decathlon.idp_core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import com.decathlon.idp_core.infrastructure.adapters.api.configuration.CorsProperties;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.SecurityConfiguration;

@ExtendWith(MockitoExtension.class)
class SecurityConfigurationTest {

  @Mock
  private CorsProperties corsProperties;

  @InjectMocks
  private SecurityConfiguration securityConfiguration;

  @Test
  void jwtAuthenticationConverter_shouldNotAssignUniversalBaselineAuthority() {
    Jwt jwt = Jwt.withTokenValue("mock-jwt-token").header("alg", "none").claim("sub", "user-id-123")
        .claim("scope", "read write").build();

    JwtAuthenticationConverter converter = securityConfiguration.jwtAuthenticationConverter();
    var authenticationToken = converter.convert(jwt);

    assertNotNull(authenticationToken, "Authentication token should not be null");
    assertTrue(authenticationToken.getAuthorities().stream()
        .anyMatch(authority -> authority.getAuthority().equals("SCOPE_read")));
    assertFalse(authenticationToken.getAuthorities().stream()
        .anyMatch(authority -> authority.getAuthority().equals("*")));
  }

  @Test
  void corsConfigurationSource_withEmptyProperties_shouldNotSetOrigins() {
    // Arrange
    when(corsProperties.allowedOrigins()).thenReturn(List.of());
    when(corsProperties.allowedOriginPatterns()).thenReturn(List.of());

    // Act
    CorsConfigurationSource source = securityConfiguration.corsConfigurationSource();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
    CorsConfiguration config = source.getCorsConfiguration(request);

    // Assert
    assertNotNull(config);
    assertNull(config.getAllowedOrigins(),
        "Allowed origins should remain null if property is empty");
    assertNull(config.getAllowedOriginPatterns(),
        "Allowed origin patterns should remain null if property is empty");

    assertEquals(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"), config.getAllowedMethods());
    assertEquals(List.of("*"), config.getAllowedHeaders());
    assertTrue(config.getAllowCredentials());
  }

  @Test
  void corsConfigurationSource_withConfiguredProperties_shouldSetOrigins() {
    // Arrange
    when(corsProperties.allowedOrigins()).thenReturn(List.of("http://localhost:8080"));
    when(corsProperties.allowedOriginPatterns()).thenReturn(List.of("https://*.decathlon.com"));

    // Act
    CorsConfigurationSource source = securityConfiguration.corsConfigurationSource();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
    CorsConfiguration config = source.getCorsConfiguration(request);

    // Assert
    assertNotNull(config);
    assertEquals(List.of("http://localhost:8080"), config.getAllowedOrigins());
    assertEquals(List.of("https://*.decathlon.com"), config.getAllowedOriginPatterns());

    assertEquals(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"), config.getAllowedMethods());
    assertEquals(List.of("*"), config.getAllowedHeaders());
    assertTrue(config.getAllowCredentials());
  }
}
