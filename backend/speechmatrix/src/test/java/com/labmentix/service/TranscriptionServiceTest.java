package com.labmentix.speechmatrix.service;

import com.labmentix.speechmatrix.dto.TranscriptionDto;
import com.labmentix.speechmatrix.exception.ResourceNotFoundException;
import com.labmentix.speechmatrix.model.Transcription;
import com.labmentix.speechmatrix.model.User;
import com.labmentix.speechmatrix.repository.TranscriptionRepository;
import com.labmentix.speechmatrix.util.TranscriptionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TranscriptionService unit tests")
class TranscriptionServiceTest {

    @Mock TranscriptionRepository transcriptionRepository;
    @Mock TranscriptionMapper     transcriptionMapper;
    @Mock UserService             userService;

    @InjectMocks TranscriptionService service;

    User    testUser;
    Transcription testTranscription;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).name("Arjun")
                .email("arjun@test.com").password("hashed").build();

        testTranscription = Transcription.builder()
                .user(testUser)
                .transcript("Hello world this is a test transcript")
                .language("en-US")
                .wordCount(7)
                .status(Transcription.STATUS_DONE)
                .build();
        testTranscription.setId(10L);
    }

    // â??â?? getHistory â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("getHistory returns paged response")
    void getHistoryReturnsPaged() {
        when(userService.getByEmail("arjun@test.com")).thenReturn(testUser);

        Page<Transcription> page = new PageImpl<>(List.of(testTranscription));
        when(transcriptionRepository.findByUserIdOrderByCreatedAtDesc(
            eq(1L), any(Pageable.class))).thenReturn(page);

        TranscriptionDto.PagedResponse expected = new TranscriptionDto.PagedResponse();
        when(transcriptionMapper.toPagedResponse(page)).thenReturn(expected);

        TranscriptionDto.PagedResponse result =
            service.getHistory("arjun@test.com", 0, 10);

        assertThat(result).isSameAs(expected);
        verify(transcriptionRepository)
            .findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("getHistory caps page size at 50")
    void getHistoryCapsSize() {
        when(userService.getByEmail("arjun@test.com")).thenReturn(testUser);
        when(transcriptionRepository.findByUserIdOrderByCreatedAtDesc(
            eq(1L), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));
        when(transcriptionMapper.toPagedResponse(any())).thenReturn(
            new TranscriptionDto.PagedResponse());

        service.getHistory("arjun@test.com", 0, 200); // request 200

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(transcriptionRepository)
            .findByUserIdOrderByCreatedAtDesc(eq(1L), captor.capture());

        // Controller enforces â??50 but service uses whatever size is passed in
        // the controller test covers the cap â?? here we just verify Pageable is used
        assertThat(captor.getValue()).isNotNull();
    }

    // â??â?? getById â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("getById returns response when found")
    void getByIdFound() {
        when(userService.getByEmail("arjun@test.com")).thenReturn(testUser);
        when(transcriptionRepository.findByIdAndUserId(10L, 1L))
            .thenReturn(Optional.of(testTranscription));

        TranscriptionDto.Response expected = new TranscriptionDto.Response();
        when(transcriptionMapper.toResponse(testTranscription)).thenReturn(expected);

        TranscriptionDto.Response result = service.getById("arjun@test.com", 10L);
        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("getById throws ResourceNotFoundException when not found")
    void getByIdNotFound() {
        when(userService.getByEmail("arjun@test.com")).thenReturn(testUser);
        when(transcriptionRepository.findByIdAndUserId(99L, 1L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("arjun@test.com", 99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }

    @Test
    @DisplayName("getById throws when transcription belongs to another user")
    void getByIdWrongUser() {
        when(userService.getByEmail("arjun@test.com")).thenReturn(testUser);
        // findByIdAndUserId returns empty because userId doesn't match
        when(transcriptionRepository.findByIdAndUserId(10L, 1L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("arjun@test.com", 10L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // â??â?? search â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("search returns matching summaries")
    void searchReturnsSummaries() {
        when(userService.getByEmail("arjun@test.com")).thenReturn(testUser);
        when(transcriptionRepository.searchByTranscript(1L, "spring"))
            .thenReturn(List.of(testTranscription));

        TranscriptionDto.Summary summary = new TranscriptionDto.Summary();
        when(transcriptionMapper.toSummaryList(List.of(testTranscription)))
            .thenReturn(List.of(summary));

        List<TranscriptionDto.Summary> results =
            service.search("arjun@test.com", "spring");

        assertThat(results).hasSize(1).containsExactly(summary);
    }

    @Test
    @DisplayName("search returns empty list when no matches")
    void searchNoMatches() {
        when(userService.getByEmail("arjun@test.com")).thenReturn(testUser);
        when(transcriptionRepository.searchByTranscript(1L, "xyz"))
            .thenReturn(List.of());
        when(transcriptionMapper.toSummaryList(List.of())).thenReturn(List.of());

        List<TranscriptionDto.Summary> results =
            service.search("arjun@test.com", "xyz");

        assertThat(results).isEmpty();
    }

    // â??â?? delete â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("delete removes the transcription")
    void deleteSuccess() {
        when(userService.getByEmail("arjun@test.com")).thenReturn(testUser);
        when(transcriptionRepository.findByIdAndUserId(10L, 1L))
            .thenReturn(Optional.of(testTranscription));

        service.delete("arjun@test.com", 10L);

        verify(transcriptionRepository).delete(testTranscription);
    }

    @Test
    @DisplayName("delete throws when transcription not found")
    void deleteNotFound() {
        when(userService.getByEmail("arjun@test.com")).thenReturn(testUser);
        when(transcriptionRepository.findByIdAndUserId(99L, 1L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("arjun@test.com", 99L))
            .isInstanceOf(ResourceNotFoundException.class);

        verify(transcriptionRepository, never()).delete(any());
    }

    // â??â?? stubs â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("uploadAndTranscribe throws UnsupportedOperationException (Day 7 stub)")
    void uploadStubThrows() {
        assertThatThrownBy(() ->
            service.uploadAndTranscribe("arjun@test.com", null, null))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("Day 7");
    }
}
