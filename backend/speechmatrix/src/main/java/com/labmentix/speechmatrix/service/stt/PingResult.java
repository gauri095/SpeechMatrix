package com.labmentix.speechmatrix.service.stt;

import lombok.Builder;
import lombok.Data;

/**
 * Result from SttProvider.ping() â€” connectivity check.
 */
@Data
@Builder
public class PingResult {

    private String  provider;
    private boolean connected;
    private long    latencyMs;
    private String  model;
    private String  message;

    public static PingResult ok(String provider, String model, long latencyMs) {
        return PingResult.builder()
                .provider(provider)
                .connected(true)
                .model(model)
                .latencyMs(latencyMs)
                .message("Connection successful")
                .build();
    }

    public static PingResult fail(String provider, String reason) {
        return PingResult.builder()
                .provider(provider)
                .connected(false)
                .latencyMs(-1)
                .message(reason)
                .build();
    }
}
