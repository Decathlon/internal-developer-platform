package com.decathlon.idp_core.infrastructure.adapters.camel.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.decathlon.idp_core.infrastructure.adapters.camel.models.WorkflowRequest;
import com.decathlon.idp_core.infrastructure.adapters.camel.service.ControlPlaneService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/workflows")
@Slf4j
public class WorkflowController {

    @Autowired
    private ControlPlaneService controlPlane;

    @PostMapping
    public ResponseEntity<Map<String, String>> create(@RequestBody WorkflowRequest request) {
        try {
            String id = controlPlane.createWorkflow(request);
            return ResponseEntity.ok(Map.of("id", id, "status", "CREATED"));
        } catch (Exception e) {
            log.error("Failed to create workflow", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<String>> getActiveWorkflows() {
        List<String> activeWorkflows = controlPlane.getActiveWorkflows();
        return ResponseEntity.ok(activeWorkflows);
    }
}
