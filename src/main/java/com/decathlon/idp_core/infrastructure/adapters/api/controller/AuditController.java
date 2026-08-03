package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.BAD_REQUEST_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_GET_ENTITY_AUDIT_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.ENDPOINT_GET_ENTITY_AUDIT_SUMMARY;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FORBIDDEN_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.INTERNAL_SERVER_ERROR_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.NOT_FOUND_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.OK_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_ENTITY_AUDIT_SUCCESS;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_ENTITY_NOT_FOUND_IDENTIFIER;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_INSUFFICIENT_RIGHTS;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_TEMPLATE_NOT_FOUND_IDENTIFIER;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_UNAUTHORIZED;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_UNEXPECTED_SERVER_ERROR;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.UNAUTHORIZED_CODE;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.decathlon.idp_core.domain.model.entity.EntityAuditInfo;
import com.decathlon.idp_core.domain.service.entity.EntityAuditService;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.audit.EntityAuditDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiExceptionHandler.ErrorResponse;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity.EntityAuditDtoOutMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/// REST API adapter providing audit endpoints.
///
/// **Infrastructure specifics:**
/// - Exposes HTTP endpoints for retrieving audit history of any objects
/// - Handles REST API request/response mapping between DTOs and domain models
/// - Integrates with OpenAPI/Swagger for API documentation
/// - Maps domain exceptions to appropriate HTTP status codes
///
/// **Separation of concerns:** This controller is dedicated solely to audit operations,
/// keeping the other controller focused on CRUD operations. This follows the Single
/// Responsibility Principle.
@RestController
@RequestMapping("/api/v1/audit/")
@Tag(name = "Audit", description = "Operations related to audit history")
@Validated
@RequiredArgsConstructor
public class AuditController {

  private final EntityAuditService entityAuditService;
  private final EntityAuditDtoOutMapper entityAuditDtoOutMapper;

  /// Retrieves the complete audit history for a specific entity.
  ///
  /// **API contract:** Returns a list of all revisions for the entity, ordered by
  /// revision number (newest first). Each revision includes the timestamp, type
  /// of
  /// operation (CREATED, UPDATED, DELETED), and the user who performed the
  /// change.
  ///
  /// @param templateIdentifier the template identifier of the entity
  /// @param entityIdentifier the unique identifier of the entity
  /// @return list of audit information DTOs for HTTP response
  @Operation(summary = ENDPOINT_GET_ENTITY_AUDIT_SUMMARY, description = ENDPOINT_GET_ENTITY_AUDIT_DESCRIPTION)
  @ApiResponse(responseCode = OK_CODE, description = RESPONSE_ENTITY_AUDIT_SUCCESS, content = {
      @Content(array = @ArraySchema(schema = @Schema(implementation = EntityAuditDtoOut.class)))})
  @ApiResponse(responseCode = BAD_REQUEST_CODE, description = "Invalid template or entity identifier", content = {
      @Content(schema = @Schema(implementation = ErrorResponse.class))})
  @ApiResponse(responseCode = UNAUTHORIZED_CODE, description = RESPONSE_UNAUTHORIZED, content = @Content)
  @ApiResponse(responseCode = FORBIDDEN_CODE, description = RESPONSE_INSUFFICIENT_RIGHTS, content = @Content)
  @ApiResponse(responseCode = NOT_FOUND_CODE, description = RESPONSE_TEMPLATE_NOT_FOUND_IDENTIFIER
      + " or " + RESPONSE_ENTITY_NOT_FOUND_IDENTIFIER, content = {
          @Content(schema = @Schema(implementation = ErrorResponse.class))})
  @ApiResponse(responseCode = INTERNAL_SERVER_ERROR_CODE, description = RESPONSE_UNEXPECTED_SERVER_ERROR, content = {
      @Content(schema = @Schema(implementation = ErrorResponse.class))})
  @GetMapping("entities/{templateIdentifier}/{entityIdentifier}")
  @ResponseStatus(OK)
  public List<EntityAuditDtoOut> getEntityAuditHistory(
      @NotBlank @PathVariable String templateIdentifier,
      @NotBlank @PathVariable String entityIdentifier) {

    List<EntityAuditInfo> auditHistory = entityAuditService
        .getEntityAuditHistory(templateIdentifier, entityIdentifier);
    return entityAuditDtoOutMapper.fromEntityAuditInfoList(auditHistory);
  }

}
