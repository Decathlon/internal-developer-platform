package com.decathlon.idp_core.domain.exception.entity;

/// Raised when deletion would remove a core authorization entity.
public class EntityDeletionNotAllowedException extends RuntimeException {

  public EntityDeletionNotAllowedException(String entity, String reason) {
    super("Deletion of " + entity + " is not allowed: " + reason);
  }
}
