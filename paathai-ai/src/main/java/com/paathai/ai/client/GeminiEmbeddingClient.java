package com.paathai.ai.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Client for Gemini Embedding API.
 * Generates 768-dimensional vector embeddings for text content.
 */
@Component
public class GeminiEmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingClient.class);
    private static final String API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String EMBEDDING_MODEL = "text-embedding-004";
    private static final int EMBEDDING_DIMENSION = 768;

    private final RestTemplate restTemplate;

    @Value("${paathai.ai.gemini.api-key:}")
    private String apiKey;

    public GeminiEmbeddingClient(@Qualifier("aiRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Generate an embedding vector for the given text.
     *
     * @param text The text to embed
     * @return 768-dimensional float array
     */
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[EMBEDDING_DIMENSION];
        }

        try {
            String url = API_BASE + EMBEDDING_MODEL + ":embedContent?key=" + apiKey;

            Map<String, Object> request = Map.of(
                "model", "models/" + EMBEDDING_MODEL,
                "content", Map.of("parts", List.of(Map.of("text", text)))
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            return extractEmbedding(response);

        } catch (Exception e) {
            log.error("Embedding generation failed: {}", e.getMessage());
            // Return zero vector on failure — not ideal but prevents pipeline failure
            return new float[EMBEDDING_DIMENSION];
        }
    }

    /**
     * Convert a float array to the pgvector string format: [0.1,0.2,...].
     */
    public static String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private float[] extractEmbedding(Map<String, Object> response) {
        if (response == null) {
            log.warn("Null response from embedding API");
            return new float[EMBEDDING_DIMENSION];
        }

        try {
            Map<String, Object> embeddingObj = (Map<String, Object>) response.get("embedding");
            if (embeddingObj == null) {
                log.warn("No embedding field in response");
                return new float[EMBEDDING_DIMENSION];
            }

            List<Number> values = (List<Number>) embeddingObj.get("values");
            if (values == null) {
                log.warn("No values in embedding");
                return new float[EMBEDDING_DIMENSION];
            }

            float[] result = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                result[i] = values.get(i).floatValue();
            }

            log.debug("Generated embedding with {} dimensions", result.length);
            return result;

        } catch (Exception e) {
            log.error("Failed to parse embedding response: {}", e.getMessage());
            return new float[EMBEDDING_DIMENSION];
        }
    }
}
