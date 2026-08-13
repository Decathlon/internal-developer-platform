package com.decathlon.idp_core.infrastructure.adapters.ingestion.exception_handler;

import static org.mockito.Mockito.verify;

import org.apache.camel.builder.RouteBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookExceptionRouteBuilder unit tests")
class WebhookExceptionRouteBuilderTest {

  @Mock
  private WebhookExceptionHandlerHelper handlerHelper;

  @Mock
  private RouteBuilder routeBuilder;

  @InjectMocks
  private WebhookExceptionRouteBuilder webhookExceptionRouteBuilder;

  @Test
  @DisplayName("Registers dedicated mapping for WebhookAuthenticationException")
  void configureExceptions_registersAuthenticationFailureMapping() {
    webhookExceptionRouteBuilder.configureExceptions(routeBuilder);

    verify(handlerHelper).registerHandler(routeBuilder, WebhookAuthenticationException.class,
        WebhookErrorCode.AUTHENTICATION_FAILED);
  }
}

