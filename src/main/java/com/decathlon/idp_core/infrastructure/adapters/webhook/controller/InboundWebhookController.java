package com.decathlon.idp_core.infrastructure.adapters.webhook.controller;

import com.decathlon.idp_core.infrastructure.adapters.webhook.service.InboundWebhookHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.springframework.http.HttpStatus.ACCEPTED;

/// Generic inbound webhook controller single entry point for ALL external event sources.
///
/// Architecture rationale (ADR-0003):
/// One endpoint receives every inbound webhook regardless of origin (GitHub, SonarQube…).
/// The configurationId path parameter identifies which connector configuration to apply
/// (security strategy, JQ mapping rules, target EntityTemplate).
/// Camel reads the connector configuration at runtime and handles the routing.
///
/// Security: public endpoint — each connector declares its own strategy (HMAC_SHA256,
/// STATIC_TOKEN, BASIC_AUTH, JWT_BEARER, NONE) validated before payload processing.
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Generic inbound webhook endpoint for all external event sources")
public class InboundWebhookController {

    private final InboundWebhookHandler handler;

    /// Receives any external webhook event for the given connector configuration.
    ///
    /// @param configurationId identifies the connector configuration in the database
    /// @param headers         all HTTP request headers, forwarded to the security validator
    /// @param rawBody         raw request body bytes for signature verification compatibility
    /// @return 202 Accepted when the event is accepted for processing
    @Operation(
            summary = "Receive inbound webhook event",
            description = "Generic endpoint. Security and mapping are driven by the connector configuration stored in DB."
    )
    @ApiResponse(responseCode = "202", description = "Event accepted for processing")
    @ApiResponse(responseCode = "401", description = "Invalid or missing credentials")
    @ApiResponse(responseCode = "404", description = "Unknown configurationId")
    @ApiResponse(responseCode = "400", description = "Malformed request body")
    @PostMapping(value = "/{configurationId}", consumes = "application/json")
    @ResponseStatus(ACCEPTED)
    public ResponseEntity<Void> receiveWebhookEvent(
            @Parameter(description = "Connector configuration identifier", required = true)
            @PathVariable String configurationId,
            @RequestHeader Map<String, String> headers,
            @RequestBody byte[] rawBody
    ) {
        handler.handle(configurationId, headers, rawBody);
        return ResponseEntity.accepted().build();
    }
}
