package com.paathai.syllabus.service;

import com.paathai.common.dto.SyllabusTreeResponse;
import com.paathai.common.dto.TopicCreateRequest;
import com.paathai.common.entity.*;
import com.paathai.common.event.SyllabusProcessed;
import com.paathai.common.exception.ResourceNotFoundException;
import com.paathai.common.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages the full syllabus lifecycle: creation, tree building,
 * CRUD for subjects/units/topics, and reordering.
 */
@Service
public class SyllabusManagementService {

    private static final Logger log = LoggerFactory.getLogger(SyllabusManagementService.class);

    private final SyllabusRepository syllabusRepository;
    private final SubjectRepository subjectRepository;
    private final UnitRepository unitRepository;
    private final SyllabusTopicRepository syllabusTopicRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SyllabusManagementService(SyllabusRepository syllabusRepository,
                                      SubjectRepository subjectRepository,
                                      UnitRepository unitRepository,
                                      SyllabusTopicRepository syllabusTopicRepository,
                                      ApplicationEventPublisher eventPublisher) {
        this.syllabusRepository = syllabusRepository;
        this.subjectRepository = subjectRepository;
        this.unitRepository = unitRepository;
        this.syllabusTopicRepository = syllabusTopicRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Get the full syllabus tree for a course.
     */
    public SyllabusTreeResponse getTree(Long courseId) {
        Syllabus syllabus = syllabusRepository.findByCourseId(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Syllabus", courseId));

        List<Subject> subjects = subjectRepository.findBySyllabusIdOrderBySortOrder(syllabus.getId());

        List<SyllabusTreeResponse.SubjectNode> subjectNodes = subjects.stream().map(subject -> {
            List<Unit> units = unitRepository.findBySubjectIdOrderBySortOrder(subject.getId());

            List<SyllabusTreeResponse.UnitNode> unitNodes = units.stream().map(unit -> {
                List<SyllabusTopic> topics = syllabusTopicRepository.findByUnitIdOrderBySortOrder(unit.getId());

                List<SyllabusTreeResponse.TopicNode> topicNodes = topics.stream().map(topic ->
                        SyllabusTreeResponse.TopicNode.builder()
                                .id(topic.getId())
                                .name(topic.getName())
                                .description(topic.getDescription())
                                .sortOrder(topic.getSortOrder())
                                .coverageStatus(topic.getCoverageStatus())
                                .build()
                ).toList();

                return SyllabusTreeResponse.UnitNode.builder()
                        .id(unit.getId())
                        .name(unit.getName())
                        .sortOrder(unit.getSortOrder())
                        .topics(topicNodes)
                        .build();
            }).toList();

            return SyllabusTreeResponse.SubjectNode.builder()
                    .id(subject.getId())
                    .name(subject.getName())
                    .sortOrder(subject.getSortOrder())
                    .units(unitNodes)
                    .build();
        }).toList();

        return SyllabusTreeResponse.builder()
                .syllabusId(syllabus.getId())
                .courseId(courseId)
                .title(syllabus.getTitle())
                .sourceType(syllabus.getSourceType())
                .subjects(subjectNodes)
                .build();
    }

    /**
     * Create a manual syllabus for a course.
     */
    @Transactional
    public Syllabus createManualSyllabus(Long courseId, String title) {
        if (syllabusRepository.existsByCourseId(courseId)) {
            throw new IllegalStateException("Syllabus already exists for course " + courseId);
        }

        Syllabus syllabus = new Syllabus();
        syllabus.setCourseId(courseId);
        syllabus.setTitle(title);
        syllabus.setSourceType("MANUAL");
        syllabusRepository.save(syllabus);

        log.info("Manual syllabus created: id={} for course {}", syllabus.getId(), courseId);
        return syllabus;
    }

    /**
     * Import syllabus from parsed hierarchy (after PDF/JSON parsing).
     */
    @Transactional
    public Syllabus importFromParsed(Long courseId, String title, String sourceType,
                                      String filePath, SyllabusParser.SyllabusHierarchy hierarchy) {
        Syllabus syllabus = new Syllabus();
        syllabus.setCourseId(courseId);
        syllabus.setTitle(title);
        syllabus.setSourceType(sourceType);
        syllabus.setRawFilePath(filePath);
        syllabusRepository.save(syllabus);

        int subjectCount = 0, unitCount = 0, topicCount = 0;

        for (int si = 0; si < hierarchy.subjects().size(); si++) {
            SyllabusParser.SubjectEntry se = hierarchy.subjects().get(si);

            Subject subject = new Subject();
            subject.setSyllabusId(syllabus.getId());
            subject.setName(se.name());
            subject.setSortOrder(si);
            subjectRepository.save(subject);
            subjectCount++;

            for (int ui = 0; ui < se.units().size(); ui++) {
                SyllabusParser.UnitEntry ue = se.units().get(ui);

                Unit unit = new Unit();
                unit.setSubjectId(subject.getId());
                unit.setName(ue.name());
                unit.setSortOrder(ui);
                unitRepository.save(unit);
                unitCount++;

                for (int ti = 0; ti < ue.topics().size(); ti++) {
                    SyllabusParser.TopicEntry te = ue.topics().get(ti);

                    SyllabusTopic topic = new SyllabusTopic();
                    topic.setUnitId(unit.getId());
                    topic.setName(te.name());
                    topic.setDescription(te.description());
                    topic.setSortOrder(ti);
                    syllabusTopicRepository.save(topic);
                    topicCount++;
                }
            }
        }

        eventPublisher.publishEvent(new SyllabusProcessed(
                syllabus.getId(), courseId, subjectCount, unitCount, topicCount));

        log.info("Syllabus imported: {} subjects, {} units, {} topics for course {}",
                subjectCount, unitCount, topicCount, courseId);
        return syllabus;
    }

    // ===== Subject CRUD =====

    @Transactional
    public Subject addSubject(Long syllabusId, String name) {
        long count = subjectRepository.countBySyllabusId(syllabusId);
        Subject subject = new Subject();
        subject.setSyllabusId(syllabusId);
        subject.setName(name);
        subject.setSortOrder((int) count);
        subjectRepository.save(subject);
        return subject;
    }

    // ===== Unit CRUD =====

    @Transactional
    public Unit addUnit(Long subjectId, String name) {
        long count = unitRepository.countBySubjectId(subjectId);
        Unit unit = new Unit();
        unit.setSubjectId(subjectId);
        unit.setName(name);
        unit.setSortOrder((int) count);
        unitRepository.save(unit);
        return unit;
    }

    // ===== Topic CRUD =====

    @Transactional
    public SyllabusTopic addTopic(Long unitId, TopicCreateRequest request) {
        long count = syllabusTopicRepository.countByUnitId(unitId);
        SyllabusTopic topic = new SyllabusTopic();
        topic.setUnitId(unitId);
        topic.setName(request.getName());
        topic.setDescription(request.getDescription());
        topic.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : (int) count);
        syllabusTopicRepository.save(topic);
        return topic;
    }

    @Transactional
    public SyllabusTopic updateTopic(Long topicId, TopicCreateRequest request) {
        SyllabusTopic topic = syllabusTopicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic", topicId));
        if (request.getName() != null) topic.setName(request.getName());
        if (request.getDescription() != null) topic.setDescription(request.getDescription());
        if (request.getSortOrder() != null) topic.setSortOrder(request.getSortOrder());
        syllabusTopicRepository.save(topic);
        return topic;
    }

    @Transactional
    public void deleteTopic(Long topicId) {
        syllabusTopicRepository.deleteById(topicId);
    }

    /**
     * Reorder topics within a unit by receiving the ordered list of topic IDs.
     */
    @Transactional
    public void reorderTopics(Long unitId, List<Long> topicIds) {
        for (int i = 0; i < topicIds.size(); i++) {
            final Long topicId = topicIds.get(i);
            SyllabusTopic topic = syllabusTopicRepository.findById(topicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Topic", topicId));
            topic.setSortOrder(i);
            syllabusTopicRepository.save(topic);
        }
    }

    /**
     * Reorder units within a subject.
     */
    @Transactional
    public void reorderUnits(Long subjectId, List<Long> unitIds) {
        for (int i = 0; i < unitIds.size(); i++) {
            final Long unitId = unitIds.get(i);
            Unit unit = unitRepository.findById(unitId)
                    .orElseThrow(() -> new ResourceNotFoundException("Unit", unitId));
            unit.setSortOrder(i);
            unitRepository.save(unit);
        }
    }
}
