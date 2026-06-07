package com.paathai.controller;

import com.paathai.common.dto.CourseCoverageResponse;
import com.paathai.syllabus.service.CourseCoverageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for course coverage.
 *
 * GET /api/courses/{courseId}/coverage         - Full hierarchical coverage
 * GET /api/courses/{courseId}/coverage/summary - Aggregate % only
 */
@RestController
@RequestMapping("/api/courses/{courseId}/coverage")
public class CoverageController {

    private final CourseCoverageService coverageService;

    public CoverageController(CourseCoverageService coverageService) {
        this.coverageService = coverageService;
    }

    @GetMapping
    public ResponseEntity<CourseCoverageResponse> getCoverage(@PathVariable Long courseId) {
        return ResponseEntity.ok(coverageService.getCoverage(courseId));
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getCoverageSummary(@PathVariable Long courseId) {
        CourseCoverageResponse coverage = coverageService.getCoverage(courseId);
        return ResponseEntity.ok(Map.of(
                "courseId", courseId,
                "overallPercent", coverage.getOverallPercent(),
                "totalTopics", coverage.getTotalTopics(),
                "coveredTopics", coverage.getCoveredTopics()
        ));
    }
}
