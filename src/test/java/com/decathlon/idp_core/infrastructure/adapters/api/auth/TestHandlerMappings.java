package com.decathlon.idp_core.infrastructure.adapters.api.auth;

import static org.mockito.Mockito.mock;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.decathlon.idp_core.infrastructure.adapters.api.controller.AuditController;
import com.decathlon.idp_core.infrastructure.adapters.api.controller.EntityController;
import com.decathlon.idp_core.infrastructure.adapters.api.controller.EntityDynamicMappingController;
import com.decathlon.idp_core.infrastructure.adapters.api.controller.EntityTemplateController;
import com.decathlon.idp_core.infrastructure.adapters.api.controller.InboundWebhookConfigurationController;

/// Builds a real MVC handler mapping over mocked controllers to test route-based authorization.
final class TestHandlerMappings {

  private TestHandlerMappings() {
  }

  static RequestMappingHandlerMapping controllers() {
    var context = new GenericApplicationContext();
    context.registerBean("entityController", EntityController.class,
        () -> mock(EntityController.class));
    context.registerBean("auditController", AuditController.class,
        () -> mock(AuditController.class));
    context.registerBean("entityTemplateController", EntityTemplateController.class,
        () -> mock(EntityTemplateController.class));
    context.registerBean("entityDynamicMappingController", EntityDynamicMappingController.class,
        () -> mock(EntityDynamicMappingController.class));
    context.registerBean("inboundWebhookConfigurationController",
        InboundWebhookConfigurationController.class,
        () -> mock(InboundWebhookConfigurationController.class));
    context.refresh();

    var mapping = new RequestMappingHandlerMapping();
    mapping.setApplicationContext(context);
    mapping.afterPropertiesSet();
    return mapping;
  }
}
