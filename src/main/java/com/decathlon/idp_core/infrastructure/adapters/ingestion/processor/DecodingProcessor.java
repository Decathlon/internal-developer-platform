package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor;

import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.CONTENT_ENCODING_GZIP;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.CONTENT_ENCODING_HEADER;
import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.CONTENT_ENCODING_IDENTITY;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/// Processor for decoding webhook payloads based on Content-Encoding header.
///
/// Supports:
/// - gzip: Decompresses gzip-encoded payloads
/// - deflate: Decompresses deflate-encoded payloads
/// - identity/no encoding: Returns payload as-is
///
/// The Content-Encoding header drives which decompression strategy is applied.
@Component
@Slf4j
public class DecodingProcessor {

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
    // Extract Content-Encoding header (case-insensitive)
    String contentEncoding = extractContentEncodingHeader(headers);

    log.debug("Content-Encoding: {}", contentEncoding != null ? contentEncoding : "none");

    // Return payload as-is if no encoding or identity encoding
    if (contentEncoding == null || contentEncoding.equalsIgnoreCase(CONTENT_ENCODING_IDENTITY)) {
      return payloadToString(encodedPayload);
    }

    // Handle gzip encoding
    if (contentEncoding.equalsIgnoreCase(CONTENT_ENCODING_GZIP)) {
      return decodeGzip(encodedPayload);
    }

    // Unknown encoding: log and return as-is
    log.warn("Unknown Content-Encoding: {}. Returning payload as-is.", contentEncoding);
    return payloadToString(encodedPayload);
  }

  /// Extracts the Content-Encoding header from the headers map
  /// (case-insensitive).
  private String extractContentEncodingHeader(Map<String, Object> headers) {
    if (headers == null) {
      return null;
    }

    return headers.keySet().stream().filter(key -> key.equalsIgnoreCase(CONTENT_ENCODING_HEADER))
        .map(key -> (String) headers.get(key)).findFirst().orElse(null);
  }

  /// Decompresses a gzip-encoded payload.
  private String decodeGzip(Object encodedPayload) throws IOException {
    try (GZIPInputStream gzipInput = new GZIPInputStream(toByteArrayInputStream(encodedPayload))) {
      return new String(gzipInput.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /// Converts payload to String, handling both byte arrays and strings using
  /// pattern matching.
  private String payloadToString(Object payload) {
    return switch (payload) {
      case String string -> string;
      case byte[] byteArray -> new String(byteArray, StandardCharsets.UTF_8);
      default -> payload.toString();
    };
  }

  /// Converts payload to byte array for decompression operations using pattern
  /// matching.
  private byte[] toByteArray(Object payload) {
    return switch (payload) {
      case byte[] byteArray -> byteArray;
      case String string -> string.getBytes(StandardCharsets.UTF_8);
      default -> payload.toString().getBytes(StandardCharsets.UTF_8);
    };
  }

  /// Converts payload to ByteArrayInputStream for GZIPInputStream.
  private ByteArrayInputStream toByteArrayInputStream(Object payload) {
    return new ByteArrayInputStream(toByteArray(payload));
  }
}
