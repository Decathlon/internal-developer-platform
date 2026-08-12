package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor.decoder;

import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookDecodingException;

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

  private final Map<String, PayloadDecoder> decoders = Map.of(CONTENT_ENCODING_IDENTITY,
      this::decodeIdentity, CONTENT_ENCODING_GZIP, this::decodeGzip);

  /// Decodes incoming payload bytes or string representations based on request
  /// headers.
  public String decode(Object encodedPayload, Map<String, Object> headers) {
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
      throw new WebhookDecodingException("Empty payload cannot be decoded as gzip");
    }

    try {
      for (int i = encodingChain.size() - 1; i >= 0; i--) {
        decodedPayload = decoders.get(encodingChain.get(i)).decode(decodedPayload);
      }
    } catch (ZipException e) {
      throw new WebhookDecodingException("Corrupted or invalid compressed gzip stream", e);
    } catch (IOException e) {
      throw new WebhookDecodingException(
          "Failed to decompress payload for encoding: " + contentEncoding, e);
    }

    return new String(decodedPayload, StandardCharsets.UTF_8);
  }

  private String extractContentEncodingHeader(Map<String, Object> headers) {
    if (headers == null)
      return null;

    return headers.entrySet().stream()
        .filter(entry -> entry.getKey().equalsIgnoreCase(CONTENT_ENCODING_HEADER))
        .map(Map.Entry::getValue).filter(Objects::nonNull).map(Object::toString).findFirst()
        .orElse(null);
  }

  private List<String> parseEncodingChain(String contentEncoding) {
    if (contentEncoding == null || contentEncoding.isBlank()) {
      return List.of();
    }

    return Arrays.stream(contentEncoding.split(",")).map(String::trim).filter(s -> !s.isEmpty())
        .map(s -> s.toLowerCase(Locale.ROOT)).toList();
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
        if (payload == null) return "";

        return switch (payload) {
            case String s -> s;
            case byte[] b -> new String(b, StandardCharsets.UTF_8);
            default -> payload.toString();
        };
    }

  private byte[] toByteArray(Object payload) {
        if (payload == null) return new byte[0];

        return switch (payload) {
            case byte[] b -> b;
            case String s -> {
                String trimmed = s.trim();
                yield isBase64(trimmed) ? Base64.getDecoder().decode(trimmed) : trimmed.getBytes(StandardCharsets.UTF_8);
            }
            default -> payload.toString().getBytes(StandardCharsets.UTF_8);
        };
    }

  private boolean isBase64(String value) {
    if (value == null || value.isBlank() || value.length() % 4 != 0) {
      return false;
    }
    return value.matches("^[A-Za-z0-9+/=]+$");
  }
}
