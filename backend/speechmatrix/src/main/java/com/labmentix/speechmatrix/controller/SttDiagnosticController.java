package com.labmentix.speechmatrix.controller;

import com.labmentix.speechmatrix.config.SttProperties;
import com.labmentix.speechmatrix.service.stt.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * STT provider diagnostics â?? dev only.
 *
 * GET  /api/dev/stt/config       â?? show active config (key masked)
 * GET  /api/dev/stt/ping         â?? live connectivity test to provider
 * POST /api/dev/stt/test-url     â?? transcribe a public audio URL
 */
@RestController
@RequestMapping("/api/dev/stt")
@RequiredArgsConstructor
@Profile("!prod")
@Slf4j
public class SttDiagnosticController {

    private final SttProvider   sttProvider;
    private final SttProperties sttProperties;

    // â??â?? GET /api/dev/stt/config â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> config() {
        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("activeProvider",      sttProperties.getProvider());
        cfg.put("providerName",        sttProvider.getProviderName());
        cfg.put("isConfigured",        sttProperties.isActiveProviderConfigured());

        // Mask API key â?? show only first 6 and last 4 chars
        String key = sttProperties.getDeepgram().getApiKey();
        cfg.put("deepgramApiKey",  maskKey(key));
        cfg.put("deepgramModel",   sttProperties.getDeepgram().getModel());
        cfg.put("deepgramBaseUrl", sttProperties.getDeepgram().getBaseUrl());

        String aaiKey = sttProperties.getAssemblyai().getApiKey();
        cfg.put("assemblyaiApiKey", maskKey(aaiKey));

        return ResponseEntity.ok(cfg);
    }

    // â??â?? GET /api/dev/stt/ping â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        log.info("STT ping requested for provider: {}", sttProperties.getProvider());
        long start = System.currentTimeMillis();

        PingResult result = sttProvider.ping();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("provider",    result.getProvider());
        resp.put("connected",   result.isConnected());
        resp.put("model",       result.getModel());
        resp.put("latencyMs",   result.getLatencyMs());
        resp.put("message",     result.getMessage());
        resp.put("totalTimeMs", System.currentTimeMillis() - start);

        int status = result.isConnected() ? 200 : 503;
        return ResponseEntity.status(status).body(resp);
    }

    // â??â?? POST /api/dev/stt/test-url â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    @PostMapping("/test-url")
    public ResponseEntity<Map<String, Object>> testUrl(
            @RequestBody Map<String, String> body) {

        String audioUrl = body.get("url");
        String language = body.getOrDefault("language", "en-US");

        if (audioUrl == null || audioUrl.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Request body must include: {\"url\": \"https://...\"}"
            ));
        }

        log.info("STT test-url: {} lang={}", audioUrl, language);

        SttRequest request = SttRequest.builder()
                .audioUrl(audioUrl)
                .language(language)
                .punctuate(true)
                .includeWords(false)
                .build();

        long start = System.currentTimeMillis();
        SttResult result = sttProvider.transcribe(request);
        long elapsed = System.currentTimeMillis() - start;

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("provider",        result.getProviderName());
        resp.put("transcript",      result.getTranscript());
        resp.put("confidence",      result.getConfidence());
        resp.put("wordCount",       result.getWordCount());
        resp.put("durationSeconds", result.getDurationSeconds());
        resp.put("processingMs",    elapsed);

        return ResponseEntity.ok(resp);
    }

    // â??â?? Private â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    private String maskKey(String key) {
        if (key == null || key.isBlank() || key.startsWith("REPLACE")) {
            return "(not set)";
        }
        if (key.length() <= 10) return "***";
        return key.substring(0, 6) + "..." + key.substring(key.length() - 4);
    }
}
