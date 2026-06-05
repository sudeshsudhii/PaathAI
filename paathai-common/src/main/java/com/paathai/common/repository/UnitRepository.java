package com.paathai.common.repository;

import com.paathai.common.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {
    List<Unit> findBySubjectIdOrderBySortOrder(Long subjectId);
    long countBySubjectId(Long subjectId);
}
