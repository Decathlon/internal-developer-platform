package com.decathlon.idp_core.infrastructure.adapters.persistence.model.audit;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.infrastructure.adapters.api.auth.UserIdentityProvider;

@Component
public class UserIdentityProviderHolder {

  private static final AtomicReference<UserIdentityProvider> userIdentityProvider = new AtomicReference<>();

  private final UserIdentityProvider injectedProvider;

  @Autowired
  UserIdentityProviderHolder(final UserIdentityProvider injectedProvider) {
    this.injectedProvider = injectedProvider;
  }

  /// This method is called by Hibernate Envers' CustomRevisionListener, which is
  /// not managed by Spring, so we need a static accessor.
  /// It will throw an exception if accessed before the Spring context is fully
  /// initialized, which should not happen in normal operation.
  /// This design allows us to bridge the gap between Spring-managed beans and
  /// Hibernate's non-Spring-managed listeners.
  public static UserIdentityProvider getUserIdentityProvider() {
    UserIdentityProvider provider = userIdentityProvider.get();
    if (provider == null) {
      throw new IllegalStateException(
          "UserIdentityProviderHolder not initialized. Spring context may not be loaded.");
    }
    return provider;
  }

  @PostConstruct
  public void init() {
    userIdentityProvider.set(this.injectedProvider);
  }
}
