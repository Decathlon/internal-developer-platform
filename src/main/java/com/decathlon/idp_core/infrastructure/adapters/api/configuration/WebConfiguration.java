package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/// Spring MVC web configuration for enhanced developer experience.
///
/// **User experience rationale:** Automatically redirects root URL to Swagger UI,
/// providing immediate API documentation access for developers and API consumers.
/// Eliminates the need to remember specific Swagger UI paths.
///
/// **Developer workflow optimization:** Enables quick API exploration and testing
/// by making documentation the default landing page for the application.

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "swagger-ui/index.html");
    }

}
