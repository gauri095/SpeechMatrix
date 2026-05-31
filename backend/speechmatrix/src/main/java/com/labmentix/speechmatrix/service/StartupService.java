package com.labmentix.speechmatrix.service;

import com.labmentix.speechmatrix.repository.TranscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs once after the Spring context is fully started.
 *
 * Why this is needed:
 * If the app crashes mid-transcription, DB rows are left with
 * status=processing indefinitely. They never get picked up again
 * because the SpeechService that was processing them is gone.
 *
 * This service marks them as "failed" on every startup so users
 * can see them in history and re-upload if needed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StartupService {

    private final TranscriptionRepository transcriptionRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onStartup() {
        log.info("=== VoxScript STT App started ===");

        // Fix any rows left stuck in processing from a previous crash
        int fixed = transcriptionRepository.markStuckProcessingAsFailed();
        if (fixed > 0) {
            log.warn("Marked {} stuck 'processing' transcription(s) as 'failed'. " +
                     "Users should re-upload those files.", fixed);
        } else {
            log.debug("No stuck processing rows found");
        }

        // Log database health summary
        try {
            long total = transcriptionRepository.count();
            log.info("DB health: {} total transcription rows", total);
        } catch (Exception e) {
            log.error("DB health check failed: {}", e.getMessage());
        }
    }
}