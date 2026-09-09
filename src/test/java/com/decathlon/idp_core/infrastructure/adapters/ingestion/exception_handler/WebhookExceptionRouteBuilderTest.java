package com.decathlon.idp_core.infrastructure.adapters.ingestion.exception_handler;

import static org.mockito.Mockito.verify;

import org.apache.camel.builder.RouteBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthForbiddenException;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthUnauthorizedException;

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
  @DisplayName("Registers dedicated mappings for webhook 401 and 403 security exceptions")
  void configureExceptions_registersAuthenticationMappings() {
    webhookExceptionRouteBuilder.configureExceptions(routeBuilder);

    verify(handlerHelper).registerHandler(routeBuilder, WebhookAuthUnauthorizedException.class,
        WebhookErrorCode.AUTHENTICATION_REQUIRED);
    verify(handlerHelper).registerHandler(routeBuilder, WebhookAuthForbiddenException.class,
        WebhookErrorCode.AUTHENTICATION_FORBIDDEN);
  }
}
