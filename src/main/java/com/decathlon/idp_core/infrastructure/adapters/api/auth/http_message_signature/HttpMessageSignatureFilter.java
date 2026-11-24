package com.decathlon.idp_core.infrastructure.adapters.api.auth.http_message_signature;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.decathlon.idp_core.infrastructure.adapters.api.configuration.HttpMessageSignatureProperties;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiErrorResponseWriter;

import lombok.extern.slf4j.Slf4j;

/// Enforces HTTP message signatures before HTTP authentication and webhook ingestion.
@Slf4j
public final class HttpMessageSignatureFilter extends OncePerRequestFilter {

  private final HttpMessageSignatureProperties properties;
  private final HttpMessageSignatureVerifier verifier;
  private final ApiErrorResponseWriter errorWriter;
  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  /// Creates a filter sharing the standard two-field JSON error writer.
  public HttpMessageSignatureFilter(HttpMessageSignatureProperties properties,
      HttpMessageSignatureVerifier verifier, ApiErrorResponseWriter errorWriter) {
    this.properties = properties;
    this.verifier = verifier;
    this.errorWriter = errorWriter;
  }

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    return properties.excludedPaths().stream()
        .anyMatch(pattern -> pathMatcher.match(pattern, path));
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response, @NonNull FilterChain chain)
      throws ServletException, IOException {
    List<String> signatures = Collections.list(request.getHeaders("Signature"));
    List<String> requestIds = Collections
        .list(request.getHeaders(properties.identifierHeaderName()));
    HttpMessageSignatureResult result;
    if (signatures.isEmpty() || requestIds.isEmpty() || signatures.getFirst().isBlank()
        || requestIds.getFirst().isBlank()) {
      result = HttpMessageSignatureResult.MISSING;
    } else if (signatures.size() != 1 || requestIds.size() != 1) {
      result = HttpMessageSignatureResult.MALFORMED;
    } else {
      result = verifier.verify(signatures.getFirst(), requestIds.getFirst());
    }
    if (result == HttpMessageSignatureResult.VALID) {
      chain.doFilter(request, response);
      return;
    }
    String correlation = requestIds.isEmpty()
        ? "[missing]"
        : requestIds.getFirst().substring(0, Math.min(requestIds.getFirst().length(), 128))
            .replaceAll("[^A-Za-z0-9._-]", "_");
    log.warn("HTTP message signature rejected: result={}, unverifiedRequestId={}", result,
        correlation);
    response.setHeader("WWW-Authenticate", "Signature realm=\"idp-core\"");
    response.setHeader("Cache-Control", "no-store");
    errorWriter.write(response, HttpStatus.UNAUTHORIZED, result.description());
  }
}
