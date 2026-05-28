package com.paathai.lecture.service;

/**
 * Interface for the transcription service.
 * MVP implementation: Whisper ASR via HTTP.
 * Can be swapped to Google Cloud Speech-to-Text or AssemblyAI.
 */
public interface TranscriptionService {

    /**
     * Transcribe an audio file asynchronously.
     * Results are published as TranscriptChunkCreated events during processing,
     * and a TranscriptCompleted event when finished.
     *
     * @param lectureId The lecture being transcribed
     * @param audioPath Path to the audio file
     */
    void transcribeAsync(Long lectureId, String audioPath);

    /**
     * Get the status of an ongoing transcription.
     *
     * @param lectureId The lecture being transcribed
     * @return Current transcription status
     */
    TranscriptionStatus getStatus(Long lectureId);

    enum TranscriptionStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
}
