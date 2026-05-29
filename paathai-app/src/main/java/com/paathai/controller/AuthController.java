package com.paathai.controller;

import com.paathai.common.entity.Course;
import com.paathai.common.entity.User;
import com.paathai.common.repository.CourseRepository;
import com.paathai.common.repository.UserRepository;
import com.paathai.security.JwtTokenProvider;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication controller.
 * POST /api/auth/register - Create account + default course, returns JWT
 * POST /api/auth/login    - Validate credentials, returns JWT
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(UserRepository userRepository,
                          CourseRepository courseRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        log.info("Registration attempt for: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Email already registered"
            ));
        }

        // Create user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName() != null ? request.getFullName() : "Student");
        user.setRole("STUDENT");
        userRepository.save(user);

        // Create default course for the user
        Course course = new Course();
        course.setName("My Lectures");
        course.setCode("DEFAULT");
        course.setDescription("Default course for uploaded lectures");
        course.setInstructorId(user.getId());
        courseRepository.save(course);

        // Generate JWT
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole());

        log.info("User registered: {} (id={})", user.getEmail(), user.getId());

        return ResponseEntity.ok(Map.of(
            "token", token,
            "userId", user.getId(),
            "email", user.getEmail(),
            "fullName", user.getFullName(),
            "defaultCourseId", course.getId()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.info("Login attempt for: {}", request.getEmail());

        return userRepository.findByEmail(request.getEmail())
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
                .map(user -> {
                    String token = jwtTokenProvider.generateToken(
                            user.getId(), user.getEmail(), user.getRole());

                    // Find default course
                    Long courseId = courseRepository.findByInstructorId(user.getId())
                            .stream().findFirst().map(Course::getId).orElse(null);

                    log.info("User logged in: {} (id={})", user.getEmail(), user.getId());

                    return ResponseEntity.ok(Map.of(
                        "token", token,
                        "userId", user.getId(),
                        "email", user.getEmail(),
                        "fullName", user.getFullName(),
                        "defaultCourseId", courseId != null ? courseId : 0
                    ));
                })
                .orElse(ResponseEntity.status(401).body(Map.of(
                    "error", "Invalid email or password"
                )));
    }

    @Data
    public static class RegisterRequest {
        private String email;
        private String password;
        private String fullName;
    }

    @Data
    public static class LoginRequest {
        private String email;
        private String password;
    }
}

