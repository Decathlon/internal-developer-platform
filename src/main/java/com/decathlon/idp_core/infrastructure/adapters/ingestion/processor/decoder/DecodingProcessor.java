package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor.decoder;

import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.CONTENT_ENCODING_GZIP;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.CONTENT_ENCODING_HEADER;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.CONTENT_ENCODING_IDENTITY;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/// Processor responsible for decoding webhook payloads based on HTTP `Content-Encoding` headers.
///
/// Supported Encodings:
/// - `gzip`: Decompresses gzip-encoded raw bytes or base64 text.
/// - `identity` / none: Pass-through processing.
///
/// Unrecognized encodings are logged as warnings and processed as pass-through for resilience.
@Component
@Slf4j
public class DecodingProcessor {

  private final Map<String, PayloadDecoder> decoders;

  public DecodingProcessor() {
    LinkedHashMap<String, PayloadDecoder> availableDecoders = new LinkedHashMap<>();
    availableDecoders.put(CONTENT_ENCODING_IDENTITY, this::decodeIdentity);
    availableDecoders.put(CONTENT_ENCODING_GZIP, this::decodeGzip);
    this.decoders = Map.copyOf(availableDecoders);
  }

  /// Decodes incoming payload bytes or string representations based on request
  /// headers.
  ///
  /// @param encodedPayload the raw inbound body (byte array or String)
  /// @param headers HTTP headers map containing optional Content-Encoding
  /// @return UTF-8 decoded String payload
  /// @throws IOException if decompression fails due to payload corruption
  public String decode(Object encodedPayload, Map<String, Object> headers) throws IOException {
    String contentEncoding = extractContentEncodingHeader(headers);
    List<String> encodingChain = parseEncodingChain(contentEncoding);

    log.debug("Content-Encoding chain resolved: {}",
        contentEncoding != null ? contentEncoding : CONTENT_ENCODING_IDENTITY);

    if (encodingChain.isEmpty()) {
      return payloadToString(encodedPayload);
    }

    if (encodingChain.stream().anyMatch(encoding -> !decoders.containsKey(encoding))) {
      log.warn("Unsupported Content-Encoding chain: {}. Treating payload as pass-through.",
          contentEncoding);
      return payloadToString(encodedPayload);
    }

    byte[] decodedPayload = toByteArray(encodedPayload);
    if (decodedPayload.length == 0 && encodingChain.contains(CONTENT_ENCODING_GZIP)) {
      throw new ZipException("Empty payload cannot be decoded as gzip");
    }
    for (int index = encodingChain.size() - 1; index >= 0; index--) {
      decodedPayload = decoders.get(encodingChain.get(index)).decode(decodedPayload);
    }

    return new String(decodedPayload, StandardCharsets.UTF_8);
  }

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

  private byte[] decodeGzip(byte[] encodedPayload) throws IOException {
    try (
        GZIPInputStream gzipInput = new GZIPInputStream(new ByteArrayInputStream(encodedPayload))) {
      return gzipInput.readAllBytes();
    }
  }

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

  private byte[] toByteArray(Object payload) {
        if (payload == null) {
            return new byte[0];
        }
        return switch (payload) {
            case byte[] byteArray -> byteArray;
            case String string -> {
                String trimmed = string.trim();
                if (isBase64(trimmed)) {
                    yield Base64.getDecoder().decode(trimmed);
                }
                yield trimmed.getBytes(StandardCharsets.UTF_8);
            }
            default -> payload.toString().getBytes(StandardCharsets.UTF_8);
        };
    }

  private boolean isBase64(String value) {
    if (value == null || value.isBlank() || value.length() % 4 != 0) {
      return false;
    }
    try {
      Base64.getDecoder().decode(value);
      return true;
    } catch (IllegalArgumentException _) {
      return false;
    }
  }
}
