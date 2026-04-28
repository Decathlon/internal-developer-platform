package com.decathlon.idp_core.infrastructure.adapters.camel.config;

import org.apache.camel.component.platform.http.PlatformHttpComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.decathlon.idp_core.domain.service.EntityService;

@Configuration
public class CamelConfig {



    @Bean(name = "platform-http")
    public PlatformHttpComponent platformHttpComponent() {
        PlatformHttpComponent component = new PlatformHttpComponent();
        // You can explicitly set the engine here if needed
        return component;
    }

    @Bean(name = "entity-service")
    public EntityService entityService(@Autowired EntityService entityService) {
        return entityService;
    }



}
