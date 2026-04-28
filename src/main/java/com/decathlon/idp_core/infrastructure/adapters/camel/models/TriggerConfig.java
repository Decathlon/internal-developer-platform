package com.decathlon.idp_core.infrastructure.adapters.camel.models;


import java.util.Map;

import lombok.Data;

@Data
public class TriggerConfig {
    private String type; // "webhook", "pubsub", "cron"
    private Map<String, Object> settings;
    private SecurityConfig security;
}
