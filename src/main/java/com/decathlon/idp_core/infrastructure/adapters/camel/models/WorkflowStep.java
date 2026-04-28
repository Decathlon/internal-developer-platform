package com.decathlon.idp_core.infrastructure.adapters.camel.models;


import lombok.Data;

@Data
public class WorkflowStep {
    private String id;
    private String type; // "transform", "enricher", "validator"
    private String engine; // "jq", "jslt"
    private String script; // The actual JQ or JSLT code
    private StepErrorConfig onFailure;
}

@Data
class StepErrorConfig {
    private String action; // "retry", "ignore", "fail"
    private int maxAttempts = 0;
    private long delay = 1000;
    private boolean continueOnFail = false;
}
