package com.paathai.common.repository;

import com.paathai.common.entity.LiveSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LiveSessionRepository extends JpaRepository<LiveSession, Long> {

    List<LiveSession> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<LiveSession> findByIdAndUserId(Long id, Long userId);

    List<LiveSession> findByStatus(String status);
}
