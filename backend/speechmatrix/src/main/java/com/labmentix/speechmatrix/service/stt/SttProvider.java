package com.labmentix.speechmatrix.service.stt;

import java.io.File;

/**
 * Common interface for all Speech-to-Text providers.
 *
 * Every provider (Deepgram, AssemblyAI, Google, Mock) implements this.
 * TranscriptionService depends only on this interface â€” never on a
 * concrete provider â€” so switching providers requires only a config change.
 *
 * Request-response model:
 *   SttRequest  â†’ provider â†’ HTTP call â†’ SttResult
 *
 * Usage:
 *   SttResult result = sttProvider.transcribe(
 *       SttRequest.builder()
 *           .audioFile(file)
 *           .language("en-US")
 *           .build());
 */
public interface SttProvider {

    /**
     * Transcribe an audio file.
     * Implementations must be thread-safe â€” Spring manages them as singletons.
     *
     * @param request  audio file + transcription options
     * @return result  transcript text, confidence, words, metadata
     * @throws SttException if the provider returns an error or times out
     */
    SttResult transcribe(SttRequest request);

    /**
     * Transcribe raw audio bytes (e.g. from microphone recording).
     *
     * @param audioBytes  raw PCM / WebM / OGG bytes from the browser
     * @param request     transcription options (language, speaker count etc.)
     */
    SttResult transcribeBytes(byte[] audioBytes, SttRequest request);

    /**
     * Test connectivity to the provider.
     * Used by GET /api/dev/stt/ping â€” sends a minimal API call and
     * returns how long it took.
     */
    PingResult ping();

    /** Human-readable provider name, e.g. "Deepgram nova-2" */
    String getProviderName();
}
