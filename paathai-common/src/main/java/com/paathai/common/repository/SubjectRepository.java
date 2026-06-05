package com.paathai.common.repository;

import com.paathai.common.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findBySyllabusIdOrderBySortOrder(Long syllabusId);
    long countBySyllabusId(Long syllabusId);
}
