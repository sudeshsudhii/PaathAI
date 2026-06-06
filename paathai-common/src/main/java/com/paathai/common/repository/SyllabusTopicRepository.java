package com.paathai.common.repository;

import com.paathai.common.entity.LiveTopicTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SyllabusTopicRepository extends JpaRepository<com.paathai.common.entity.SyllabusTopic, Long> {

    /**
     * Find syllabus topics similar to the given embedding via pgvector cosine similarity.
     * Filters by course — joins through syllabi → subjects → units → syllabus_topics.
     */
    @Query(value = """
        SELECT st.* FROM syllabus_topics st
        JOIN units u ON st.unit_id = u.id
        JOIN subjects s ON u.subject_id = s.id
        JOIN syllabi sy ON s.syllabus_id = sy.id
        WHERE sy.course_id = :courseId AND st.embedding IS NOT NULL
        ORDER BY st.embedding <=> CAST(:embedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<com.paathai.common.entity.SyllabusTopic> findSimilarByCourseId(
            @Param("courseId") Long courseId,
            @Param("embedding") String embedding,
            @Param("limit") int limit);

    List<com.paathai.common.entity.SyllabusTopic> findByUnitIdOrderBySortOrder(Long unitId);

    long countByUnitId(Long unitId);

    /**
     * Find all syllabus topics for a course by joining through the hierarchy.
     */
    @Query(value = """
        SELECT st.* FROM syllabus_topics st
        JOIN units u ON st.unit_id = u.id
        JOIN subjects s ON u.subject_id = s.id
        JOIN syllabi sy ON s.syllabus_id = sy.id
        WHERE sy.course_id = :courseId
        ORDER BY s.sort_order, u.sort_order, st.sort_order
        """, nativeQuery = true)
    List<com.paathai.common.entity.SyllabusTopic> findAllByCourseId(@Param("courseId") Long courseId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE SyllabusTopic t SET t.coverageStatus = :status WHERE t.id = :topicId")
    void updateCoverageStatus(@Param("topicId") Long topicId, @Param("status") String status);
}
