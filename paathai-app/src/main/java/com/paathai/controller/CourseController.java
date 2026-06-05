package com.paathai.controller;

import com.paathai.common.dto.CourseCreateRequest;
import com.paathai.common.dto.CourseResponse;
import com.paathai.common.repository.LectureRepository;
import com.paathai.lecture.service.CourseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Course CRUD operations.
 *
 * POST   /api/courses           - Create course
 * GET    /api/courses           - List user's courses
 * GET    /api/courses/{id}      - Get course details
 * PUT    /api/courses/{id}      - Update course
 * DELETE /api/courses/{id}      - Archive course (soft delete)
 * GET    /api/courses/{id}/lectures - List lectures for course
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private static final Logger log = LoggerFactory.getLogger(CourseController.class);

    private final CourseService courseService;
    private final LectureRepository lectureRepository;

    public CourseController(CourseService courseService, LectureRepository lectureRepository) {
        this.courseService = courseService;
        this.lectureRepository = lectureRepository;
    }

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(Authentication auth,
                                                        @RequestBody CourseCreateRequest request) {
        Long userId = (Long) auth.getPrincipal();
        CourseResponse response = courseService.createCourse(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> listCourses(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(courseService.listCourses(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourse(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourse(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> updateCourse(Authentication auth,
                                                        @PathVariable Long id,
                                                        @RequestBody CourseCreateRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(courseService.updateCourse(id, userId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> archiveCourse(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        courseService.archiveCourse(id, userId);
        return ResponseEntity.ok(Map.of("message", "Course archived successfully"));
    }

    @GetMapping("/{id}/lectures")
    public ResponseEntity<?> getCourseLectures(@PathVariable Long id) {
        var lectures = lectureRepository.findByCourseId(id).stream()
                .map(l -> Map.of(
                        "id", l.getId(),
                        "title", l.getTitle() != null ? l.getTitle() : "Untitled",
                        "status", l.getStatus(),
                        "createdAt", l.getCreatedAt().toString()
                ))
                .toList();
        return ResponseEntity.ok(lectures);
    }
}
