package com.paathai.ai.client;

import com.paathai.ai.costcontroller.LlmClient;
import com.paathai.ai.costcontroller.LlmResponse;
import com.paathai.common.dto.FeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Gemini LLM client implementing the LlmClient interface.
 * Calls Google's Generative Language API for text generation.
 */
@Component
public class GeminiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmClient.class);
    private static final String API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final RestTemplate restTemplate;

    @Value("${paathai.ai.gemini.api-key:}")
    private String apiKey;

    @Value("${paathai.ai.gemini.model-flash:gemini-2.0-flash}")
    private String modelFlash;

    public GeminiLlmClient(@Qualifier("aiRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public LlmResponse execute(String prompt, FeatureType featureType) {
        long startTime = System.currentTimeMillis();
        String model = modelFlash;

        try {
            String url = API_BASE + model + ":generateContent?key=" + apiKey;

            Map<String, Object> request = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(Map.of("text", prompt)))
                )
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            String content = extractTextFromResponse(response);
            long latency = System.currentTimeMillis() - startTime;

            // Extract token counts from usageMetadata if available
            int inputTokens = extractTokenCount(response, "promptTokenCount");
            int outputTokens = extractTokenCount(response, "candidatesTokenCount");

            log.info("Gemini {} responded in {}ms ({} input, {} output tokens)",
                     model, latency, inputTokens, outputTokens);

            return new LlmResponse(content, inputTokens, outputTokens, model, latency);

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("Gemini API call failed after {}ms: {}", latency, e.getMessage());
            return new LlmResponse(
                "Error: Unable to generate response. " + e.getMessage(),
                estimateTokens(prompt), 0, model, latency
            );
        }
    }

    @Override
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) Math.ceil(text.length() / 4.0);
    }

    @Override
    public String getModelName() {
        return modelFlash;
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> response) {
        if (response == null) return "No response from Gemini";

        try {
            List<Map<String, Object>> candidates =
                    (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) return "No candidates in response";

            Map<String, Object> firstCandidate = candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
            if (content == null) return "No content in candidate";

            List<Map<String, Object>> parts =
                    (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) return "No parts in content";

            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            log.warn("Failed to parse Gemini response: {}", e.getMessage());
            return "Failed to parse response";
        }
    }

    @SuppressWarnings("unchecked")
    private int extractTokenCount(Map<String, Object> response, String field) {
        try {
            if (response == null) return 0;
            Map<String, Object> usage = (Map<String, Object>) response.get("usageMetadata");
            if (usage == null) return 0;
            Object count = usage.get(field);
            return count instanceof Number ? ((Number) count).intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
