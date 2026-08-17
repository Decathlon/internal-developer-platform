package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor.decoder;

import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookDecodingException;

import lombok.extern.slf4j.Slf4j;

/// Processor responsible for decoding webhook payloads based on HTTP `Content-Encoding` headers.
///
/// Supported Encodings:
/// - `gzip`: Decompresses gzip-encoded raw bytes.
/// - `identity` / none: Pass-through processing.
///
/// Unrecognized encodings are rejected with a `WebhookDecodingException` to prevent silent data
/// corruption. Gzip decompression is bounded by `idp.ingestion.max-decompressed-bytes` (default
/// 10 MB) to protect against Zip Bomb (DoS) attacks.
@Component
@Slf4j
public class DecodingProcessor {

  private static final int SANITIZED_HEADER_MAX_LENGTH = 128;
  private static final int DECOMPRESSION_BUFFER_SIZE = 8192;

  private final Map<String, PayloadDecoder> decoders;

  // Max decompressed size — configurable via idp.ingestion.max-decompressed-bytes
  // to prevent Zip Bomb attacks.
  private final long maxDecompressedBytes;

  public DecodingProcessor(
      @Value("${idp.ingestion.max-decompressed-bytes:10485760}") long maxDecompressedBytes) {
    this.maxDecompressedBytes = maxDecompressedBytes;
    this.decoders = Map.of(CONTENT_ENCODING_IDENTITY, this::decodeIdentity, CONTENT_ENCODING_GZIP,
        this::decodeGzip);
  }

  /// Decodes incoming payload bytes or string representations based on request
  /// headers.
  public String decode(Object encodedPayload, Map<String, Object> headers) {
    String contentEncoding = extractContentEncodingHeader(headers);
    List<String> encodingChain = parseEncodingChain(contentEncoding);

    log.debug("Content-Encoding chain resolved: {}",
        contentEncoding != null ? sanitizeHeaderValue(contentEncoding) : CONTENT_ENCODING_IDENTITY);

    if (encodingChain.isEmpty()) {
      return payloadToString(encodedPayload);
    }

    // Reject unsupported encodings explicitly — silent pass-through risks silent
    // data corruption.
    List<String> unsupported = encodingChain.stream()
        .filter(encoding -> !decoders.containsKey(encoding)).toList();
    if (!unsupported.isEmpty()) {
      throw new WebhookDecodingException(
          "Unsupported Content-Encoding: '" + sanitizeHeaderValue(contentEncoding)
              + "'. Supported encodings: " + String.join(", ", decoders.keySet()));
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
          "Failed to decompress payload for encoding: " + sanitizeHeaderValue(contentEncoding), e);
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

  /// Decompresses a gzip payload with a hard upper bound to prevent Zip Bomb
  /// (DoS) attacks.
  ///
  /// Reads in chunks and aborts with `WebhookDecodingException` if the
  /// decompressed size
  /// exceeds `maxDecompressedBytes`.
  private byte[] decodeGzip(byte[] encodedPayload) throws IOException {
    try (GZIPInputStream gzipInput = new GZIPInputStream(new ByteArrayInputStream(encodedPayload));
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[DECOMPRESSION_BUFFER_SIZE];
      long totalBytesRead = 0;
      int bytesRead;
      while ((bytesRead = gzipInput.read(buffer)) != -1) {
        totalBytesRead += bytesRead;
        if (totalBytesRead > maxDecompressedBytes) {
          throw new WebhookDecodingException("Decompressed payload exceeds maximum allowed size of "
              + maxDecompressedBytes + " bytes");
        }
        output.write(buffer, 0, bytesRead);
      }
      return output.toByteArray();
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
      case String s -> s.getBytes(StandardCharsets.UTF_8);
      default -> payload.toString().getBytes(StandardCharsets.UTF_8);
    };
  }

  /// Strips control characters (including CRLF) and truncates header values to
  /// prevent log injection.
  private String sanitizeHeaderValue(String headerValue) {
    if (headerValue == null)
      return null;
    String sanitized = headerValue.replaceAll("[\\x00-\\x1F\\x7F]", "");
    return sanitized.length() > SANITIZED_HEADER_MAX_LENGTH
        ? sanitized.substring(0, SANITIZED_HEADER_MAX_LENGTH) + "..."
        : sanitized;
  }
}
