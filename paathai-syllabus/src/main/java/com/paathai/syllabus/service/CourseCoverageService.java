package com.paathai.syllabus.service;

import com.paathai.common.dto.CourseCoverageResponse;
import com.paathai.common.entity.Subject;
import com.paathai.common.entity.SyllabusTopic;
import com.paathai.common.entity.Unit;
import com.paathai.common.exception.ResourceNotFoundException;
import com.paathai.common.entity.Syllabus;
import com.paathai.common.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Deterministic coverage calculation engine.
 *
 * <p>Formula: {@code coverage% = coveredTopics / totalTopics × 100}</p>
 * <p>A topic is COMPLETED if its ID appears in {@code live_topic_timeline.syllabus_topic_id}
 * for any session linked to the course. No LLM calls — pure SQL aggregation.</p>
 */
@Service
public class CourseCoverageService {

    private static final Logger log = LoggerFactory.getLogger(CourseCoverageService.class);

    private final SyllabusRepository syllabusRepository;
    private final SubjectRepository subjectRepository;
    private final UnitRepository unitRepository;
    private final SyllabusTopicRepository syllabusTopicRepository;
    private final LiveTopicTimelineRepository liveTopicTimelineRepository;

    public CourseCoverageService(SyllabusRepository syllabusRepository,
                                  SubjectRepository subjectRepository,
                                  UnitRepository unitRepository,
                                  SyllabusTopicRepository syllabusTopicRepository,
                                  LiveTopicTimelineRepository liveTopicTimelineRepository) {
        this.syllabusRepository = syllabusRepository;
        this.subjectRepository = subjectRepository;
        this.unitRepository = unitRepository;
        this.syllabusTopicRepository = syllabusTopicRepository;
        this.liveTopicTimelineRepository = liveTopicTimelineRepository;
    }

    /**
     * Calculate full hierarchical coverage for a course.
     */
    public CourseCoverageResponse getCoverage(Long courseId) {
        Syllabus syllabus = syllabusRepository.findByCourseId(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Syllabus", courseId));

        // Get all covered topic IDs in one query
        Set<Long> coveredTopicIds = liveTopicTimelineRepository
                .findDistinctCoveredTopicIdsByCourseId(courseId);

        List<Subject> subjects = subjectRepository.findBySyllabusIdOrderBySortOrder(syllabus.getId());

        int totalTopics = 0;
        int totalCovered = 0;

        List<CourseCoverageResponse.SubjectCoverage> subjectCoverages = new java.util.ArrayList<>();

        for (Subject subject : subjects) {
            List<Unit> units = unitRepository.findBySubjectIdOrderBySortOrder(subject.getId());
            List<CourseCoverageResponse.UnitCoverage> unitCoverages = new java.util.ArrayList<>();

            int subjectTotal = 0, subjectCovered = 0;

            for (Unit unit : units) {
                List<SyllabusTopic> topics = syllabusTopicRepository.findByUnitIdOrderBySortOrder(unit.getId());
                List<CourseCoverageResponse.TopicCoverage> topicCoverages = new java.util.ArrayList<>();

                int unitTotal = topics.size();
                int unitCovered = 0;

                for (SyllabusTopic topic : topics) {
                    String status;
                    if (coveredTopicIds.contains(topic.getId())) {
                        status = "COMPLETED";
                        unitCovered++;
                    } else {
                        status = "NOT_STARTED";
                    }
                    topicCoverages.add(CourseCoverageResponse.TopicCoverage.builder()
                            .id(topic.getId())
                            .name(topic.getName())
                            .status(status)
                            .build());
                }

                double unitPercent = unitTotal > 0 ? (double) unitCovered / unitTotal * 100.0 : 0.0;
                String unitStatus = unitCovered == 0 ? "NOT_STARTED"
                        : unitCovered == unitTotal ? "COMPLETED" : "IN_PROGRESS";

                unitCoverages.add(CourseCoverageResponse.UnitCoverage.builder()
                        .id(unit.getId())
                        .name(unit.getName())
                        .percent(Math.round(unitPercent * 10.0) / 10.0)
                        .status(unitStatus)
                        .topics(topicCoverages)
                        .build());

                subjectTotal += unitTotal;
                subjectCovered += unitCovered;
            }

            double subjectPercent = subjectTotal > 0 ? (double) subjectCovered / subjectTotal * 100.0 : 0.0;
            String subjectStatus = subjectCovered == 0 ? "NOT_STARTED"
                    : subjectCovered == subjectTotal ? "COMPLETED" : "IN_PROGRESS";

            subjectCoverages.add(CourseCoverageResponse.SubjectCoverage.builder()
                    .id(subject.getId())
                    .name(subject.getName())
                    .percent(Math.round(subjectPercent * 10.0) / 10.0)
                    .status(subjectStatus)
                    .units(unitCoverages)
                    .build());

            totalTopics += subjectTotal;
            totalCovered += subjectCovered;
        }

        double overallPercent = totalTopics > 0 ? (double) totalCovered / totalTopics * 100.0 : 0.0;

        return CourseCoverageResponse.builder()
                .courseId(courseId)
                .overallPercent(Math.round(overallPercent * 10.0) / 10.0)
                .totalTopics(totalTopics)
                .coveredTopics(totalCovered)
                .subjects(subjectCoverages)
                .build();
    }

    /**
     * Refresh coverage status for a specific topic after a syllabus mapping event.
     */
    @Transactional
    public void refreshTopicStatus(Long syllabusTopicId) {
        syllabusTopicRepository.updateCoverageStatus(syllabusTopicId, "COMPLETED");
    }
}
