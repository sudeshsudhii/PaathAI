package com.paathai.common.repository;

import com.paathai.common.entity.KnowledgeEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeEdgeRepository extends JpaRepository<KnowledgeEdge, Long> {
    List<KnowledgeEdge> findByCourseId(Long courseId);
    List<KnowledgeEdge> findByCourseIdAndEdgeType(Long courseId, String edgeType);
    List<KnowledgeEdge> findBySourceNodeId(Long sourceNodeId);
    List<KnowledgeEdge> findByTargetNodeId(Long targetNodeId);
}
