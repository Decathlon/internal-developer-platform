package com.decathlon.idp_core.infrastructure.adapters.ingestion.security;

/// Functional interface for creating typed exceptions with a message.
@FunctionalInterface
interface ExceptionFactory {
  RuntimeException create(String message);
}
