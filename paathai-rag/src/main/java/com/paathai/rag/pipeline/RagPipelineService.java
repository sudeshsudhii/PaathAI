package com.paathai.rag.pipeline;

import com.paathai.ai.client.GeminiEmbeddingClient;
import com.paathai.ai.costcontroller.AICostController;
import com.paathai.ai.costcontroller.LlmResponse;
import com.paathai.common.dto.FeatureType;
import com.paathai.common.entity.LiveTranscriptChunk;
import com.paathai.common.entity.TranscriptChunk;
import com.paathai.common.repository.LiveTranscriptChunkRepository;
import com.paathai.common.repository.TranscriptChunkRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Full RAG pipeline: query → embed → search → context assembly → LLM generation.
 */
@Service
public class RagPipelineService {

    private static final Logger log = LoggerFactory.getLogger(RagPipelineService.class);
    private static final int MAX_RESULTS = 5;

    private final TranscriptChunkRepository chunkRepository;
    private final LiveTranscriptChunkRepository liveChunkRepository;
    private final GeminiEmbeddingClient embeddingClient;
    private final AICostController aiCostController;

    public RagPipelineService(TranscriptChunkRepository chunkRepository,
                               LiveTranscriptChunkRepository liveChunkRepository,
                               GeminiEmbeddingClient embeddingClient,
                               AICostController aiCostController) {
        this.chunkRepository = chunkRepository;
        this.liveChunkRepository = liveChunkRepository;
        this.embeddingClient = embeddingClient;
        this.aiCostController = aiCostController;
    }

    /**
     * Execute semantic search with RAG-augmented answer generation.
     */
    public SearchResponse search(String query, Long studentId) {
        log.info("RAG search for query: '{}' by student {}", query, studentId);

        // 1. Generate embedding for the query
        float[] queryEmbedding = embeddingClient.embed(query);
        String vectorString = GeminiEmbeddingClient.toVectorString(queryEmbedding);

        // 2. Find similar chunks via pgvector
        List<TranscriptChunk> similarChunks = chunkRepository.findSimilarChunks(vectorString, MAX_RESULTS);

        if (similarChunks.isEmpty()) {
            log.info("No similar chunks found for query: '{}'", query);
            return new SearchResponse(
                "I couldn't find any relevant lecture content for your question. " +
                "Try uploading a lecture first, or rephrase your question.",
                List.of()
            );
        }

        // 3. Assemble context from retrieved chunks
        String context = similarChunks.stream()
                .map(chunk -> String.format("[Chunk %d]: %s", chunk.getChunkIndex(), chunk.getContent()))
                .collect(Collectors.joining("\n\n"));

        // 4. Build the RAG prompt
        String ragPrompt = String.format("""
            You are a helpful study assistant for a university student. \
            Answer the question based on the lecture content provided below. \
            If the answer cannot be found in the context, say so clearly. \
            Always reference which parts of the lecture support your answer.

            === LECTURE CONTENT ===
            %s

            === STUDENT QUESTION ===
            %s

            Provide a clear, educational answer:
            """, context, query);

        // 5. Call LLM through the cost controller pipeline
        LlmResponse llmResponse = aiCostController.execute(ragPrompt, FeatureType.SEARCH, studentId);

        // 6. Build response with sources
        List<SourceChunk> sources = similarChunks.stream()
                .map(chunk -> new SourceChunk(
                    chunk.getContent(),
                    chunk.getChunkIndex(),
                    chunk.getTranscriptId()
                ))
                .toList();

        log.info("RAG search completed: {} sources, {} tokens used",
                 sources.size(), llmResponse.totalTokens());

        return new SearchResponse(llmResponse.content(), sources);
    }

    /**
     * Search within an active live session's transcript chunks using pgvector.
     */
    public SearchResponse searchLive(String query, Long sessionId, Long studentId) {
        log.info("Live RAG search for query: '{}' in session {} by student {}", query, sessionId, studentId);

        float[] queryEmbedding = embeddingClient.embed(query);
        String vectorString = GeminiEmbeddingClient.toVectorString(queryEmbedding);

        List<LiveTranscriptChunk> similarChunks = liveChunkRepository
                .findSimilarChunksBySessionId(sessionId, vectorString, MAX_RESULTS);

        if (similarChunks.isEmpty()) {
            return new SearchResponse(
                "No matching content found in this live session yet. " +
                "The lecture needs more content to search effectively.",
                List.of()
            );
        }

        String context = similarChunks.stream()
                .map(chunk -> String.format("[%s - seq %d]: %s",
                        formatOffset(chunk.getStartOffsetMs()),
                        chunk.getSequenceNumber(),
                        chunk.getContent()))
                .collect(Collectors.joining("\n\n"));

        String ragPrompt = String.format("""
            You are a helpful study assistant. Answer the question based on the \
            LIVE lecture transcript below. Reference timestamps when possible.

            === LIVE LECTURE TRANSCRIPT ===
            %s

            === STUDENT QUESTION ===
            %s

            Provide a concise answer:
            """, context, query);

        LlmResponse llmResponse = aiCostController.execute(ragPrompt, FeatureType.SEARCH, studentId);

        List<SourceChunk> sources = similarChunks.stream()
                .map(chunk -> new SourceChunk(
                    chunk.getContent(),
                    chunk.getSequenceNumber(),
                    sessionId // Using sessionId as the source identifier for live
                ))
                .toList();

        return new SearchResponse(llmResponse.content(), sources);
    }

    private static String formatOffset(Integer offsetMs) {
        if (offsetMs == null) return "00:00";
        int totalSeconds = offsetMs / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Data
    @AllArgsConstructor
    public static class SearchResponse {
        private String answer;
        private List<SourceChunk> sources;
    }

    @Data
    @AllArgsConstructor
    public static class SourceChunk {
        private String text;
        private int chunkIndex;
        private Long transcriptId;
    }
}
