package com.labmentix.dto;

import com.labmentix.speechmatrix.model.Transcription;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TranscriptionDto.SearchResult highlight tests")
class SearchResultHighlightTest {

    @Test
    @DisplayName("Wraps matching text in <mark> tags")
    void wrapsMatchInMark() {
        Transcription t = makeTranscription(
            "Spring Boot makes building REST APIs fast");

        TranscriptionDto.SearchResult r =
            TranscriptionDto.SearchResult.from(t, "Spring");

        assertThat(r.getHighlightedPreview())
            .contains("<mark>Spring</mark>");
    }

    @Test
    @DisplayName("Returns text without mark when no match")
    void noMarkWhenNoMatch() {
        Transcription t = makeTranscription("Hello world");
        TranscriptionDto.SearchResult r =
            TranscriptionDto.SearchResult.from(t, "xyz");

        assertThat(r.getHighlightedPreview()).doesNotContain("<mark>");
        assertThat(r.getHighlightedPreview()).isEqualTo("Hello world");
    }

    @Test
    @DisplayName("Match at start of text produces correct prefix")
    void matchAtStart() {
        Transcription t = makeTranscription("Spring Boot is great");
        TranscriptionDto.SearchResult r =
            TranscriptionDto.SearchResult.from(t, "Spring");

        assertThat(r.getHighlightedPreview())
            .startsWith("<mark>Spring</mark>");
    }

    @Test
    @DisplayName("Long text before match shows ellipsis prefix")
    void longTextBeforeMatch() {
        String longPrefix = "a ".repeat(100);  // > 80 chars before match
        Transcription t = makeTranscription(longPrefix + "Spring Boot");
        TranscriptionDto.SearchResult r =
            TranscriptionDto.SearchResult.from(t, "Spring");

        assertThat(r.getHighlightedPreview()).startsWith("…");
        assertThat(r.getHighlightedPreview()).contains("<mark>Spring</mark>");
    }

    @Test
    @DisplayName("Long text after match shows ellipsis suffix")
    void longTextAfterMatch() {
        String longSuffix = " word".repeat(100);  // > 80 chars after match
        Transcription t = makeTranscription("Spring" + longSuffix);
        TranscriptionDto.SearchResult r =
            TranscriptionDto.SearchResult.from(t, "Spring");

        assertThat(r.getHighlightedPreview()).endsWith("…");
        assertThat(r.getHighlightedPreview()).contains("<mark>Spring</mark>");
    }

    @Test
    @DisplayName("Null transcript returns null preview")
    void nullTranscript() {
        Transcription t = makeTranscription(null);
        TranscriptionDto.SearchResult r =
            TranscriptionDto.SearchResult.from(t, "spring");
        assertThat(r.getHighlightedPreview()).isNull();
    }

    @Test
    @DisplayName("From() copies all fields from entity")
    void copiesFields() {
        Transcription t = makeTranscription("Test transcript");
        t.setId(42L);
        t.setLanguage("hi-IN");
        t.setWordCount(10);
        t.setStatus("done");

        TranscriptionDto.SearchResult r =
            TranscriptionDto.SearchResult.from(t, "Test");

        assertThat(r.getId()).isEqualTo(42L);
        assertThat(r.getLanguage()).isEqualTo("hi-IN");
        assertThat(r.getWordCount()).isEqualTo(10);
        assertThat(r.getStatus()).isEqualTo("done");
    }

    private Transcription makeTranscription(String text) {
        Transcription t = new Transcription();
        t.setTranscript(text);
        t.setLanguage("en-US");
        t.setStatus("done");
        t.setWordCount(text != null ? text.split("\\s+").length : 0);
        return t;
    }
}