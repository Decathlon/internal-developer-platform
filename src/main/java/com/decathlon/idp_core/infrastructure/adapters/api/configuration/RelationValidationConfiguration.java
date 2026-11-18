package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.decathlon.idp_core.domain.model.enums.RelationTargetValidationMode;

/// Bridges external relation validation settings into the domain mode.
@Configuration
@EnableConfigurationProperties(RelationValidationProperties.class)
public class RelationValidationConfiguration {

  /// Exposes the configured mode without making the domain depend on Spring
  /// configuration types.
  ///
  /// @param properties the externalized relation validation settings
  /// @return the configured target validation mode
  @Bean
  public RelationTargetValidationMode relationTargetValidationMode(
      RelationValidationProperties properties) {
    return properties.targetValidationMode();
  }
}
