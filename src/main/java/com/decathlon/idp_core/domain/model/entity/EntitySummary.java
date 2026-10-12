package com.decathlon.idp_core.domain.model.entity;

import java.util.UUID;

import lombok.Builder;

/// Lightweight projection of an [Entity] for efficient summary views.
///
/// Provides the persistence UUID and essential business identification without
/// property and relation data. Used when resolving graph roots and in listing,
/// search, or API operations that need entity identity without full entity data.
///
/// **Business purpose:**
/// - Entity listings in admin interfaces
/// - Search result previews
/// - Relationship target references
/// - Performance-optimized read operations where full entity data isn't required
@Builder
public record EntitySummary(UUID id, String identifier, String name, String templateIdentifier) {
}
