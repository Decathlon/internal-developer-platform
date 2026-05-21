package com.decathlon.idp_core.infrastructure.adapters.persistence.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA auditing so {@code @CreatedDate} and {@code @LastModifiedDate}
 * are populated on webhook connector persistence operations.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfiguration {
}
