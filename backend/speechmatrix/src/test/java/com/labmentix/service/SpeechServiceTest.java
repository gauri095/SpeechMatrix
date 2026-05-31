package com.labmentix.speechmatrix.service;

import com.labmentix.speechmatrix.service.stt.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpeechService unit tests")
class SpeechServiceTest {

    @Mock SttProvider sttProvider;

    @InjectMocks SpeechService speechService;

    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(speechService, "maxRetryAttempts", 3);
        ReflectionTestUtils.setField(speechService, "retryDelayMs",     0L);
        ReflectionTestUtils.setField(speechService, "chunkSizeBytes",   10_485_760L);
    }

    // â??â?? transcribeFile â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("transcribeFile delegates to sttProvider and normalises result")
    void transcribeFileDelegates() throws Exception {
        File audioFile = createTempFile("test.mp3", 100);

        SttResult raw = SttResult.builder()
                .transcript("  hello   world  ")
                .confidence(0.987654321)
                .durationSeconds(3.5)
                .providerName("Deepgram nova-2")
                .build();
        when(sttProvider.transcribe(any())).thenReturn(raw);

        SttResult result = speechService.transcribeFile(
            audioFile, "en-US", "audio/mpeg", false);

        // Normalised â?? extra spaces collapsed, confidence rounded
        assertThat(result.getTranscript()).isEqualTo("hello world");
        assertThat(result.getConfidence()).isEqualTo(0.9877); // 4dp
        verify(sttProvider, times(1)).transcribe(any());
    }

    // â??â?? transcribeBytes â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("transcribeBytes passes bytes to provider")
    void transcribeBytesDelegates() {
        byte[] bytes = new byte[1024];

        SttResult raw = SttResult.builder()
                .transcript("Recording transcribed")
                .confidence(0.95)
                .durationSeconds(2.0)
                .build();
        when(sttProvider.transcribe(any())).thenReturn(raw);

        SttResult result = speechService.transcribeBytes(bytes, "en-US", "audio/webm");

        assertThat(result.getTranscript()).isEqualTo("Recording transcribed");
        verify(sttProvider).transcribe(argThat(r ->
            r.getAudioBytes() == bytes && "audio/webm".equals(r.getMimeType())));
    }

    // â??â?? Retry logic â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("Retries up to maxRetryAttempts on 503 error")
    void retriesOnTransientError() throws Exception {
        File audioFile = createTempFile("retry.mp3", 100);

        // First two calls throw 503, third succeeds
        SttResult success = SttResult.builder()
                .transcript("Success after retry")
                .confidence(0.9)
                .durationSeconds(2.0)
                .build();

        when(sttProvider.transcribe(any()))
            .thenThrow(new SttException("Service unavailable", 503))
            .thenThrow(new SttException("Service unavailable", 503))
            .thenReturn(success);

        SttResult result = speechService.transcribeFile(
            audioFile, "en-US", "audio/mpeg", false);

        assertThat(result.getTranscript()).isEqualTo("Success after retry");
        verify(sttProvider, times(3)).transcribe(any());
    }

    @Test
    @DisplayName("Does NOT retry on 401 (permanent auth error)")
    void noRetryOn401() throws Exception {
        File audioFile = createTempFile("auth.mp3", 100);

        when(sttProvider.transcribe(any()))
            .thenThrow(new SttException("Unauthorized", 401));

        assertThatThrownBy(() ->
            speechService.transcribeFile(audioFile, "en-US", "audio/mpeg", false))
            .isInstanceOf(SttException.class)
            .hasMessageContaining("Unauthorized");

        // Must NOT retry â?? only called once
        verify(sttProvider, times(1)).transcribe(any());
    }

    @Test
    @DisplayName("Throws SttException after exhausting all retry attempts")
    void throwsAfterAllRetriesFail() throws Exception {
        File audioFile = createTempFile("fail.mp3", 100);

        when(sttProvider.transcribe(any()))
            .thenThrow(new SttException("Server error", 503));

        assertThatThrownBy(() ->
            speechService.transcribeFile(audioFile, "en-US", "audio/mpeg", false))
            .isInstanceOf(SttException.class)
            .hasMessageContaining("3 attempts");

        verify(sttProvider, times(3)).transcribe(any());
    }

    // â??â?? Normalisation â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("normalise trims whitespace and collapses spaces")
    void normaliseCollapsesSpaces() {
        SttResult raw = SttResult.builder()
                .transcript("  hello   world  ")
                .confidence(0.9)
                .durationSeconds(1.0)
                .build();

        SttResult normalised = speechService.normalise(raw);
        assertThat(normalised.getTranscript()).isEqualTo("hello world");
    }

    @Test
    @DisplayName("normalise rounds confidence to 4 decimal places")
    void normaliseRoundsConfidence() {
        SttResult raw = SttResult.builder()
                .transcript("text")
                .confidence(0.987654321)
                .durationSeconds(1.0)
                .build();

        SttResult normalised = speechService.normalise(raw);
        assertThat(normalised.getConfidence()).isEqualTo(0.9877);
    }

    @Test
    @DisplayName("normalise returns same result for null transcript")
    void normaliseNullTranscript() {
        SttResult raw = SttResult.builder().confidence(0.9).durationSeconds(1.0).build();
        SttResult normalised = speechService.normalise(raw);
        assertThat(normalised.getTranscript()).isNull();
    }

    @Test
    @DisplayName("normalise returns null for null input")
    void normaliseNull() {
        assertThat(speechService.normalise(null)).isNull();
    }

    // â??â?? Chunking â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("needsChunking returns false for small file")
    void noChunkingSmallFile() throws Exception {
        File small = createTempFile("small.mp3", 100);
        assertThat(speechService.needsChunking(small)).isFalse();
    }

    @Test
    @DisplayName("needsChunking returns true for file over chunkSizeBytes")
    void chunkingLargeFile() throws Exception {
        ReflectionTestUtils.setField(speechService, "chunkSizeBytes", 50L);
        File large = createTempFile("large.mp3", 100);
        assertThat(speechService.needsChunking(large)).isTrue();
    }

    // â??â?? Helpers â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    private File createTempFile(String name, int sizeBytes) throws Exception {
        Path p = tempDir.resolve(name);
        Files.write(p, new byte[sizeBytes]);
        return p.toFile();
    }
}
