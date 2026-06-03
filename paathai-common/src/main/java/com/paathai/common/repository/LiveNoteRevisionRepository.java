package com.paathai.common.repository;

import com.paathai.common.entity.LiveNoteRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LiveNoteRevisionRepository extends JpaRepository<LiveNoteRevision, Long> {

    Optional<LiveNoteRevision> findFirstBySessionIdOrderByRevisionNumberDesc(Long sessionId);

    List<LiveNoteRevision> findBySessionIdOrderByRevisionNumber(Long sessionId);
}
