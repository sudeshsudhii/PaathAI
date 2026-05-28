package com.paathai.ai.prompts;

import com.paathai.common.dto.PromptType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages versioned prompt templates.
 *
 * Rules:
 * 1. No hardcoded prompts in any @Service class
 * 2. Load prompts dynamically from the filesystem
 * 3. Version prompts with monotonically increasing versions
 * 4. Track prompt performance via prompt_metrics table
 * 5. Support rollback to any previous version
 */
@Service
public class PromptRegistry {

    private static final Logger log = LoggerFactory.getLogger(PromptRegistry.class);

    @Value("${paathai.prompts.base-path:prompts}")
    private String basePath;

    // Maps PromptType → active version number
    private final Map<PromptType, Integer> activeVersions = new ConcurrentHashMap<>();

    // Cache: "TYPE:VERSION" → prompt content
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Default all prompt types to v1
        for (PromptType type : PromptType.values()) {
            activeVersions.put(type, 1);
        }
        log.info("PromptRegistry initialized with base path: {}", basePath);
    }

    /**
     * Get the currently active prompt for a given type.
     */
    public String getActivePrompt(PromptType type) {
        int version = activeVersions.getOrDefault(type, 1);
        return getPromptVersion(type, version);
    }

    /**
     * Get a specific version of a prompt.
     */
    public String getPromptVersion(PromptType type, int version) {
        String cacheKey = type.name() + ":" + version;
        return promptCache.computeIfAbsent(cacheKey, key -> loadPromptFromDisk(type, version));
    }

    /**
     * Activate a specific version for a prompt type.
     */
    public void activateVersion(PromptType type, int version) {
        // Verify the version file exists
        String content = loadPromptFromDisk(type, version);
        if (content == null) {
            throw new IllegalArgumentException(
                String.format("Prompt version %d not found for %s", version, type));
        }
        activeVersions.put(type, version);
        log.info("Activated prompt version {} for {}", version, type);
    }

    /**
     * Get the currently active version number for a prompt type.
     */
    public int getActiveVersion(PromptType type) {
        return activeVersions.getOrDefault(type, 1);
    }

    private String loadPromptFromDisk(PromptType type, int version) {
        Path promptPath = Path.of(basePath, type.getDirectoryName(), "v" + version + ".md");
        try {
            if (Files.exists(promptPath)) {
                String content = Files.readString(promptPath);
                log.debug("Loaded prompt {} v{} ({} chars)", type, version, content.length());
                return content;
            } else {
                log.warn("Prompt file not found: {}", promptPath);
                return null;
            }
        } catch (IOException e) {
            log.error("Failed to load prompt from {}", promptPath, e);
            return null;
        }
    }
}
