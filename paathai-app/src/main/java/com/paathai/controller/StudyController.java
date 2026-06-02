package com.paathai.controller;

import com.paathai.rag.pipeline.RagPipelineService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Study controller for semantic search.
 * POST /api/study/search - Semantic search across lecture content using RAG
 */
@RestController
@RequestMapping("/api/study")
public class StudyController {

    private static final Logger log = LoggerFactory.getLogger(StudyController.class);

    private final RagPipelineService ragPipelineService;

    public StudyController(RagPipelineService ragPipelineService) {
        this.ragPipelineService = ragPipelineService;
    }

    @PostMapping("/search")
    public ResponseEntity<?> search(Authentication auth, @RequestBody SearchRequest request) {
        Long userId = (Long) auth.getPrincipal();
        log.info("Search request from user {}: '{}'", userId, request.getQuery());

        RagPipelineService.SearchResponse response =
                ragPipelineService.search(request.getQuery(), userId);

        return ResponseEntity.ok(response);
    }

    @Data
    public static class SearchRequest {
        private String query;
    }
}
