package com.paathai.study.live;

import com.paathai.ai.costcontroller.AICostController;
import com.paathai.ai.costcontroller.LlmResponse;
import com.paathai.common.dto.FeatureType;
import com.paathai.common.entity.LiveNoteRevision;
import com.paathai.common.entity.LiveSession;
import com.paathai.common.entity.LiveTopicTimeline;
import com.paathai.common.entity.LiveTranscriptChunk;
import com.paathai.common.event.LiveNotesUpdated;
import com.paathai.common.event.LiveTranscriptChunkGenerated;
import com.paathai.common.repository.LiveNoteRevisionRepository;
import com.paathai.common.repository.LiveSessionRepository;
import com.paathai.common.repository.LiveTopicTimelineRepository;
import com.paathai.common.repository.LiveTranscriptChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Generates live notes independently from transcript chunks.
 *
 * <p>Decoupled from topic detection — listens directly to
 * {@link LiveTranscriptChunkGenerated}. If a recent topic exists it is used
 * as context; if topic detection has failed or hasn't run yet, notes are
 * generated anyway with a generic context label.</p>
 *
 * <p>Uses a multi-condition buffer to batch chunks before calling the LLM:
 * <ul>
 *   <li>≥5 chunks accumulated</li>
 *   <li>≥1000 estimated tokens accumulated</li>
 *   <li>≥20 seconds since the first buffered chunk</li>
 * </ul>
 * Whichever fires first triggers notes generation.</p>
 */
@Service
public class LiveNotesGenerationService {

    private static final Logger log = LoggerFactory.getLogger(LiveNotesGenerationService.class);

    private static final int CHUNK_THRESHOLD = 5;
    private static final int TOKEN_THRESHOLD = 1000;
    private static final long TIME_THRESHOLD_MS = 20_000;

    private final LiveNoteRevisionRepository noteRevisionRepository;
    private final LiveTranscriptChunkRepository transcriptChunkRepository;
    private final LiveTopicTimelineRepository topicTimelineRepository;
    private final LiveSessionRepository sessionRepository;
    private final AICostController aiCostController;
    private final ApplicationEventPublisher eventPublisher;

    /** Per-session chunk buffer. */
    private final Map<Long, NotesChunkBuffer> sessionBuffers = new ConcurrentHashMap<>();

    public LiveNotesGenerationService(LiveNoteRevisionRepository noteRevisionRepository,
                                      LiveTranscriptChunkRepository transcriptChunkRepository,
                                      LiveTopicTimelineRepository topicTimelineRepository,
                                      LiveSessionRepository sessionRepository,
                                      AICostController aiCostController,
                                      ApplicationEventPublisher eventPublisher) {
        this.noteRevisionRepository = noteRevisionRepository;
        this.transcriptChunkRepository = transcriptChunkRepository;
        this.topicTimelineRepository = topicTimelineRepository;
        this.sessionRepository = sessionRepository;
        this.aiCostController = aiCostController;
        this.eventPublisher = eventPublisher;
    }

    // ------------------------------------------------------------------ //
    //  Event Listener — fan-out from LiveTranscriptChunkGenerated
    // ------------------------------------------------------------------ //

    @EventListener
    @Async("notesGenerationExecutor")
    public void onTranscriptChunkGenerated(LiveTranscriptChunkGenerated event) {
        Long sessionId = event.getSessionId();

        NotesChunkBuffer buffer = sessionBuffers.computeIfAbsent(sessionId, k -> new NotesChunkBuffer());

        int estimatedTokens = (int) Math.ceil(event.getText().length() / 4.0);

        synchronized (buffer) {
            buffer.texts.add(event.getText());
            buffer.accumulatedTokens += estimatedTokens;
            if (buffer.firstChunkTime == null) {
                buffer.firstChunkTime = Instant.now();
            }

            boolean chunkTrigger = buffer.texts.size() >= CHUNK_THRESHOLD;
            boolean tokenTrigger = buffer.accumulatedTokens >= TOKEN_THRESHOLD;
            boolean timeTrigger = Instant.now().toEpochMilli() - buffer.firstChunkTime.toEpochMilli() >= TIME_THRESHOLD_MS;

            if (!chunkTrigger && !tokenTrigger && !timeTrigger) {
                return; // Not ready yet
            }

            // Flush buffer
            List<String> flushedTexts = new ArrayList<>(buffer.texts);
            buffer.texts.clear();
            buffer.accumulatedTokens = 0;
            buffer.firstChunkTime = null;

            // Run generation outside synchronized block
            generateNotes(sessionId, flushedTexts);
        }
    }

    // ------------------------------------------------------------------ //
    //  Notes Generation Logic
    // ------------------------------------------------------------------ //

    private void generateNotes(Long sessionId, List<String> bufferedTexts) {
        log.info("Generating live notes for session {} ({} buffered chunks)", sessionId, bufferedTexts.size());

        try {
            // Get current latest revision
            LiveNoteRevision lastRevision = noteRevisionRepository
                    .findFirstBySessionIdOrderByRevisionNumberDesc(sessionId)
                    .orElse(null);

            int newRevisionNumber = lastRevision != null ? lastRevision.getRevisionNumber() + 1 : 1;
            String currentNotes = lastRevision != null ? lastRevision.getContent() : "";

            // Best-effort topic context — independent of topic detection success
            String currentTopicLabel = resolveCurrentTopic(sessionId);

            // Get recent transcript for broader context (last 20 chunks from DB)
            List<LiveTranscriptChunk> recentChunks = transcriptChunkRepository
                    .findTop20BySessionIdOrderBySequenceNumberDesc(sessionId);

            String recentTranscript = recentChunks.stream()
                    .sorted((a, b) -> a.getSequenceNumber().compareTo(b.getSequenceNumber()))
                    .map(LiveTranscriptChunk::getContent)
                    .collect(Collectors.joining(" "));

            Long userId = sessionRepository.findById(sessionId)
                    .map(LiveSession::getUserId)
                    .orElse(1L);

            String prompt = String.format("""
                You are a live note-taker for an ongoing lecture. Update the existing notes with new content \
                from the recent transcript segment. Do NOT regenerate existing sections.

                Current topic: %s
                Current notes so far:
                %s

                New transcript content:
                %s

                Provide the UPDATED full notes document in Markdown format.
                """,
                    currentTopicLabel,
                    currentNotes.isEmpty() ? "(No notes yet)" : currentNotes,
                    recentTranscript);

            LlmResponse response = aiCostController.execute(prompt, FeatureType.LIVE_NOTES_UPDATE, userId);
            String newNotes = response.content();

            // Simple delta calculation
            String delta = newNotes.length() > currentNotes.length()
                    ? newNotes.substring(Math.min(newNotes.length(), currentNotes.length()))
                    : "";

            LiveNoteRevision revision = new LiveNoteRevision();
            revision.setSessionId(sessionId);
            revision.setRevisionNumber(newRevisionNumber);
            revision.setContent(newNotes);
            revision.setDeltaContent(delta);
            revision.setTriggerTopic(currentTopicLabel);
            revision.setEstimatedTokens(response.totalTokens());

            noteRevisionRepository.save(revision);

            eventPublisher.publishEvent(new LiveNotesUpdated(
                    sessionId, newRevisionNumber, newNotes, delta));

        } catch (Exception e) {
            log.error("Live notes generation failed for session {}", sessionId, e);
        }
    }

    /**
     * Best-effort lookup of the most recent detected topic for this session.
     * Returns "General Lecture" if topic detection hasn't produced anything yet.
     */
    private String resolveCurrentTopic(Long sessionId) {
        try {
            List<LiveTopicTimeline> topics = topicTimelineRepository
                    .findBySessionIdOrderByDetectedAtMs(sessionId);
            if (!topics.isEmpty()) {
                LiveTopicTimeline latest = topics.get(topics.size() - 1);
                String label = latest.getTopic();
                if (latest.getSubtopic() != null && !latest.getSubtopic().isEmpty()) {
                    label += " - " + latest.getSubtopic();
                }
                return label;
            }
        } catch (Exception e) {
            log.warn("Could not resolve topic for session {}: {}", sessionId, e.getMessage());
        }
        return "General Lecture";
    }

    // ------------------------------------------------------------------ //
    //  Buffer
    // ------------------------------------------------------------------ //

    private static class NotesChunkBuffer {
        final List<String> texts = new ArrayList<>();
        int accumulatedTokens = 0;
        Instant firstChunkTime = null;
    }
}
