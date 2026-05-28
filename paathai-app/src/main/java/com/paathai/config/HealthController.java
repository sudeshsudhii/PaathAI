package com.paathai.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Health check and API info endpoint.
 */
@RestController
public class HealthController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
            "application", "PaathAI",
            "description", "AI-powered syllabus-aware learning intelligence platform",
            "status", "UP",
            "timestamp", Instant.now().toString(),
            "version", "0.1.0-SNAPSHOT",
            "endpoints", Map.of(
                "health", "/api/health",
                "info", "/"
            )
        ));
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", Instant.now().toString()
        ));
    }
}
