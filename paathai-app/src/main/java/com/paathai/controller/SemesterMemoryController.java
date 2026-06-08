package com.paathai.controller;

import com.paathai.common.entity.SemesterSummary;
import com.paathai.study.service.SemesterMemoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for semester memory retrieval.
 *
 * GET /api/courses/{courseId}/memory                       - Get all summaries
 * GET /api/courses/{courseId}/memory/units/{unitId}        - Get unit memory
 * GET /api/courses/{courseId}/memory/lectures/{lectureId}  - Get lecture summary
 */
@RestController
@RequestMapping("/api/courses/{courseId}/memory")
public class SemesterMemoryController {

    private final SemesterMemoryService memoryService;

    public SemesterMemoryController(SemesterMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @GetMapping
    public ResponseEntity<?> getMemory(@PathVariable Long courseId) {
        return ResponseEntity.ok(memoryService.getMemory(courseId));
    }

    @GetMapping("/units/{unitId}")
    public ResponseEntity<?> getUnitMemory(@PathVariable Long courseId, @PathVariable Long unitId) {
        return ResponseEntity.ok(memoryService.getMemoryForUnit(unitId));
    }

    @GetMapping("/lectures/{lectureId}")
    public ResponseEntity<?> getLectureSummary(@PathVariable Long courseId, @PathVariable Long lectureId) {
        SemesterSummary summary = memoryService.getLectureSummary(lectureId);
        if (summary == null) {
            return ResponseEntity.ok(Map.of("message", "No summary available yet"));
        }
        return ResponseEntity.ok(summary);
    }
}
