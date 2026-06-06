package com.paathai.common.repository;

import com.paathai.common.entity.LiveTopicTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface LiveTopicTimelineRepository extends JpaRepository<LiveTopicTimeline, Long> {

    List<LiveTopicTimeline> findBySessionIdOrderByDetectedAtMs(Long sessionId);

    Optional<LiveTopicTimeline> findFirstBySessionIdOrderByDetectedAtMsDesc(Long sessionId);

    /**
     * Find all distinct syllabus topic IDs covered across all sessions for a course.
     */
    @Query(value = """
        SELECT DISTINCT ltt.syllabus_topic_id
        FROM live_topic_timeline ltt
        JOIN live_sessions ls ON ltt.session_id = ls.id
        JOIN lectures l ON ls.lecture_id = l.id
        WHERE l.course_id = :courseId AND ltt.syllabus_topic_id IS NOT NULL
        """, nativeQuery = true)
    Set<Long> findDistinctCoveredTopicIdsByCourseId(@Param("courseId") Long courseId);
}
