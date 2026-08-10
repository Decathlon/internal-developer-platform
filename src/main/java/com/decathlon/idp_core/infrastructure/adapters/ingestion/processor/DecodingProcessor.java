package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor;

import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.CONTENT_ENCODING_GZIP;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.CONTENT_ENCODING_HEADER;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.CONTENT_ENCODING_IDENTITY;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/// Processor for decoding webhook payloads based on Content-Encoding header.
///
/// Supports:
/// - gzip: Decompresses gzip-encoded payloads
/// - identity/no encoding: Returns payload as-is
///
/// Any unknown encoding is logged and treated as pass-through to keep ingestion resilient.
@Component
@Slf4j
public class DecodingProcessor {

  private final Map<String, PayloadDecoder> decoders;

  public DecodingProcessor() {
    LinkedHashMap<String, PayloadDecoder> availableDecoders = new LinkedHashMap<String, PayloadDecoder>();
    availableDecoders.put(CONTENT_ENCODING_IDENTITY, this::decodeIdentity);
    availableDecoders.put(CONTENT_ENCODING_GZIP, this::decodeGzip);
    decoders = Map.copyOf(availableDecoders);
  }

  /// Decodes a webhook payload based on the Content-Encoding header.
  ///
  /// Evaluates the `Content-Encoding` header from the webhook request and applies
  /// the appropriate decompression algorithm. If no encoding is specified or the
  /// encoding is `identity`, the payload is returned unchanged.
  ///
  /// @param encodedPayload the raw, possibly encoded payload (bytes or string)
  /// @param headers the HTTP headers from the webhook request (may contain
  /// Content-Encoding)
  /// @return the decoded payload as a String
  /// @throws IOException if decompression fails (e.g., corrupted gzip data)
  public String decode(Object encodedPayload, Map<String, Object> headers) throws IOException {
    String contentEncoding = extractContentEncodingHeader(headers);
    List<String> encodingChain = parseEncodingChain(contentEncoding);

    log.debug("Content-Encoding chain: {}",
        contentEncoding != null ? contentEncoding : CONTENT_ENCODING_IDENTITY);

    // No encoding header means identity.
    if (encodingChain.isEmpty()) {
      return payloadToString(encodedPayload);
    }

    if (encodingChain.stream().anyMatch(encoding -> !decoders.containsKey(encoding))) {
      log.warn("Unsupported Content-Encoding chain: {}. Returning payload as-is.", contentEncoding);
      return payloadToString(encodedPayload);
    }

    // Decode in reverse order because codings are listed in application order.
    byte[] decodedPayload = toByteArray(encodedPayload);
    for (int index = encodingChain.size() - 1; index >= 0; index--) {
      decodedPayload = decoders.get(encodingChain.get(index)).decode(decodedPayload);
    }
    return new String(decodedPayload, StandardCharsets.UTF_8);
  }

  /// Extracts the Content-Encoding header from the headers map
  /// (case-insensitive).
  private String extractContentEncodingHeader(Map<String, Object> headers) {
    if (headers == null) {
      return null;
    }

    return headers.keySet().stream().filter(key -> key.equalsIgnoreCase(CONTENT_ENCODING_HEADER))
        .map(headers::get).filter(Objects::nonNull).map(Object::toString).findFirst().orElse(null);
  }

  private List<String> parseEncodingChain(String contentEncoding) {
    if (contentEncoding == null || contentEncoding.isBlank()) {
      return List.of();
    }

    return Arrays.stream(contentEncoding.split(",")).map(String::trim)
        .filter(value -> !value.isEmpty()).map(value -> value.toLowerCase(Locale.ROOT)).toList();
  }

  private byte[] decodeIdentity(byte[] payload) {
    return payload;
  }

  /// Decompresses a gzip-encoded payload.
  private byte[] decodeGzip(byte[] encodedPayload) throws IOException {
    try (
        GZIPInputStream gzipInput = new GZIPInputStream(new ByteArrayInputStream(encodedPayload))) {
      return gzipInput.readAllBytes();
    }
  }

  /// Converts payload to String, handling both byte arrays and strings using
  /// pattern matching.
  private String payloadToString(Object payload) {
    if (payload == null) {
      return "";
    }
    return switch (payload) {
      case String string -> string;
      case byte[] byteArray -> new String(byteArray, StandardCharsets.UTF_8);
      default -> payload.toString();
    };
  }

  /// Converts payload to byte array for decompression operations using pattern
  /// matching.
  private byte[] toByteArray(Object payload) {
    if (payload == null) {
      return new byte[0];
    }
    return switch (payload) {
      case byte[] byteArray -> byteArray;
      case String string -> string.getBytes(StandardCharsets.UTF_8);
      default -> payload.toString().getBytes(StandardCharsets.UTF_8);
    };
  }

  @FunctionalInterface
  private interface PayloadDecoder {

    byte[] decode(byte[] payload) throws IOException;
  }
}
