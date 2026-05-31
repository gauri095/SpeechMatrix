package com.labmentix.speechmatrix.exception;

import com.labmentix.speechmatrix.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
//import org.springframework.web.HttpRequestMethodNotAllowedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Central exception handler for the entire application.
 *
 * Every handler:
 *  1. Returns ApiError — consistent shape every time
 *  2. Includes path — so the client knows which endpoint failed
 *  3. Includes requestId — correlates with server logs for support
 *  4. Logs at appropriate level — WARN for expected errors, ERROR for bugs
 *
 * Handler order: Spring picks the most specific handler. The catch-all
 * Exception.class handler at the bottom never fires if a more specific
 * handler matches.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── Validation (@Valid / @Validated) ──────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        f -> f.getDefaultMessage() != null
                                ? f.getDefaultMessage() : "Invalid value",
                        (a, b) -> a
                ));

        log.warn("Validation failed [{}]: {}", request.getRequestURI(), fieldErrors);

        return ResponseEntity.badRequest().body(
                ApiError.of(400, "Validation Failed",
                        "One or more fields are invalid",
                        request.getRequestURI(), fieldErrors));
    }

    // ── Missing required request parameter ───────────────────────────────────
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        log.warn("Missing param '{}' at [{}]", ex.getParameterName(), request.getRequestURI());

        return ResponseEntity.badRequest().body(
                ApiError.of(400, "Bad Request",
                        "Required parameter '" + ex.getParameterName() + "' is missing",
                        request.getRequestURI()));
    }

    // ── Wrong type for path variable / request param ──────────────────────────
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String message = String.format(
                "Parameter '%s' must be of type %s but received: '%s'",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown",
                ex.getValue());

        log.warn("Type mismatch at [{}]: {}", request.getRequestURI(), message);

        return ResponseEntity.badRequest().body(
                ApiError.of(400, "Bad Request", message, request.getRequestURI()));
    }

    // ── Auth: wrong credentials ───────────────────────────────────────────────
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {

        log.warn("Bad credentials attempt at [{}]", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiError.withHint(401, "Unauthorized",
                        "Email or password is incorrect",
                        request.getRequestURI(),
                        "Check your credentials. Use POST /api/auth/register to create an account."));
    }

    // ── Auth: account disabled / locked ──────────────────────────────────────
    @ExceptionHandler({ LockedException.class, DisabledException.class })
    public ResponseEntity<ApiError> handleAccountStatus(
            Exception ex, HttpServletRequest request) {

        log.warn("Account status issue at [{}]: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiError.of(403, "Forbidden", ex.getMessage(), request.getRequestURI()));
    }

    // ── 404: resource not found ───────────────────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.debug("Resource not found at [{}]: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.of(404, "Not Found", ex.getMessage(), request.getRequestURI()));
    }

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<Map<String, Object>> handleAll(
//            Exception ex, HttpServletRequest request) {
//
//        // Handle 405 inline without importing the specific exception class
//        if (ex.getClass().getSimpleName().equals("HttpRequestMethodNotAllowedException")) {
//            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
//                    body(405, "Method Not Allowed",
//                            "This HTTP method is not supported for this endpoint",
//                            request.getRequestURI()));
//        }
//
//        log.error("Unhandled exception at [{}]: ", request.getRequestURI(), ex);
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
//                body(500, "Internal Server Error",
//                        "An unexpected error occurred", request.getRequestURI()));
//    }

    // ── 404: no handler found (unknown route) ─────────────────────────────────
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> handleNoHandler(
            NoHandlerFoundException ex, HttpServletRequest request) {

        log.debug("No handler for {} {}", ex.getHttpMethod(), ex.getRequestURL());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.withHint(404, "Not Found",
                        "No endpoint found for " + ex.getHttpMethod() + " " + ex.getRequestURL(),
                        request.getRequestURI(),
                        "Check the API documentation. Valid routes start with /api/"));
    }



    // ── 409: duplicate resource ───────────────────────────────────────────────
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicate(
            DuplicateResourceException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiError.of(409, "Conflict", ex.getMessage(), request.getRequestURI()));
    }

    // ── 413: file too large ───────────────────────────────────────────────────
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {

        log.warn("Upload too large at [{}]", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
                ApiError.withHint(413, "Payload Too Large",
                        "File exceeds the 50 MB limit",
                        request.getRequestURI(),
                        "Compress the audio or split it into smaller files"));
    }

    // ── 415: wrong content type ───────────────────────────────────────────────
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaType(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(
                ApiError.withHint(415, "Unsupported Media Type",
                        "Content-Type '" + ex.getContentType() + "' is not supported",
                        request.getRequestURI(),
                        "Use 'Content-Type: application/json' for JSON endpoints "
                                + "or 'Content-Type: multipart/form-data' for file uploads"));
    }

    // ── 422: invalid audio file ───────────────────────────────────────────────
    @ExceptionHandler(InvalidAudioFileException.class)
    public ResponseEntity<ApiError> handleInvalidAudio(
            InvalidAudioFileException ex, HttpServletRequest request) {

        log.warn("Invalid audio at [{}]: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                ApiError.withHint(422, "Unprocessable Entity",
                        ex.getMessage(),
                        request.getRequestURI(),
                        "Supported formats: MP3, WAV, M4A, FLAC, OGG, WebM"));
    }

    // ── 400: illegal argument ─────────────────────────────────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArg(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.warn("Illegal argument at [{}]: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.badRequest().body(
                ApiError.of(400, "Bad Request", ex.getMessage(), request.getRequestURI()));
    }

    // ── 501: not yet implemented ──────────────────────────────────────────────
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ApiError> handleNotImplemented(
            UnsupportedOperationException ex, HttpServletRequest request) {

        log.info("Not implemented endpoint called: [{}]", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(
                ApiError.withHint(501, "Not Implemented",
                        ex.getMessage() != null ? ex.getMessage()
                                : "This feature is not yet implemented",
                        request.getRequestURI(),
                        "Check the roadmap — this endpoint will be available soon"));
    }

    // ── 502: STT provider error ───────────────────────────────────────────────
    @ExceptionHandler(SttProviderException.class)
    public ResponseEntity<ApiError> handleSttProvider(
            SttProviderException ex, HttpServletRequest request) {

        log.error("STT provider error at [{}]: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                ApiError.withHint(502, "Bad Gateway",
                        ex.getMessage() != null ? ex.getMessage()
                                : "Speech recognition service is unavailable",
                        request.getRequestURI(),
                        "Check your STT API key in application.properties "
                                + "and verify the audio file is a valid format"));
    }

    // ── 500: catch-all ────────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAll(
            Exception ex, HttpServletRequest request) {

        // Log full stack trace for unexpected exceptions
        log.error("Unhandled exception at [{}]: ", request.getRequestURI(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiError.withHint(500, "Internal Server Error",
                        "An unexpected error occurred",
                        request.getRequestURI(),
                        "If this persists, contact support with requestId: "
                                + org.slf4j.MDC.get("requestId")));
    }
}