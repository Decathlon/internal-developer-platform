package com.decathlon.idp_core.infrastructure.adapters.entity_mapping.jslt;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/// Spring configuration for JSLT mapping adapter infrastructure.
/// Provides shared JSON serialization for payload parsing and expression evaluation.
@Configuration
public class JsltConfiguration {

  /// Provides a shared ObjectMapper bean for mapping adapters.
  /// Bean scope is singleton and safe for concurrent reuse.
  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }
}
