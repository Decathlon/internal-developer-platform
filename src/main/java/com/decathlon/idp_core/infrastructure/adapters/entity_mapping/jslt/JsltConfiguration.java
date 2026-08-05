package com.decathlon.idp_core.infrastructure.adapters.entity_mapping.jslt;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/// Spring configuration for JSLT mapping adapter infrastructure.
/// Provides shared JSON serialization for payload parsing and expression evaluation.
@Configuration
public class JsltConfiguration {

  /// Provides a shared ObjectMapper bean for mapping adapters.
  ///
  /// Conditional declaration avoids overriding Spring Boot's application-wide
  /// ObjectMapper (modules, naming strategy, date/time configuration, etc.).
  @Bean
  @ConditionalOnMissingBean(ObjectMapper.class)
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }
}
