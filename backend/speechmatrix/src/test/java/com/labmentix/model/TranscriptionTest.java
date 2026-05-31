package com.labmentix.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Transcription entity tests")
class TranscriptionTest {

    @Test
    @DisplayName("Default status is pending")
    void defaultStatusIsPending() {
        Transcription t = new Transcription();
        assertThat(t.getStatus()).isEqualTo(Transcription.STATUS_PENDING);
    }

    @Test
    @DisplayName("calculateWordCount counts correctly")
    void wordCountCalculation() {
        Transcription t = new Transcription();
        t.setTranscript("Spring Boot makes Java web development fast and easy");
        t.calculateWordCount();
        assertThat(t.getWordCount()).isEqualTo(9);
    }

    @Test
    @DisplayName("calculateWordCount handles blank transcript")
    void wordCountBlankTranscript() {
        Transcription t = new Transcription();
        t.setTranscript("   ");
        t.calculateWordCount();
        assertThat(t.getWordCount()).isNull();
    }

    @Test
    @DisplayName("Status helper methods work correctly")
    void statusHelpers() {
        Transcription t = new Transcription();

        t.setStatus(Transcription.STATUS_DONE);
        assertThat(t.isDone()).isTrue();
        assertThat(t.isFailed()).isFalse();
        assertThat(t.isProcessing()).isFalse();

        t.setStatus(Transcription.STATUS_FAILED);
        assertThat(t.isFailed()).isTrue();

        t.setStatus(Transcription.STATUS_PROCESSING);
        assertThat(t.isProcessing()).isTrue();
    }

    @Test
    @DisplayName("Builder sets default language to en-US")
    void defaultLanguage() {
        Transcription t = Transcription.builder()
                .transcript("Hello world")
                .build();
        assertThat(t.getLanguage()).isEqualTo("en-US");
    }
}

