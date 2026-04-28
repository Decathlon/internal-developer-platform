package com.decathlon.idp_core.infrastructure.adapters.camel.models;


import lombok.Data;

@Data
public class SinkConfig {
    private String type; // "bean", "pubsub", "postgres"
    private String target; // e.g., bean name or topic name
}
