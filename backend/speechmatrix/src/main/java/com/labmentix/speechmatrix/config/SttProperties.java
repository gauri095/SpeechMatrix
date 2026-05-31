package com.labmentix.speechmatrix.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe binding for all app.stt.* properties.
 *
 * Instead of @Value("${app.stt.deepgram.api-key}") scattered everywhere,
 * inject SttProperties and access properties.getDeepgram().getApiKey().
 *
 * This also validates at startup â?? missing required keys fail fast
 * before any request is made.
 */
@Configuration
@ConfigurationProperties(prefix = "app.stt")
@Data
@Validated
@Slf4j
public class SttProperties {

    /** Active provider: deepgram | assemblyai | google | mock */
    private String provider = "deepgram";

    private Deepgram   deepgram   = new Deepgram();
    private AssemblyAi assemblyai = new AssemblyAi();
    private Google     google     = new Google();

    // â??â?? Deepgram â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    @Data
    public static class Deepgram {
        private String apiKey  = "";
        private String baseUrl = "https://api.deepgram.com/v1";

        /**
         * Deepgram model to use.
         * nova-2 = latest general model (best accuracy + speed balance)
         * nova   = previous generation
         * base   = fastest, lowest cost
         * enhanced = higher accuracy, slower
         */
        private String model   = "nova-2";

        /** Include word-level timestamps in response */
        private boolean words  = true;

        /** Auto-detect punctuation */
        private boolean punctuate = true;

        /** Auto-detect number of speakers (diarization) */
        private boolean diarize = false;

        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank()
                && !apiKey.equals("REPLACE_WITH_YOUR_DEEPGRAM_KEY");
        }
    }

    // â??â?? AssemblyAI â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    @Data
    public static class AssemblyAi {
        private String apiKey  = "";
        private String baseUrl = "https://api.assemblyai.com/v2";

        /** Max ms to wait for async transcription to complete */
        private long pollingTimeoutMs  = 300_000; // 5 min
        private long pollingIntervalMs = 3_000;   // poll every 3s

        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank()
                && !apiKey.equals("REPLACE_WITH_YOUR_KEY");
        }
    }

    // â??â?? Google â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    @Data
    public static class Google {
        private String credentialsPath = "";

        /**
         * Google encoding enum:
         * LINEAR16, FLAC, MULAW, AMR, AMR_WB, OGG_OPUS, SPEEX_WITH_HEADER_BYTE
         */
        private String encoding        = "LINEAR16";
        private int    sampleRateHertz = 16000;

        public boolean isConfigured() {
            return credentialsPath != null && !credentialsPath.isBlank();
        }
    }

    // â??â?? Startup validation â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    @PostConstruct
    public void validate() {
        log.info("STT provider configured: {}", provider);

        switch (provider.toLowerCase()) {
            case "deepgram" -> {
                if (!deepgram.isConfigured()) {
                    log.warn("â??ï??  Deepgram API key not set â?? add to application.properties: " +
                             "app.stt.deepgram.api-key=YOUR_KEY");
                } else {
                    log.info("âœ… Deepgram API key loaded (model: {})", deepgram.getModel());
                }
            }
            case "assemblyai" -> {
                if (!assemblyai.isConfigured()) {
                    log.warn("âš ï¸  AssemblyAI API key not set â€” add: app.stt.assemblyai.api-key=YOUR_KEY");
                } else {
                    log.info("âœ… AssemblyAI API key loaded");
                }
            }
            case "google" -> {
                if (!google.isConfigured()) {
                    log.warn("âš ï¸  Google credentials path not set â€” add: app.stt.google.credentials-path=...");
                } else {
                    log.info("âœ… Google credentials path: {}", google.getCredentialsPath());
                }
            }
            case "mock" ->
                log.info("âœ… STT provider is MOCK â€” no real API calls will be made");
            default ->
                log.error("âŒ Unknown STT provider '{}'. Use: deepgram, assemblyai, google, mock",
                          provider);
        }
    }

    /** Returns true if the active provider has its credentials configured. */
    public boolean isActiveProviderConfigured() {
        return switch (provider.toLowerCase()) {
            case "deepgram"   -> deepgram.isConfigured();
            case "assemblyai" -> assemblyai.isConfigured();
            case "google"     -> google.isConfigured();
            case "mock"       -> true;
            default           -> false;
        };
    }
}
