package com.decathlon.idp_core.infrastructure.adapters.persistence.model.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.infrastructure.adapters.api.auth.UserIdentityProvider;

/// Unit tests for UserIdentityProviderHolder.
/// Covers the static holder pattern used to bridge Spring-managed beans with Hibernate Envers.
@DisplayName("UserIdentityProviderHolder Tests")
class UserIdentityProviderHolderTest {

  private UserIdentityProvider mockProvider;

  @BeforeEach
  void setUp() {
    // Reset the static holder before each test
    resetHolder();
    mockProvider = mock(UserIdentityProvider.class);
  }

  /// Helper to reset the static holder for test isolation
  private void resetHolder() {
    try {
      java.lang.reflect.Field field = UserIdentityProviderHolder.class
          .getDeclaredField("userIdentityProvider");
      field.setAccessible(true);

      // If the field is an AtomicReference, clear its inner value instead of
      // trying to overwrite the static final field itself (which is forbidden).
      if (java.util.concurrent.atomic.AtomicReference.class.isAssignableFrom(field.getType())) {
        @SuppressWarnings("unchecked")
        java.util.concurrent.atomic.AtomicReference<Object> ref = (java.util.concurrent.atomic.AtomicReference<Object>) field
            .get(null);
        if (ref != null) {
          ref.set(null);
        }
      } else {
        // Fallback for non-final/non-AtomicReference implementations
        field.set(null, null);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to reset UserIdentityProviderHolder", e);
    }
  }

  @Nested
  @DisplayName("Initialization Tests")
  class InitializationTests {

    @Test
    @DisplayName("Should initialize static provider after PostConstruct")
    void shouldInitializeStaticProvider() {
      // Given
      var holder = new UserIdentityProviderHolder(mockProvider);

      // When
      holder.init();

      // Then
      assertThat(UserIdentityProviderHolder.getUserIdentityProvider()).isNotNull()
          .isEqualTo(mockProvider);
    }

    @Test
    @DisplayName("Should use injected provider during initialization")
    void shouldUseInjectedProvider() {
      // Given
      UserIdentityProvider customProvider = mock(UserIdentityProvider.class);
      var holder = new UserIdentityProviderHolder(customProvider);

      // When
      holder.init();

      // Then
      assertThat(UserIdentityProviderHolder.getUserIdentityProvider()).isEqualTo(customProvider);
    }

    @Test
    @DisplayName("Should throw IllegalStateException if accessed before initialization")
    void shouldThrowExceptionIfNotInitialized() {
      // When & Then
      assertThatThrownBy(UserIdentityProviderHolder::getUserIdentityProvider)
          .isInstanceOf(IllegalStateException.class).hasMessage(
              "UserIdentityProviderHolder not initialized. Spring context may not be loaded.");
    }

    @Test
    @DisplayName("Should reinitialize with new provider")
    void shouldReinitializeWithNewProvider() {
      // Given
      var provider1 = mock(UserIdentityProvider.class);
      var provider2 = mock(UserIdentityProvider.class);

      var holder1 = new UserIdentityProviderHolder(provider1);
      holder1.init();

      // When - Reinitialize with different provider
      var holder2 = new UserIdentityProviderHolder(provider2);
      holder2.init();

      // Then
      assertThat(UserIdentityProviderHolder.getUserIdentityProvider()).isEqualTo(provider2);
    }
  }

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("Should handle concurrent initialization safely")
    void shouldHandleConcurrentInitializationSafely() throws InterruptedException {
      // Given
      var provider1 = mock(UserIdentityProvider.class);
      var provider2 = mock(UserIdentityProvider.class);

      var thread1Results = new java.util.concurrent.CountDownLatch(1);
      var thread2Results = new java.util.concurrent.CountDownLatch(1);

      // When - Initialize from two threads
      Thread t1 = new Thread(() -> {
        var holder = new UserIdentityProviderHolder(provider1);
        holder.init();
        thread1Results.countDown();
      });

      Thread t2 = new Thread(() -> {
        var holder = new UserIdentityProviderHolder(provider2);
        holder.init();
        thread2Results.countDown();
      });

      t1.start();
      t2.start();
      thread1Results.await();
      thread2Results.await();

      // Then - Should have some provider set (either one is acceptable due to race
      // condition)
      assertThat(UserIdentityProviderHolder.getUserIdentityProvider()).isNotNull();
    }

    @Test
    @DisplayName("Should allow concurrent reads after initialization")
    void shouldAllowConcurrentReadsAfterInitialization() throws InterruptedException {
      // Given
      var provider = mock(UserIdentityProvider.class);
      var holder = new UserIdentityProviderHolder(provider);
      holder.init();

      var readCount = 100;
      var barrier = new java.util.concurrent.CyclicBarrier(readCount);
      var latch = new java.util.concurrent.CountDownLatch(readCount);

      // When - Read from multiple threads
      for (int i = 0; i < readCount; i++) {
        new Thread(() -> {
          try {
            barrier.await();
            UserIdentityProvider result = UserIdentityProviderHolder.getUserIdentityProvider();
            assertThat(result).isEqualTo(provider);
          } catch (Exception e) {
            throw new RuntimeException(e);
          } finally {
            latch.countDown();
          }
        }).start();
      }

      latch.await();

      assertThat(UserIdentityProviderHolder.getUserIdentityProvider()).isEqualTo(provider);
    }
  }

  @Nested
  @DisplayName("Static Accessor Tests")
  class StaticAccessorTests {

    @Test
    @DisplayName("Should return the initialized provider")
    void shouldReturnInitializedProvider() {
      // Given
      var holder = new UserIdentityProviderHolder(mockProvider);
      holder.init();

      // When
      UserIdentityProvider result = UserIdentityProviderHolder.getUserIdentityProvider();

      // Then
      assertThat(result).isEqualTo(mockProvider);
    }

    @Test
    @DisplayName("Should persist provider across multiple accesses")
    void shouldPersistProviderAcrossAccesses() {
      // Given
      var holder = new UserIdentityProviderHolder(mockProvider);
      holder.init();

      // When & Then
      UserIdentityProvider result1 = UserIdentityProviderHolder.getUserIdentityProvider();
      UserIdentityProvider result2 = UserIdentityProviderHolder.getUserIdentityProvider();
      UserIdentityProvider result3 = UserIdentityProviderHolder.getUserIdentityProvider();

      assertThat(result1).isEqualTo(result2).isEqualTo(result3).isEqualTo(mockProvider);
    }

    @Test
    @DisplayName("Should work as bridge for Hibernate Envers listeners")
    void shouldBridgeHibernateEnversAccess() {
      // Given
      var provider = mock(UserIdentityProvider.class);
      var holder = new UserIdentityProviderHolder(provider);
      holder.init();

      // When - Simulate Envers listener access (non-Spring context)
      UserIdentityProvider enversProvider = UserIdentityProviderHolder.getUserIdentityProvider();

      // Then
      assertThat(enversProvider).isNotNull().isEqualTo(provider);
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should accept UserIdentityProvider in constructor")
    void shouldAcceptProviderInConstructor() {
      // When
      UserIdentityProviderHolder holder = new UserIdentityProviderHolder(mockProvider);

      // Then
      assertThat(holder).isNotNull();
    }

    @Test
    @DisplayName("Should accept null provider in constructor (initialization required)")
    void shouldAcceptNullProviderInConstructor() {
      // When & Then
      UserIdentityProviderHolder holder = new UserIdentityProviderHolder(null);
      assertThat(holder).isNotNull();
    }

    @Test
    @DisplayName("Should initialize with null provider and throw on access")
    void shouldThrowWhenInitializedWithNullProvider() {
      // Given
      var holder = new UserIdentityProviderHolder(null);
      holder.init();

      // When & Then - Null provider means userIdentityProvider becomes null
      assertThatThrownBy(UserIdentityProviderHolder::getUserIdentityProvider)
          .isInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should provide clear error message on uninitialized access")
    void shouldProvideClearErrorMessage() {
      // When & Then
      assertThatThrownBy(UserIdentityProviderHolder::getUserIdentityProvider)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("UserIdentityProviderHolder not initialized")
          .hasMessageContaining("Spring context");
    }

    @Test
    @DisplayName("Should handle provider access after context initialization failure")
    void shouldHandleContextInitializationFailure() {
      // When - No initialization happens
      // Then - Accessing without init should fail
      assertThatThrownBy(UserIdentityProviderHolder::getUserIdentityProvider)
          .isInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  @DisplayName("Integration Pattern Tests")
  class IntegrationPatternTests {

    @Test
    @DisplayName("Should follow Spring component pattern with injection")
    void shouldFollowSpringComponentPattern() {
      // Given
      var holder = new UserIdentityProviderHolder(mockProvider);

      // When
      holder.init();

      // Then - Simulates what Spring would do
      UserIdentityProvider provider = UserIdentityProviderHolder.getUserIdentityProvider();
      assertThat(provider).isEqualTo(mockProvider);
    }

    @Test
    @DisplayName("Should enable Hibernate Envers listener to access provider")
    void shouldEnableHibernateEnversAccess() {
      // Given
      var provider = mock(UserIdentityProvider.class);
      var holder = new UserIdentityProviderHolder(provider);
      holder.init();
      UserIdentityProvider enversAccessibleProvider = UserIdentityProviderHolder
          .getUserIdentityProvider();

      // Then
      assertThat(enversAccessibleProvider).isNotNull().isEqualTo(provider);
    }

    @Test
    @DisplayName("Should serve as singleton-like holder for Spring-unmanaged code")
    void shouldServeSingletonPatternForNonSpringCode() {
      // Given
      var holder = new UserIdentityProviderHolder(mockProvider);
      holder.init();

      UserIdentityProvider access1 = UserIdentityProviderHolder.getUserIdentityProvider();
      UserIdentityProvider access2 = UserIdentityProviderHolder.getUserIdentityProvider();

      assertThat(access1).isSameAs(access2).isEqualTo(mockProvider);
    }
  }
}
