package com.paathai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * PaathAI — AI-powered syllabus-aware learning intelligence platform.
 *
 * Modular monolith with event-driven internal communication.
 * All modules deploy as a single JAR.
 */
@SpringBootApplication
@EnableAsync
public class PaathAIApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaathAIApplication.class, args);
    }
}
