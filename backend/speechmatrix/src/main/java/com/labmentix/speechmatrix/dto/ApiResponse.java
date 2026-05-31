package com.labmentix.speechmatrix.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Uniform envelope for every successful API response.
 *
 * All controllers return ResponseEntity<ApiResponse<T>> so the
 * frontend always gets the same top-level shape:
 *
 * {
 *   "success":   true,
 *   "message":   "Transcription created",
 *   "data":      { ...payload... },
 *   "timestamp": "2026-05-22T10:30:00"
 * }
 *
 * On error, GlobalExceptionHandler returns its own map â?? this wrapper
 * is only for successful 2xx responses.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)  // omit null fields (e.g. message)
public class ApiResponse<T> {

    private final boolean       success;
    private final String        message;
    private final T             data;
    private final LocalDateTime timestamp;

    private ApiResponse(boolean success, String message, T data) {
        this.success   = success;
        this.message   = message;
        this.data      = data;
        this.timestamp = LocalDateTime.now();
    }

    // â??â?? Factory methods â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    /** 200/201 with payload and message */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /** 200/201 with payload, no message */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data);
    }

    /** 200/204 with message only, no payload (e.g. delete, logout) */
    public static <Void> ApiResponse<Void> message(String message) {
        return new ApiResponse<>(true, message, null);
    }
}
