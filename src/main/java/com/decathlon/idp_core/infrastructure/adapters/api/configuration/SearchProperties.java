package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import jakarta.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import com.decathlon.idp_core.domain.constant.SearchConstraints;

/// Configuration properties for search and listing pagination.
///
/// Binds `app.max-page-size`, which can be overridden with the
/// `MAX_PAGE_SIZE` environment variable. Defaults to
/// [SearchConstraints#DEFAULT_MAX_PAGE_SIZE].
@Validated
@ConfigurationProperties(prefix = "app")
public record SearchProperties(
    @DefaultValue("" + SearchConstraints.DEFAULT_MAX_PAGE_SIZE) @Min(1) int maxPageSize) {
}
