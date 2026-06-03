package com.paathai.common.repository;

import com.paathai.common.entity.LiveTranscriptChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface LiveTranscriptChunkRepository extends JpaRepository<LiveTranscriptChunk, Long> {

    List<LiveTranscriptChunk> findBySessionIdOrderBySequenceNumber(Long sessionId);

    List<LiveTranscriptChunk> findTop20BySessionIdOrderBySequenceNumberDesc(Long sessionId);

    /** Find PROVISIONAL chunks at or before a given sequence number (for promotion). */
    List<LiveTranscriptChunk> findBySessionIdAndStatusAndSequenceNumberLessThanEqual(
            Long sessionId, String status, Integer sequenceNumber);

    /** Bulk-promote all non-FINAL chunks to FINAL on session end. */
    @Modifying
    @Transactional
    @Query("UPDATE LiveTranscriptChunk c SET c.status = 'FINAL' WHERE c.sessionId = :sessionId AND c.status <> 'FINAL'")
    int finalizeAllChunks(@Param("sessionId") Long sessionId);

    /** Find chunks with embeddings for live search (native query for pgvector). */
    @Query(value = "SELECT * FROM live_transcript_chunks " +
            "WHERE session_id = :sessionId AND embedding IS NOT NULL " +
            "ORDER BY embedding <=> CAST(:embedding AS vector) LIMIT :limit",
            nativeQuery = true)
    List<LiveTranscriptChunk> findSimilarChunksBySessionId(
            @Param("sessionId") Long sessionId,
            @Param("embedding") String embedding,
            @Param("limit") int limit);
}
