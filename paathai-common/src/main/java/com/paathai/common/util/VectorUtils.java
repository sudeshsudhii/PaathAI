package com.paathai.common.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VectorUtils {

    private static final Logger log = LoggerFactory.getLogger(VectorUtils.class);
    public static float[] parseEmbedding(String embeddingJson) {
        if (embeddingJson == null || embeddingJson.isBlank()) {
            return new float[0];
        }
        String clean = embeddingJson.replace("[", "").replace("]", "").trim();
        if (clean.isEmpty()) return new float[0];
        String[] parts = clean.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Float.parseFloat(parts[i].trim());
            } catch (NumberFormatException nfe) {
                result[i] = 0f;
            }
        }
        return result;
    }

    public static double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length == 0 || vectorB.length == 0 || vectorA.length != vectorB.length) {
            return -1.0; // Lowest similarity
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static double cosineSimilarity(String embeddingJsonA, String embeddingJsonB) {
        float[] vectorA = parseEmbedding(embeddingJsonA);
        float[] vectorB = parseEmbedding(embeddingJsonB);
        return cosineSimilarity(vectorA, vectorB);
    }
}
