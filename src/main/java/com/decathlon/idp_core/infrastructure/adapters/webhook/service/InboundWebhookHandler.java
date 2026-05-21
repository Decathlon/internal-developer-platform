package com.decathlon.idp_core.infrastructure.adapters.webhook.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorNotFoundException;
import com.decathlon.idp_core.domain.model.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.port.WebhookConnectorRepositoryPort;
import com.decathlon.idp_core.infrastructure.adapters.webhook.security.WebhookSecurityValidatorDispatcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/// Generic inbound webhook handler — infrastructure entry point for all connectors.
///
/// **Responsibilities (infrastructure only, no business logic):**
/// 1. Resolve the [WebhookConnector] configuration from the database by `configurationId`
/// 2. Validate the request credentials by delegating to [WebhookSecurityValidatorDispatcher]
///    which applies the **Strategy pattern** to route to the correct security implementation.
/// 3. Accept the request and eventually forward the raw payload to Camel for mapping + ingestion.
///    This last step is not implemented yet: the handler currently stops after security validation.
///
/// **Architecture note (ADR-0003):**
/// This class intentionally contains no source-specific logic (no GitHub, no SonarQube, etc.).
/// All payload interpretation (JQ filter + field mapping) is delegated to Camel,
/// which reads the [WebhookConnector] mappings at runtime.
///
/// **Extension guide (Strategy pattern):**
/// To support a new security strategy, add a new Spring bean implementing [WebhookSecurityValidator].
/// The dispatcher auto-discovers it — no changes required here.
@Slf4j
@Service
@RequiredArgsConstructor
public class InboundWebhookHandler {

    private final WebhookConnectorRepositoryPort connectorRepository;
    private final WebhookSecurityValidatorDispatcher securityDispatcher;

    /// Processes any inbound webhook event.
    ///
    /// @param configurationId identifies the connector configuration stored in the database
    /// @param headers         all HTTP request headers (used for signature / auth extraction)
    /// @param rawBody         raw request body bytes (used for HMAC digest + later JQ mapping)
    /// @throws WebhookConnectorNotFoundException if no connector is registered for this identifier
    /// @throws WebhookAuthenticationException    if security validation fails
    public void handle(String configurationId, Map<String, String> headers, byte[] rawBody) {
        WebhookConnector connector = connectorRepository.findByIdentifier(configurationId)
                .orElseThrow(() -> new WebhookConnectorNotFoundException(configurationId));

        if (!connector.enabled()) {
            log.warn("Webhook connector '{}' is disabled. Ignoring incoming event.", configurationId);
            return;
        }

        // Delegates to the appropriate security strategy via the Strategy pattern dispatcher.
        // Each WebhookSecurityValidator implementation handles one WebhookSecurityType.
        securityDispatcher.dispatch(connector.security(), headers, rawBody);

        // TODO (ADR-0003 / Camel integration):
        // Forward rawBody + connector to a Camel route:
        //   producerTemplate.sendBodyAndHeader("direct:webhook-ingest", rawBody, "connector", connector);
        // The Camel route will apply JQ mappings and call EntityService.
        // Until then, webhook ingestion is effectively "accepted after auth" only.
        log.info("Webhook event received for connector '{}' ({} bytes). Pending Camel routing.",
                configurationId, rawBody.length);
    }
}
