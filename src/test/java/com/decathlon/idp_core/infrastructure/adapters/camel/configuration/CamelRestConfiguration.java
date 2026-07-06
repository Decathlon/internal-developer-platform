package com.decathlon.idp_core.infrastructure.adapters.camel.configuration;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamelRestConfiguration extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        restConfiguration()
            .component("servlet")
            .bindingMode(RestBindingMode.json)
            // Sets the context path for Camel APIs (e.g., http://localhost:8080/camel/...)
            .contextPath("/camel") 
            .apiContextPath("/api-doc")
            .apiProperty("api.title", "IDP Ingestion Webhooks API")
            .apiProperty("api.version", "1.0.0")
            // Match Spring Boot 4 / Jakarta EE serialization patterns
            .dataFormatProperty("prettyPrint", "true");
    }
}