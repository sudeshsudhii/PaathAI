package com.paathai.common.repository;

import com.paathai.common.entity.TranscriptChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TranscriptChunkRepository extends JpaRepository<TranscriptChunk, Long> {

    List<TranscriptChunk> findByTranscriptIdOrderByChunkIndex(Long transcriptId);

    @Query(value = "SELECT * FROM transcript_chunks WHERE embedding IS NOT NULL AND :embedding = :embedding LIMIT :limit", nativeQuery = true)
    List<TranscriptChunk> findSimilarChunks(@Param("embedding") String embedding, @Param("limit") int limit);
}

