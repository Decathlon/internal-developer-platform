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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookDecodingException;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.metrics.IngestionObservabilitySupport;

import io.opentelemetry.api.metrics.Meter;

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
public class DecodingProcessor {

  private static final int SANITIZED_HEADER_MAX_LENGTH = 128;
  private static final int DECOMPRESSION_BUFFER_SIZE = 8192;
  private static final long MAX_INPUT_PAYLOAD_BYTES = 10L * 1024 * 1024; // 10MB
  private static final String BYTES_SUFFIX = " bytes";
  private static final String INPUT_PAYLOAD_STRING_LENGTH_PREFIX = "Input payload string length ";
  private static final String INPUT_PAYLOAD_SIZE_PREFIX = "Input payload size ";
  private static final String EXCEEDS_MAX_SIZE_FRAGMENT = " exceeds maximum allowed size of ";

  private final Map<String, PayloadDecoder> decoders;
  private final Meter meter;
  private final long maxDecompressedBytes;

  public DecodingProcessor(
      @Value("${idp.ingestion.max-decompressed-bytes:10485760}") long maxDecompressedBytes,
      @Qualifier("decodingMeter") Meter meter) {
    this.maxDecompressedBytes = maxDecompressedBytes;
    this.meter = meter;
    this.decoders = Map.of(CONTENT_ENCODING_IDENTITY, this::decodeIdentity, CONTENT_ENCODING_GZIP,
        this::decodeGzip);
  }

  /// Decodes incoming payload bytes or string representations based on request
  /// headers. Accepts Object to support both byte[] and String from Camel.
  /// All conversions are validated against MAX_INPUT_PAYLOAD_BYTES.
  public String decode(Object encodedPayload, Map<String, Object> headers) {
    return decode(encodedPayload, headers, UNKNOWN_VALUE);
  }

  /// Decodes incoming payload bytes or string representations based on request
  /// headers and the resolved connector identifier. The connector is passed
  /// explicitly from the route so metrics are attributed to the actual webhook
  /// configuration instead of a potentially spoofed request header.
  public String decode(Object encodedPayload, Map<String, Object> headers,
      String connectorIdentifier) {
    String contentEncoding = extractContentEncodingHeader(headers);
    List<String> encodingChain = parseEncodingChain(contentEncoding);
    String connector = connectorIdentifier == null || connectorIdentifier.isBlank()
        ? UNKNOWN_VALUE
        : connectorIdentifier;

    try {
      if (encodingChain.isEmpty()) {
        String decoded = payloadToString(encodedPayload, connector);
        long payloadSizeBytes = encodedPayload instanceof byte[] bytes
            ? bytes.length
            : decoded.getBytes(StandardCharsets.UTF_8).length;
        recordPayloadSizeMetric(connector, payloadSizeBytes);
        incrementDecodedMetric(connector, "identity");
        return decoded;
      }

      List<String> unsupported = encodingChain.stream()
          .filter(encoding -> !decoders.containsKey(encoding)).toList();
      if (!unsupported.isEmpty()) {
        incrementDecodeErrorMetric(connector, "unsupported_encoding");
        throw new WebhookDecodingException(
            "Unsupported Content-Encoding: '" + sanitizeHeaderValue(contentEncoding)
                + "'. Supported encodings: " + String.join(", ", decoders.keySet()));
      }

      byte[] decodedPayload = toByteArray(encodedPayload, connector);
      if (decodedPayload.length == 0 && encodingChain.contains(CONTENT_ENCODING_GZIP)) {
        incrementDecodeErrorMetric(connector, "empty_gzip_payload");
        throw new WebhookDecodingException("Empty payload cannot be decoded as gzip");
      }

      return decodePayloadWithEncodingChain(decodedPayload, encodingChain, contentEncoding,
          connector);
    } catch (RuntimeException exception) {
      if (!(exception instanceof WebhookDecodingException)) {
        incrementDecodeErrorMetric(connector, exception.getClass().getSimpleName());
      }
      throw exception;
    }
  }

  private byte[] decodeIdentity(byte[] payload) {
    return payload;
  }

  private String resolveEncodingMetricLabel(List<String> encodingChain) {
    return IngestionObservabilitySupport.resolveEncodingMetricLabel(encodingChain);
  }

  private void throwPayloadSizeExceeded(String connector, String message) {
    incrementDecodeErrorMetric(connector, "payload_too_large");
    throw new WebhookDecodingException(message);
  }

  private void throwDecompressionLimitExceeded(String connector, String message) {
    incrementDecodeErrorMetric(connector, "decompression_limit_exceeded");
    throw new WebhookDecodingException(message);
  }

  private String decodePayloadWithEncodingChain(byte[] decodedPayload, List<String> encodingChain,
      String contentEncoding, String connector) {
    try {
      for (int i = encodingChain.size() - 1; i >= 0; i--) {
        String encoding = encodingChain.get(i);
        if (CONTENT_ENCODING_GZIP.equals(encoding)) {
          decodedPayload = decodeGzip(decodedPayload, connector);
        } else {
          decodedPayload = decoders.get(encoding).decode(decodedPayload);
        }
      }
      String result = new String(decodedPayload, StandardCharsets.UTF_8);
      recordPayloadSizeMetric(connector, decodedPayload.length);
      incrementDecodedMetric(connector, resolveEncodingMetricLabel(encodingChain));
      return result;
    } catch (ZipException e) {
      incrementDecodeErrorMetric(connector, "corrupted_gzip_payload");
      throw new WebhookDecodingException("Corrupted or invalid compressed gzip stream", e);
    } catch (IOException e) {
      incrementDecodeErrorMetric(connector, "decompression_failure");
      throw new WebhookDecodingException(
          "Failed to decompress payload for encoding: " + sanitizeHeaderValue(contentEncoding), e);
    }
  }

  private byte[] decodeGzip(byte[] encodedPayload) throws IOException {
    return decodeGzip(encodedPayload, UNKNOWN_VALUE);
  }

  private byte[] decodeGzip(byte[] encodedPayload, String connector) throws IOException {
    try (GZIPInputStream gzipInput = new GZIPInputStream(new ByteArrayInputStream(encodedPayload));
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[DECOMPRESSION_BUFFER_SIZE];
      long totalBytesRead = 0;
      int bytesRead;
      while ((bytesRead = gzipInput.read(buffer)) != -1) {
        totalBytesRead += bytesRead;
        if (totalBytesRead > maxDecompressedBytes) {
          String message = "Decompressed payload exceeds maximum allowed size of "
              + maxDecompressedBytes + BYTES_SUFFIX;
          throwDecompressionLimitExceeded(connector, message);
        }
        output.write(buffer, 0, bytesRead);
      }
      return output.toByteArray();
    }
  }

  private String payloadToString(Object payload, String connector) {
    if (payload == null) {
      return "";
    }

    return switch (payload) {
      case String s -> {
        if (s.length() > MAX_INPUT_PAYLOAD_BYTES) {
          throwPayloadSizeExceeded(connector,
              INPUT_PAYLOAD_STRING_LENGTH_PREFIX + s.length() + EXCEEDS_MAX_SIZE_FRAGMENT
                  + MAX_INPUT_PAYLOAD_BYTES + BYTES_SUFFIX);
        }
        yield s;
      }
      case byte[] b -> {
        if (b.length > MAX_INPUT_PAYLOAD_BYTES) {
          throwPayloadSizeExceeded(connector,
              INPUT_PAYLOAD_SIZE_PREFIX + b.length + BYTES_SUFFIX + EXCEEDS_MAX_SIZE_FRAGMENT
                  + MAX_INPUT_PAYLOAD_BYTES + BYTES_SUFFIX);
        }
        yield new String(b, StandardCharsets.UTF_8);
      }
      default -> {
        String str = payload.toString();
        if (str.length() > MAX_INPUT_PAYLOAD_BYTES) {
          throwPayloadSizeExceeded(connector,
              INPUT_PAYLOAD_STRING_LENGTH_PREFIX + str.length() + EXCEEDS_MAX_SIZE_FRAGMENT
                  + MAX_INPUT_PAYLOAD_BYTES + BYTES_SUFFIX);
        }
        yield str;
      }
    };
  }

  private byte[] toByteArray(Object payload, String connector) {
    if (payload == null) {
      return new byte[0];
    }

    return switch (payload) {
      case byte[] b -> {
        if (b.length > MAX_INPUT_PAYLOAD_BYTES) {
          throwPayloadSizeExceeded(connector,
              INPUT_PAYLOAD_SIZE_PREFIX + b.length + BYTES_SUFFIX + EXCEEDS_MAX_SIZE_FRAGMENT
                  + MAX_INPUT_PAYLOAD_BYTES + BYTES_SUFFIX);
        }
        yield b;
      }
      case String s -> {
        if (s.length() > MAX_INPUT_PAYLOAD_BYTES) {
          throwPayloadSizeExceeded(connector,
              INPUT_PAYLOAD_STRING_LENGTH_PREFIX + s.length() + EXCEEDS_MAX_SIZE_FRAGMENT
                  + MAX_INPUT_PAYLOAD_BYTES + BYTES_SUFFIX);
        }
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_INPUT_PAYLOAD_BYTES) {
          throwPayloadSizeExceeded(connector,
              INPUT_PAYLOAD_SIZE_PREFIX + bytes.length + BYTES_SUFFIX + EXCEEDS_MAX_SIZE_FRAGMENT
                  + MAX_INPUT_PAYLOAD_BYTES + BYTES_SUFFIX);
        }
        yield bytes;
      }
      default -> {
        String str = payload.toString();
        if (str.length() > MAX_INPUT_PAYLOAD_BYTES) {
          throwPayloadSizeExceeded(connector,
              INPUT_PAYLOAD_STRING_LENGTH_PREFIX + str.length() + EXCEEDS_MAX_SIZE_FRAGMENT
                  + MAX_INPUT_PAYLOAD_BYTES + BYTES_SUFFIX);
        }
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_INPUT_PAYLOAD_BYTES) {
          throwPayloadSizeExceeded(connector,
              INPUT_PAYLOAD_SIZE_PREFIX + bytes.length + BYTES_SUFFIX + EXCEEDS_MAX_SIZE_FRAGMENT
                  + MAX_INPUT_PAYLOAD_BYTES + BYTES_SUFFIX);
        }
        yield bytes;
      }
    };
  }

  private void recordPayloadSizeMetric(String connector, long payloadSizeBytes) {
    IngestionObservabilitySupport.recordPayloadSizeMetric(meter, connector, payloadSizeBytes);
  }

  private void incrementDecodedMetric(String connector, String encoding) {
    IngestionObservabilitySupport.recordDecodedMetric(meter, connector, encoding);
  }

  private void incrementDecodeErrorMetric(String connector, String reason) {
    IngestionObservabilitySupport.recordDecodeErrorMetric(meter, connector, reason);
  }

  private String extractContentEncodingHeader(Map<String, Object> headers) {
    if (headers == null) {
      return null;
    }

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

  private String sanitizeHeaderValue(String headerValue) {
    if (headerValue == null) {
      return null;
    }
    String sanitized = headerValue.replaceAll("[\\x00-\\x1F\\x7F]", "");
    return sanitized.length() > SANITIZED_HEADER_MAX_LENGTH
        ? sanitized.substring(0, SANITIZED_HEADER_MAX_LENGTH) + "..."
        : sanitized;
  }

}
