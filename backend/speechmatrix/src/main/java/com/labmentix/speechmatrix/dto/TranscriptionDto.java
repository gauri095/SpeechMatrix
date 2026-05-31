package com.labmentix.speechmatrix.dto;

import com.labmentix.speechmatrix.model.Transcription;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class TranscriptionDto {

    // ── Upload / record request ───────────────────────────────────────────────
    @Data
    public static class UploadRequest {

        @Pattern(
            regexp = "^[a-z]{2}-[A-Z]{2}$",
            message = "Language must be a BCP-47 code like en-US, hi-IN, fr-FR"
        )
        private String  language     = "en-US";
        private Integer speakerCount;
    }

    // ── History filter (query params for GET /history) ────────────────────────
    @Data
    public static class HistoryFilter {

        /** 0-indexed page number */
        private int    page    = 0;

        /** Items per page — capped at 50 in service */
        private int    size    = 10;

        /** pending | processing | done | failed | null = all */
        private String status;

        /** BCP-47 code e.g. en-US | null = all */
        private String language;

        /** createdAt (default) | wordCount | durationSeconds | confidenceScore */
        private String sortBy  = "createdAt";

        /** desc (default) | asc */
        private String sortDir = "desc";

        /** Date range filter — ISO-8601 strings */
        private String from;
        private String to;

        public boolean hasStatus()   { return status   != null && !status.isBlank(); }
        public boolean hasLanguage() { return language != null && !language.isBlank(); }
        public boolean hasFrom()     { return from     != null && !from.isBlank(); }
        public boolean hasTo()       { return to       != null && !to.isBlank(); }
    }

    // ── Full detail (GET /api/speech/:id) ─────────────────────────────────────
    @Data
    public static class Response {
        private Long          id;
        private String        audioFile;
        private String        transcript;
        private String        summary;
        private String        language;
        private Integer       speakerCount;
        private Double        durationSeconds;
        private Double        confidenceScore;
        private Integer       wordCount;
        private String        sttProvider;
        private String        status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    // ── List item (GET /api/speech/history) ───────────────────────────────────
    @Data
    public static class Summary {
        private Long          id;
        private String        transcriptPreview;   // first 120 chars
        private String        language;
        private Integer       wordCount;
        private Double        durationSeconds;
        private Double        confidenceScore;
        private String        sttProvider;
        private String        status;
        private LocalDateTime createdAt;
    }

    // ── Search result with highlighted match ──────────────────────────────────
    @Data
    public static class SearchResult {
        private Long          id;
        private String        highlightedPreview;  // snippet with <mark>match</mark>
        private String        language;
        private Integer       wordCount;
        private Double        durationSeconds;
        private String        status;
        private LocalDateTime createdAt;

        /**
         * Builds a search result with a 200-char preview around the first match,
         * wrapped in <mark> tags for frontend highlighting.
         */
        public static SearchResult from(Transcription t, String query) {
            SearchResult r = new SearchResult();
            r.setId(t.getId());
            r.setLanguage(t.getLanguage());
            r.setWordCount(t.getWordCount());
            r.setDurationSeconds(t.getDurationSeconds());
            r.setStatus(t.getStatus());
            r.setCreatedAt(t.getCreatedAt());
            r.setHighlightedPreview(buildPreview(t.getTranscript(), query));
            return r;
        }

        private static String buildPreview(String text, String query) {
            if (text == null || query == null) return null;

            String lower = text.toLowerCase();
            int    idx   = lower.indexOf(query.toLowerCase());
            if (idx < 0) {
                // No match found — return plain start
                return text.length() > 200 ? text.substring(0, 200) + "…" : text;
            }

            // Show up to 80 chars before and after the match
            int start  = Math.max(0, idx - 80);
            int end    = Math.min(text.length(), idx + query.length() + 80);

            String prefix   = start > 0 ? "…" : "";
            String suffix   = end < text.length() ? "…" : "";
            String before   = text.substring(start, idx);
            String match    = text.substring(idx, idx + query.length());
            String after    = text.substring(idx + query.length(), end);

            return prefix + before + "<mark>" + match + "</mark>" + after + suffix;
        }
    }

    // ── Paginated response wrapper ────────────────────────────────────────────
    @Data
    public static class PagedResponse {
        private List<Summary> content;
        private int     page;
        private int     size;
        private long    totalElements;
        private int     totalPages;
        private boolean last;
        private boolean first;
        private SortInfo sort;

        @Data
        public static class SortInfo {
            private String field;
            private String direction;
        }
    }

    // ── Paginated search response ─────────────────────────────────────────────
    @Data
    public static class SearchPagedResponse {
        private List<SearchResult> content;
        private String             query;
        private int                page;
        private int                size;
        private long               totalElements;
        private int                totalPages;
        private boolean            last;
        private boolean            first;
    }

    // ── Usage statistics ──────────────────────────────────────────────────────
    @Data
    public static class StatsResponse {
        private long   totalTranscriptions;
        private long   totalWords;
        private double totalDurationSeconds;
        private long   doneCount;
        private long   failedCount;
        private long   pendingCount;
        private long   processingCount;
        private double avgConfidence;
        private List<String> languages;      // distinct languages used
    }
}