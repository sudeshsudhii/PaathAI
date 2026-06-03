package com.paathai.rag.live;

import com.paathai.ai.client.GeminiEmbeddingClient;
import com.paathai.common.entity.LiveTranscriptChunk;
import com.paathai.common.event.LiveTranscriptChunkGenerated;
import com.paathai.common.repository.LiveTranscriptChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Indexes live transcript chunks by generating embeddings immediately,
 * enabling real-time semantic search during an active session.
 *
 * <p>Listens to {@link LiveTranscriptChunkGenerated} and updates the
 * {@code embedding} column on the {@code live_transcript_chunks} table.</p>
 */
@Service
public class LiveSearchIndexingService {

    private static final Logger log = LoggerFactory.getLogger(LiveSearchIndexingService.class);

    private final LiveTranscriptChunkRepository chunkRepository;
    private final GeminiEmbeddingClient embeddingClient;

    public LiveSearchIndexingService(LiveTranscriptChunkRepository chunkRepository,
                                     GeminiEmbeddingClient embeddingClient) {
        this.chunkRepository = chunkRepository;
        this.embeddingClient = embeddingClient;
    }

    @EventListener
    @Async("searchIndexExecutor")
    public void onTranscriptChunkGenerated(LiveTranscriptChunkGenerated event) {
        Long sessionId = event.getSessionId();
        int seq = event.getSequenceNumber();
        String text = event.getText();

        if (text == null || text.isBlank()) {
            return;
        }

        try {
            // Generate embedding
            float[] embedding = embeddingClient.embed(text);
            String vectorString = GeminiEmbeddingClient.toVectorString(embedding);

            // Find and update the chunk
            chunkRepository.findBySessionIdAndStatusAndSequenceNumberLessThanEqual(
                    sessionId, "PROVISIONAL", seq)
                    .stream()
                    .filter(c -> c.getSequenceNumber().equals(seq))
                    .findFirst()
                    .ifPresentOrElse(chunk -> {
                        chunk.setEmbedding(vectorString);
                        chunkRepository.save(chunk);
                        log.debug("Indexed live chunk: session={}, seq={}", sessionId, seq);
                    }, () -> {
                        // Fallback: try finding by session + seq regardless of status
                        chunkRepository.findBySessionIdOrderBySequenceNumber(sessionId)
                                .stream()
                                .filter(c -> c.getSequenceNumber().equals(seq))
                                .findFirst()
                                .ifPresent(chunk -> {
                                    chunk.setEmbedding(vectorString);
                                    chunkRepository.save(chunk);
                                    log.debug("Indexed live chunk (fallback): session={}, seq={}", sessionId, seq);
                                });
                    });

        } catch (Exception e) {
            log.error("Failed to index live chunk session={}, seq={}: {}", sessionId, seq, e.getMessage());
        }
    }
}
