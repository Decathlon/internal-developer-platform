package com.decathlon.idp_core.infrastructure.adapters.ingestion.exception_handler;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.entity.EntityAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity.EntityDeletionBlockedException;
import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingConfigurationException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingJsltErrorException;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.ExpressionEvaluationFailedException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConfigurationMissingException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorNotFoundException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookDisabledException;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthForbiddenException;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookAuthUnauthorizedException;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookDecodingException;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookSecurityException;

import lombok.RequiredArgsConstructor;

/// Declares error handling mappings for inbound webhooks.
@Component
@RequiredArgsConstructor
public class WebhookExceptionRouteBuilder {

  private final WebhookExceptionHandlerHelper handlerHelper;

  /// Registers webhook exception mappings with consistent HTTP and
  /// logging behavior.
  @SuppressWarnings("unchecked")
  public void configureExceptions(RouteBuilder routeBuilder) {
    handlerHelper.registerHandler(routeBuilder, WebhookConnectorNotFoundException.class,
        WebhookErrorCode.CONNECTOR_NOT_FOUND);
    handlerHelper.registerHandler(routeBuilder, WebhookDisabledException.class,
        WebhookErrorCode.CONNECTOR_DISABLED);
    handlerHelper.registerHandler(routeBuilder, WebhookConfigurationMissingException.class,
        WebhookErrorCode.CONFIGURATION_MISSING);
    handlerHelper.registerHandler(routeBuilder, WebhookDecodingException.class,
        WebhookErrorCode.INVALID_COMPRESSED_PAYLOAD);
    handlerHelper.registerHandler(routeBuilder, WebhookAuthenticationException.class,
        WebhookErrorCode.AUTHENTICATION_FAILED);
    handlerHelper.registerHandler(routeBuilder, WebhookAuthUnauthorizedException.class,
        WebhookErrorCode.AUTHENTICATION_REQUIRED);
    handlerHelper.registerHandler(routeBuilder, WebhookAuthForbiddenException.class,
        WebhookErrorCode.AUTHENTICATION_FORBIDDEN);
    handlerHelper.registerHandler(routeBuilder, WebhookSecurityException.class,
        WebhookErrorCode.AUTHENTICATION_REQUIRED);

    handlerHelper.registerHandlers(routeBuilder, WebhookErrorCode.ENTITY_INGESTION_ERROR, true,
        EntityDynamicMappingJsltErrorException.class, ExpressionEvaluationFailedException.class,
        EntityDynamicMappingConfigurationException.class, EntityValidationException.class,
        EntityNotFoundException.class, EntityAlreadyExistsException.class,
        EntityDeletionBlockedException.class);

    handlerHelper.registerHandler(routeBuilder, Exception.class, WebhookErrorCode.UNEXPECTED_ERROR);

  }
}
