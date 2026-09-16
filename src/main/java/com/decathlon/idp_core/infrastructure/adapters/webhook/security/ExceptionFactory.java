package com.decathlon.idp_core.infrastructure.adapters.webhook.security;

/// Functional interface for creating typed exceptions with a message.
@FunctionalInterface
interface ExceptionFactory {
  RuntimeException create(String message);
}
