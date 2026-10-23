package com.decathlon.idp_core.infrastructure.adapters.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import com.decathlon.idp_core.domain.model.authorization.AuthorizationMode;
import com.decathlon.idp_core.domain.model.authorization.AuthorizationPolicy;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;
import com.decathlon.idp_core.domain.model.principal.PrincipalKind;
import com.decathlon.idp_core.domain.service.principal.PrincipalProvisioningService;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.AuthenticationProperties;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.PrincipalProvisioningMode;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.PrincipalProvisioningProperties;
import com.decathlon.idp_core.infrastructure.adapters.api.exception.MissingPrincipalIdentifierException;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiErrorResponseWriter;
import com.decathlon.idp_core.infrastructure.adapters.api.principal.PrincipalExtractor;

@ExtendWith(MockitoExtension.class)
class JitProvisioningFilterTest {

  @Mock
  private PrincipalExtractor principalExtractor;

  @Mock
  private PrincipalProvisioningService provisioningService;

  @Mock
  private AuthenticationProperties authProperties;

  @Mock
  private PrincipalProvisioningProperties provisioningProperties;

  @Mock
  private AuthorizationPolicy authorizationPolicy;

  @Mock
  private ApiErrorResponseWriter errorResponseWriter;

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
    when(provisioningProperties.mode()).thenReturn(PrincipalProvisioningMode.JIT);
    Entity principalEntity = new Entity(null, "principal", "User", "user-123",
        List.of(new com.decathlon.idp_core.domain.model.entity.Property(null, "kind", "HUMAN")),
        List.of());
    when(provisioningService.provisionPrincipal(principalInfo)).thenReturn(principalEntity);
    when(provisioningService.findPrincipalWithSameKind(principalInfo))
        .thenReturn(Optional.of(principalEntity));

    // Act
    jitProvisioningFilter.doFilterInternal(request, response, filterChain);

    // Assert
    verify(principalExtractor).extractPrincipalInfo(auth);
    verify(provisioningService).provisionPrincipal(principalInfo);
    verify(request).setAttribute(ProvisionedPrincipalContext.REQUEST_ATTRIBUTE,
        new ProvisionedPrincipalContext(principalInfo, Optional.of(principalEntity)));
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_inLinkOnlyMode_linksExistingPrincipalWithoutProvisioning()
      throws ServletException, IOException {
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    SecurityContextHolder.getContext().setAuthentication(auth);
    PrincipalInfo principalInfo = new PrincipalInfo("user-123", PrincipalKind.HUMAN, "User",
        java.util.Map.of(), List.of());
    Entity principalEntity = new Entity(null, "principal", "User", "user-123",
        List.of(new com.decathlon.idp_core.domain.model.entity.Property(null, "kind", "HUMAN")),
        List.of());
    when(provisioningProperties.mode()).thenReturn(PrincipalProvisioningMode.LINK_ONLY);
    when(principalExtractor.extractPrincipalInfo(auth)).thenReturn(principalInfo);
    when(provisioningService.findPrincipalWithSameKind(principalInfo))
        .thenReturn(Optional.of(principalEntity));

    jitProvisioningFilter.doFilterInternal(request, response, filterChain);

    verify(provisioningService, never()).provisionPrincipal(any());
    verify(request).setAttribute(ProvisionedPrincipalContext.REQUEST_ATTRIBUTE,
        new ProvisionedPrincipalContext(principalInfo, Optional.of(principalEntity)));
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternal_inLinkOnlyMode_rejectsMissingIdentity()
      throws ServletException, IOException {
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    SecurityContextHolder.getContext().setAuthentication(auth);
    PrincipalInfo principalInfo = new PrincipalInfo("unregistered", PrincipalKind.HUMAN, "User",
        java.util.Map.of(), List.of());
    when(provisioningProperties.mode()).thenReturn(PrincipalProvisioningMode.LINK_ONLY);
    when(principalExtractor.extractPrincipalInfo(auth)).thenReturn(principalInfo);

    jitProvisioningFilter.doFilterInternal(request, response, filterChain);

    verify(provisioningService, never()).provisionPrincipal(any());
    verify(errorResponseWriter).write(response, HttpStatus.UNAUTHORIZED,
        "No linked principal matches the authenticated identity");
    verify(filterChain, never()).doFilter(any(), any());
  }

  @Test
  void doFilterInternal_inLinkOnlyMode_provisionsAndAllowsUnlinkedGlobalPrincipal()
      throws ServletException, IOException {
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    SecurityContextHolder.getContext().setAuthentication(auth);
    PrincipalInfo principalInfo = new PrincipalInfo("global-user", PrincipalKind.HUMAN, "User",
        java.util.Map.of(), List.of());
    when(provisioningProperties.mode()).thenReturn(PrincipalProvisioningMode.LINK_ONLY);
    when(authorizationPolicy.mode()).thenReturn(AuthorizationMode.GLOBAL);
    when(authorizationPolicy.globalPrincipalIdentifiers())
        .thenReturn(java.util.Set.of("global-user"));
    when(principalExtractor.extractPrincipalInfo(auth)).thenReturn(principalInfo);
    when(provisioningService.findPrincipalWithSameKind(principalInfo)).thenReturn(Optional.empty());
    Entity principalEntity = new Entity(null, "principal", "User", "global-user",
        List.of(new com.decathlon.idp_core.domain.model.entity.Property(null, "kind", "HUMAN")),
        List.of());
    when(provisioningService.provisionGlobalAdministrator(principalInfo))
        .thenReturn(principalEntity);

    jitProvisioningFilter.doFilterInternal(request, response, filterChain);

    verify(provisioningService).provisionGlobalAdministrator(principalInfo);
    verify(request).setAttribute(ProvisionedPrincipalContext.REQUEST_ATTRIBUTE,
        new ProvisionedPrincipalContext(principalInfo, Optional.of(principalEntity)));
    verify(filterChain).doFilter(request, response);
    verify(errorResponseWriter, never()).write(any(), any(), any());
  }

  @ParameterizedTest(name = "{0} {1} wrongKind={2}")
  @MethodSource("principalLinkScenarios")
  void doFilterInternal_appliesProvisioningModeForBothActorKinds(PrincipalProvisioningMode mode,
      PrincipalKind kind, boolean existingWrongKind) throws ServletException, IOException {
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    SecurityContextHolder.getContext().setAuthentication(auth);
    PrincipalInfo principalInfo = new PrincipalInfo("linked-identity", kind, "Actor",
        java.util.Map.of(), List.of());
    when(provisioningProperties.mode()).thenReturn(mode);
    when(principalExtractor.extractPrincipalInfo(auth)).thenReturn(principalInfo);

    boolean shouldContinue = mode == PrincipalProvisioningMode.JIT && !existingWrongKind;
    if (mode == PrincipalProvisioningMode.LINK_ONLY) {
      when(provisioningService.findPrincipalWithSameKind(principalInfo))
          .thenReturn(Optional.empty());
    } else if (existingWrongKind) {
      String storedKind = kind == PrincipalKind.HUMAN ? "SERVICE_ACCOUNT" : "HUMAN";
      Entity wrongKindPrincipal = new Entity(null, "principal", "Actor", "linked-identity",
          List.of(
              new com.decathlon.idp_core.domain.model.entity.Property(null, "kind", storedKind)),
          List.of());
      when(provisioningService.provisionPrincipal(principalInfo)).thenReturn(wrongKindPrincipal);
      when(provisioningService.findPrincipalWithSameKind(principalInfo))
          .thenReturn(Optional.empty());
    } else {
      Entity matchingPrincipal = new Entity(null, "principal", "Actor", "linked-identity",
          List.of(
              new com.decathlon.idp_core.domain.model.entity.Property(null, "kind", kind.name())),
          List.of());
      when(provisioningService.provisionPrincipal(principalInfo)).thenReturn(matchingPrincipal);
      when(provisioningService.findPrincipalWithSameKind(principalInfo))
          .thenReturn(Optional.of(matchingPrincipal));
    }

    jitProvisioningFilter.doFilterInternal(request, response, filterChain);

    if (shouldContinue) {
      verify(filterChain).doFilter(request, response);
      verify(errorResponseWriter, never()).write(any(), any(), any());
    } else {
      verify(errorResponseWriter).write(response, HttpStatus.UNAUTHORIZED,
          "No linked principal matches the authenticated identity");
      verify(filterChain, never()).doFilter(any(), any());
    }
    if (mode == PrincipalProvisioningMode.LINK_ONLY) {
      verify(provisioningService, never()).provisionPrincipal(any());
    }
  }

  private static Stream<Arguments> principalLinkScenarios() {
    Stream.Builder<Arguments> scenarios = Stream.builder();
    for (PrincipalKind kind : PrincipalKind.values()) {
      for (PrincipalProvisioningMode mode : PrincipalProvisioningMode.values()) {
        scenarios.add(Arguments.of(mode, kind, false));
        scenarios.add(Arguments.of(mode, kind, true));
      }
    }
    return scenarios.build();
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

  @Test
  void doFilterInternal_whenIdentifierClaimIsMissing_rejectsAuthentication()
      throws ServletException, IOException {
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    SecurityContextHolder.getContext().setAuthentication(auth);
    when(principalExtractor.extractPrincipalInfo(auth))
        .thenThrow(new MissingPrincipalIdentifierException("id"));

    jitProvisioningFilter.doFilterInternal(request, response, filterChain);

    verify(errorResponseWriter).write(response, HttpStatus.UNAUTHORIZED,
        "Required principal identifier claim 'id' is missing or blank");
    verify(filterChain, never()).doFilter(any(), any());
    verify(provisioningService, never()).provisionPrincipal(any());
  }

  @Test
  void doFilterInternal_whenOtherPrincipalExtractionFailureOccurs_propagatesIt()
      throws ServletException, IOException {
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(true);
    SecurityContextHolder.getContext().setAuthentication(auth);
    when(principalExtractor.extractPrincipalInfo(auth))
        .thenThrow(new IllegalStateException("Unexpected extraction failure"));

    org.assertj.core.api.Assertions
        .assertThatThrownBy(
            () -> jitProvisioningFilter.doFilterInternal(request, response, filterChain))
        .isInstanceOf(IllegalStateException.class).hasMessage("Unexpected extraction failure");

    verify(errorResponseWriter, never()).write(any(), any(), any());
    verify(filterChain, never()).doFilter(any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"/actuator/health", "/actuator/info", "/swagger-ui/index.html",
      "/swagger-ui/", "/v3/api-docs/swagger-config", "/"})
  void shouldNotFilter_returnsTrueForPublicEndpoints(String path) {
    // Arrange
    // Simulate the default paths loaded from application.yml
    when(authProperties.jitProvisioningExcludedPaths()).thenReturn(
        List.of("/", "/actuator/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**"));
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
    // Simulate the default paths loaded from application.yml
    when(authProperties.jitProvisioningExcludedPaths()).thenReturn(
        List.of("/", "/actuator/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**"));
    when(request.getRequestURI()).thenReturn(path);

    // Act
    boolean result = jitProvisioningFilter.shouldNotFilter(request);

    // Assert
    assertThat(result).isFalse();
  }
}
