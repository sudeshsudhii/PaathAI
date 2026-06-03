package com.paathai.common.repository;

import com.paathai.common.entity.LiveAudioChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiveAudioChunkRepository extends JpaRepository<LiveAudioChunk, Long> {

    List<LiveAudioChunk> findBySessionIdAndProcessedFalseOrderBySequenceNumber(Long sessionId);

    List<LiveAudioChunk> findBySessionIdOrderBySequenceNumber(Long sessionId);
}
