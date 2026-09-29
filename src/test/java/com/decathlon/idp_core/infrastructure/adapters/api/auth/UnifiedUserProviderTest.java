package com.decathlon.idp_core.infrastructure.adapters.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;
import com.decathlon.idp_core.domain.model.principal.PrincipalKind;
import com.decathlon.idp_core.infrastructure.adapters.api.principal.PrincipalExtractor;

class UnifiedUserProviderTest {

  private UnifiedUserProvider unifiedUserProvider;
  private SecurityContext securityContext;
  private PrincipalExtractor principalExtractor;

  @BeforeEach
  void setUp() {
    principalExtractor = mock(PrincipalExtractor.class);
    unifiedUserProvider = new UnifiedUserProvider(principalExtractor);
    securityContext = mock(SecurityContext.class);
    SecurityContextHolder.setContext(securityContext);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldReturnUnknownWhenAuthenticationIsNull() {
    when(securityContext.getAuthentication()).thenReturn(null);

    assertThat(unifiedUserProvider.getAuthId()).isEqualTo("UNKNOWN");
    assertThat(unifiedUserProvider.getName()).isEqualTo("UNKNOWN");
  }

  @Test
  void shouldReturnUnknownWhenNotAuthenticated() {
    Authentication auth = mock(Authentication.class);
    when(auth.isAuthenticated()).thenReturn(false);
    when(securityContext.getAuthentication()).thenReturn(auth);

    assertThat(unifiedUserProvider.getAuthId()).isEqualTo("UNKNOWN");
    assertThat(unifiedUserProvider.getName()).isEqualTo("UNKNOWN");
  }

  @Test
  void shouldReturnUnknownWhenAnonymousUser() {
    AnonymousAuthenticationToken anonymousAuth = new AnonymousAuthenticationToken("key",
        "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    when(securityContext.getAuthentication()).thenReturn(anonymousAuth);

    assertThat(unifiedUserProvider.getAuthId()).isEqualTo("UNKNOWN");
    assertThat(unifiedUserProvider.getName()).isEqualTo("UNKNOWN");
  }

  @Test
  void shouldReturnIdentifierExtractedForAuthenticatedPrincipal() {
    Authentication auth = mock(Authentication.class);

    when(auth.isAuthenticated()).thenReturn(true);
    when(securityContext.getAuthentication()).thenReturn(auth);
    when(principalExtractor.extractPrincipalInfo(auth)).thenReturn(principal("stable-id"));

    assertThat(unifiedUserProvider.getAuthId()).isEqualTo("stable-id");
    verify(principalExtractor).extractPrincipalInfo(auth);
  }

  @Test
  void shouldReturnNameWhenGetNameIsCalled() {
    Authentication auth = mock(Authentication.class);

    when(auth.isAuthenticated()).thenReturn(true);
    when(auth.getName()).thenReturn("expected-user-name");
    when(securityContext.getAuthentication()).thenReturn(auth);

    assertThat(unifiedUserProvider.getName()).isEqualTo("expected-user-name");
  }

  private PrincipalInfo principal(String identifier) {
    return new PrincipalInfo(identifier, PrincipalKind.HUMAN, identifier, Map.of(), List.of());
  }
}
