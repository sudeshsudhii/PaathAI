package com.paathai.common.repository;

import com.paathai.common.entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LectureRepository extends JpaRepository<Lecture, Long> {
    List<Lecture> findByUploadedByOrderByCreatedAtDesc(Long userId);
    List<Lecture> findByCourseId(Long courseId);
}

