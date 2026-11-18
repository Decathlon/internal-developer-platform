package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
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
@EnableConfigurationProperties(SearchProperties.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebConfiguration implements WebMvcConfigurer {

  private final int maxPageSize;

  public WebConfiguration(SearchProperties searchProperties) {
    this.maxPageSize = searchProperties.maxPageSize();
  }

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addRedirectViewController("/", "swagger-ui/index.html");
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(new ValidatingPageableArgumentResolver(maxPageSize));
  }

}
