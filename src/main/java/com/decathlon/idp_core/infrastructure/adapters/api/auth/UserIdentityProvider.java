package com.decathlon.idp_core.infrastructure.adapters.api.auth;

public interface UserIdentityProvider {
  String getAuthId();
  String getName();
}
