package com.labmentix.service;

import com.labmentix.speechmatrix.dto.TranscriptionDto;
import com.labmentix.speechmatrix.model.Transcription;
import com.labmentix.speechmatrix.model.User;
import com.labmentix.speechmatrix.repository.TranscriptionRepository;
import com.labmentix.speechmatrix.util.AudioFileValidator;
import com.labmentix.speechmatrix.util.TranscriptionMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TranscriptionService unit tests — history filter routing")
class TranscriptionServiceFilterTest {

    @Mock TranscriptionRepository transcriptionRepository;
    @Mock TranscriptionMapper     transcriptionMapper;
    @Mock UserService             userService;
    @Mock AudioStorageService     audioStorageService;
    @Mock AudioFileValidator      audioFileValidator;
    @Mock SpeechService           speechService;

    @InjectMocks TranscriptionService service;

    User testUser;
    Page<Transcription> emptyPage;
    TranscriptionDto.PagedResponse emptyResponse;

    @BeforeEach
    void setUp() {
        testUser  = User.builder().id(1L).email("arjun@test.com")
                        .name("Arjun").password("hashed").build();
        emptyPage = new PageImpl<>(List.of());
        emptyResponse = new TranscriptionDto.PagedResponse();

        when(userService.getByEmail("arjun@test.com")).thenReturn(testUser);
        when(transcriptionMapper.toPagedResponse(any(Page.class), any(), any()))
            .thenReturn(emptyResponse);
    }

    // ── Filter routing ────────────────────────────────────────────────────────

    @Test
    @DisplayName("No filters → findByUserIdOrderByCreatedAtDesc")
    void noFilterCallsDefault() {
        when(transcriptionRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any()))
            .thenReturn(emptyPage);

        TranscriptionDto.HistoryFilter filter = new TranscriptionDto.HistoryFilter();
        service.getHistory("arjun@test.com", filter);

        verify(transcriptionRepository)
            .findByUserIdOrderByCreatedAtDesc(eq(1L), any());
        verify(transcriptionRepository, never())
            .findByUserIdAndStatusOrderByCreatedAtDesc(any(), any(), any());
    }

    @Test
    @DisplayName("Status filter only → findByUserIdAndStatus")
    void statusFilterCallsStatusMethod() {
        when(transcriptionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
            eq(1L), eq("done"), any())).thenReturn(emptyPage);

        TranscriptionDto.HistoryFilter filter = new TranscriptionDto.HistoryFilter();
        filter.setStatus("done");
        service.getHistory("arjun@test.com", filter);

        verify(transcriptionRepository)
            .findByUserIdAndStatusOrderByCreatedAtDesc(eq(1L), eq("done"), any());
    }

    @Test
    @DisplayName("Language filter only → findByUserIdAndLanguage")
    void languageFilterCallsLanguageMethod() {
        when(transcriptionRepository.findByUserIdAndLanguageOrderByCreatedAtDesc(
            eq(1L), eq("hi-IN"), any())).thenReturn(emptyPage);

        TranscriptionDto.HistoryFilter filter = new TranscriptionDto.HistoryFilter();
        filter.setLanguage("hi-IN");
        service.getHistory("arjun@test.com", filter);

        verify(transcriptionRepository)
            .findByUserIdAndLanguageOrderByCreatedAtDesc(eq(1L), eq("hi-IN"), any());
    }

    @Test
    @DisplayName("Status + Language → findByUserIdAndStatusAndLanguage")
    void statusAndLanguageFilterCombined() {
        when(transcriptionRepository.findByUserIdAndStatusAndLanguageOrderByCreatedAtDesc(
            eq(1L), eq("done"), eq("en-US"), any())).thenReturn(emptyPage);

        TranscriptionDto.HistoryFilter filter = new TranscriptionDto.HistoryFilter();
        filter.setStatus("done");
        filter.setLanguage("en-US");
        service.getHistory("arjun@test.com", filter);

        verify(transcriptionRepository)
            .findByUserIdAndStatusAndLanguageOrderByCreatedAtDesc(
                eq(1L), eq("done"), eq("en-US"), any());
    }

    @Test
    @DisplayName("Date range filter → findByUserIdAndDateRange")
    void dateRangeFilterCallsDateMethod() {
        when(transcriptionRepository.findByUserIdAndDateRange(
            eq(1L), any(), any(), any())).thenReturn(emptyPage);

        TranscriptionDto.HistoryFilter filter = new TranscriptionDto.HistoryFilter();
        filter.setFrom("2026-01-01T00:00:00");
        filter.setTo("2026-12-31T23:59:59");
        service.getHistory("arjun@test.com", filter);

        verify(transcriptionRepository)
            .findByUserIdAndDateRange(eq(1L), any(), any(), any());
    }

    // ── Page size cap ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Size over 50 is capped at 50")
    void pageSizeCapped() {
        when(transcriptionRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any()))
            .thenReturn(emptyPage);

        TranscriptionDto.HistoryFilter filter = new TranscriptionDto.HistoryFilter();
        filter.setSize(999);
        service.getHistory("arjun@test.com", filter);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(transcriptionRepository)
            .findByUserIdOrderByCreatedAtDesc(eq(1L), captor.capture());

        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
    }

    // ── Sort field resolution ─────────────────────────────────────────────────

    @ParameterizedTest(name = "sortBy={0} → field={1}")
    @CsvSource({
        "wordcount,      wordCount",
        "wordCount,      wordCount",
        "duration,       durationSeconds",
        "durationSeconds,durationSeconds",
        "confidence,     confidenceScore",
        "confidenceScore,confidenceScore",
        "createdAt,      createdAt",
        "unknown,        createdAt",    // unknown → safe default
        ","              + "createdAt"  // null → safe default
    })
    @DisplayName("Sort field names are correctly resolved")
    void sortFieldResolved(String input, String expectedField) {
        when(transcriptionRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any()))
            .thenReturn(emptyPage);

        TranscriptionDto.HistoryFilter filter = new TranscriptionDto.HistoryFilter();
        filter.setSortBy(input);
        service.getHistory("arjun@test.com", filter);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(transcriptionRepository)
            .findByUserIdOrderByCreatedAtDesc(eq(1L), captor.capture());

        Pageable p = captor.getValue();
        assertThat(p.getSort().getOrderFor(expectedField)).isNotNull();
    }

    // ── GET by ID — ownership ─────────────────────────────────────────────────

    @Test
    @DisplayName("getById throws ResourceNotFoundException for wrong user")
    void getByIdWrongUser() {
        when(transcriptionRepository.findByIdAndUserId(99L, 1L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("arjun@test.com", 99L))
            .hasMessageContaining("99");
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("search delegates to searchByTranscriptPaged")
    void searchDelegatesToPaged() {
        when(transcriptionRepository.searchByTranscriptPaged(eq(1L), eq("spring"), any()))
            .thenReturn(emptyPage);

        when(transcriptionMapper.toSearchPagedResponse(any(), any()))
            .thenReturn(new TranscriptionDto.SearchPagedResponse());

        service.search("arjun@test.com", "spring", 0, 10);

        verify(transcriptionRepository)
            .searchByTranscriptPaged(eq(1L), eq("spring"), any());
    }
}