package com.labmentix.service;

import com.labmentix.speechmatrix.dto.TranscriptionDto;
import com.labmentix.speechmatrix.exception.ResourceNotFoundException;
import com.labmentix.speechmatrix.model.Transcription;
import com.labmentix.speechmatrix.model.User;
import com.labmentix.speechmatrix.repository.TranscriptionRepository;
import com.labmentix.speechmatrix.util.AudioFileValidator;
import com.labmentix.speechmatrix.util.TranscriptionMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TranscriptionService — stats and delete tests")
class TranscriptionServiceStatsAndDeleteTest {

    @Mock TranscriptionRepository transcriptionRepository;
    @Mock TranscriptionMapper     transcriptionMapper;
    @Mock UserService             userService;
    @Mock AudioStorageService     audioStorageService;
    @Mock AudioFileValidator      audioFileValidator;
    @Mock SpeechService           speechService;

    @InjectMocks TranscriptionService service;

    User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("arjun@test.com")
                .name("Arjun").password("h").build();
        when(userService.getByEmail("arjun@test.com")).thenReturn(testUser);
    }

    // ── getStats ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getStats aggregates all counters correctly")
    void getStatsAggregates() {
        when(transcriptionRepository.countByUserId(1L)).thenReturn(10L);
        when(transcriptionRepository.sumWordCountByUserId(1L)).thenReturn(5000L);
        when(transcriptionRepository.sumDurationByUserId(1L)).thenReturn(300.0);
        when(transcriptionRepository.countByUserIdAndStatus(1L, "done")).thenReturn(8L);
        when(transcriptionRepository.countByUserIdAndStatus(1L, "failed")).thenReturn(1L);
        when(transcriptionRepository.countByUserIdAndStatus(1L, "pending")).thenReturn(0L);
        when(transcriptionRepository.countByUserIdAndStatus(1L, "processing")).thenReturn(1L);
        when(transcriptionRepository.avgConfidenceByUserId(1L)).thenReturn(0.945);
        when(transcriptionRepository.findDistinctLanguagesByUserId(1L))
            .thenReturn(List.of("en-US", "hi-IN"));

        TranscriptionDto.StatsResponse stats = service.getStats("arjun@test.com");

        assertThat(stats.getTotalTranscriptions()).isEqualTo(10L);
        assertThat(stats.getTotalWords()).isEqualTo(5000L);
        assertThat(stats.getTotalDurationSeconds()).isEqualTo(300.0);
        assertThat(stats.getDoneCount()).isEqualTo(8L);
        assertThat(stats.getFailedCount()).isEqualTo(1L);
        assertThat(stats.getProcessingCount()).isEqualTo(1L);
        assertThat(stats.getAvgConfidence()).isEqualTo(0.945);
        assertThat(stats.getLanguages()).containsExactlyInAnyOrder("en-US", "hi-IN");
    }

    @Test
    @DisplayName("getStats handles null aggregates gracefully (new user)")
    void getStatsNullAggregates() {
        when(transcriptionRepository.countByUserId(1L)).thenReturn(0L);
        when(transcriptionRepository.sumWordCountByUserId(1L)).thenReturn(null);
        when(transcriptionRepository.sumDurationByUserId(1L)).thenReturn(null);
        when(transcriptionRepository.countByUserIdAndStatus(any(), any())).thenReturn(0L);
        when(transcriptionRepository.avgConfidenceByUserId(1L)).thenReturn(null);
        when(transcriptionRepository.findDistinctLanguagesByUserId(1L)).thenReturn(List.of());

        TranscriptionDto.StatsResponse stats = service.getStats("arjun@test.com");

        assertThat(stats.getTotalWords()).isEqualTo(0L);
        assertThat(stats.getTotalDurationSeconds()).isEqualTo(0.0);
        assertThat(stats.getAvgConfidence()).isEqualTo(0.0);
        assertThat(stats.getLanguages()).isEmpty();
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete removes transcription and its audio file")
    void deleteRemovesBoth() {
        Transcription t = Transcription.builder()
            .user(testUser).audioFile("uploads/1/abc.mp3")
            .transcript("text").status("done").build();
        t.setId(5L);

        when(transcriptionRepository.findByIdAndUserId(5L, 1L))
            .thenReturn(Optional.of(t));
        doNothing().when(audioStorageService).delete("uploads/1/abc.mp3");
        doNothing().when(transcriptionRepository).delete(t);

        service.delete("arjun@test.com", 5L);

        verify(audioStorageService).delete("uploads/1/abc.mp3");
        verify(transcriptionRepository).delete(t);
    }

    @Test
    @DisplayName("delete throws ResourceNotFoundException for unknown id")
    void deleteUnknownIdThrows() {
        when(transcriptionRepository.findByIdAndUserId(999L, 1L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("arjun@test.com", 999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");

        verify(audioStorageService, never()).delete(any());
        verify(transcriptionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete throws for transcription belonging to another user")
    void deleteOtherUserThrows() {
        // findByIdAndUserId returns empty when userId doesn't match
        when(transcriptionRepository.findByIdAndUserId(10L, 1L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("arjun@test.com", 10L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("delete continues if audio file deletion fails silently")
    void deleteAudioFileSilentFail() {
        Transcription t = Transcription.builder()
            .user(testUser).audioFile("uploads/1/gone.mp3")
            .status("done").build();
        t.setId(6L);

        when(transcriptionRepository.findByIdAndUserId(6L, 1L))
            .thenReturn(Optional.of(t));
        // audioStorageService.delete never throws — it logs and continues

        service.delete("arjun@test.com", 6L);

        verify(transcriptionRepository).delete(t);
    }
}