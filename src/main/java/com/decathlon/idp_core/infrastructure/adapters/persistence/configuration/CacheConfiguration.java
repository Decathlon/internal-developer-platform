package com.decathlon.idp_core.infrastructure.adapters.persistence.configuration;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateService;
import com.github.benmanes.caffeine.cache.Caffeine;

/// Cache configuration backing read-mostly domain lookups that would
/// otherwise hit the database on every call.
///
/// **Business purpose:** `EntityTemplate` definitions are read on nearly every
/// entity read/write request (property type resolution, relation target
/// template resolution) but change infrequently (admin-driven CRUD). Caching
/// them removes a redundant DB round trip from every entity operation without
/// affecting correctness, since template mutations explicitly evict their
/// cache entry (see [EntityTemplateService]).
///
/// **Why Caffeine:** high-performance, near-optimal caching library with a
/// simple size/time-bound eviction policy — avoids unbounded memory growth
/// while keeping hot templates resident.
@Configuration
@EnableCaching
public class CacheConfiguration {

  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager(
        EntityTemplateService.ENTITY_TEMPLATES_CACHE);
    cacheManager.setCaffeine(Caffeine.newBuilder().maximumSize(500)
        .expireAfterWrite(10, TimeUnit.MINUTES).recordStats());
    return cacheManager;
  }

}
