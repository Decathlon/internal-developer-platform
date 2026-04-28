package com.decathlon.idp_core.infrastructure.adapters.camel.service;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.model.entity.Entity;

@Component
public class WorkflowRouteBuilder extends RouteBuilder {

    @Override
    public void configure() {

        onException(Exception.class)
                .handled(true) // Mark as handled so it doesn't propagate back to the caller as a 500 error
                .log(LoggingLevel.ERROR, "CENTRAL ERROR: ${exception.message}")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setBody(simple("{\"error\": \"Process failed\", \"detail\": \"${exception.message}\"}"));

        routeTemplate("etlTemplate")
                .templateParameter("workflowId")
                .templateParameter("sourceUri")
                .templateParameter("mappingQuery")
                .templateParameter("mappingEngine") // "jq", "java", "groovy"
                .templateParameter("securityType") // "apiKey", "github", "jwt"
                .templateParameter("securitySettings") // JSON/Map of settings

                // Ingestion step - this is where the data enters the system, and we can apply
                // initial transformations or validations
                .from("{{sourceUri}}")
                .routeId("{{workflowId}}")
                .log("Starting workflow {{workflowId}}")

                // Security Step
                // .setProperty("securityType", simple("{{securityType}}"))
                // .setProperty("securitySettings", method("{{securitySettings}}"))
                // .process("genericSecurityProcessor")

                // Transform step - this is where the main data transformation happens using JQ.
                // The script is dynamic and can be defined per workflow.
                .convertBodyTo(String.class)
                .transform().jq("{{mappingQuery}}")
                .unmarshal().json(Entity.class)
                // Save step - this is where the processed data is saved to the database or
                // another system
                .to("bean:entityService?method=createEntity")
                // Response step - this is where we can set the response back to the caller,
                // confirming that the process was successful
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                .setBody(constant("{\"status\": \"success\"}")) // Replace the POJO with a simple String
                .setHeader("Content-Type", constant("application/json"));

    }
}

/*
 * Adding security step
 *
 * routeTemplate("etlTemplate")
 * .templateParameter("securityType") // "apiKey", "github", "jwt"
 * .templateParameter("securitySettings") // JSON/Map of settings
 * .from("{{sourceUri}}")
 * // 1. Store security info in exchange properties for the processor
 * .setProperty("securityType", simple("{{securityType}}"))
 * .setProperty("securitySettings", simple("{{securitySettings}}"))
 *
 * // 2. Call the generic processor
 * .process("genericSecurityProcessor")
 *
 * // 3. Proceed to JQ and Bean
 * .transform().jq("{{jqScript}}")
 * .to("bean:{{beanName}}");
 *
 *
 *
 */

/*
 *
 * Ingestion + Async Processing
 *
 * Part A: The Ingestor (Fast)
 *
 * routeTemplate("asyncIngestorTemplate")
 * .templateParameter("workflowId")
 * .templateParameter("sourceUri")
 * .from("{{sourceUri}}")
 * .routeId("ingest-{{workflowId}}")
 * .process("genericSecurityProcessor") // Always validate first!
 * // Store raw message in DB for persistence
 * .to("bean:stagingService?method=saveRaw")
 * // Hand off to the worker asynchronously
 * .to("seda:worker-{{workflowId}}?waitForTaskToComplete=Never")
 * // Respond to caller immediately
 * .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(202))
 * .setBody(constant("{\"status\": \"accepted\"}"));
 *
 * Part B: The Worker (Async Processing)
 * Java
 * routeTemplate("asyncWorkerTemplate")
 * .templateParameter("workflowId")
 * .templateParameter("jqScript")
 * .templateParameter("beanName")
 * .from("seda:worker-{{workflowId}}?concurrentConsumers=5")
 * .routeId("worker-{{workflowId}}")
 * .doTry()
 * .transform().jq("{{jqScript}}")
 * .to("bean:{{beanName}}")
 * // Update staging status to PROCESSED
 * .to("bean:stagingService?method=markAsProcessed")
 * .doCatch(Exception.class)
 * // Update staging status to FAILED for retry/audit
 * .to("bean:stagingService?method=markAsFailed")
 * .end();
 *
 *
 *
 */
