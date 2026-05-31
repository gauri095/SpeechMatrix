package com.labmentix.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("SttProperties binding tests")
class SttPropertiesTest {

    @Autowired
    SttProperties props;

    @Test
    @DisplayName("Provider is 'mock' in test profile")
    void providerIsMock() {
        assertThat(props.getProvider()).isEqualTo("mock");
    }

    @Test
    @DisplayName("Deepgram model defaults to nova-2")
    void deepgramModelDefault() {
        assertThat(props.getDeepgram().getModel()).isEqualTo("nova-2");
    }

    @Test
    @DisplayName("Deepgram base URL is correct")
    void deepgramBaseUrl() {
        assertThat(props.getDeepgram().getBaseUrl())
            .isEqualTo("https://api.deepgram.com/v1");
    }

    @Test
    @DisplayName("AssemblyAI polling timeout is 5 minutes")
    void assemblyaiPollingTimeout() {
        assertThat(props.getAssemblyai().getPollingTimeoutMs())
            .isEqualTo(300_000L);
    }

    @Test
    @DisplayName("isActiveProviderConfigured returns true for mock")
    void mockProviderConfigured() {
        // test profile sets provider=mock
        assertThat(props.isActiveProviderConfigured()).isTrue();
    }

    @Test
    @DisplayName("Deepgram.isConfigured returns false for placeholder key")
    void deepgramNotConfigured() {
        SttProperties.Deepgram dg = new SttProperties.Deepgram();
        dg.setApiKey("REPLACE_WITH_YOUR_DEEPGRAM_KEY");
        assertThat(dg.isConfigured()).isFalse();
    }

    @Test
    @DisplayName("Deepgram.isConfigured returns false for blank key")
    void deepgramBlankKey() {
        SttProperties.Deepgram dg = new SttProperties.Deepgram();
        dg.setApiKey("   ");
        assertThat(dg.isConfigured()).isFalse();
    }

    @Test
    @DisplayName("Deepgram.isConfigured returns true for real-looking key")
    void deepgramRealKey() {
        SttProperties.Deepgram dg = new SttProperties.Deepgram();
        dg.setApiKey("abc123def456ghi789jkl012mno345pqr678stu");
        assertThat(dg.isConfigured()).isTrue();
    }
}
