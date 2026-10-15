package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor;

import static com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionObservabilityConstants.*;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;
import com.decathlon.idp_core.domain.model.enums.WebhookSecurityType;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookSecurity;
import com.decathlon.idp_core.domain.port.WebhookSecurityStrategy;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.configuration.IngestionObservabilityConstants;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.exception.WebhookSecurityException;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.metrics.IngestionObservabilitySupport;
import com.decathlon.idp_core.infrastructure.adapters.ingestion.security.WebhookRequestAuthenticator;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SecurityProcessor {

  private static final String WEBHOOK_AUTHENTICATION_FAILED_MESSAGE = "Webhook authentication failed";
  private static final String SPAN_ATTRIBUTE_WEBHOOK_IDENTIFIER = "idp.webhook.identifier";
  private static final String SPAN_ATTRIBUTE_SECURITY_RESULT = "idp.webhook.security.result";
  private static final String RESULT_SUCCESS = "SUCCESS";
  private static final String RESULT_REJECTED = "REJECTED";
  private static final String RESULT_ERROR = "ERROR";

  private final List<WebhookSecurityStrategy> strategies;
  private final Meter meter;

  public SecurityProcessor(List<WebhookSecurityStrategy> strategies,
      @Qualifier("securityMeter") Meter meter) {
    this.strategies = List.copyOf(strategies);
    this.meter = meter;
  }

  public void validate(Map<String, Object> headers, byte[] rawPayload,
      WebhookConnector webhookConnector) {
    String webhookIdentifier = webhookConnector.identifier();
    WebhookSecurity security = webhookConnector.security();
    String securityType = security == null || security.type() == null
        ? WebhookSecurityType.NONE.name()
        : security.type().name();

    incrementCounter(IngestionObservabilityConstants.SECURITY_REQUESTS_TOTAL, ATTRIBUTE_CONNECTOR,
        webhookIdentifier, "security_type", securityType);
    IngestionObservabilitySupport.setSpanAttribute(Span.current(),
        SPAN_ATTRIBUTE_WEBHOOK_IDENTIFIER, webhookIdentifier);

    if (security == null || security.type() == WebhookSecurityType.NONE) {
      recordSecurityResult(webhookIdentifier, securityType, RESULT_SUCCESS);
      return;
    }

    try {
      WebhookSecurityStrategy strategy = strategies.stream()
          .filter(candidate -> candidate.supports(security.type())).findFirst()
          .orElseThrow(() -> new WebhookSecurityException(WEBHOOK_AUTHENTICATION_FAILED_MESSAGE));

      if (!(strategy instanceof WebhookRequestAuthenticator authenticator)) {
        throw new WebhookSecurityException(WEBHOOK_AUTHENTICATION_FAILED_MESSAGE);
      }

      authenticator.validateRequest(headers, rawPayload == null ? new byte[0] : rawPayload,
          security.config());

      recordSecurityResult(webhookIdentifier, securityType, RESULT_SUCCESS);
      log.debug("Webhook security validation passed for connector '{}' with strategy '{}'.",
          webhookIdentifier, security.type());
    } catch (RuntimeException exception) {
      String result = isSecurityRejected(exception) ? RESULT_REJECTED : RESULT_ERROR;
      recordSecurityResult(webhookIdentifier, securityType, result);
      incrementCounter(IngestionObservabilityConstants.SECURITY_VALIDATION_ERRORS_TOTAL,
          ATTRIBUTE_CONNECTOR, webhookIdentifier, ATTRIBUTE_SECURITY_TYPE, securityType,
          ATTRIBUTE_REASON, classifyFailure(exception));
      throw exception;
    }
  }

  private void recordSecurityResult(String webhookIdentifier, String securityType, String result) {
    incrementCounter(IngestionObservabilityConstants.SECURITY_RESULT, ATTRIBUTE_CONNECTOR,
        webhookIdentifier, ATTRIBUTE_SECURITY_TYPE, securityType, "result", result);
    IngestionObservabilitySupport.setSpanAttribute(Span.current(), SPAN_ATTRIBUTE_SECURITY_RESULT,
        result);
  }

  private void incrementCounter(String metricName, String... attributes) {
    IngestionObservabilitySupport.incrementCounter(meter, metricName, attributes);
  }

  private boolean isSecurityRejected(RuntimeException exception) {
    return exception instanceof WebhookAuthenticationException
        || exception instanceof WebhookSecurityException;
  }

  private String classifyFailure(RuntimeException exception) {
    String reason = exception.getClass().getSimpleName();
    return reason.isBlank() ? "unknown" : reason;
  }
}
