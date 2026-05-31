package com.sttapp.service;

import com.sttapp.repository.TranscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StartupService unit tests")
class StartupServiceTest {

    @Mock TranscriptionRepository transcriptionRepository;
    @InjectMocks StartupService   startupService;

    @Test
    @DisplayName("onStartup calls markStuckProcessingAsFailed")
    void callsMarkStuck() {
        when(transcriptionRepository.markStuckProcessingAsFailed()).thenReturn(0);
        when(transcriptionRepository.count()).thenReturn(5L);

        startupService.onStartup();

        verify(transcriptionRepository).markStuckProcessingAsFailed();
    }

    @Test
    @DisplayName("onStartup logs warning when stuck rows fixed")
    void logsWhenStuckRowsFixed() {
        when(transcriptionRepository.markStuckProcessingAsFailed()).thenReturn(3);
        when(transcriptionRepository.count()).thenReturn(10L);

        // Should complete without throwing — logging is side-effect only
        startupService.onStartup();

        verify(transcriptionRepository).markStuckProcessingAsFailed();
        verify(transcriptionRepository).count();
    }

    @Test
    @DisplayName("onStartup continues when DB health check fails")
    void continuesOnDbError() {
        when(transcriptionRepository.markStuckProcessingAsFailed()).thenReturn(0);
        when(transcriptionRepository.count()).thenThrow(new RuntimeException("DB unavailable"));

        // Must NOT throw — startup should continue even if DB health check fails
        org.assertj.core.api.Assertions.assertThatCode(
            () -> startupService.onStartup()
        ).doesNotThrowAnyException();
    }
}