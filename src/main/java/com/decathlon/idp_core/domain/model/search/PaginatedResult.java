package com.decathlon.idp_core.domain.model.search;

import java.util.List;

public record PaginatedResult<T> (List<T> content, long totalElements, int totalPages,
    int currentPage) {
}
