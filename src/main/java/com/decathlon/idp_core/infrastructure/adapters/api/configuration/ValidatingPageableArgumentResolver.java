package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.decathlon.idp_core.domain.constant.ValidationMessages;
import com.decathlon.idp_core.domain.exception.search.InvalidSearchQueryException;
import com.decathlon.idp_core.domain.model.search.PaginationCriteria;

/// Resolves Spring Data `Pageable` arguments and rejects invalid `page` and `size`
/// query parameters with [InvalidSearchQueryException] (HTTP 400).
public class ValidatingPageableArgumentResolver extends PageableHandlerMethodArgumentResolver {

  private static final String PAGE_PARAM = "page";
  private static final String SIZE_PARAM = "size";

  private final int maxPageSize;

  public ValidatingPageableArgumentResolver(int maxPageSize) {
    this.maxPageSize = maxPageSize;
    setMaxPageSize(maxPageSize);
  }

  @Override
  public @NonNull Pageable resolveArgument(@NonNull MethodParameter methodParameter,
      @Nullable ModelAndViewContainer mavContainer, @NonNull NativeWebRequest webRequest,
      @Nullable WebDataBinderFactory binderFactory) {
    validatePageableParameters(webRequest);
    return super.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);
  }

  private void validatePageableParameters(NativeWebRequest webRequest) {
    Optional<Integer> page = parseInteger(webRequest, PAGE_PARAM,
        ValidationMessages.PAGE_INDEX_NOT_INTEGER);
    Optional<Integer> size = parseInteger(webRequest, SIZE_PARAM,
        ValidationMessages.PAGE_SIZE_NOT_INTEGER);
    if (page.isEmpty() && size.isEmpty()) {
      return;
    }
    new PaginationCriteria(page.orElse(0), size.orElse(1), null).validateBounds(maxPageSize);
  }

  private static Optional<Integer> parseInteger(NativeWebRequest webRequest, String name,
      String errorMessage) {
    String raw = webRequest.getParameter(name);
    if (raw == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(Integer.parseInt(raw));
    } catch (NumberFormatException _) {
      throw new InvalidSearchQueryException(errorMessage);
    }
  }
}
