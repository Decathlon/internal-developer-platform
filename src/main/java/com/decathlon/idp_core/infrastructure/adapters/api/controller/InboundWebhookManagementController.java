package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.*;
import static org.springframework.http.HttpStatus.*;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.decathlon.idp_core.domain.model.inbound_connectors.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.service.webhook.WebhookConnectorService;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerConfiguration;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.InboundWebhookCreateDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.webhook.InboundWebhookDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiExceptionHandler;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.connector.webhook.InboundWebhookMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/// REST controller exposing inbound webhook configuration management endpoints.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/inbound-webhooks")
@Tag(name = "Inbound Webhook Management", description = "Operations for managing inbound webhook connector configurations")
public class InboundWebhookManagementController {

  private final WebhookConnectorService webhookConnectorService;
  private final InboundWebhookMapper inboundWebhookMapper;

  /// Creates a new inbound webhook connector configuration.
  ///
  /// @param request creation payload
  /// @return created connector response
  @Operation(summary = ENDPOINT_POST_WEBHOOK_CONNECTOR_SUMMARY, description = ENDPOINT_POST_WEBHOOK_CONNECTOR_DESCRIPTION)
  @ApiResponse(responseCode = "201", description = "Webhook connector created")
  @ApiResponse(responseCode = "400", description = "Invalid request payload")
  @ApiResponse(responseCode = "409", description = "Identifier already exists")
  @PostMapping
  @ResponseStatus(CREATED)
  public InboundWebhookDtoOut createInboundWebhook(
      @Valid @RequestBody InboundWebhookCreateDtoIn request) {
    WebhookConnector webhookConnector = webhookConnectorService
        .createWebhookConnector(inboundWebhookMapper.toDomain(request,
            webhookConnectorService.resolveAndValidateMappings(request.mappingIdentifiers())));
    return inboundWebhookMapper.fromWebhookConnectorToDto(webhookConnector);
  }

  @Operation(summary = ENDPOINT_GET_WEBHOOK_CONNECTOR_PAGINATED_SUMMARY, description = ENDPOINT_GET_WEBHOOK_CONNECTOR_PAGINATED_DESCRIPTION)
  @ApiResponse(responseCode = OK_CODE, description = RESPONSE_WEBHOOK_CONNECTOR_PAGINATED_SUCCESS, content = @Content(schema = @Schema(implementation = SwaggerConfiguration.WebhookConnectorPageResponse.class)))
  @ApiResponse(responseCode = BAD_REQUEST_CODE, description = RESPONSE_INVALID_PAGINATION, content = {
      @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class))})
  @Parameter(name = "page", description = PARAM_PAGE_DESCRIPTION, in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "integer", defaultValue = "0")))
  @Parameter(name = "size", description = PARAM_SIZE_DESCRIPTION, in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "integer", defaultValue = "20")))
  @Parameter(name = "sort", description = PARAM_SORT_DESCRIPTION, in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "string", defaultValue = "identifier,asc")))
  @GetMapping
  @ResponseStatus(OK)
  public Page<InboundWebhookDtoOut> getWebhooksPaginated(
      @PageableDefault(size = 20, sort = "identifier") @Parameter(hidden = true) Pageable pageable) {
    return webhookConnectorService.getAllWebhookConnector(pageable)
        .map(inboundWebhookMapper::fromWebhookConnectorToDto);
  }

  @Operation(summary = ENDPOINT_DELETE_WEBHOOK_CONNECTOR_SUMMARY, description = ENDPOINT_DELETE_WEBHOOK_CONNECTOR_DESCRIPTION)
  @ApiResponse(responseCode = NO_CONTENT_CODE, description = RESPONSE_WEBHOOK_CONNECTOR_DELETED)
  @ApiResponse(responseCode = NOT_FOUND_CODE, description = RESPONSE_WEBHOOK_CONNECTOR_NOT_FOUND_IDENTIFIER, content = {
      @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class))})
  @ResponseStatus(NO_CONTENT)
  @DeleteMapping("/{identifier}")
  public void deleteWebhookConnector(@PathVariable String identifier) {
    webhookConnectorService.deleteWebhookConnector(identifier);
  }

  @Operation(summary = ENDPOINT_GET_WEBHOOK_CONNECTOR_BY_IDENTIFIER_SUMMARY, description = ENDPOINT_GET_WEBHOOK_CONNECTOR_BY_IDENTIFIER_DESCRIPTION)
  @ApiResponse(responseCode = OK_CODE, description = RESPONSE_WEBHOOK_CONNECTOR_FOUND, content = {
      @Content(schema = @Schema(implementation = InboundWebhookDtoOut.class))})
  @ApiResponse(responseCode = NOT_FOUND_CODE, description = RESPONSE_WEBHOOK_CONNECTOR_NOT_FOUND_IDENTIFIER, content = {
      @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class))})
  @GetMapping("/{identifier}")
  @ResponseStatus(OK)
  public InboundWebhookDtoOut getWebhookConnectorByIdentifier(@PathVariable String identifier) {
    WebhookConnector webhookConnector = webhookConnectorService.getWebhookConnector(identifier);
    return inboundWebhookMapper.fromWebhookConnectorToDto(webhookConnector);
  }

  @Operation(summary = ENDPOINT_PUT_WEBHOOK_CONNECTOR_SUMMARY, description = ENDPOINT_PUT_WEBHOOK_CONNECTOR_DESCRIPTION)
  @ApiResponse(responseCode = OK_CODE, description = RESPONSE_WEBHOOK_CONNECTOR_UPDATED, content = {
      @Content(schema = @Schema(implementation = InboundWebhookDtoOut.class))})
  @ApiResponse(responseCode = NOT_FOUND_CODE, description = RESPONSE_WEBHOOK_CONNECTOR_NOT_FOUND_IDENTIFIER, content = {
      @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class))})
  @ApiResponse(responseCode = BAD_REQUEST_CODE, description = "Invalid request payload", content = {
      @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class))})
  @ApiResponse(responseCode = CONFLICT_CODE, description = "Webhook connector title already exists", content = {
      @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class))})
  @PutMapping("/{identifier}")
  @ResponseStatus(OK)
  public InboundWebhookDtoOut putWebhookConnector(@PathVariable String identifier,
      @Valid @RequestBody InboundWebhookCreateDtoIn request) {
    var resolvedMappings = webhookConnectorService
        .resolveAndValidateMappings(request.mappingIdentifiers());
    return inboundWebhookMapper
        .fromWebhookConnectorToDto(webhookConnectorService.updateWebhookConnector(identifier,
            inboundWebhookMapper.toDomainForUpdate(identifier, request, resolvedMappings)));
  }
}
