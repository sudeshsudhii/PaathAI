package com.paathai.controller;

import com.paathai.syllabus.service.KnowledgeGraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for knowledge graph operations.
 *
 * GET    /api/courses/{courseId}/graph       - Get graph nodes + edges
 * POST   /api/courses/{courseId}/graph/build - Trigger graph build from syllabus
 * POST   /api/graph/edges                   - Add manual edge
 * DELETE /api/graph/edges/{id}              - Remove edge
 */
@RestController
public class KnowledgeGraphController {

    private final KnowledgeGraphService graphService;

    public KnowledgeGraphController(KnowledgeGraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/api/courses/{courseId}/graph")
    public ResponseEntity<?> getGraph(@PathVariable Long courseId) {
        return ResponseEntity.ok(graphService.getGraph(courseId));
    }

    @PostMapping("/api/courses/{courseId}/graph/build")
    public ResponseEntity<?> buildGraph(@PathVariable Long courseId) {
        graphService.buildFromSyllabus(courseId);
        return ResponseEntity.ok(Map.of("message", "Knowledge graph built successfully"));
    }

    @PostMapping("/api/graph/edges")
    public ResponseEntity<?> addEdge(@RequestBody Map<String, Object> body) {
        Long courseId = ((Number) body.get("courseId")).longValue();
        Long sourceNodeId = ((Number) body.get("sourceNodeId")).longValue();
        Long targetNodeId = ((Number) body.get("targetNodeId")).longValue();
        String edgeType = (String) body.get("edgeType");

        var edge = graphService.addEdge(courseId, sourceNodeId, targetNodeId, edgeType);
        return ResponseEntity.ok(edge);
    }

    @DeleteMapping("/api/graph/edges/{id}")
    public ResponseEntity<?> deleteEdge(@PathVariable Long id) {
        graphService.deleteEdge(id);
        return ResponseEntity.ok(Map.of("message", "Edge deleted"));
    }
}
