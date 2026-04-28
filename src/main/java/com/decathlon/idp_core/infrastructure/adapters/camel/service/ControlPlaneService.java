/**
 * Service responsible for managing workflow routes in the Camel context.
 * <p>
 * Provides functionality to dynamically create workflow routes based on incoming requests,
 * and to retrieve a list of currently active workflow routes.
 * </p>
 *
 * <p>
 * Workflows are identified by a unique UUID and are instantiated using a templated route builder.
 * The source URI for each workflow is determined by the trigger type specified in the request,
 * supporting both "webhook" and "pubsub" triggers.
 * </p>
 *
 * <p>
 * Example usage:
 * <pre>
 *     String workflowId = controlPlaneService.createWorkflow(request);
 *     List&lt;String&gt; activeWorkflows = controlPlaneService.getActiveWorkflows();
 * </pre>
 * </p>
 *
 * @author
 */
package com.decathlon.idp_core.infrastructure.adapters.camel.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.builder.TemplatedRouteBuilder;
import org.springframework.stereotype.Service;

import com.decathlon.idp_core.infrastructure.adapters.camel.models.WorkflowRequest;

@Service
public class ControlPlaneService {

    private final CamelContext camelContext;
    
    public ControlPlaneService(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    public String createWorkflow(WorkflowRequest request) {
        String uuid = UUID.randomUUID().toString();
        String sourceUri = "";

        // 1. Determine the Source URI (The Ingress)
        if ("webhook".equals(request.getTrigger().getType())) {
            sourceUri = "platform-http:" + request.getTrigger().getSettings().get("endpoint") + "?httpMethodRestrict=POST";
        } else if ("pubsub".equals(request.getTrigger().getType())) {
            sourceUri = "google-pubsub:" + request.getTrigger().getSettings().get("projectId")
                    + ":" + request.getTrigger().getSettings().get("topic");
        }

        Map<String, Object> securitySettings = request.getSecurity().getSettings();

        // 2. Instantiate the Template
        TemplatedRouteBuilder.builder(camelContext, "etlTemplate")
                .routeId("wf-" + uuid)
                .parameter("workflowId", uuid)
                .parameter("sourceUri", sourceUri)
                .parameter("securityType", request.getSecurity().getType())
                .parameter("securitySettings", securitySettings)
                .add();

        return uuid; // Return the ID so the user can track the pipeline
    }

    public List<String> getActiveWorkflows() {
        return camelContext.getRoutes().stream()
                .map(Route::getId)
                .filter(id -> id.startsWith("wf-"))
                .toList();
    }
}
