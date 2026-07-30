package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.FORBIDDEN_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.INTERNAL_SERVER_ERROR_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.NOT_FOUND_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.OK_CODE;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_INSUFFICIENT_RIGHTS;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_UNAUTHORIZED;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.RESPONSE_UNEXPECTED_SERVER_ERROR;
import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.UNAUTHORIZED_CODE;
import static org.springframework.http.HttpStatus.OK;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.decathlon.idp_core.domain.exception.principal.PrincipalNotFoundException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.principal.PrincipalInfo;
import com.decathlon.idp_core.domain.service.principal.PrincipalProvisioningService;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntityDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiExceptionHandler.ErrorResponse;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity.EntityDtoOutMapper;
import com.decathlon.idp_core.infrastructure.adapters.api.principal.PrincipalExtractor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/// REST API adapter providing principal self-service endpoints.
///
/// **Business purpose:** Enables authenticated actors (humans or service accounts)
/// to retrieve their own Principal entity from the catalog. Supports:
/// - Profile inspection (who am I?)
/// - Group membership visibility
/// - Service account metadata retrieval
///
/// **Design rationale:** Separates principal self-service from general entity
/// management. Uses dedicated `/principals/me` path to clearly signal intent.
@RestController
@RequestMapping("/api/v1/entities/principals")
@RequiredArgsConstructor
@Tag(name = "Principals", description = "Principal identity and profile management")
public class PrincipalController {

  private final PrincipalExtractor principalExtractor;
  private final PrincipalProvisioningService provisioningService;
  private final EntityDtoOutMapper entityDtoOutMapper;

  @GetMapping("/me")
  @ResponseStatus(OK)
  @Operation(summary = "Get current authenticated principal", description = """
      Returns the Principal entity of the currently authenticated actor (human or service account).
      The principal is automatically provisioned via JIT if this is the first authentication.

      **Response includes:**
      - Principal identifier (unique ID)
      - Display name
      - Principal kind (HUMAN or SERVICE_ACCOUNT)
      - Attributes (email, client_id, etc.)
      - Group memberships (via relations)

      **Use cases:**
      - Profile inspection: "Who am I?"
      - Authorization context retrieval
      - Service account validation
      """)
  @ApiResponse(responseCode = OK_CODE, description = "Principal entity retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EntityDtoOut.class)))
  @ApiResponse(responseCode = UNAUTHORIZED_CODE, description = RESPONSE_UNAUTHORIZED, content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(responseCode = FORBIDDEN_CODE, description = RESPONSE_INSUFFICIENT_RIGHTS, content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(responseCode = NOT_FOUND_CODE, description = "Principal not found in catalog (JIT provisioning may have failed)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(responseCode = INTERNAL_SERVER_ERROR_CODE, description = RESPONSE_UNEXPECTED_SERVER_ERROR, content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
  public EntityDtoOut getCurrentPrincipal(Authentication authentication) {
    PrincipalInfo principalInfo = principalExtractor.extractPrincipalInfo(authentication);

    Entity principal = provisioningService.getPrincipal(principalInfo.identifier())
        .orElseThrow(() -> new PrincipalNotFoundException(principalInfo.identifier()));

    return entityDtoOutMapper.fromEntity(principal);
  }
}
