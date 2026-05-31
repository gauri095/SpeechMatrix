package com.labmentix.speechmatrix.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Builder;
import lombok.Data;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

/**
 * Single source of truth for all auth error JSON responses.
 *
 * Every 401/403/400 from the security layer uses this shape:
 * {
 *   "timestamp": "2026-05-22T10:30:00",
 *   "status":    401,
 *   "error":     "Unauthorized",
 *   "message":   "Token is invalid or expired",
 *   "path":      "/api/speech/history",
 *   "hint":      "Include header: Authorization: Bearer <token>"
 * }
 */
public class AuthErrorResponse {

    // â”€â”€ Pre-built standard responses â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public static final ErrorShape MISSING_TOKEN = ErrorShape.builder()
            .status(401).error("Unauthorized")
            .message("No Authorization header found")
            .hint("Include header: Authorization: Bearer <token>")
            .build();

    public static final ErrorShape INVALID_TOKEN = ErrorShape.builder()
            .status(401).error("Unauthorized")
            .message("Token is invalid or malformed")
            .hint("Re-authenticate via POST /api/auth/login")
            .build();

    public static final ErrorShape EXPIRED_TOKEN = ErrorShape.builder()
            .status(401).error("Unauthorized")
            .message("Token has expired")
            .hint("Re-authenticate via POST /api/auth/login to get a new token")
            .build();

    public static final ErrorShape REVOKED_TOKEN = ErrorShape.builder()
            .status(401).error("Unauthorized")
            .message("Token has been revoked (logged out)")
            .hint("Re-authenticate via POST /api/auth/login")
            .build();

    public static final ErrorShape USER_DELETED = ErrorShape.builder()
            .status(401).error("Unauthorized")
            .message("User account associated with this token no longer exists")
            .hint("Register a new account at POST /api/auth/register")
            .build();

    public static final ErrorShape BAD_CREDENTIALS = ErrorShape.builder()
            .status(401).error("Unauthorized")
            .message("Email or password is incorrect")
            .hint("Check your credentials and try again")
            .build();

    public static final ErrorShape ACCESS_DENIED = ErrorShape.builder()
            .status(403).error("Forbidden")
            .message("You do not have permission to access this resource")
            .hint(null)
            .build();

    // â”€â”€ Write to servlet response â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public static void write(HttpServletResponse response,
                             HttpServletRequest  request,
                             ErrorShape          shape) throws IOException {
        response.setStatus(shape.getStatus());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String hintJson = shape.getHint() != null
            ? String.format(",\"hint\":\"%s\"", escape(shape.getHint()))
            : "";

        String json = String.format(
            "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\"," +
            "\"message\":\"%s\",\"path\":\"%s\"%s}",
            LocalDateTime.now(),
            shape.getStatus(),
            escape(shape.getError()),
            escape(shape.getMessage()),
            escape(request.getRequestURI()),
            hintJson
        );

        PrintWriter writer = response.getWriter();
        writer.print(json);
        writer.flush();
    }

    // â”€â”€ Escape quotes inside JSON string values â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    // â??â?? Shape model â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    @Data
    @Builder
    public static class ErrorShape {
        private int    status;
        private String error;
        private String message;
        private String hint;
    }
}
