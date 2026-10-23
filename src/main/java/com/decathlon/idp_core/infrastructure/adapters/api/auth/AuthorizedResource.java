package com.decathlon.idp_core.infrastructure.adapters.api.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Declares the authorization resource exposed by a controller or endpoint.
///
/// A class-level declaration provides the default resource type; a method-level
/// declaration overrides it and may flag a non-safe HTTP method as read-only.
/// Path variables named `templateIdentifier`, `entityIdentifier` and `identifier`
/// are used to populate the resource context.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AuthorizedResource {

  /// Resource type; blank on a method means "inherit from the controller".
  String value() default "";

  /// Marks an endpoint that uses POST but only reads data (search, dry-run).
  boolean readOnly() default false;
}
