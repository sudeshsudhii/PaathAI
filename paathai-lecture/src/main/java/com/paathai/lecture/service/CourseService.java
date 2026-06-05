package com.paathai.lecture.service;

import com.paathai.common.dto.CourseCreateRequest;
import com.paathai.common.dto.CourseResponse;
import com.paathai.common.entity.Course;
import com.paathai.common.exception.ResourceNotFoundException;
import com.paathai.common.repository.CourseRepository;
import com.paathai.common.repository.LectureRepository;
import com.paathai.common.repository.SyllabusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for Course CRUD operations.
 * Handles creation, listing, updating, and soft-delete (archival) of courses.
 */
@Service
public class CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final SyllabusRepository syllabusRepository;

    public CourseService(CourseRepository courseRepository,
                         LectureRepository lectureRepository,
                         SyllabusRepository syllabusRepository) {
        this.courseRepository = courseRepository;
        this.lectureRepository = lectureRepository;
        this.syllabusRepository = syllabusRepository;
    }

    public CourseResponse createCourse(Long userId, CourseCreateRequest request) {
        Course course = new Course();
        course.setName(request.getName());
        course.setCode(request.getCode());
        course.setSemester(request.getSemester());
        course.setDescription(request.getDescription());
        course.setInstructorId(userId);
        course.setStatus("ACTIVE");
        courseRepository.save(course);

        log.info("Course created: id={}, name='{}' by user {}", course.getId(), course.getName(), userId);
        return toResponse(course);
    }

    public List<CourseResponse> listCourses(Long userId) {
        return courseRepository.findByInstructorIdAndStatusOrderByUpdatedAtDesc(userId, "ACTIVE")
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CourseResponse getCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        return toResponse(course);
    }

    @Transactional
    public CourseResponse updateCourse(Long courseId, Long userId, CourseCreateRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        if (!course.getInstructorId().equals(userId)) {
            throw new IllegalStateException("Not authorized to update this course");
        }

        if (request.getName() != null) course.setName(request.getName());
        if (request.getCode() != null) course.setCode(request.getCode());
        if (request.getSemester() != null) course.setSemester(request.getSemester());
        if (request.getDescription() != null) course.setDescription(request.getDescription());
        courseRepository.save(course);

        log.info("Course updated: id={}", courseId);
        return toResponse(course);
    }

    @Transactional
    public void archiveCourse(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        if (!course.getInstructorId().equals(userId)) {
            throw new IllegalStateException("Not authorized to archive this course");
        }

        course.setStatus("ARCHIVED");
        courseRepository.save(course);
        log.info("Course archived: id={}", courseId);
    }

    private CourseResponse toResponse(Course course) {
        long lectureCount = lectureRepository.findByCourseId(course.getId()).size();
        boolean hasSyllabus = syllabusRepository.existsByCourseId(course.getId());

        return CourseResponse.builder()
                .id(course.getId())
                .name(course.getName())
                .code(course.getCode())
                .semester(course.getSemester())
                .description(course.getDescription())
                .status(course.getStatus())
                .lectureCount(lectureCount)
                .syllabusAttached(hasSyllabus)
                .createdAt(course.getCreatedAt().toString())
                .updatedAt(course.getUpdatedAt().toString())
                .build();
    }
}
