package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity_graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import com.decathlon.idp_core.domain.model.entity.EntitySummary;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphTraversalMode;
import com.decathlon.idp_core.domain.model.entity_graph.EntityRelatedByQuery;
import com.decathlon.idp_core.domain.model.search.PaginatedResult;
import com.decathlon.idp_core.domain.model.search.PaginationCriteria;
import com.decathlon.idp_core.domain.model.search.SortCriteria;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntitySummaryDto;

@DisplayName("EntityRelatedSummaryDtoOutMapper and EntityRelatedByQuery tests")
class EntityRelatedSummaryDtoOutMapperTest {

  @Test
  @DisplayName("Should map populated PaginatedResult into Page of EntitySummaryDto without properties")
  void shouldMapPopulatedResult() {
    EntitySummary s1 = new EntitySummary(UUID.randomUUID(), "asset-1", "Asset 1", "asset");
    EntitySummary s2 = new EntitySummary(UUID.randomUUID(), "asset-2", "Asset 2", "asset");
    PaginatedResult<EntitySummary> result = new PaginatedResult<>(List.of(s1, s2), 5L, 3, 1);
    PaginationCriteria criteria = new PaginationCriteria(1, 2, "identifier,asc");

    Page<EntitySummaryDto> page = EntityRelatedSummaryDtoOutMapper.toSummaryPage(result, criteria);

    assertThat(page.getNumber()).isEqualTo(1);
    assertThat(page.getSize()).isEqualTo(2);
    assertThat(page.getTotalElements()).isEqualTo(5L);
    assertThat(page.getTotalPages()).isEqualTo(3);
    assertThat(page.getContent()).containsExactly(
        new EntitySummaryDto("asset-1", "Asset 1", "asset"),
        new EntitySummaryDto("asset-2", "Asset 2", "asset"));
    assertThat(page.getContent()).allSatisfy(dto -> assertThat(dto.properties()).isNull());
  }

  @Test
  @DisplayName("Should handle null or empty PaginatedResult preserving total count")
  void shouldHandleNullOrEmptyResult() {
    PaginationCriteria criteria = new PaginationCriteria(2, 10, "identifier,asc");

    Page<EntitySummaryDto> fromNull = EntityRelatedSummaryDtoOutMapper.toSummaryPage(null,
        criteria);
    assertThat(fromNull.getContent()).isEmpty();
    assertThat(fromNull.getTotalElements()).isZero();

    Page<EntitySummaryDto> outOfRangePage = EntityRelatedSummaryDtoOutMapper
        .toSummaryPage(new PaginatedResult<>(List.of(), 15L, 2, 2), criteria);
    assertThat(outOfRangePage.getContent()).isEmpty();
    assertThat(outOfRangePage.getTotalElements()).isEqualTo(15L);
  }

  @Test
  @DisplayName("EntityRelatedByQuery should enforce non-null invariants")
  void shouldValidateEntityRelatedByQueryInvariants() {
    UUID rootId = UUID.randomUUID();
    PaginationCriteria pagination = new PaginationCriteria(0, 20, "identifier,asc");
    SortCriteria sort = new SortCriteria("identifier", SortCriteria.Direction.ASC);

    assertThatThrownBy(() -> new EntityRelatedByQuery(null, "rel", 1,
        EntityGraphTraversalMode.DIRECT_LINEAGE, pagination, sort))
            .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new EntityRelatedByQuery(rootId, null, 1,
        EntityGraphTraversalMode.DIRECT_LINEAGE, pagination, sort))
            .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new EntityRelatedByQuery(rootId, "rel", 1, null, pagination, sort))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new EntityRelatedByQuery(rootId, "rel", 1,
        EntityGraphTraversalMode.DIRECT_LINEAGE, null, sort))
            .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new EntityRelatedByQuery(rootId, "rel", 1,
        EntityGraphTraversalMode.DIRECT_LINEAGE, pagination, null))
            .isInstanceOf(NullPointerException.class);
  }
}
