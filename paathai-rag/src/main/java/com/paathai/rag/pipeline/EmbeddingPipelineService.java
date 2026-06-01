package com.paathai.rag.pipeline;

import com.paathai.ai.client.GeminiEmbeddingClient;
import com.paathai.common.event.TranscriptCompleted;
import com.paathai.common.entity.Transcript;
import com.paathai.common.entity.TranscriptChunk;
import com.paathai.rag.chunking.ChunkingStrategy;
import com.paathai.rag.chunking.ChunkingStrategy.ChunkingConfig;
import com.paathai.rag.chunking.ChunkingStrategy.TextChunk;
import com.paathai.common.repository.TranscriptChunkRepository;
import com.paathai.common.repository.TranscriptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates the embedding pipeline: chunk transcript → generate embeddings → store.
 * Triggered by TranscriptCompleted events.
 */
@Service
public class EmbeddingPipelineService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingPipelineService.class);

    private final TranscriptRepository transcriptRepository;
    private final TranscriptChunkRepository chunkRepository;
    private final ChunkingStrategy chunkingStrategy;
    private final GeminiEmbeddingClient embeddingClient;

    public EmbeddingPipelineService(TranscriptRepository transcriptRepository,
                                     TranscriptChunkRepository chunkRepository,
                                     ChunkingStrategy chunkingStrategy,
                                     GeminiEmbeddingClient embeddingClient) {
        this.transcriptRepository = transcriptRepository;
        this.chunkRepository = chunkRepository;
        this.chunkingStrategy = chunkingStrategy;
        this.embeddingClient = embeddingClient;
    }

    @EventListener
    @Async("searchIndexExecutor")
    public void onTranscriptCompleted(TranscriptCompleted event) {
        log.info("Embedding pipeline started for lecture {}, transcript {}",
                 event.getLectureId(), event.getTranscriptId());

        try {
            processTranscript(event.getTranscriptId());
        } catch (Exception e) {
            log.error("Embedding pipeline failed for transcript {}: {}",
                      event.getTranscriptId(), e.getMessage(), e);
        }
    }

    /**
     * Chunk transcript and generate embeddings for each chunk.
     */
    public void processTranscript(Long transcriptId) {
        Transcript transcript = transcriptRepository.findById(transcriptId)
                .orElseThrow(() -> new RuntimeException("Transcript not found: " + transcriptId));

        // 1. Chunk the transcript
        ChunkingConfig config = ChunkingConfig.defaults();
        List<TextChunk> chunks = chunkingStrategy.chunk(transcript.getFullText(), config);

        log.info("Chunked transcript {} into {} chunks", transcriptId, chunks.size());

        // 2. For each chunk, generate embedding and store
        for (int i = 0; i < chunks.size(); i++) {
            TextChunk textChunk = chunks.get(i);

            TranscriptChunk entity = new TranscriptChunk();
            entity.setTranscriptId(transcriptId);
            entity.setChunkIndex(i);
            entity.setContent(textChunk.content());
            entity.setStartOffsetMs(textChunk.startOffset());
            entity.setEndOffsetMs(textChunk.endOffset());
            entity.setEstimatedTokens(textChunk.estimatedTokens());

            // Generate embedding
            try {
                float[] embedding = embeddingClient.embed(textChunk.content());
                entity.setEmbedding(GeminiEmbeddingClient.toVectorString(embedding));
                log.debug("Generated embedding for chunk {} ({} tokens)", i, textChunk.estimatedTokens());
            } catch (Exception e) {
                log.warn("Failed to generate embedding for chunk {}: {}", i, e.getMessage());
                // Store chunk without embedding — it won't appear in vector searches
                // but the text is preserved
            }

            chunkRepository.save(entity);
        }

        log.info("Embedding pipeline completed for transcript {}: {} chunks stored",
                 transcriptId, chunks.size());
    }
}

