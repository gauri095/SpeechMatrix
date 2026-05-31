package com.labmentix.speechmatrix.service.stt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SttResult unit tests")
class SttResultTest {

    @Test
    @DisplayName("getWordCount counts words in transcript")
    void wordCount() {
        SttResult r = SttResult.builder()
                .transcript("Spring Boot makes API development easy")
                .build();
        assertThat(r.getWordCount()).isEqualTo(6);
    }

    @Test
    @DisplayName("getWordCount returns 0 for null transcript")
    void wordCountNull() {
        assertThat(SttResult.builder().build().getWordCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("getWordCount returns 0 for blank transcript")
    void wordCountBlank() {
        assertThat(SttResult.builder().transcript("   ").build().getWordCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("isEmpty returns true for null transcript")
    void isEmptyNull() {
        assertThat(SttResult.builder().build().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("isEmpty returns true for blank transcript")
    void isEmptyBlank() {
        assertThat(SttResult.builder().transcript("  ").build().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("isEmpty returns false for real transcript")
    void isEmptyFalse() {
        assertThat(SttResult.builder().transcript("Hello world").build().isEmpty()).isFalse();
    }
}
