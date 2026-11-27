package com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity;

import java.util.UUID;

/// Spring Data native-query projection for paginated related entity summaries
/// including the window-computed total element count.
public interface RelatedEntitySummaryProjection {

  /// Database UUID of the matched entity.
  UUID getId();

  /// Business identifier of the matched entity within its template.
  String getIdentifier();

  /// Display name of the matched entity.
  String getName();

  /// Identifier of the template owning the matched entity.
  String getTemplateIdentifier();

  /// Total number of distinct matched entities across all pages.
  long getTotalCount();
}
