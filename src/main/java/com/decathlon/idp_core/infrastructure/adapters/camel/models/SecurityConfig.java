package com.decathlon.idp_core.infrastructure.adapters.camel.models;

import java.util.Map;

import lombok.Data;

@Data
public class SecurityConfig {
    private String type; // "hmac_sha256", "api_key", "iam"
    private Map<String, Object> settings; // Reference to a Vault/Secret key
}
