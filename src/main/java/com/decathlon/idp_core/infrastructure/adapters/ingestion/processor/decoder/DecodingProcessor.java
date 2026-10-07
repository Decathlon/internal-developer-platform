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
///
/// Security: All payload conversions (String→byte[], byte[]→String) are validated against
/// MAX_INPUT_PAYLOAD_BYTES to prevent unbounded allocations.
@Component
@Slf4j
public class DecodingProcessor {

  private static final int SANITIZED_HEADER_MAX_LENGTH = 128;
  private static final int DECOMPRESSION_BUFFER_SIZE = 8192;
  // Maximum input payload size (before decompression) — aligns with
  // PayloadValidationProcessor
  // Prevents unbounded String→byte[] or byte[]→String conversions
  private static final long MAX_INPUT_PAYLOAD_BYTES = 10L * 1024 * 1024; // 10MB

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
  /// headers. Accepts Object to support both byte[] and String from Camel.
  /// All conversions are validated against MAX_INPUT_PAYLOAD_BYTES.
  public String decode(Object encodedPayload, Map<String, Object> headers) {
    String contentEncoding = extractContentEncodingHeader(headers);
    List<String> encodingChain = parseEncodingChain(contentEncoding);

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
      return new String(decodedPayload, StandardCharsets.UTF_8);
    } catch (ZipException e) {
      throw new WebhookDecodingException("Corrupted or invalid compressed gzip stream", e);
    } catch (IOException e) {
      throw new WebhookDecodingException(
          "Failed to decompress payload for encoding: " + sanitizeHeaderValue(contentEncoding), e);
    }
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
  /// decompressed size exceeds `maxDecompressedBytes`.
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

  /// Converts payload to String with validation against MAX_INPUT_PAYLOAD_BYTES.
  /// Prevents unbounded byte[]→String allocations.
  private String payloadToString(Object payload) {
    if (payload == null) return "";

    return switch (payload) {
      case String s -> {
        if (s.length() > MAX_INPUT_PAYLOAD_BYTES) {
          throw new WebhookDecodingException(
              "Input payload string length " + s.length() + " exceeds maximum allowed size of "
                  + MAX_INPUT_PAYLOAD_BYTES + " bytes");
        }
        yield s;
      }
      case byte[] b -> {
        // Validate array size before converting to string
        if (b.length > MAX_INPUT_PAYLOAD_BYTES) {
          throw new WebhookDecodingException(
              "Input payload size " + b.length + " bytes exceeds maximum allowed size of "
                  + MAX_INPUT_PAYLOAD_BYTES + " bytes");
        }
        yield new String(b, StandardCharsets.UTF_8);
      }
      default -> {
        // For unknown types, convert via toString() and validate
        String str = payload.toString();
        if (str.length() > MAX_INPUT_PAYLOAD_BYTES) {
          throw new WebhookDecodingException(
              "Input payload string length " + str.length() + " exceeds maximum allowed size of "
                  + MAX_INPUT_PAYLOAD_BYTES + " bytes");
        }
        yield str;
      }
    };
  }

  /// Converts payload to byte array with validation against
  /// MAX_INPUT_PAYLOAD_BYTES.
  /// Prevents unbounded String→byte[] and other conversions.
  private byte[] toByteArray(Object payload) {
    if (payload == null) return new byte[0];

    return switch (payload) {
      case byte[] b -> {
        // Validate array size
        if (b.length > MAX_INPUT_PAYLOAD_BYTES) {
          throw new WebhookDecodingException(
              "Input payload size " + b.length + " bytes exceeds maximum allowed size of "
                  + MAX_INPUT_PAYLOAD_BYTES + " bytes");
        }
        yield b;
      }
      case String s -> {
        if (s.length() > MAX_INPUT_PAYLOAD_BYTES) {
          throw new WebhookDecodingException(
              "Input payload string length " + s.length() + " exceeds maximum allowed size of "
                  + MAX_INPUT_PAYLOAD_BYTES + " bytes");
        }
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_INPUT_PAYLOAD_BYTES) {
          throw new WebhookDecodingException(
              "Input payload size " + bytes.length + " bytes exceeds maximum allowed size of "
                  + MAX_INPUT_PAYLOAD_BYTES + " bytes");
        }
        yield bytes;
      }
      default -> {
        // For unknown types, convert via toString() and validate
        String str = payload.toString();
        if (str.length() > MAX_INPUT_PAYLOAD_BYTES) {
          throw new WebhookDecodingException(
              "Input payload string length " + str.length() + " exceeds maximum allowed size of "
                  + MAX_INPUT_PAYLOAD_BYTES + " bytes");
        }
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_INPUT_PAYLOAD_BYTES) {
          throw new WebhookDecodingException(
              "Input payload size " + bytes.length + " bytes exceeds maximum allowed size of "
                  + MAX_INPUT_PAYLOAD_BYTES + " bytes");
        }
        yield bytes;
      }
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
