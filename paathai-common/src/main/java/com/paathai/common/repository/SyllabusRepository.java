package com.paathai.common.repository;

import com.paathai.common.entity.Syllabus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SyllabusRepository extends JpaRepository<Syllabus, Long> {
    Optional<Syllabus> findByCourseId(Long courseId);
    boolean existsByCourseId(Long courseId);
}
