package com.decathlon.idp_core.infrastructure.adapters.ingestion.exception_handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

class WebhookExceptionHandlerHelperTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void writesProblemDetailAndPreservesWebhookErrorCodeAndRequestPath() throws Exception {
    var helper = new WebhookExceptionHandlerHelper(objectMapper);
    try (var camelContext = new DefaultCamelContext()) {
      camelContext.addRoutes(new RouteBuilder() {
        @Override
        public void configure() {
          helper.registerHandler(this, IllegalArgumentException.class,
              WebhookErrorCode.AUTHENTICATION_FAILED);
          from("direct:webhook-error").process(exchange -> {
            exchange.getMessage().setHeader(Exchange.HTTP_PATH, "/webhooks/test-connector");
            throw new IllegalArgumentException("Sensitive validation detail");
          });
        }
      });
      camelContext.start();

      ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
      try {
        Exchange response = producerTemplate.request("direct:webhook-error", exchange -> {
        });

        assertEquals(HttpStatus.UNAUTHORIZED.value(),
            response.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE));
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON_VALUE,
            response.getMessage().getHeader(Exchange.CONTENT_TYPE));
        var body = objectMapper.readTree(response.getMessage().getBody(String.class));
        assertEquals("about:blank", body.get("type").asText());
        assertEquals("Unauthorized", body.get("title").asText());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), body.get("status").asInt());
        assertEquals("Webhook authentication failed", body.get("detail").asText());
        assertEquals("/webhooks/test-connector", body.get("instance").asText());
        assertEquals("webhook_authentication_failed", body.get("error").asText());
        assertEquals("Webhook authentication failed", body.get("error_description").asText());
        assertTrue(body.hasNonNull("timestamp"));
      } finally {
        producerTemplate.stop();
      }
    }
  }
}
