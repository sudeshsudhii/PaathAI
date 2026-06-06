package com.paathai.controller;

import com.paathai.common.dto.SyllabusTreeResponse;
import com.paathai.common.dto.TopicCreateRequest;
import com.paathai.common.entity.Subject;
import com.paathai.common.entity.SyllabusTopic;
import com.paathai.common.entity.Unit;
import com.paathai.syllabus.service.SyllabusManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for syllabus management.
 *
 * GET    /api/courses/{courseId}/syllabus              - Get full syllabus tree
 * POST   /api/courses/{courseId}/syllabus/manual       - Create manual syllabus
 * POST   /api/syllabus/{syllabusId}/subjects           - Add subject
 * POST   /api/syllabus/subjects/{subjectId}/units      - Add unit
 * POST   /api/syllabus/units/{unitId}/topics           - Add topic
 * PUT    /api/syllabus/topics/{topicId}                - Update topic
 * DELETE /api/syllabus/topics/{topicId}                - Delete topic
 * PUT    /api/syllabus/units/{unitId}/topics/reorder   - Reorder topics
 * PUT    /api/syllabus/subjects/{subjectId}/units/reorder - Reorder units
 */
@RestController
public class SyllabusController {

    private static final Logger log = LoggerFactory.getLogger(SyllabusController.class);

    private final SyllabusManagementService syllabusManagementService;

    public SyllabusController(SyllabusManagementService syllabusManagementService) {
        this.syllabusManagementService = syllabusManagementService;
    }

    @GetMapping("/api/courses/{courseId}/syllabus")
    public ResponseEntity<SyllabusTreeResponse> getSyllabusTree(@PathVariable Long courseId) {
        return ResponseEntity.ok(syllabusManagementService.getTree(courseId));
    }

    @PostMapping("/api/courses/{courseId}/syllabus/manual")
    public ResponseEntity<?> createManualSyllabus(@PathVariable Long courseId,
                                                    @RequestBody Map<String, String> body) {
        var syllabus = syllabusManagementService.createManualSyllabus(courseId,
                body.getOrDefault("title", "Untitled Syllabus"));
        return ResponseEntity.ok(Map.of(
                "syllabusId", syllabus.getId(),
                "message", "Manual syllabus created"
        ));
    }

    @PostMapping("/api/syllabus/{syllabusId}/subjects")
    public ResponseEntity<Subject> addSubject(@PathVariable Long syllabusId,
                                               @RequestBody Map<String, String> body) {
        Subject subject = syllabusManagementService.addSubject(syllabusId, body.get("name"));
        return ResponseEntity.ok(subject);
    }

    @PostMapping("/api/syllabus/subjects/{subjectId}/units")
    public ResponseEntity<Unit> addUnit(@PathVariable Long subjectId,
                                         @RequestBody Map<String, String> body) {
        Unit unit = syllabusManagementService.addUnit(subjectId, body.get("name"));
        return ResponseEntity.ok(unit);
    }

    @PostMapping("/api/syllabus/units/{unitId}/topics")
    public ResponseEntity<SyllabusTopic> addTopic(@PathVariable Long unitId,
                                                    @RequestBody TopicCreateRequest request) {
        return ResponseEntity.ok(syllabusManagementService.addTopic(unitId, request));
    }

    @PutMapping("/api/syllabus/topics/{topicId}")
    public ResponseEntity<SyllabusTopic> updateTopic(@PathVariable Long topicId,
                                                      @RequestBody TopicCreateRequest request) {
        return ResponseEntity.ok(syllabusManagementService.updateTopic(topicId, request));
    }

    @DeleteMapping("/api/syllabus/topics/{topicId}")
    public ResponseEntity<?> deleteTopic(@PathVariable Long topicId) {
        syllabusManagementService.deleteTopic(topicId);
        return ResponseEntity.ok(Map.of("message", "Topic deleted"));
    }

    @PutMapping("/api/syllabus/units/{unitId}/topics/reorder")
    public ResponseEntity<?> reorderTopics(@PathVariable Long unitId,
                                            @RequestBody List<Long> topicIds) {
        syllabusManagementService.reorderTopics(unitId, topicIds);
        return ResponseEntity.ok(Map.of("message", "Topics reordered"));
    }

    @PutMapping("/api/syllabus/subjects/{subjectId}/units/reorder")
    public ResponseEntity<?> reorderUnits(@PathVariable Long subjectId,
                                           @RequestBody List<Long> unitIds) {
        syllabusManagementService.reorderUnits(subjectId, unitIds);
        return ResponseEntity.ok(Map.of("message", "Units reordered"));
    }
}
