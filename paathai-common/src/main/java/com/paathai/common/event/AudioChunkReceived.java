package com.paathai.common.event;

/**
 * Published when a raw audio chunk arrives via WebSocket.
 * Consumed by: LiveTranscriptionService.
 */
public class AudioChunkReceived extends BaseEvent {

    private final Long sessionId;
    private final int sequenceNumber;
    private final String audioPath;
    private final int sizeBytes;

    public AudioChunkReceived(Long sessionId, int sequenceNumber, String audioPath, int sizeBytes) {
        super("LIVE");
        this.sessionId = sessionId;
        this.sequenceNumber = sequenceNumber;
        this.audioPath = audioPath;
        this.sizeBytes = sizeBytes;
    }

    public Long getSessionId() { return sessionId; }
    public int getSequenceNumber() { return sequenceNumber; }
    public String getAudioPath() { return audioPath; }
    public int getSizeBytes() { return sizeBytes; }
}
