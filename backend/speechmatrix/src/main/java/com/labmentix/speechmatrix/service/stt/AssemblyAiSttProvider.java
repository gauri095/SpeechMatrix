package com.labmentix.speechmatrix.service.stt;

import com.labmentix.speechmatrix.config.SttProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Files;
import java.time.Duration;

/**
 * AssemblyAI Speech-to-Text provider.
 *
 * API: https://www.assemblyai.com/docs
 *
 * AssemblyAI is ASYNC â?? 3-step flow:
 *   Step 1: POST /v2/upload  â?? upload file â?? get upload_url
 *   Step 2: POST /v2/transcript { audio_url } â?? get transcript id
 *   Step 3: GET  /v2/transcript/{id} (poll until status = "completed")
 *
 * Polling timeout controlled by:
 *   app.stt.assemblyai.polling-timeout-ms=300000
 *   app.stt.assemblyai.polling-interval-ms=3000
 */
@Component("assemblyAiSttProvider")
@RequiredArgsConstructor
@Slf4j
public class AssemblyAiSttProvider implements SttProvider {

    private final SttProperties props;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    // â??â?? transcribe â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    @Override
    public SttResult transcribe(SttRequest request) {
        try {
            String audioUrl;

            if (request.getAudioUrl() != null) {
                audioUrl = request.getAudioUrl();
            } else if (request.getAudioFile() != null) {
                audioUrl = uploadFile(request.getAudioFile());
            } else if (request.getAudioBytes() != null) {
                audioUrl = uploadBytes(request.getAudioBytes());
            } else {
                throw new SttException("SttRequest requires audioFile, audioBytes, or audioUrl");
            }

            // Step 2: submit for transcription
            String transcriptId = submitTranscript(audioUrl, request);
            log.debug("AssemblyAI transcript id: {}", transcriptId);

            // Step 3: poll until done
            return pollForResult(transcriptId);

        } catch (SttException e) {
            throw e;
        } catch (Exception e) {
            throw new SttException("AssemblyAI error: " + e.getMessage(), e);
        }
    }

    @Override
    public SttResult transcribeBytes(byte[] audioBytes, SttRequest request) {
        SttRequest req = SttRequest.builder()
                .audioBytes(audioBytes)
                .language(request.getLanguage())
                .punctuate(request.isPunctuate())
                .diarize(request.isDiarize())
                .includeWords(request.isIncludeWords())
                .build();
        return transcribe(req);
    }

    // â??â?? ping â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    @Override
    public PingResult ping() {
        if (!props.getAssemblyai().isConfigured()) {
            return PingResult.fail("assemblyai", "API key not configured");
        }
        // Test by checking account health endpoint
        long start = System.currentTimeMillis();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(props.getAssemblyai().getBaseUrl() + "/transcript?limit=1"))
                    .header("Authorization", props.getAssemblyai().getApiKey())
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> resp =
                HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            long elapsed = System.currentTimeMillis() - start;

            if (resp.statusCode() == 200 || resp.statusCode() == 401) {
                if (resp.statusCode() == 401) {
                    return PingResult.fail("assemblyai", "API key invalid");
                }
                return PingResult.ok("assemblyai", "default", elapsed);
            }
            return PingResult.fail("assemblyai", "Unexpected status: " + resp.statusCode());

        } catch (Exception e) {
            return PingResult.fail("assemblyai", "Ping failed: " + e.getMessage());
        }
    }

    @Override
    public String getProviderName() { return "AssemblyAI"; }

    // â??â?? Step 1: Upload file â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    private String uploadFile(File file) throws Exception {
        byte[] bytes = Files.readAllBytes(file.toPath());
        return uploadBytes(bytes);
    }

    private String uploadBytes(byte[] bytes) throws Exception {
        log.debug("AssemblyAI: uploading {} bytes", bytes.length);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(props.getAssemblyai().getBaseUrl() + "/upload"))
                .header("Authorization", props.getAssemblyai().getApiKey())
                .header("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response =
            HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new SttException("AssemblyAI upload failed: HTTP " + response.statusCode());
        }

        // Parse: {"upload_url": "https://..."}
        String url = extractJsonField(response.body(), "upload_url");
        if (url == null) {
            throw new SttException("AssemblyAI upload response missing upload_url");
        }
        log.debug("AssemblyAI upload_url obtained");
        return url;
    }

    // â??â?? Step 2: Submit transcript job â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    private String submitTranscript(String audioUrl, SttRequest req) throws Exception {
        String body = buildSubmitBody(audioUrl, req);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(props.getAssemblyai().getBaseUrl() + "/transcript"))
                .header("Authorization", props.getAssemblyai().getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response =
            HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new SttException("AssemblyAI submit failed: HTTP " + response.statusCode());
        }

        String id = extractJsonField(response.body(), "id");
        if (id == null) {
            throw new SttException("AssemblyAI submit response missing id");
        }
        return id;
    }

    // â??â?? Step 3: Poll for result â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    private SttResult pollForResult(String transcriptId) throws Exception {
        SttProperties.AssemblyAi cfg = props.getAssemblyai();
        long deadline = System.currentTimeMillis() + cfg.getPollingTimeoutMs();
        String pollUrl = cfg.getBaseUrl() + "/transcript/" + transcriptId;

        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(cfg.getPollingIntervalMs());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(pollUrl))
                    .header("Authorization", cfg.getApiKey())
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response =
                HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            String status = extractJsonField(response.body(), "status");
            log.debug("AssemblyAI poll status: {}", status);

            if ("completed".equals(status)) {
                return parseAssemblyAiResponse(response.body());
            }
            if ("error".equals(status)) {
                String error = extractJsonField(response.body(), "error");
                throw new SttException("AssemblyAI transcription failed: " + error);
            }
            // status = "queued" or "processing" â€” keep polling
        }

        throw new SttException("AssemblyAI transcription timed out after "
            + cfg.getPollingTimeoutMs() / 1000 + "s");
    }

    // â”€â”€ JSON helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private String buildSubmitBody(String audioUrl, SttRequest req) {
        return String.format(
            "{\"audio_url\":\"%s\",\"language_code\":\"%s\"," +
            "\"punctuate\":%b,\"speaker_labels\":%b}",
            audioUrl, req.getLanguage(), req.isPunctuate(), req.isDiarize());
    }

    private SttResult parseAssemblyAiResponse(String json) {
        String text       = extractJsonField(json, "text");
        double confidence = extractJsonDouble(json, "confidence");
        double duration   = extractJsonDouble(json, "audio_duration");

        return SttResult.builder()
                .transcript(text != null ? text : "")
                .confidence(confidence)
                .durationSeconds(duration)
                .providerName(getProviderName())
                .build();
    }

    private String extractJsonField(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int idx = json.indexOf(searchKey);
        if (idx < 0) return null;
        int start = idx + searchKey.length();
        int end   = json.indexOf('"', start);
        return end > start ? json.substring(start, end) : null;
    }

    private double extractJsonDouble(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int idx = json.indexOf(searchKey);
        if (idx < 0) return 0.0;
        int start = idx + searchKey.length();
        while (start < json.length() && json.charAt(start) == ' ') start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end))
               || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
        try { return Double.parseDouble(json.substring(start, end)); }
        catch (NumberFormatException e) { return 0.0; }
    }
}
