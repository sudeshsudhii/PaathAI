package com.paathai.study.service;

import com.paathai.common.entity.SemesterSummary;
import com.paathai.common.repository.SemesterSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Storage and retrieval service for semester memory.
 *
 * <p>Provides interfaces for storing lecture, unit, and subject summaries.
 * No tutor implementation — only storage and retrieval.</p>
 */
@Service
public class SemesterMemoryService {

    private static final Logger log = LoggerFactory.getLogger(SemesterMemoryService.class);

    private final SemesterSummaryRepository summaryRepository;

    public SemesterMemoryService(SemesterSummaryRepository summaryRepository) {
        this.summaryRepository = summaryRepository;
    }

    /**
     * Store or update a lecture summary.
     */
    @Transactional
    public SemesterSummary storeLectureSummary(Long courseId, Long lectureId, String title, String summary) {
        return summaryRepository.findBySourceIdAndSummaryType(lectureId, "LECTURE")
                .map(existing -> {
                    existing.setContent(summary);
                    existing.setTitle(title);
                    return summaryRepository.save(existing);
                })
                .orElseGet(() -> {
                    SemesterSummary s = new SemesterSummary();
                    s.setCourseId(courseId);
                    s.setSummaryType("LECTURE");
                    s.setSourceId(lectureId);
                    s.setTitle(title);
                    s.setContent(summary);
                    return summaryRepository.save(s);
                });
    }

    /**
     * Store or update a unit summary.
     */
    @Transactional
    public SemesterSummary storeUnitSummary(Long courseId, Long unitId, String title, String summary) {
        return summaryRepository.findBySourceIdAndSummaryType(unitId, "UNIT")
                .map(existing -> {
                    existing.setContent(summary);
                    existing.setTitle(title);
                    return summaryRepository.save(existing);
                })
                .orElseGet(() -> {
                    SemesterSummary s = new SemesterSummary();
                    s.setCourseId(courseId);
                    s.setSummaryType("UNIT");
                    s.setSourceId(unitId);
                    s.setTitle(title);
                    s.setContent(summary);
                    return summaryRepository.save(s);
                });
    }

    /**
     * Store or update a subject summary.
     */
    @Transactional
    public SemesterSummary storeSubjectSummary(Long courseId, Long subjectId, String title, String summary) {
        return summaryRepository.findBySourceIdAndSummaryType(subjectId, "SUBJECT")
                .map(existing -> {
                    existing.setContent(summary);
                    existing.setTitle(title);
                    return summaryRepository.save(existing);
                })
                .orElseGet(() -> {
                    SemesterSummary s = new SemesterSummary();
                    s.setCourseId(courseId);
                    s.setSummaryType("SUBJECT");
                    s.setSourceId(subjectId);
                    s.setTitle(title);
                    s.setContent(summary);
                    return summaryRepository.save(s);
                });
    }

    /**
     * Get all summaries for a course, grouped by type.
     */
    public Map<String, List<SemesterSummary>> getMemory(Long courseId) {
        return summaryRepository.findByCourseIdOrderByCreatedAtDesc(courseId).stream()
                .collect(Collectors.groupingBy(SemesterSummary::getSummaryType));
    }

    /**
     * Get all lecture summaries for a specific unit.
     */
    public List<SemesterSummary> getMemoryForUnit(Long unitId) {
        return summaryRepository.findBySourceIdAndSummaryType(unitId, "UNIT")
                .map(List::of)
                .orElse(List.of());
    }

    /**
     * Get a specific lecture summary.
     */
    public SemesterSummary getLectureSummary(Long lectureId) {
        return summaryRepository.findBySourceIdAndSummaryType(lectureId, "LECTURE")
                .orElse(null);
    }
}
