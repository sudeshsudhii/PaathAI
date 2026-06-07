package com.paathai.common.repository;

import com.paathai.common.entity.KnowledgeNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeNodeRepository extends JpaRepository<KnowledgeNode, Long> {
    List<KnowledgeNode> findByCourseId(Long courseId);
    List<KnowledgeNode> findByCourseIdAndNodeType(Long courseId, String nodeType);
    Optional<KnowledgeNode> findByCourseIdAndSourceTypeAndSourceId(Long courseId, String sourceType, Long sourceId);
}
