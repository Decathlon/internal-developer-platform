package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity;

import java.util.List;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity.RawSearchFilterNode;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.FilterNodeDtoIn;

/// Converts a [FilterNodeDtoIn] tree into a [RawSearchFilterNode] tree.
///
/// **Responsibility:** Pure structural adapter — copies DTO fields to domain-native raw types
/// with no validation, no enum parsing, and no business logic. All validation and type resolution
/// are handled downstream by the domain parser
/// ([com.decathlon.idp_core.domain.service.search.SearchFilterParser]).
@Component
public class SearchFilterMapper {

  /// Converts a nullable [FilterNodeDtoIn] to a [RawSearchFilterNode].
  ///
  /// @param dto the root node DTO; may be null, in which case null is returned
  /// (the domain parser
  /// treats null as "no filter")
  /// @return the raw domain tree, or null when dto is null
  public RawSearchFilterNode toRaw(FilterNodeDtoIn dto) {
    if (dto == null) {
      return null;
    }
    return convertNode(dto);
  }

  private RawSearchFilterNode convertNode(FilterNodeDtoIn dto) {
    if (dto.connector() != null || dto.criteria() != null) {
      List<RawSearchFilterNode> children = dto.criteria() == null
          ? List.of()
          : dto.criteria().stream().map(this::convertNode).toList();
      return new RawSearchFilterNode.Group(dto.connector(), children);
    }
    return new RawSearchFilterNode.Criterion(dto.field(), dto.operation(), dto.value());
  }
}
