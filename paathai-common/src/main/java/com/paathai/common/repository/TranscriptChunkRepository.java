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

    default List<TranscriptChunk> findSimilarChunks(String embedding, int limit) {
        float[] targetVector = com.paathai.common.util.VectorUtils.parseEmbedding(embedding);
        return findAll().stream()
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

