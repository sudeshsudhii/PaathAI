package com.paathai.controller;

import com.paathai.common.event.LectureCreated;
import com.paathai.common.entity.Course;
import com.paathai.common.entity.Lecture;
import com.paathai.common.entity.Notes;
import com.paathai.common.repository.CourseRepository;
import com.paathai.common.repository.LectureRepository;
import com.paathai.common.repository.NotesRepository;
import com.paathai.study.notes.NotesGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lecture management controller.
 * POST /api/lectures/upload     - Upload audio file
 * GET  /api/lectures            - List user's lectures
 * GET  /api/lectures/{id}       - Get lecture details
 * GET  /api/lectures/{id}/notes - Get generated notes
 */
@RestController
@RequestMapping("/api/lectures")
public class LectureController {

    private static final Logger log = LoggerFactory.getLogger(LectureController.class);

    private final LectureRepository lectureRepository;
    private final CourseRepository courseRepository;
    private final NotesRepository notesRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${paathai.audio.storage-path:/tmp/paathai-uploads}")
    private String audioStoragePath;

    public LectureController(LectureRepository lectureRepository,
                              CourseRepository courseRepository,
                              NotesRepository notesRepository,
                              ApplicationEventPublisher eventPublisher) {
        this.lectureRepository = lectureRepository;
        this.courseRepository = courseRepository;
        this.notesRepository = notesRepository;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadLecture(
            Authentication auth,
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title) {

        Long userId = (Long) auth.getPrincipal();
        log.info("Upload request: title='{}' by user {} (size={})",
                 title, userId, file.getSize());

        // Find user's default course
        Long courseId = courseRepository.findByInstructorId(userId)
                .stream().findFirst().map(Course::getId)
                .orElseThrow(() -> new RuntimeException("No course found for user"));

        try {
            // Ensure upload directory exists
            File uploadDir = new File(audioStoragePath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // Save file with UUID prefix
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            String filepath = uploadDir.getAbsolutePath() + File.separator + filename;
            file.transferTo(new File(filepath));

            // Create lecture record
            Lecture lecture = new Lecture();
            lecture.setCourseId(courseId);
            lecture.setUploadedBy(userId);
            lecture.setTitle(title);
            lecture.setAudioPath(filepath);
            lecture.setStatus("PENDING");
            lectureRepository.save(lecture);

            // Publish event to trigger transcription pipeline
            eventPublisher.publishEvent(new LectureCreated(
                lecture.getId(), userId, courseId, filepath
            ));

            log.info("Lecture uploaded: id={}, path={}", lecture.getId(), filepath);

            return ResponseEntity.ok(Map.of(
                "lectureId", lecture.getId(),
                "title", lecture.getTitle(),
                "status", lecture.getStatus(),
                "message", "Lecture uploaded successfully. Transcription started."
            ));

        } catch (IOException e) {
            log.error("File upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Upload failed: " + e.getMessage()
            ));
        }
    }

    @GetMapping
    public ResponseEntity<?> listLectures(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        List<Lecture> lectures = lectureRepository.findByUploadedByOrderByCreatedAtDesc(userId);

        List<Map<String, Object>> response = lectures.stream()
                .map(l -> Map.<String, Object>of(
                    "id", l.getId(),
                    "title", l.getTitle() != null ? l.getTitle() : "Untitled",
                    "status", l.getStatus(),
                    "createdAt", l.getCreatedAt().toString()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLecture(@PathVariable Long id) {
        return lectureRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/notes")
    public ResponseEntity<?> getLectureNotes(@PathVariable Long id) {
        return notesRepository.findFirstByLectureIdOrderByCreatedAtDesc(id)
                .map(notes -> ResponseEntity.ok(Map.of(
                    "lectureId", id,
                    "content", notes.getContent(),
                    "format", notes.getFormat(),
                    "generatedAt", notes.getCreatedAt().toString()
                )))
                .orElse(ResponseEntity.ok(Map.of(
                    "lectureId", id,
                    "content", "Notes are still being generated. Please check back in a moment.",
                    "format", "TEXT",
                    "generatedAt", ""
                )));
    }
}

