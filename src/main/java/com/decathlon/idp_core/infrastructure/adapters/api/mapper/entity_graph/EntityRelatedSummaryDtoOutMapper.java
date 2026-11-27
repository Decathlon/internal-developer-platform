package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity_graph;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.decathlon.idp_core.domain.model.entity.EntitySummary;
import com.decathlon.idp_core.domain.model.search.PaginatedResult;
import com.decathlon.idp_core.domain.model.search.PaginationCriteria;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntitySummaryDto;

/// Utility mapper converting domain [PaginatedResult] of [EntitySummary] records
/// into a Spring [Page] of [EntitySummaryDto] for REST responses.
public final class EntityRelatedSummaryDtoOutMapper {

  private EntityRelatedSummaryDtoOutMapper() {
  }

  /// Maps a paginated domain summary result to a Spring [Page] of
  /// [EntitySummaryDto].
  ///
  /// @param result domain paginated result containing entity summaries and total
  /// count
  /// @param criteria validated pagination criteria used for the request
  /// @return Spring [Page] of [EntitySummaryDto] preserving pagination metadata
  public static Page<EntitySummaryDto> toSummaryPage(PaginatedResult<EntitySummary> result,
      PaginationCriteria criteria) {
    Pageable pageable = PageRequest.of(criteria.page(), criteria.size());
    if (result == null || result.content() == null || result.content().isEmpty()) {
      long total = result == null ? 0L : result.totalElements();
      return new PageImpl<>(List.of(), pageable, total);
    }

    List<EntitySummaryDto> content = result.content().stream()
        .map(summary -> new EntitySummaryDto(summary.identifier(), summary.name(),
            summary.templateIdentifier()))
        .toList();
    return new PageImpl<>(content, pageable, result.totalElements());
  }
}
