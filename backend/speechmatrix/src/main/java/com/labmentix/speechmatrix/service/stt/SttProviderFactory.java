package com.labmentix.speechmatrix.service.stt;

import com.labmentix.speechmatrix.config.SttProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Resolves which SttProvider implementation to use based on
 * app.stt.provider in application.properties.
 *
 * Injects a single @Primary SttProvider bean â€” callers don't need to
 * know which concrete provider is active.
 *
 * Supported values:
 *   app.stt.provider=deepgram    â†’ DeepgramSttProvider
 *   app.stt.provider=assemblyai  â†’ AssemblyAiSttProvider
 *   app.stt.provider=mock        â†’ MockSttProvider
 *
 * To add a new provider (e.g. Google):
 *   1. Implement SttProvider â†’ GoogleSttProvider.java
 *   2. Add @Component("googleSttProvider")
 *   3. Add a "google" case in the switch below
 */
@Configuration
@Slf4j
public class SttProviderFactory {

    @Bean
    @Primary
    public SttProvider activeSttProvider(
            SttProperties properties,
            @Qualifier("deepgramSttProvider")   SttProvider deepgram,
            @Qualifier("assemblyAiSttProvider") SttProvider assemblyAi,
            @Qualifier("mockSttProvider")       SttProvider mock) {

        String provider = properties.getProvider().toLowerCase().trim();

        SttProvider active = switch (provider) {
            case "deepgram"   -> deepgram;
            case "assemblyai" -> assemblyAi;
            case "mock"       -> mock;
            default -> {
                log.error("Unknown STT provider '{}', falling back to mock", provider);
                yield mock;
            }
        };

        log.info("Active STT provider: {} â†’ {}", provider, active.getProviderName());
        return active;
    }
}
