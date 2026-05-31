package com.labmentix.speechmatrix.util;

import com.labmentix.speechmatrix.dto.TranscriptionDto;
import com.labmentix.speechmatrix.model.Transcription;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Single place for all Transcription entity ↔ DTO conversions.
 *
 * Output shapes:
 *  Response             — full detail (GET /api/speech/:id, POST /upload)
 *  Summary              — lightweight list item (GET /api/speech/history)
 *  PagedResponse        — wraps Page<Summary> with pagination + sort metadata
 *  SearchResult         — summary with highlighted preview (GET /api/speech/search)
 *  SearchPagedResponse  — wraps Page<Transcription> for paginated search
 */
@Component
public class TranscriptionMapper {

    private static final int PREVIEW_LENGTH  = 120;

    // ── Entity → Response ─────────────────────────────────────────────────────
    public TranscriptionDto.Response toResponse(Transcription t) {
        if (t == null) return null;

        TranscriptionDto.Response r = new TranscriptionDto.Response();
        r.setId(t.getId());
        r.setAudioFile(t.getAudioFile());
        r.setTranscript(t.getTranscript());
        r.setSummary(t.getSummary());
        r.setLanguage(t.getLanguage());
        r.setSpeakerCount(t.getSpeakerCount());
        r.setDurationSeconds(t.getDurationSeconds());
        r.setConfidenceScore(t.getConfidenceScore());
        r.setWordCount(t.getWordCount());
        r.setSttProvider(t.getSttProvider());
        r.setStatus(t.getStatus());
        r.setCreatedAt(t.getCreatedAt());
        r.setUpdatedAt(t.getUpdatedAt());
        return r;
    }

    // ── Entity → Summary ──────────────────────────────────────────────────────
    public TranscriptionDto.Summary toSummary(Transcription t) {
        if (t == null) return null;

        TranscriptionDto.Summary s = new TranscriptionDto.Summary();
        s.setId(t.getId());
        s.setTranscriptPreview(truncate(t.getTranscript(), PREVIEW_LENGTH));
        s.setLanguage(t.getLanguage());
        s.setWordCount(t.getWordCount());
        s.setDurationSeconds(t.getDurationSeconds());
        s.setConfidenceScore(t.getConfidenceScore());
        s.setSttProvider(t.getSttProvider());
        s.setStatus(t.getStatus());
        s.setCreatedAt(t.getCreatedAt());
        return s;
    }

    // ── Page<Entity> → PagedResponse ──────────────────────────────────────────
    public TranscriptionDto.PagedResponse toPagedResponse(
            Page<Transcription> page,
            String sortField,
            String sortDir) {

        TranscriptionDto.PagedResponse pr = new TranscriptionDto.PagedResponse();
        pr.setContent(page.getContent().stream().map(this::toSummary).toList());
        pr.setPage(page.getNumber());
        pr.setSize(page.getSize());
        pr.setTotalElements(page.getTotalElements());
        pr.setTotalPages(page.getTotalPages());
        pr.setLast(page.isLast());
        pr.setFirst(page.isFirst());

        // Include sort metadata so the frontend knows current sort state
        TranscriptionDto.PagedResponse.SortInfo sort =
            new TranscriptionDto.PagedResponse.SortInfo();
        sort.setField(sortField);
        sort.setDirection(sortDir);
        pr.setSort(sort);

        return pr;
    }

    // Overload — no sort info (used by simple callers)
    public TranscriptionDto.PagedResponse toPagedResponse(Page<Transcription> page) {
        return toPagedResponse(page, "createdAt", "desc");
    }

    // ── Page<Entity> → SearchPagedResponse ───────────────────────────────────
    public TranscriptionDto.SearchPagedResponse toSearchPagedResponse(
            Page<Transcription> page, String query) {

        TranscriptionDto.SearchPagedResponse r =
            new TranscriptionDto.SearchPagedResponse();
        r.setQuery(query);
        r.setContent(page.getContent().stream()
            .map(t -> TranscriptionDto.SearchResult.from(t, query))
            .toList());
        r.setPage(page.getNumber());
        r.setSize(page.getSize());
        r.setTotalElements(page.getTotalElements());
        r.setTotalPages(page.getTotalPages());
        r.setLast(page.isLast());
        r.setFirst(page.isFirst());
        return r;
    }

    // ── List<Entity> → List<Summary> ─────────────────────────────────────────
    public List<TranscriptionDto.Summary> toSummaryList(List<Transcription> list) {
        return list.stream().map(this::toSummary).toList();
    }

    // ── List<Entity> → List<SearchResult> ────────────────────────────────────
    public List<TranscriptionDto.SearchResult> toSearchResultList(
            List<Transcription> list, String query) {
        return list.stream()
            .map(t -> TranscriptionDto.SearchResult.from(t, query))
            .toList();
    }

    // ── Private ───────────────────────────────────────────────────────────────
    private String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() > maxLen
            ? text.substring(0, maxLen) + "…"
            : text;
    }
}