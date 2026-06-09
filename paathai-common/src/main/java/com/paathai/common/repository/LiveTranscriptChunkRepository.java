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

    /** Find chunks with embeddings for live search (in-memory fallback for H2). */
    default List<LiveTranscriptChunk> findSimilarChunksBySessionId(
            Long sessionId,
            String embedding,
            int limit) {
        float[] targetVector = com.paathai.common.util.VectorUtils.parseEmbedding(embedding);
        return findBySessionIdOrderBySequenceNumber(sessionId).stream()
                .filter(c -> c.getEmbedding() != null && !c.getEmbedding().isEmpty())
                .sorted((a, b) -> {
                    double simA = com.paathai.common.util.VectorUtils.cosineSimilarity(
                            com.paathai.common.util.VectorUtils.parseEmbedding(a.getEmbedding()), targetVector);
                    double simB = com.paathai.common.util.VectorUtils.cosineSimilarity(
                            com.paathai.common.util.VectorUtils.parseEmbedding(b.getEmbedding()), targetVector);
                    return Double.compare(simB, simA);
                })
                .limit(limit)
                .toList();
    }
}
