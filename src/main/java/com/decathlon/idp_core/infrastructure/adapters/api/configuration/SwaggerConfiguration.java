package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import static io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP;
import static io.swagger.v3.oas.models.security.SecurityScheme.Type.OAUTH2;

import java.util.List;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity.EntityDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_template.EntityTemplateDtoOut;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/// OpenAPI/Swagger configuration for comprehensive API documentation and testing.
///
/// **Documentation strategy:**
/// - Auto-generates interactive API documentation from controller annotations
/// - Provides OAuth2 and Bearer token authentication for API testing
/// - Creates type-safe response models for paginated endpoints
///
/// **Security integration:**
/// - OAuth2 client credentials flow for machine-to-machine authentication
/// - JWT Bearer token support for user-based authentication
/// - Configurable token endpoints for different deployment environments
///
/// **API testing benefits:**
/// - Interactive Swagger UI for manual API testing and exploration
/// - Automatic request/response validation against OpenAPI schema
/// - Authentication testing without external tools

@Configuration
@Profile("!test")
public class SwaggerConfiguration {

    public static final String AUTHENTICATION_SUFFIX = " authentication";
    public static final String CLIENT_ID = "clientId";
    public static final String BEARER = "bearer";

    @Value("${spring.security.oauth2.client.provider.idp-core.token-uri}")
    private String oauth2url;

    @Value("${app.idp-core-prefix-url}")
    private String idpCorePrefixUrl;

    @Bean
    public OpenAPI openAPI() {
        ModelConverters.getInstance().addConverter(new ModelResolver(Json.mapper()));
        return new OpenAPI()
                .info(new Info()
                        .title("Idp core API")
                        .description("API dedicated to idp core functionalities")
                        .version("v1"))
                .addServersItem(new Server().url(idpCorePrefixUrl))
                .schemaRequirement(CLIENT_ID,
                        new SecurityScheme().description(CLIENT_ID + AUTHENTICATION_SUFFIX)
                                .name(CLIENT_ID)
                                .type(OAUTH2)
                                .flows(new OAuthFlows().clientCredentials(
                                        new OAuthFlow().tokenUrl(oauth2url))))
                .addSecurityItem(new SecurityRequirement().addList(CLIENT_ID))
                .schemaRequirement(BEARER,
                        new SecurityScheme().description(BEARER + AUTHENTICATION_SUFFIX)
                                .name(BEARER)
                                .scheme(BEARER)
                                .bearerFormat("JWT")
                                .type(HTTP))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    @Bean
    public GroupedOpenApi allApis() {
        return GroupedOpenApi.builder().group("internal").pathsToMatch("/**").build();
    }

    @Schema(description = "Paginated response containing Template objects")
    public static class TemplatePageResponse extends PageImpl<EntityTemplateDtoOut> {
        public TemplatePageResponse(List<EntityTemplateDtoOut> content, Pageable pageable, long total) {
            super(content, pageable, total);
        }
    }

    @Schema(description = "Paginated response containing Entity objects")
    public static class EntityPageResponse extends PageImpl<EntityDtoOut> {
        public EntityPageResponse(List<EntityDtoOut> content, Pageable pageable, long total) {
            super(content, pageable, total);
        }
    }


}
