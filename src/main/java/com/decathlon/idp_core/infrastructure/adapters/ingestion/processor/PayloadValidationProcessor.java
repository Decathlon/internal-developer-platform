package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor;

import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionConstants.*;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookDecodingException;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookPayloadTooLargeException;

import lombok.extern.slf4j.Slf4j;

/// Validates and materializes the webhook payload for downstream processing.
///
/// This processor:
/// 1. Materializes the Camel message body to a byte array
/// 2. Checks that the payload does not exceed the maximum allowed size (if present)
/// 3. Validates that compressed payloads (with Content-Encoding header) are not null
/// 4. Stores the raw payload in a Camel exchange property for reuse by subsequent steps
///
/// The raw payload is stored as a property to allow multiple processors
/// to access it without needing to re-materialize the body.
///
/// **Validation Rules:**
/// - Null/empty payload without Content-Encoding → ALLOWED (decoder returns empty string)
/// - Null/empty payload WITH Content-Encoding → REJECTED (cannot decompress null)
/// - Non-null payload exceeding MAX_PAYLOAD_BYTES → REJECTED
@Component
@Slf4j
public class PayloadValidationProcessor {

  private static final long MAX_PAYLOAD_BYTES = 10L * 1024 * 1024; // 10MB

  /// Materializes and validates the webhook payload.
  ///
  /// @param exchange the Camel exchange containing the incoming webhook message
  /// @throws WebhookDecodingException if Content-Encoding is present but payload
  /// is null/empty
  /// @throws WebhookPayloadTooLargeException if payload exceeds MAX_PAYLOAD_BYTES
  public void validate(Exchange exchange) {
    byte[] rawPayload = exchange.getMessage().getBody(byte[].class);
    String contentEncoding = exchange.getIn().getHeader(CONTENT_ENCODING_HEADER, String.class);

    // If payload is null/empty and Content-Encoding is present, it's an error.
    // This is a decoding problem, not a size problem, and it should be mapped to
    // 400.
    if ((rawPayload == null || rawPayload.length == 0) && contentEncoding != null) {
      log.error(
          "Webhook payload validation failed: null/empty payload with Content-Encoding header");
      throw new WebhookDecodingException("Invalid, unsupported, or oversized compressed payload");
    }

    // Check payload size only if it's not null.
    if (rawPayload != null && rawPayload.length > MAX_PAYLOAD_BYTES) {
      log.error("Webhook payload validation failed: payload size {} exceeds maximum {} bytes",
          rawPayload.length, MAX_PAYLOAD_BYTES);
      throw new WebhookPayloadTooLargeException(
          "Webhook payload exceeds maximum allowed size of " + MAX_PAYLOAD_BYTES + " bytes");
    }

    if (rawPayload != null) {
      log.debug("Payload validation successful: {} bytes", rawPayload.length);
    } else {
      log.debug("Payload validation successful: null payload (no Content-Encoding)");
    }

    // Store the raw payload in a property for downstream processors.
    exchange.setProperty(RAW_PAYLOAD_BODY_PROPERTY, rawPayload);
    exchange.getMessage().setBody(rawPayload);
  }
}
