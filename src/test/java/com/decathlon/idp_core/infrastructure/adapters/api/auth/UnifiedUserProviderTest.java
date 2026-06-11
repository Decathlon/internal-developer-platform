package com.decathlon.idp_core.infrastructure.adapters.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class UnifiedUserProviderTest {

  private UnifiedUserProvider unifiedUserProvider;
  private SecurityContext securityContext;

  @BeforeEach
  void setUp() {
    unifiedUserProvider = new UnifiedUserProvider();
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
  void shouldReturnSubjectWhenJwtAuthentication() {
    JwtAuthenticationToken jwtAuth = mock(JwtAuthenticationToken.class);
    Jwt jwt = mock(Jwt.class);

    when(jwtAuth.isAuthenticated()).thenReturn(true);
    when(jwtAuth.getToken()).thenReturn(jwt);
    when(jwt.getSubject()).thenReturn("jwt-user-id");
    when(securityContext.getAuthentication()).thenReturn(jwtAuth);

    assertThat(unifiedUserProvider.getAuthId()).isEqualTo("jwt-user-id");
  }

  @Test
  void shouldReturnSubjectWhenOidcUser() {
    Authentication auth = mock(Authentication.class);
    OidcUser oidcUser = mock(OidcUser.class);

    when(auth.isAuthenticated()).thenReturn(true);
    when(auth.getPrincipal()).thenReturn(oidcUser);
    when(oidcUser.getSubject()).thenReturn("oidc-user-id");
    when(securityContext.getAuthentication()).thenReturn(auth);

    assertThat(unifiedUserProvider.getAuthId()).isEqualTo("oidc-user-id");
  }

  @Test
  void shouldReturnSubAttributeWhenOAuth2User() {
    Authentication auth = mock(Authentication.class);
    OAuth2User oauth2User = mock(OAuth2User.class);

    when(auth.isAuthenticated()).thenReturn(true);
    when(auth.getPrincipal()).thenReturn(oauth2User);
    when(oauth2User.getAttribute("sub")).thenReturn("oauth2-sub-id");
    when(securityContext.getAuthentication()).thenReturn(auth);

    assertThat(unifiedUserProvider.getAuthId()).isEqualTo("oauth2-sub-id");
  }

  @Test
  void shouldReturnIdAttributeWhenOAuth2UserHasNoSub() {
    Authentication auth = mock(Authentication.class);
    OAuth2User oauth2User = mock(OAuth2User.class);

    when(auth.isAuthenticated()).thenReturn(true);
    when(auth.getPrincipal()).thenReturn(oauth2User);
    when(oauth2User.getAttribute("sub")).thenReturn(null);
    when(oauth2User.getAttribute("id")).thenReturn("oauth2-id-attribute");
    when(securityContext.getAuthentication()).thenReturn(auth);

    assertThat(unifiedUserProvider.getAuthId()).isEqualTo("oauth2-id-attribute");
  }

  @Test
  void shouldReturnFallbackNameWhenOAuth2UserHasNoSubOrId() {
    Authentication auth = mock(Authentication.class);
    OAuth2User oauth2User = mock(OAuth2User.class);

    when(auth.isAuthenticated()).thenReturn(true);
    when(auth.getName()).thenReturn("fallback-oauth2-name");
    when(auth.getPrincipal()).thenReturn(oauth2User);
    when(oauth2User.getAttribute("sub")).thenReturn(null);
    when(oauth2User.getAttribute("id")).thenReturn(null);
    when(securityContext.getAuthentication()).thenReturn(auth);

    assertThat(unifiedUserProvider.getAuthId()).isEqualTo("fallback-oauth2-name");
  }

  @Test
  void shouldReturnNameForBasicOrOtherAuthentication() {
    Authentication auth = mock(Authentication.class);

    when(auth.isAuthenticated()).thenReturn(true);
    when(auth.getName()).thenReturn("basic-auth-user");
    when(auth.getPrincipal()).thenReturn("Standard String Principal"); // N'est ni OAuth2User ni
                                                                       // OidcUser
    when(securityContext.getAuthentication()).thenReturn(auth);

    assertThat(unifiedUserProvider.getAuthId()).isEqualTo("basic-auth-user");
  }

  @Test
  void shouldReturnNameWhenGetNameIsCalled() {
    Authentication auth = mock(Authentication.class);

    when(auth.isAuthenticated()).thenReturn(true);
    when(auth.getName()).thenReturn("expected-user-name");
    when(securityContext.getAuthentication()).thenReturn(auth);

    assertThat(unifiedUserProvider.getName()).isEqualTo("expected-user-name");
  }
}
