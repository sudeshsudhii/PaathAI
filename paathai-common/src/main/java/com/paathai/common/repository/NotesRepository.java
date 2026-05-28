package com.paathai.common.repository;

import com.paathai.common.entity.Notes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotesRepository extends JpaRepository<Notes, Long> {
    List<Notes> findByLectureId(Long lectureId);
    Optional<Notes> findFirstByLectureIdOrderByCreatedAtDesc(Long lectureId);
}

