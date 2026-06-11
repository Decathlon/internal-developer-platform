package com.decathlon.idp_core.infrastructure.adapters.persistence.model.audit;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.decathlon.idp_core.infrastructure.adapters.api.auth.UserIdentityProvider;

/**
 * Unit tests for CustomRevisionListener.
 *
 * Tests verify that the listener correctly captures user identity for Hibernate
 * Envers revisions.
 */
@DisplayName("CustomRevisionListener Tests")
@ExtendWith(MockitoExtension.class)
class CustomRevisionListenerTest {

  @Mock
  private UserIdentityProvider userIdentityProvider;

  @Mock
  private CustomRevisionEntity revisionEntity;

  private CustomRevisionListener listener;

  @BeforeEach
  void setUp() {
    listener = new CustomRevisionListener();
  }

  @Nested
  @DisplayName("newRevision Tests")
  class NewRevisionTests {

    @Test
    @DisplayName("Should set authId when user identity is available")
    void shouldSetAuthIdWhenUserIdentityIsAvailable() {
      String expectedAuthId = "user@example.com";

      try (MockedStatic<UserIdentityProviderHolder> holder = org.mockito.Mockito
          .mockStatic(UserIdentityProviderHolder.class)) {
        holder.when(UserIdentityProviderHolder::getUserIdentityProvider)
            .thenReturn(userIdentityProvider);
        when(userIdentityProvider.getAuthId()).thenReturn(expectedAuthId);

        listener.newRevision(revisionEntity);

        verify(revisionEntity).setAuthId(expectedAuthId);
      }
    }

    @Test
    @DisplayName("Should set authId to 'Unknown' when getAuthId returns null")
    void shouldSetAuthIdToUnknownWhenGetAuthIdReturnsNull() {
      try (MockedStatic<UserIdentityProviderHolder> holder = org.mockito.Mockito
          .mockStatic(UserIdentityProviderHolder.class)) {
        holder.when(UserIdentityProviderHolder::getUserIdentityProvider)
            .thenReturn(userIdentityProvider);
        when(userIdentityProvider.getAuthId()).thenReturn(null);

        listener.newRevision(revisionEntity);

        verify(revisionEntity).setAuthId("Unknown");
      }
    }

    @Test
    @DisplayName("Should set authId to 'Unknown' when getAuthId returns blank string")
    void shouldSetAuthIdToUnknownWhenGetAuthIdReturnsBlank() {
      try (MockedStatic<UserIdentityProviderHolder> holder = org.mockito.Mockito
          .mockStatic(UserIdentityProviderHolder.class)) {
        holder.when(UserIdentityProviderHolder::getUserIdentityProvider)
            .thenReturn(userIdentityProvider);
        when(userIdentityProvider.getAuthId()).thenReturn("   ");

        listener.newRevision(revisionEntity);

        verify(revisionEntity).setAuthId("Unknown");
      }
    }

    @Test
    @DisplayName("Should set authId to 'Unknown' when getAuthId returns empty string")
    void shouldSetAuthIdToUnknownWhenGetAuthIdReturnsEmpty() {
      try (MockedStatic<UserIdentityProviderHolder> holder = org.mockito.Mockito
          .mockStatic(UserIdentityProviderHolder.class)) {
        holder.when(UserIdentityProviderHolder::getUserIdentityProvider)
            .thenReturn(userIdentityProvider);
        when(userIdentityProvider.getAuthId()).thenReturn("");

        listener.newRevision(revisionEntity);

        verify(revisionEntity).setAuthId("Unknown");
      }
    }
  }
}
