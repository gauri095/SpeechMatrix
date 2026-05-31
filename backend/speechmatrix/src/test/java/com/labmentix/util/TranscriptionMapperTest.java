package com.labmentix.speechmatrix.util;

import com.sttapp.dto.TranscriptionDto;
import com.sttapp.model.Transcription;
import com.sttapp.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TranscriptionMapper unit tests")
class TranscriptionMapperTest {

    TranscriptionMapper mapper = new TranscriptionMapper();

    Transcription sample;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(1L).name("Riya").email("riya@test.com")
                .password("hashed").build();

        sample = Transcription.builder()
                .user(user)
                .transcript("Spring Boot makes building REST APIs fast and easy to maintain")
                .language("en-US")
                .wordCount(12)
                .durationSeconds(4.5)
                .confidenceScore(0.97)
                .sttProvider("deepgram")
                .status(Transcription.STATUS_DONE)
                .build();

        // Simulate JPA setting the id
        sample.setId(42L);
        sample.setCreatedAt(LocalDateTime.now());
        sample.setUpdatedAt(LocalDateTime.now());
    }

    // â??â?? toResponse â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("toResponse maps all fields correctly")
    void toResponseAllFields() {
        TranscriptionDto.Response r = mapper.toResponse(sample);

        assertThat(r.getId()).isEqualTo(42L);
        assertThat(r.getTranscript()).isEqualTo(sample.getTranscript());
        assertThat(r.getLanguage()).isEqualTo("en-US");
        assertThat(r.getWordCount()).isEqualTo(12);
        assertThat(r.getDurationSeconds()).isEqualTo(4.5);
        assertThat(r.getConfidenceScore()).isEqualTo(0.97);
        assertThat(r.getSttProvider()).isEqualTo("deepgram");
        assertThat(r.getStatus()).isEqualTo(Transcription.STATUS_DONE);
        assertThat(r.getCreatedAt()).isNotNull();
        assertThat(r.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("toResponse returns null for null input")
    void toResponseNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    // â??â?? toSummary â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("toSummary truncates long transcripts to 120 chars + ellipsis")
    void toSummaryTruncates() {
        String longText = "word ".repeat(50); // 250 chars
        sample.setTranscript(longText);

        TranscriptionDto.Summary s = mapper.toSummary(sample);

        assertThat(s.getTranscriptPreview()).hasSize(121); // 120 + "â??"
        assertThat(s.getTranscriptPreview()).endsWith("â??");
    }

    @Test
    @DisplayName("toSummary keeps short transcripts as-is")
    void toSummaryShortTranscript() {
        sample.setTranscript("Short text");
        TranscriptionDto.Summary s = mapper.toSummary(sample);

        assertThat(s.getTranscriptPreview()).isEqualTo("Short text");
        assertThat(s.getTranscriptPreview()).doesNotEndWith("â??");
    }

    @Test
    @DisplayName("toSummary handles null transcript")
    void toSummaryNullTranscript() {
        sample.setTranscript(null);
        TranscriptionDto.Summary s = mapper.toSummary(sample);
        assertThat(s.getTranscriptPreview()).isNull();
    }

    @Test
    @DisplayName("toSummary returns null for null input")
    void toSummaryNull() {
        assertThat(mapper.toSummary(null)).isNull();
    }

    // â??â?? toPagedResponse â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("toPagedResponse maps pagination metadata correctly")
    void toPagedResponseMetadata() {
        Page<Transcription> page = new PageImpl<>(
            List.of(sample),
            PageRequest.of(0, 10),
            25L  // total elements
        );

        TranscriptionDto.PagedResponse pr = mapper.toPagedResponse(page);

        assertThat(pr.getContent()).hasSize(1);
        assertThat(pr.getPage()).isEqualTo(0);
        assertThat(pr.getSize()).isEqualTo(10);
        assertThat(pr.getTotalElements()).isEqualTo(25L);
        assertThat(pr.getTotalPages()).isEqualTo(3);
        assertThat(pr.isFirst()).isTrue();
        assertThat(pr.isLast()).isFalse();
    }

    @Test
    @DisplayName("toPagedResponse with empty page returns empty content")
    void toPagedResponseEmpty() {
        Page<Transcription> page = new PageImpl<>(List.of());
        TranscriptionDto.PagedResponse pr = mapper.toPagedResponse(page);

        assertThat(pr.getContent()).isEmpty();
        assertThat(pr.getTotalElements()).isEqualTo(0L);
    }

    // â??â?? toSummaryList â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("toSummaryList maps each item in the list")
    void toSummaryList() {
        Transcription t2 = Transcription.builder()
                .transcript("Second transcript").status("done").build();
        t2.setId(43L);

        List<TranscriptionDto.Summary> list =
            mapper.toSummaryList(List.of(sample, t2));

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getId()).isEqualTo(42L);
        assertThat(list.get(1).getId()).isEqualTo(43L);
    }

    @Test
    @DisplayName("toSummaryList returns empty list for empty input")
    void toSummaryListEmpty() {
        assertThat(mapper.toSummaryList(List.of())).isEmpty();
    }
}
