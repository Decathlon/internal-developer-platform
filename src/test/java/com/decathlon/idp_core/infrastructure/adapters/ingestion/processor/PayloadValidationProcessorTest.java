package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookPayloadTooLargeException;

@DisplayName("PayloadValidationProcessor unit tests")
class PayloadValidationProcessorTest {

  private static final long MAX_PAYLOAD_BYTES = 10L * 1024 * 1024;

  private final PayloadValidationProcessor processor = new PayloadValidationProcessor();

  @Test
  @DisplayName("Should reject a payload that exceeds the maximum allowed size")
  void shouldRejectPayloadWhenSizeExceedsMaximum() {
    Exchange exchange = mock(Exchange.class);
    Message message = mock(Message.class);
    byte[] oversizedPayload = new byte[(int) (MAX_PAYLOAD_BYTES + 1)];

    when(exchange.getMessage()).thenReturn(message);
    when(exchange.getIn()).thenReturn(message);
    when(message.getBody(byte[].class)).thenReturn(oversizedPayload);

    assertThatThrownBy(() -> processor.validate(exchange))
        .isInstanceOf(WebhookPayloadTooLargeException.class)
        .hasMessageContaining("exceeds maximum allowed size of 10485760 bytes");

    verify(message).getBody(byte[].class);
    verify(message).getHeader("Content-Encoding", String.class);
    verify(exchange, never()).setProperty(anyString(), any());
    verify(message, never()).setBody(any());
  }

  @Test
  @DisplayName("Should store the raw payload when size is within the limit")
  void shouldStoreRawPayloadWhenSizeIsWithinLimit() {
    Exchange exchange = mock(Exchange.class);
    Message message = mock(Message.class);
    byte[] payload = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);

    when(exchange.getMessage()).thenReturn(message);
    when(exchange.getIn()).thenReturn(message);
    when(message.getBody(byte[].class)).thenReturn(payload);

    processor.validate(exchange);

    verify(exchange).setProperty("rawPayloadBody", payload);
    verify(message).setBody(payload);
  }
}
