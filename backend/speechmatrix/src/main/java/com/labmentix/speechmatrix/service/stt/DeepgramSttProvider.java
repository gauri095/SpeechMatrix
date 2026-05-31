package com.labmentix.speechmatrix.service.stt;

import com.labmentix.speechmatrix.config.SttProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Deepgram Speech-to-Text provider.
 *
 * API: https://developers.deepgram.com/docs/getting-started-with-pre-recorded-audio
 *
 * Deepgram API flow:
 *   POST https://api.deepgram.com/v1/listen
 *   Authorization: Token <api-key>
 *   Content-Type: audio/mpeg  (or audio/wav, audio/webm, etc.)
 *   Body: raw audio bytes
 *   â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
 *   Response 200: JSON with transcript, confidence, words, duration
 *   Response 4xx: error JSON
 *
 * Query parameters used:
 *   ?model=nova-2        highest accuracy general model
 *   &language=en-US      BCP-47 language code
 *   &punctuate=true      auto-add punctuation
 *   &diarize=true        label speakers (SPEAKER_0, SPEAKER_1)
 *   &words=true          include word-level timestamps
 */
@Component("deepgramSttProvider")
@RequiredArgsConstructor
@Slf4j
public class DeepgramSttProvider implements SttProvider {

    private final SttProperties props;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    // â??â?? transcribe(File) â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    @Override
    public SttResult transcribe(SttRequest request) {
        if (request.getAudioUrl() != null) {
            return transcribeUrl(request);
        }
        if (request.getAudioFile() != null) {
            return transcribeFile(request);
        }
        if (request.getAudioBytes() != null) {
            return transcribeBytes(request.getAudioBytes(), request);
        }
        throw new SttException("SttRequest must have audioFile, audioBytes, or audioUrl");
    }

    // â??â?? transcribeBytes(byte[]) â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    @Override
    public SttResult transcribeBytes(byte[] audioBytes, SttRequest request) {
        log.debug("Deepgram transcribeBytes: {} bytes, lang={}", audioBytes.length, request.getLanguage());

        String url = buildUrl(request);
        String mimeType = request.getMimeType() != null ? request.getMimeType() : "audio/wav";

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Token " + props.getDeepgram().getApiKey())
                .header("Content-Type", mimeType)
                .POST(HttpRequest.BodyPublishers.ofByteArray(audioBytes))
                .timeout(Duration.ofSeconds(120))
                .build();

        return sendAndParse(httpRequest);
    }

    // â??â?? ping â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    @Override
    public PingResult ping() {
        if (!props.getDeepgram().isConfigured()) {
            return PingResult.fail("deepgram", "API key not configured");
        }

        // Use Deepgram's public sample file for the ping â?? tiny, fast
        SttRequest pingRequest = SttRequest.builder()
                .audioUrl("https://static.deepgram.com/examples/Bueller-Life-moves-pretty-fast.wav")
                .language("en-US")
                .includeWords(false)
                .punctuate(false)
                .build();

        long start = System.currentTimeMillis();
        try {
            SttResult result = transcribeUrl(pingRequest);
            long elapsed = System.currentTimeMillis() - start;

            return PingResult.ok("deepgram", props.getDeepgram().getModel(), elapsed);
        } catch (Exception e) {
            return PingResult.fail("deepgram", "Ping failed: " + e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "Deepgram " + props.getDeepgram().getModel();
    }

    // â”€â”€ Private: transcribe from URL (testing + ping) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private SttResult transcribeUrl(SttRequest request) {
        log.debug("Deepgram transcribeUrl: {}", request.getAudioUrl());

        String url     = buildUrl(request);
        String jsonBody = "{\"url\":\"" + request.getAudioUrl() + "\"}";

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Token " + props.getDeepgram().getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(60))
                .build();

        return sendAndParse(httpRequest);
    }

    // â”€â”€ Private: transcribe from File â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private SttResult transcribeFile(SttRequest request) {
        File file = request.getAudioFile();
        log.debug("Deepgram transcribeFile: {} ({} bytes)", file.getName(), file.length());

        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String mime  = detectMimeType(file.getName());

            SttRequest bytesRequest = SttRequest.builder()
                    .language(request.getLanguage())
                    .speakerCount(request.getSpeakerCount())
                    .includeWords(request.isIncludeWords())
                    .punctuate(request.isPunctuate())
                    .diarize(request.isDiarize())
                    .mimeType(mime)
                    .build();

            return transcribeBytes(bytes, bytesRequest);

        } catch (IOException e) {
            throw new SttException("Failed to read audio file: " + e.getMessage(), e);
        }
    }

    // â”€â”€ Private: send HTTP request + parse response â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private SttResult sendAndParse(HttpRequest request) {
        try {
            HttpResponse<String> response =
                HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            log.debug("Deepgram response: status={}", response.statusCode());

            if (response.statusCode() == 401) {
                throw new SttException("Deepgram API key is invalid or expired", 401);
            }
            if (response.statusCode() == 402) {
                throw new SttException("Deepgram account quota exceeded", 402);
            }
            if (response.statusCode() >= 400) {
                throw new SttException(
                    "Deepgram returned HTTP " + response.statusCode() +
                    ": " + response.body(), response.statusCode());
            }

            return parseDeepgramResponse(response.body());

        } catch (SttException e) {
            throw e;
        } catch (Exception e) {
            throw new SttException("Deepgram request failed: " + e.getMessage(), e);
        }
    }

    // â”€â”€ Private: JSON parsing (manual â€” no extra dependency needed) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private SttResult parseDeepgramResponse(String json) {
        try {
            // Extract transcript
            String transcript  = extractJsonString(json, "\"transcript\":");
            double confidence  = extractJsonDouble(json, "\"confidence\":");
            double duration    = extractJsonDouble(json, "\"duration\":");

            if (transcript == null) {
                log.warn("Deepgram response missing transcript. Body snippet: {}",
                    json.length() > 200 ? json.substring(0, 200) : json);
                transcript = "";
            }

            SttResult.SttResultBuilder builder = SttResult.builder()
                    .transcript(transcript)
                    .confidence(confidence)
                    .durationSeconds(duration)
                    .providerName(getProviderName());

            return builder.build();

        } catch (Exception e) {
            log.error("Failed to parse Deepgram response: {}", e.getMessage());
            throw new SttException("Could not parse Deepgram response: " + e.getMessage());
        }
    }

    // â??â?? Private: URL builder â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    private String buildUrl(SttRequest request) {
        SttProperties.Deepgram cfg = props.getDeepgram();
        StringBuilder url = new StringBuilder(cfg.getBaseUrl()).append("/listen?");
        url.append("model=").append(cfg.getModel());
        url.append("&language=").append(request.getLanguage());
        url.append("&punctuate=").append(request.isPunctuate());
        url.append("&diarize=").append(request.isDiarize());
        url.append("&words=").append(request.isIncludeWords());
        return url.toString();
    }

    // â??â?? Private: MIME type from file extension â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    private String detectMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".mp3"))  return "audio/mpeg";
        if (lower.endsWith(".wav"))  return "audio/wav";
        if (lower.endsWith(".m4a"))  return "audio/mp4";
        if (lower.endsWith(".flac")) return "audio/flac";
        if (lower.endsWith(".ogg"))  return "audio/ogg";
        if (lower.endsWith(".webm")) return "audio/webm";
        return "audio/wav"; // safe default
    }

    // â??â?? Private: minimal JSON field extractors â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    private String extractJsonString(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int start = json.indexOf('"', idx + key.length()) + 1;
        int end   = json.indexOf('"', start);
        return (start > 0 && end > start) ? json.substring(start, end) : null;
    }

    private double extractJsonDouble(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return 0.0;
        int start = idx + key.length();
        while (start < json.length() && (json.charAt(start) == ' ')) start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end))
               || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
        try {
            return Double.parseDouble(json.substring(start, end));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
