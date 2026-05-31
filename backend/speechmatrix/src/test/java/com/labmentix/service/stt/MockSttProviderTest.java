package com.labmentix.speechmatrix.service.stt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("MockSttProvider unit tests")
class MockSttProviderTest {

    MockSttProvider provider;

    @BeforeEach
    void setUp() { provider = new MockSttProvider(); }

    @Test
    @DisplayName("transcribe returns non-empty transcript")
    void transcribeReturnsTranscript() {
        SttRequest request = SttRequest.builder()
                .audioUrl("https://example.com/test.wav")
                .language("en-US")
                .build();

        SttResult result = provider.transcribe(request);

        assertThat(result.getTranscript()).isNotBlank();
        assertThat(result.getConfidence()).isEqualTo(0.99);
        assertThat(result.getProviderName()).isEqualTo("Mock (test)");
    }

    @Test
    @DisplayName("transcribeBytes includes byte count in transcript")
    void transcribeBytesIncludesSize() {
        byte[] bytes = new byte[1024];
        SttRequest request = SttRequest.builder().language("en-US").build();

        SttResult result = provider.transcribeBytes(bytes, request);

        assertThat(result.getTranscript()).contains("1024");
    }

    @Test
    @DisplayName("transcribeBytes estimates duration from byte count")
    void transcribeBytesEstimatesDuration() {
        byte[] bytes = new byte[32_000]; // ~1 second at 16kHz 16-bit mono
        SttRequest request = SttRequest.builder().language("en-US").build();

        SttResult result = provider.transcribeBytes(bytes, request);

        assertThat(result.getDurationSeconds()).isCloseTo(1.0, within(0.1));
    }

    @Test
    @DisplayName("ping returns connected=true")
    void pingConnected() {
        PingResult ping = provider.ping();
        assertThat(ping.isConnected()).isTrue();
        assertThat(ping.getLatencyMs()).isGreaterThanOrEqualTo(0);
        assertThat(ping.getProvider()).isEqualTo("mock");
    }

    @Test
    @DisplayName("getProviderName returns mock label")
    void providerName() {
        assertThat(provider.getProviderName()).isEqualTo("Mock (test)");
    }

    @Test
    @DisplayName("transcribe with null request fields does not throw")
    void transcribeMinimalRequest() {
        SttRequest request = SttRequest.builder()
                .audioUrl("https://example.com/audio.wav")
                .build();

        assertThatCode(() -> provider.transcribe(request))
            .doesNotThrowAnyException();
    }
}
