package com.decathlon.idp_core.infrastructure.adapters.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;
import com.decathlon.idp_core.domain.model.principal.PrincipalKind;
import com.decathlon.idp_core.domain.service.principal.PrincipalProvisioningService;
import com.decathlon.idp_core.infrastructure.adapters.api.principal.PrincipalExtractor;

@ExtendWith(MockitoExtension.class)
class JitProvisioningFilterTest {

  @Mock
  private PrincipalExtractor principalExtractor;

  @Mock
  private PrincipalProvisioningService provisioningService;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  @InjectMocks
  private JitProvisioningFilter jitProvisioningFilter;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void doFilterInternal_whenAuthenticatedUser_provisionsPrincipalAndContinuesChain()
      throws ServletException, IOException {
    // Arrange
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    SecurityContextHolder.getContext().setAuthentication(auth);

    PrincipalInfo principalInfo = mock(PrincipalInfo.class);
    when(principalInfo.identifier()).thenReturn("user-123");
    when(principalInfo.kind()).thenReturn(PrincipalKind.valueOf("HUMAN"));
    when(principalExtractor.extractPrincipalInfo(auth)).thenReturn(principalInfo);

    // Act
    jitProvisioningFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(principalExtractor).extractPrincipalInfo(auth);
    verify(provisioningService).provisionPrincipal(principalInfo);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_whenAuthenticationIsNull_skipsProvisioningAndContinuesChain()
      throws ServletException, IOException {
    // Arrange: SecurityContextHolder has null authentication by default

    // Act
    jitProvisioningFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(principalExtractor, never()).extractPrincipalInfo(any());
    verify(provisioningService, never()).provisionPrincipal(any());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_whenAuthenticationNotAuthenticated_skipsProvisioningAndContinuesChain()
      throws ServletException, IOException {
    // Arrange
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(false);
    SecurityContextHolder.getContext().setAuthentication(auth);

    // Act
    jitProvisioningFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(principalExtractor, never()).extractPrincipalInfo(any());
    verify(provisioningService, never()).provisionPrincipal(any());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_whenAnonymousAuthentication_skipsProvisioningAndContinuesChain()
      throws ServletException, IOException {
    // Arrange
    AnonymousAuthenticationToken anonymousAuth = new AnonymousAuthenticationToken("key",
        "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    SecurityContextHolder.getContext().setAuthentication(anonymousAuth);

    // Act
    jitProvisioningFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(principalExtractor, never()).extractPrincipalInfo(any());
    verify(provisioningService, never()).provisionPrincipal(any());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_whenProvisioningThrowsException_failsOpenAndContinuesChain()
      throws ServletException, IOException {
    // Arrange
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    SecurityContextHolder.getContext().setAuthentication(auth);

    PrincipalInfo principalInfo = mock(PrincipalInfo.class);
    when(principalExtractor.extractPrincipalInfo(auth)).thenReturn(principalInfo);
    doThrow(new RuntimeException("Catalog service unavailable")).when(provisioningService)
        .provisionPrincipal(principalInfo);

    // Act & Assert (Should not throw exception)
    jitProvisioningFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/actuator/health", "/actuator/info", "/swagger-ui/index.html",
      "/swagger-ui/", "/v3/api-docs/swagger-config", "/"})
  void shouldNotFilter_returnsTrueForPublicEndpoints(String path) {
    // Arrange
    when(request.getRequestURI()).thenReturn(path);

    // Act
    boolean result = jitProvisioningFilter.shouldNotFilter(request);

    // Assert
    assertThat(result).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"/api/v1/users", "/api/v1/catalog", "/actuator-custom", "/swagger"})
  void shouldNotFilter_returnsFalseForProtectedEndpoints(String path) {
    // Arrange
    when(request.getRequestURI()).thenReturn(path);

    // Act
    boolean result = jitProvisioningFilter.shouldNotFilter(request);

    // Assert
    assertThat(result).isFalse();
  }
}
