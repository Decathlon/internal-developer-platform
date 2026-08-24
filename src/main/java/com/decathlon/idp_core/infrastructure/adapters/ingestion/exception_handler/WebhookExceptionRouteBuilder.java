package com.decathlon.idp_core.infrastructure.adapters.ingestion.exception_handler;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.entity.EntityDeletionBlockedException;
import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingConfigurationException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConfigurationMissingException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorNotFoundException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookDisabledException;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookDecodingException;

import lombok.RequiredArgsConstructor;

/// Declares error handling mappings for inbound webhooks.
@Component
@RequiredArgsConstructor
public class WebhookExceptionRouteBuilder {

  private final WebhookExceptionHandlerHelper handlerHelper;

  /// Registers webhook exception mappings with consistent HTTP and
  /// logging behavior.
  public void configureExceptions(RouteBuilder routeBuilder) {
    handlerHelper.registerHandler(routeBuilder, WebhookConnectorNotFoundException.class,
        WebhookErrorCode.CONNECTOR_NOT_FOUND);
    handlerHelper.registerHandler(routeBuilder, WebhookDisabledException.class,
        WebhookErrorCode.CONNECTOR_DISABLED);
    handlerHelper.registerHandler(routeBuilder, WebhookDecodingException.class,
        WebhookErrorCode.INVALID_ENCODED_PAYLOAD, true);
    handlerHelper.registerHandler(routeBuilder, WebhookConfigurationMissingException.class,
        WebhookErrorCode.CONFIGURATION_MISSING);
    handlerHelper.registerHandler(routeBuilder, EntityValidationException.class,
        WebhookErrorCode.ENTITY_INGESTION_ERROR, true);
    handlerHelper.registerHandler(routeBuilder, EntityNotFoundException.class,
        WebhookErrorCode.ENTITY_INGESTION_ERROR, true);
    handlerHelper.registerHandler(routeBuilder, EntityDynamicMappingConfigurationException.class,
        WebhookErrorCode.ENTITY_INGESTION_ERROR, true);
    handlerHelper.registerHandler(routeBuilder, EntityDeletionBlockedException.class,
        WebhookErrorCode.ENTITY_INGESTION_ERROR, true);
    handlerHelper.registerHandler(routeBuilder, Exception.class, WebhookErrorCode.UNEXPECTED_ERROR);

  }
}
