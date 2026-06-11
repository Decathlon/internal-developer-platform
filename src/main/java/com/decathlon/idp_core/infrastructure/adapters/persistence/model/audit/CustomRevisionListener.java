package com.decathlon.idp_core.infrastructure.adapters.persistence.model.audit;

import org.hibernate.envers.RevisionListener;

public class CustomRevisionListener implements RevisionListener {

  private static final String UNKNOWN_AUTH_ID = "Unknown";

  @Override
  public void newRevision(Object revisionEntity) {
    var customRevisionEntity = (CustomRevisionEntity) revisionEntity;
    var userIdentityProvider = UserIdentityProviderHolder.getUserIdentityProvider();

    String authId = userIdentityProvider.getAuthId();
    if (authId == null || authId.isBlank()) {
      authId = UNKNOWN_AUTH_ID;
    }

    customRevisionEntity.setAuthId(authId);
  }
}
