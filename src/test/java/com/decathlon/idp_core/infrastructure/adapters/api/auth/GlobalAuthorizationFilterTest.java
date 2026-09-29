package com.decathlon.idp_core.infrastructure.adapters.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.decathlon.idp_core.domain.exception.authorization.PrincipalNotAuthorizedException;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationMode;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationPolicy;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationRequest;
import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;
import com.decathlon.idp_core.domain.model.principal.PrincipalKind;
import com.decathlon.idp_core.domain.service.authorization.GlobalAuthorizationService;
import com.decathlon.idp_core.domain.service.principal.PrincipalProvisioningService;
import com.decathlon.idp_core.infrastructure.adapters.api.principal.PrincipalExtractor;

class GlobalAuthorizationFilterTest {

  private final PrincipalExtractor principalExtractor = mock(PrincipalExtractor.class);
  private final PrincipalProvisioningService provisioningService = mock(
      PrincipalProvisioningService.class);
  private final GlobalAuthorizationService authorizationService = mock(
      GlobalAuthorizationService.class);
  private final PrincipalInfo principal = new PrincipalInfo("platform-user", PrincipalKind.HUMAN,
      "Platform User", Map.of(), List.of());
  private GlobalAuthorizationFilter filter;
  private Authentication authentication;

  @BeforeEach
  void setUp() {
    var policy = new AuthorizationPolicy(AuthorizationMode.GLOBAL, Set.of());
    filter = new GlobalAuthorizationFilter(principalExtractor, provisioningService,
        authorizationService, policy);
    authentication = mock(Authentication.class);
    when(authentication.isAuthenticated()).thenReturn(true);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    when(principalExtractor.extractPrincipalInfo(authentication)).thenReturn(principal);
    when(provisioningService.getPrincipal("platform-user")).thenReturn(Optional.empty());
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @ParameterizedTest
  @MethodSource("readMethods")
  void shouldMarkReadMethodsAsReadAndContinue(String method) throws ServletException, IOException {
    var request = new MockHttpServletRequest("GET", "/api/v1/entities");
    request.setMethod(method);
    var response = new MockHttpServletResponse();
    FilterChain chain = (req, res) -> {
    };

    filter.doFilter(request, response, chain);

    var requestCaptor = org.mockito.ArgumentCaptor.forClass(AuthorizationRequest.class);
    verify(authorizationService).authorize(requestCaptor.capture(), any(AuthorizationPolicy.class));
    assertThat(requestCaptor.getValue().readOperation()).isTrue();
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void shouldReturnForbiddenAndStopChainWhenAuthorizationIsDenied()
      throws ServletException, IOException {
    var request = new MockHttpServletRequest("POST", "/api/v1/entities");
    var response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    doThrow(new PrincipalNotAuthorizedException("platform-user")).when(authorizationService)
        .authorize(any(AuthorizationRequest.class), any(AuthorizationPolicy.class));

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(403);
    verify(chain, never()).doFilter(any(), any());
  }

  @Test
  void shouldTreatNonUppercaseReadMethodsAsWrites() throws ServletException, IOException {
    var request = new MockHttpServletRequest("POST", "/api/v1/entities");
    request.setMethod("get");
    var response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    doThrow(new PrincipalNotAuthorizedException("platform-user")).when(authorizationService)
        .authorize(any(AuthorizationRequest.class), any(AuthorizationPolicy.class));

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(403);
    verify(chain, never()).doFilter(any(), any());
  }

  @Test
  void shouldFailClosedWhenPrincipalLookupFails() throws ServletException, IOException {
    var request = new MockHttpServletRequest("POST", "/api/v1/entities");
    var response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    when(provisioningService.getPrincipal("platform-user"))
        .thenThrow(new IllegalStateException("catalog unavailable"));

    assertThatThrownBy(() -> filter.doFilter(request, response, chain))
        .isInstanceOf(IllegalStateException.class);
    verify(chain, never()).doFilter(any(), any());
  }

  private static Stream<String> readMethods() {
    return Stream.of("GET", "HEAD", "OPTIONS");
  }
}
