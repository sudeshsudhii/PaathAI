package com.paathai.common.repository;

import com.paathai.common.entity.SemesterSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterSummaryRepository extends JpaRepository<SemesterSummary, Long> {
    List<SemesterSummary> findByCourseIdAndSummaryType(Long courseId, String summaryType);
    Optional<SemesterSummary> findBySourceIdAndSummaryType(Long sourceId, String summaryType);
    List<SemesterSummary> findByCourseIdOrderByCreatedAtDesc(Long courseId);
}
