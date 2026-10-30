package com.decathlon.idp_core.infrastructure.adapters.ingestion.exception_handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookDecodingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("Webhook exception handler helper tests")
class WebhookExceptionHandlerHelperTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("Maps decoding exceptions to RFC 7807 responses")
  void registerHandler_whenDecodingException_returnsProblemDetail() throws Exception {
    var context = new DefaultCamelContext();
    var handlerHelper = new WebhookExceptionHandlerHelper(objectMapper);
    context.addRoutes(new RouteBuilder() {
      @Override
      public void configure() {
        handlerHelper.registerHandler(this, WebhookDecodingException.class,
            WebhookErrorCode.INVALID_COMPRESSED_PAYLOAD);
        from("direct:decode").process(exchange -> {
          throw new WebhookDecodingException("Corrupted gzip payload");
        });
      }
    });

    ProducerTemplate producerTemplate = null;
    try {
      context.start();
      producerTemplate = context.createProducerTemplate();
      producerTemplate.start();

      Exchange response = producerTemplate.request("direct:decode", exchange -> {
      });

      assertEquals(400, response.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
      assertEquals("application/problem+json",
          response.getMessage().getHeader(Exchange.CONTENT_TYPE));
      JsonNode body = objectMapper.readTree(response.getMessage().getBody(String.class));
      assertEquals("about:blank", body.path("type").asText());
      assertEquals("Bad Request", body.path("title").asText());
      assertEquals(400, body.path("status").asInt());
      assertEquals("Invalid, unsupported, or oversized compressed payload",
          body.path("detail").asText());
      assertEquals("invalid_compressed_payload", body.path("error").asText());
    } finally {
      if (producerTemplate != null) {
        producerTemplate.stop();
      }
      context.stop();
    }
  }
}
