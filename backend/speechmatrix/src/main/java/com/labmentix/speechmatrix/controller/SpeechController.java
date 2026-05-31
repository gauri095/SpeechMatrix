package com.labmentix.speechmatrix.controller;

import com.labmentix.speechmatrix.dto.ApiResponse;
import com.labmentix.speechmatrix.dto.TranscriptionDto;
import com.labmentix.speechmatrix.service.TranscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/speech")
@RequiredArgsConstructor
public class SpeechController {

    private final TranscriptionService transcriptionService;

    // ── POST /api/speech/upload ───────────────────────────────────────────────
    @PostMapping(value = "/upload",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TranscriptionDto.Response>> upload(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "language",     defaultValue = "en-US") String language,
            @RequestParam(value = "speakerCount", required = false)        Integer speakerCount) {

        TranscriptionDto.UploadRequest request = new TranscriptionDto.UploadRequest();
        request.setLanguage(language);
        request.setSpeakerCount(speakerCount);

        TranscriptionDto.Response result =
            transcriptionService.uploadAndTranscribe(user.getUsername(), file, request);

        return ResponseEntity.status(201)
            .body(ApiResponse.success("Audio uploaded and transcribed", result));
    }

    // ── GET /api/speech/history ───────────────────────────────────────────────
    // All params optional — returns all transcriptions when no filters set
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<TranscriptionDto.PagedResponse>> history(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "0")         int    page,
            @RequestParam(defaultValue = "10")        int    size,
            @RequestParam(required = false)           String status,
            @RequestParam(required = false)           String language,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir,
            @RequestParam(required = false)           String from,
            @RequestParam(required = false)           String to) {

        TranscriptionDto.HistoryFilter filter = new TranscriptionDto.HistoryFilter();
        filter.setPage(page);
        filter.setSize(Math.min(size, 50));
        filter.setStatus(status);
        filter.setLanguage(language);
        filter.setSortBy(sortBy);
        filter.setSortDir(sortDir);
        filter.setFrom(from);
        filter.setTo(to);

        return ResponseEntity.ok(ApiResponse.success(
            "History retrieved",
            transcriptionService.getHistory(user.getUsername(), filter)));
    }

    // ── GET /api/speech/search ────────────────────────────────────────────────
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<?>> search(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam           String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "true") boolean paged) {

        if (q == null || q.trim().length() < 2) {
            return ResponseEntity.badRequest().body(
                ApiResponse.success("Query must be at least 2 characters", List.of()));
        }

        if (paged) {
            // Paginated search with highlights
            return ResponseEntity.ok(ApiResponse.success(
                "Search completed",
                transcriptionService.search(user.getUsername(), q, page, size)));
        } else {
            // Simple list (useful for autocomplete / quick lookup)
            return ResponseEntity.ok(ApiResponse.success(
                "Search completed",
                transcriptionService.searchList(user.getUsername(), q)));
        }
    }

    // ── GET /api/speech/stats ─────────────────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<TranscriptionDto.StatsResponse>> stats(
            @AuthenticationPrincipal UserDetails user) {

        return ResponseEntity.ok(ApiResponse.success(
            transcriptionService.getStats(user.getUsername())));
    }

    // ── GET /api/speech/{id} ──────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TranscriptionDto.Response>> getById(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {

        return ResponseEntity.ok(ApiResponse.success(
            transcriptionService.getById(user.getUsername(), id)));
    }

    // ── DELETE /api/speech/{id} ───────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {

        transcriptionService.delete(user.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.message("Transcription deleted successfully"));
    }

    // ── GET /api/speech/ping ──────────────────────────────────────────────────
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of(
            "status", "Speech API ready",
            "endpoints", Map.of(
                "POST /api/speech/upload",
                    "file(required), language(en-US), speakerCount",
                "GET /api/speech/history",
                    "page, size, status, language, sortBy, sortDir, from, to",
                "GET /api/speech/search",
                    "q(required, min 2 chars), page, size, paged(true/false)",
                "GET /api/speech/stats",  "usage statistics",
                "GET /api/speech/{id}",   "full transcript",
                "DELETE /api/speech/{id}","delete transcript"
            )
        ));
    }
}