package com.labmentix.speechmatrix.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Single canonical shape for every error response in the API.
 *
 * Every error — validation, auth, 404, 500 — returns this shape:
 * {
 *   "timestamp":  "2026-05-26T10:30:00",
 *   "status":     404,
 *   "error":      "Not Found",
 *   "message":    "Transcription with id 42 not found",
 *   "path":       "/api/speech/42",
 *   "requestId":  "abc123def456",      // from MDC — correlates with server logs
 *   "details":    { ... }              // only present for validation errors
 * }
 *
 * Consistent shape = frontend can always read .message for a user-facing string
 * and .requestId for support tickets.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private String    timestamp;
    private int       status;
    private String    error;
    private String    message;
    private String    path;
    private String    requestId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object    details;      // Map<String,String> for validation errors

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String    hint;         // optional user-actionable hint

    // ── Factory ───────────────────────────────────────────────────────────────

    public static ApiError of(int status, String error, String message, String path) {
        return ApiError.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .requestId(MDC.get("requestId"))
                .build();
    }

    public static ApiError of(int status, String error, String message,
                               String path, Object details) {
        return ApiError.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .requestId(MDC.get("requestId"))
                .details(details)
                .build();
    }

    public static ApiError withHint(int status, String error, String message,
                                     String path, String hint) {
        return ApiError.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .hint(hint)
                .requestId(MDC.get("requestId"))
                .build();
    }
}