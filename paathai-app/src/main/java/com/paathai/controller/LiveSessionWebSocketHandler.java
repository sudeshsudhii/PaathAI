package com.paathai.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paathai.common.event.LiveAcademicInsight;
import com.paathai.common.event.LiveNotesUpdated;
import com.paathai.common.event.LiveSyllabusMapped;
import com.paathai.common.event.LiveTopicDetected;
import com.paathai.common.event.LiveTranscriptChunkConfirmed;
import com.paathai.common.event.LiveTranscriptChunkGenerated;
import com.paathai.lecture.service.AudioChunkService;
import com.paathai.lecture.service.LiveSessionService;
import com.paathai.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles live lecture recording WebSocket connections.
 * Receives binary audio chunks and text control commands.
 * Sends JSON events (transcripts, notes, topics) to the client.
 */
@Component
public class LiveSessionWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LiveSessionWebSocketHandler.class);
    
    private final LiveSessionService liveSessionService;
    private final AudioChunkService audioChunkService;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    
    // Mapping internal WS session to business entities
    private final Map<String, Long> wsSessionToUserId = new ConcurrentHashMap<>();
    private final Map<String, Long> wsSessionToLiveSessionId = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> sessionSequenceMap = new ConcurrentHashMap<>();
    
    private final Map<Long, WebSocketSession> activeLiveSessions = new ConcurrentHashMap<>();

    public LiveSessionWebSocketHandler(LiveSessionService liveSessionService,
                                       AudioChunkService audioChunkService,
                                       JwtTokenProvider jwtTokenProvider,
                                       ObjectMapper objectMapper) {
        this.liveSessionService = liveSessionService;
        this.audioChunkService = audioChunkService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.objectMapper = objectMapper;
    }

    public void sendMessageToSession(Long liveSessionId, String message) {
        WebSocketSession session = activeLiveSessions.get(liveSessionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (Exception e) {
                log.error("Failed to send message to live session {}", liveSessionId, e);
            }
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        String query = uri != null ? uri.getQuery() : "";
        String token = extractTokenFromQuery(query);
        
        if (token != null && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            wsSessionToUserId.put(session.getId(), userId);
            log.info("WebSocket connected: session {} for user {}", session.getId(), userId);
        } else {
            log.warn("WebSocket unauthorized connection attempt: session {}", session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = wsSessionToUserId.get(session.getId());
        if (userId == null) {
            return;
        }

        JsonNode node = objectMapper.readTree(message.getPayload());
        String action = node.has("action") ? node.get("action").asText() : "";
        
        switch (action) {
            case "START":
                String title = node.has("title") ? node.get("title").asText() : "Live Session";
                Long courseId = node.has("courseId") && !node.get("courseId").isNull() ? node.get("courseId").asLong() : null;
                Long sessionId = liveSessionService.startSession(userId, title, courseId);
                wsSessionToLiveSessionId.put(session.getId(), sessionId);
                sessionSequenceMap.put(session.getId(), new AtomicInteger(0));
                activeLiveSessions.put(sessionId, session);
                
                log.info("Started live session {} for user {}", sessionId, userId);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                        Map.of("type", "session_status", "data", Map.of("status", "RECORDING"))
                )));
                break;
                
            case "PAUSE":
                if (wsSessionToLiveSessionId.containsKey(session.getId())) {
                    liveSessionService.pauseSession(wsSessionToLiveSessionId.get(session.getId()));
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                            Map.of("type", "session_status", "data", Map.of("status", "PAUSED"))
                    )));
                }
                break;
                
            case "RESUME":
                if (wsSessionToLiveSessionId.containsKey(session.getId())) {
                    liveSessionService.resumeSession(wsSessionToLiveSessionId.get(session.getId()));
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                            Map.of("type", "session_status", "data", Map.of("status", "RECORDING"))
                    )));
                }
                break;
                
            case "STOP":
                if (wsSessionToLiveSessionId.containsKey(session.getId())) {
                    liveSessionService.stopSession(wsSessionToLiveSessionId.get(session.getId()));
                    wsSessionToLiveSessionId.remove(session.getId());
                    sessionSequenceMap.remove(session.getId());
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                            Map.of("type", "session_status", "data", Map.of("status", "COMPLETED"))
                    )));
                }
                break;
                
            default:
                log.warn("Unknown WebSocket action: {}", action);
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        Long liveSessionId = wsSessionToLiveSessionId.get(session.getId());
        AtomicInteger sequenceGenerator = sessionSequenceMap.get(session.getId());
        
        if (liveSessionId != null && sequenceGenerator != null) {
            int seqNum = sequenceGenerator.getAndIncrement();
            
            ByteBuffer payload = message.getPayload();
            byte[] audioData = new byte[payload.remaining()];
            payload.get(audioData);
            
            audioChunkService.processChunk(liveSessionId, seqNum, audioData);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long liveSessionId = wsSessionToLiveSessionId.remove(session.getId());
        if (liveSessionId != null) {
            log.info("WebSocket disconnected, stopping active session {}", liveSessionId);
            liveSessionService.stopSession(liveSessionId);
            activeLiveSessions.remove(liveSessionId);
        }
        wsSessionToUserId.remove(session.getId());
        sessionSequenceMap.remove(session.getId());
        log.info("WebSocket closed: session {} with status {}", session.getId(), status);
    }
    
    @EventListener
    public void onLiveTranscriptChunkGenerated(LiveTranscriptChunkGenerated event) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "type", "transcript_chunk",
                    "data", Map.of(
                            "seq", event.getSequenceNumber(),
                            "text", event.getText(),
                            "startMs", event.getStartOffsetMs(),
                            "endMs", event.getEndOffsetMs()
                    )
            ));
            sendMessageToSession(event.getSessionId(), payload);
        } catch (Exception e) {
            log.error("Failed to serialize transcript chunk event", e);
        }
    }

    @EventListener
    public void onLiveTopicDetected(LiveTopicDetected event) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "type", "topic_update",
                    "data", Map.of(
                            "topic", event.getTopic(),
                            "subtopic", event.getSubtopic() != null ? event.getSubtopic() : "",
                            "confidence", event.getConfidence(),
                            "atMs", event.getDetectedAtMs()
                    )
            ));
            sendMessageToSession(event.getSessionId(), payload);
        } catch (Exception e) {
            log.error("Failed to serialize topic detected event", e);
        }
    }

    @EventListener
    public void onLiveNotesUpdated(LiveNotesUpdated event) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "type", "notes_update",
                    "data", Map.of(
                            "revision", event.getRevisionNumber(),
                            "content", event.getContent(),
                            "delta", event.getDeltaContent() != null ? event.getDeltaContent() : ""
                    )
            ));
            sendMessageToSession(event.getSessionId(), payload);
        } catch (Exception e) {
            log.error("Failed to serialize notes updated event", e);
        }
    }

    @EventListener
    public void onSyllabusMapped(LiveSyllabusMapped event) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "type", "syllabus_update",
                    "data", Map.of(
                            "topicName", event.getTopicName(),
                            "syllabusTopicId", event.getMatchedSyllabusTopicId(),
                            "syllabusPath", event.getSyllabusPath(),
                            "matchConfidence", event.getMatchConfidence()
                    )
            ));
            sendMessageToSession(event.getSessionId(), payload);
        } catch (Exception e) {
            log.error("Failed to serialize syllabus mapped event", e);
        }
    }

    @EventListener
    public void onAcademicInsight(LiveAcademicInsight event) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "type", "academic_insight",
                    "data", Map.of(
                            "currentTopic", event.getCurrentTopic(),
                            "currentSubtopic", event.getCurrentSubtopic() != null ? event.getCurrentSubtopic() : "",
                            "keyConcepts", event.getKeyConcepts(),
                            "definitions", event.getDefinitions(),
                            "prerequisites", event.getPrerequisites(),
                            "examImportance", event.getExamImportance(),
                            "coverageStatus", event.getCoverageStatus()
                    )
            ));
            sendMessageToSession(event.getSessionId(), payload);
        } catch (Exception e) {
            log.error("Failed to serialize academic insight event", e);
        }
    }

    @EventListener
    public void onTranscriptChunkConfirmed(LiveTranscriptChunkConfirmed event) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "type", "transcript_confirmed",
                    "data", Map.of(
                            "seq", event.getSequenceNumber(),
                            "text", event.getText(),
                            "startMs", event.getStartOffsetMs(),
                            "endMs", event.getEndOffsetMs()
                    )
            ));
            sendMessageToSession(event.getSessionId(), payload);
        } catch (Exception e) {
            log.error("Failed to serialize transcript confirmed event", e);
        }
    }
    
    private String extractTokenFromQuery(String query) {
        if (query == null || query.isEmpty()) return null;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                return keyValue[1];
            }
        }
        return null;
    }
}
