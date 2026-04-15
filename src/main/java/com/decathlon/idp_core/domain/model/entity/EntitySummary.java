package com.decathlon.idp_core.domain.model.entity;

import lombok.Builder;

/// Lightweight projection of an [Entity] for efficient summary views.
///
/// Provides essential business identification without the full property and relation
/// data. Used in listing operations, search results, and API responses where only
/// key identifying information is needed for business purposes.
///
/// **Business purpose:**
/// - Entity listings in admin interfaces
/// - Search result previews
/// - Relationship target references
/// - Performance-optimized read operations where full entity data isn't required
@Builder
public record EntitySummary(String identifier, String name, String templateIdentifier) {}
