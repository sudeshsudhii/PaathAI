package com.paathai.syllabus.service;

import com.paathai.common.entity.*;
import com.paathai.common.event.SyllabusProcessed;
import com.paathai.common.exception.ResourceNotFoundException;
import com.paathai.common.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Minimal knowledge graph service using PostgreSQL relational tables.
 *
 * <p>Builds PARENT_CHILD edges from syllabus hierarchy on {@link SyllabusProcessed} events.
 * Supports manual PREREQUISITE and RELATED edge creation.</p>
 */
@Service
public class KnowledgeGraphService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeGraphService.class);

    private final KnowledgeNodeRepository nodeRepository;
    private final KnowledgeEdgeRepository edgeRepository;
    private final SubjectRepository subjectRepository;
    private final UnitRepository unitRepository;
    private final SyllabusTopicRepository syllabusTopicRepository;
    private final SyllabusRepository syllabusRepository;

    public KnowledgeGraphService(KnowledgeNodeRepository nodeRepository,
                                  KnowledgeEdgeRepository edgeRepository,
                                  SubjectRepository subjectRepository,
                                  UnitRepository unitRepository,
                                  SyllabusTopicRepository syllabusTopicRepository,
                                  SyllabusRepository syllabusRepository) {
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.subjectRepository = subjectRepository;
        this.unitRepository = unitRepository;
        this.syllabusTopicRepository = syllabusTopicRepository;
        this.syllabusRepository = syllabusRepository;
    }

    /**
     * Automatically build PARENT_CHILD edges from the syllabus hierarchy.
     */
    @EventListener
    @Async("generalEventExecutor")
    @Transactional
    public void onSyllabusProcessed(SyllabusProcessed event) {
        buildFromSyllabus(event.getCourseId());
    }

    @Transactional
    public void buildFromSyllabus(Long courseId) {
        log.info("Building knowledge graph from syllabus for course {}", courseId);

        Syllabus syllabus = syllabusRepository.findByCourseId(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Syllabus", courseId));

        List<Subject> subjects = subjectRepository.findBySyllabusIdOrderBySortOrder(syllabus.getId());

        for (Subject subject : subjects) {
            KnowledgeNode subjectNode = findOrCreateNode(courseId, "SUBJECT", "SYLLABUS",
                    subject.getId(), subject.getName(), null);

            List<Unit> units = unitRepository.findBySubjectIdOrderBySortOrder(subject.getId());
            for (Unit unit : units) {
                KnowledgeNode unitNode = findOrCreateNode(courseId, "UNIT", "SYLLABUS",
                        unit.getId(), unit.getName(), null);
                createEdgeIfNotExists(courseId, subjectNode.getId(), unitNode.getId(), "PARENT_CHILD");

                List<SyllabusTopic> topics = syllabusTopicRepository.findByUnitIdOrderBySortOrder(unit.getId());
                for (SyllabusTopic topic : topics) {
                    KnowledgeNode topicNode = findOrCreateNode(courseId, "TOPIC", "SYLLABUS",
                            topic.getId(), topic.getName(), topic.getDescription());
                    createEdgeIfNotExists(courseId, unitNode.getId(), topicNode.getId(), "PARENT_CHILD");
                }
            }
        }

        log.info("Knowledge graph built for course {}", courseId);
    }

    /**
     * Get the full graph (nodes + edges) for a course.
     */
    public Map<String, Object> getGraph(Long courseId) {
        return Map.of(
                "nodes", nodeRepository.findByCourseId(courseId),
                "edges", edgeRepository.findByCourseId(courseId)
        );
    }

    /**
     * Add a manual edge between two nodes.
     */
    @Transactional
    public KnowledgeEdge addEdge(Long courseId, Long sourceNodeId, Long targetNodeId, String edgeType) {
        KnowledgeEdge edge = new KnowledgeEdge();
        edge.setCourseId(courseId);
        edge.setSourceNodeId(sourceNodeId);
        edge.setTargetNodeId(targetNodeId);
        edge.setEdgeType(edgeType);
        return edgeRepository.save(edge);
    }

    @Transactional
    public void deleteEdge(Long edgeId) {
        edgeRepository.deleteById(edgeId);
    }

    /**
     * Get prerequisites for a node (traverse PREREQUISITE edges from target).
     */
    public List<KnowledgeNode> getPrerequisites(Long nodeId) {
        return edgeRepository.findByTargetNodeId(nodeId).stream()
                .filter(e -> "PREREQUISITE".equals(e.getEdgeType()))
                .map(e -> nodeRepository.findById(e.getSourceNodeId()).orElse(null))
                .filter(n -> n != null)
                .toList();
    }

    // ===== Helpers =====

    private KnowledgeNode findOrCreateNode(Long courseId, String nodeType, String sourceType,
                                            Long sourceId, String label, String description) {
        return nodeRepository.findByCourseIdAndSourceTypeAndSourceId(courseId, sourceType, sourceId)
                .orElseGet(() -> {
                    KnowledgeNode node = new KnowledgeNode();
                    node.setCourseId(courseId);
                    node.setNodeType(nodeType);
                    node.setSourceType(sourceType);
                    node.setSourceId(sourceId);
                    node.setLabel(label);
                    node.setDescription(description);
                    return nodeRepository.save(node);
                });
    }

    private void createEdgeIfNotExists(Long courseId, Long sourceId, Long targetId, String edgeType) {
        boolean exists = edgeRepository.findBySourceNodeId(sourceId).stream()
                .anyMatch(e -> e.getTargetNodeId().equals(targetId) && e.getEdgeType().equals(edgeType));
        if (!exists) {
            KnowledgeEdge edge = new KnowledgeEdge();
            edge.setCourseId(courseId);
            edge.setSourceNodeId(sourceId);
            edge.setTargetNodeId(targetId);
            edge.setEdgeType(edgeType);
            edgeRepository.save(edge);
        }
    }
}
