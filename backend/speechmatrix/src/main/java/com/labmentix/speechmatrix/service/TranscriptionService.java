package com.labmentix.speechmatrix.service;

import com.labmentix.speechmatrix.dto.TranscriptionDto;
import com.labmentix.speechmatrix.exception.ResourceNotFoundException;
import com.labmentix.speechmatrix.exception.SttProviderException;
import com.labmentix.speechmatrix.model.Transcription;
import com.labmentix.speechmatrix.model.User;
import com.labmentix.speechmatrix.repository.TranscriptionRepository;
import com.labmentix.speechmatrix.service.stt.*;
import com.labmentix.speechmatrix.util.AudioFileValidator;
import com.labmentix.speechmatrix.util.TranscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranscriptionService {

    private final TranscriptionRepository transcriptionRepository;
    private final TranscriptionMapper     transcriptionMapper;
    private final UserService             userService;
    private final AudioStorageService     audioStorageService;
    private final AudioFileValidator      audioFileValidator;
    private final SpeechService           speechService;

    private static final int MAX_PAGE_SIZE = 50;

    // ══════════════════════════════════════════════════════════════════════════
    // UPLOAD & TRANSCRIBE
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public TranscriptionDto.Response uploadAndTranscribe(
            String email,
            MultipartFile file,
            TranscriptionDto.UploadRequest request) {

        User user = userService.getByEmail(email);
        log.info("Upload: user={} file={} size={} lang={}",
            email, file.getOriginalFilename(), file.getSize(),
            request != null ? request.getLanguage() : "en-US");

        audioFileValidator.validate(file);

        String storedPath = audioStorageService.store(file, user.getId());
        String mimeType   = audioFileValidator.getMimeType(file.getOriginalFilename());
        String lang       = (request != null && request.getLanguage() != null)
                          ? request.getLanguage() : "en-US";

        // Persist with status=processing so the row is visible in history
        Transcription transcription = Transcription.builder()
                .user(user)
                .audioFile(storedPath)
                .language(lang)
                .status(Transcription.STATUS_PROCESSING)
                .sttProvider(speechService.getProviderName())
                .build();

        if (request != null && request.getSpeakerCount() != null) {
            transcription.setSpeakerCount(request.getSpeakerCount());
        }
        transcription = transcriptionRepository.save(transcription);

        try {
            File audioFile = audioStorageService.load(storedPath);
            boolean diarize = request != null
                && request.getSpeakerCount() != null
                && request.getSpeakerCount() > 1;

            SttResult result = speechService.needsChunking(audioFile)
                ? speechService.transcribeLargeFile(audioFile, lang, mimeType)
                : speechService.transcribeFile(audioFile, lang, mimeType, diarize);

            transcription.setTranscript(result.getTranscript());
            transcription.setConfidenceScore(result.getConfidence());
            transcription.setDurationSeconds(result.getDurationSeconds());
            transcription.setWordCount(result.getWordCount());
            if (result.getSpeakerCount() != null)
                transcription.setSpeakerCount(result.getSpeakerCount());
            transcription.setStatus(Transcription.STATUS_DONE);
            transcription = transcriptionRepository.save(transcription);

            log.info("Transcription done: id={} words={}", transcription.getId(), result.getWordCount());

        } catch (SttException e) {
            transcription.setStatus(Transcription.STATUS_FAILED);
            transcriptionRepository.save(transcription);
            log.error("STT failed id={}: {}", transcription.getId(), e.getMessage());
            throw new SttProviderException("Transcription failed: " + e.getMessage(), e);
        }

        return transcriptionMapper.toResponse(transcription);
    }

    // ── Stub for mic recording (Day 14) ───────────────────────────────────────
    public TranscriptionDto.Response recordAndTranscribe(
            String email, byte[] audioBytes,
            TranscriptionDto.UploadRequest request) {
        throw new UnsupportedOperationException(
            "recordAndTranscribe implemented on Day 14");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET HISTORY — paginated, filtered, sorted
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public TranscriptionDto.PagedResponse getHistory(String email,
                                                     TranscriptionDto.HistoryFilter filter) {
        User user    = userService.getByEmail(email);
        int  size    = Math.min(filter.getSize(), MAX_PAGE_SIZE);
        int  page    = Math.max(filter.getPage(), 0);
        String sortBy  = resolveSortField(filter.getSortBy());
        Sort.Direction dir = "asc".equalsIgnoreCase(filter.getSortDir())
            ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));

        Page<Transcription> result;

        // Route to the most specific repository method based on filters
        if (filter.hasStatus() && filter.hasLanguage()) {
            result = transcriptionRepository
                .findByUserIdAndStatusAndLanguageOrderByCreatedAtDesc(
                    user.getId(), filter.getStatus(), filter.getLanguage(), pageable);

        } else if (filter.hasStatus()) {
            result = transcriptionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(
                    user.getId(), filter.getStatus(), pageable);

        } else if (filter.hasLanguage()) {
            result = transcriptionRepository
                .findByUserIdAndLanguageOrderByCreatedAtDesc(
                    user.getId(), filter.getLanguage(), pageable);

        } else if (filter.hasFrom() || filter.hasTo()) {
            LocalDateTime from = filter.hasFrom()
                ? LocalDateTime.parse(filter.getFrom(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : LocalDateTime.of(2000, 1, 1, 0, 0);
            LocalDateTime to = filter.hasTo()
                ? LocalDateTime.parse(filter.getTo(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : LocalDateTime.now();

            result = transcriptionRepository
                .findByUserIdAndDateRange(user.getId(), from, to, pageable);

        } else {
            result = transcriptionRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        }

        log.debug("History[{}]: page={} size={} total={} filters=[status={} lang={}]",
            email, page, size, result.getTotalElements(),
            filter.getStatus(), filter.getLanguage());

        return transcriptionMapper.toPagedResponse(result, filter.getSortBy(), filter.getSortDir());
    }

    // Convenience overload used by tests that don't need filters
    @Transactional(readOnly = true)
    public TranscriptionDto.PagedResponse getHistory(String email, int page, int size) {
        TranscriptionDto.HistoryFilter filter = new TranscriptionDto.HistoryFilter();
        filter.setPage(page);
        filter.setSize(size);
        return getHistory(email, filter);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET BY ID
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public TranscriptionDto.Response getById(String email, Long id) {
        User user = userService.getByEmail(email);
        Transcription t = transcriptionRepository
            .findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Transcription", id));
        return transcriptionMapper.toResponse(t);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SEARCH — full-text, paginated, with highlights
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public TranscriptionDto.SearchPagedResponse search(
            String email, String query, int page, int size) {

        User user = userService.getByEmail(email);
        size = Math.min(size, MAX_PAGE_SIZE);

        Pageable pageable = PageRequest.of(page, size,
            Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Transcription> results = transcriptionRepository
            .searchByTranscriptPaged(user.getId(), query.trim(), pageable);

        log.debug("Search '{}' for {}: {} results (page {}/{})",
            query, email, results.getTotalElements(), page, results.getTotalPages());

        return transcriptionMapper.toSearchPagedResponse(results, query);
    }

    // List-based search (for small result sets / autocomplete)
    @Transactional(readOnly = true)
    public List<TranscriptionDto.SearchResult> searchList(String email, String query) {
        User user = userService.getByEmail(email);
        List<Transcription> results =
            transcriptionRepository.searchByTranscript(user.getId(), query.trim());
        return transcriptionMapper.toSearchResultList(results, query);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // STATS
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public TranscriptionDto.StatsResponse getStats(String email) {
        User user   = userService.getByEmail(email);
        Long userId = user.getId();

        TranscriptionDto.StatsResponse stats = new TranscriptionDto.StatsResponse();
        stats.setTotalTranscriptions(transcriptionRepository.countByUserId(userId));

        Long words = transcriptionRepository.sumWordCountByUserId(userId);
        stats.setTotalWords(words != null ? words : 0L);

        Double dur = transcriptionRepository.sumDurationByUserId(userId);
        stats.setTotalDurationSeconds(dur != null ? dur : 0.0);

        stats.setDoneCount(
            transcriptionRepository.countByUserIdAndStatus(userId, Transcription.STATUS_DONE));
        stats.setFailedCount(
            transcriptionRepository.countByUserIdAndStatus(userId, Transcription.STATUS_FAILED));
        stats.setPendingCount(
            transcriptionRepository.countByUserIdAndStatus(userId, Transcription.STATUS_PENDING));
        stats.setProcessingCount(
            transcriptionRepository.countByUserIdAndStatus(userId, Transcription.STATUS_PROCESSING));

        Double avgConf = transcriptionRepository.avgConfidenceByUserId(userId);
        stats.setAvgConfidence(avgConf != null ? avgConf : 0.0);

        stats.setLanguages(transcriptionRepository.findDistinctLanguagesByUserId(userId));

        return stats;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DELETE
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public void delete(String email, Long id) {
        User user = userService.getByEmail(email);
        Transcription t = transcriptionRepository
            .findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Transcription", id));

        audioStorageService.delete(t.getAudioFile());
        transcriptionRepository.delete(t);
        log.info("Deleted transcription {} for {}", id, email);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    @Transactional
    public Transcription save(Transcription t) {
        return transcriptionRepository.save(t);
    }

    /**
     * Maps sort field name from API param to JPA entity field name.
     * Prevents injection if an unexpected value is passed.
     */
    private String resolveSortField(String sortBy) {
        if (sortBy == null) return "createdAt";
        return switch (sortBy.toLowerCase()) {
            case "wordcount"       -> "wordCount";
            case "duration",
                 "durationseconds" -> "durationSeconds";
            case "confidence",
                 "confidencescore" -> "confidenceScore";
            default                -> "createdAt";
        };
    }
}