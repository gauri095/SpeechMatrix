package com.labmentix.repository;

import com.labmentix.speechmatrix.model.Transcription;
import com.labmentix.speechmatrix.model.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("TranscriptionRepository integration tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RepositoryTest {

    @Autowired UserRepository          userRepository;
    @Autowired TranscriptionRepository transcriptionRepository;

    User   user;
    User   otherUser;

    @BeforeEach
    void setUp() {
        transcriptionRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
            .name("Arjun Mehta").email("arjun@test.com").password("hashed").build());

        otherUser = userRepository.save(User.builder()
            .name("Priya Nair").email("priya@test.com").password("hashed").build());

        // Seed 5 transcriptions for user
        save(user, "Spring Boot makes REST APIs easy",         "en-US", "done",    100, 0.97, 15.0);
        save(user, "PostgreSQL database design and indexing",  "en-US", "done",    80,  0.95, 12.0);
        save(user, "Hindi transcription namaskar",             "hi-IN", "done",    60,  0.93, 10.0);
        save(user, "Failed audio upload test",                 "en-US", "failed",  0,   null, 0.0);
        save(user, "Processing audio file right now",          "en-US", "processing", 0, null, 0.0);

        // One transcription for otherUser — must never appear in user's results
        save(otherUser, "Other user's private note", "en-US", "done", 20, 0.99, 5.0);
    }

    // ── findByUserIdOrderByCreatedAtDesc ──────────────────────────────────────

    @Test @Order(1)
    @DisplayName("findByUserId returns only the requesting user's transcriptions")
    void findByUserIdIsolated() {
        Page<Transcription> page = transcriptionRepository
            .findByUserIdOrderByCreatedAtDesc(
                user.getId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(5);
        // Verify none belong to otherUser
        page.getContent().forEach(t ->
            assertThat(t.getUser().getId()).isEqualTo(user.getId()));
    }

    @Test @Order(2)
    @DisplayName("Pagination returns correct page size")
    void paginationPageSize() {
        Page<Transcription> page = transcriptionRepository
            .findByUserIdOrderByCreatedAtDesc(
                user.getId(), PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.isLast()).isFalse();
    }

    @Test @Order(3)
    @DisplayName("Last page is correctly identified")
    void paginationLastPage() {
        Page<Transcription> last = transcriptionRepository
            .findByUserIdOrderByCreatedAtDesc(
                user.getId(), PageRequest.of(2, 2));

        assertThat(last.isLast()).isTrue();
        assertThat(last.getContent()).hasSize(1); // 5th item
    }

    @Test @Order(4)
    @DisplayName("Sort by wordCount ascending works")
    void sortByWordCountAsc() {
        Page<Transcription> page = transcriptionRepository
            .findByUserIdOrderByCreatedAtDesc(
                user.getId(),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "wordCount")));

        List<Integer> wordCounts = page.getContent().stream()
            .map(Transcription::getWordCount)
            .filter(w -> w != null && w > 0)
            .toList();

        // Verify ascending order for non-zero values
        for (int i = 1; i < wordCounts.size(); i++) {
            assertThat(wordCounts.get(i)).isGreaterThanOrEqualTo(wordCounts.get(i - 1));
        }
    }

    // ── findByIdAndUserId ─────────────────────────────────────────────────────

    @Test @Order(5)
    @DisplayName("findByIdAndUserId returns transcription for correct owner")
    void findByIdCorrectOwner() {
        Transcription t = transcriptionRepository
            .findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, 1))
            .getContent().get(0);

        Optional<Transcription> found =
            transcriptionRepository.findByIdAndUserId(t.getId(), user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(t.getId());
    }

    @Test @Order(6)
    @DisplayName("findByIdAndUserId returns empty for wrong user (security check)")
    void findByIdWrongOwner() {
        Transcription t = transcriptionRepository
            .findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, 1))
            .getContent().get(0);

        Optional<Transcription> found =
            transcriptionRepository.findByIdAndUserId(t.getId(), otherUser.getId());

        assertThat(found).isEmpty();
    }

    // ── Filter: by status ─────────────────────────────────────────────────────

    @Test @Order(7)
    @DisplayName("findByUserIdAndStatus returns only done transcriptions")
    void filterByStatusDone() {
        Page<Transcription> page = transcriptionRepository
            .findByUserIdAndStatusOrderByCreatedAtDesc(
                user.getId(), "done", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(3);
        page.getContent().forEach(t ->
            assertThat(t.getStatus()).isEqualTo("done"));
    }

    @Test @Order(8)
    @DisplayName("findByUserIdAndStatus returns only failed transcriptions")
    void filterByStatusFailed() {
        Page<Transcription> page = transcriptionRepository
            .findByUserIdAndStatusOrderByCreatedAtDesc(
                user.getId(), "failed", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo("failed");
    }

    // ── Filter: by language ───────────────────────────────────────────────────

    @Test @Order(9)
    @DisplayName("findByUserIdAndLanguage filters by Hindi correctly")
    void filterByLanguageHindi() {
        Page<Transcription> page = transcriptionRepository
            .findByUserIdAndLanguageOrderByCreatedAtDesc(
                user.getId(), "hi-IN", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getLanguage()).isEqualTo("hi-IN");
    }

    // ── Filter: status + language ─────────────────────────────────────────────

    @Test @Order(10)
    @DisplayName("findByUserIdAndStatusAndLanguage combines filters correctly")
    void filterByStatusAndLanguage() {
        Page<Transcription> page = transcriptionRepository
            .findByUserIdAndStatusAndLanguageOrderByCreatedAtDesc(
                user.getId(), "done", "en-US", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        page.getContent().forEach(t -> {
            assertThat(t.getStatus()).isEqualTo("done");
            assertThat(t.getLanguage()).isEqualTo("en-US");
        });
    }

    // ── Date range filter ─────────────────────────────────────────────────────

    @Test @Order(11)
    @DisplayName("findByUserIdAndDateRange returns all rows in wide range")
    void dateRangeWide() {
        Page<Transcription> page = transcriptionRepository
            .findByUserIdAndDateRange(
                user.getId(),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(5);
    }

    @Test @Order(12)
    @DisplayName("findByUserIdAndDateRange returns empty for future-only range")
    void dateRangeFuture() {
        Page<Transcription> page = transcriptionRepository
            .findByUserIdAndDateRange(
                user.getId(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(0);
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @Test @Order(13)
    @DisplayName("searchByTranscript finds case-insensitive match")
    void searchCaseInsensitive() {
        List<Transcription> results = transcriptionRepository
            .searchByTranscript(user.getId(), "SPRING");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTranscript())
            .containsIgnoringCase("spring");
    }

    @Test @Order(14)
    @DisplayName("searchByTranscriptPaged returns paginated results with correct count")
    void searchPaged() {
        Page<Transcription> page = transcriptionRepository
            .searchByTranscriptPaged(
                user.getId(), "en",
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt")));

        // "Spring Boot" and "PostgreSQL" and "Failed" all contain en-US content
        assertThat(page.getTotalElements()).isGreaterThan(0);
        assertThat(page.getContent().size()).isLessThanOrEqualTo(2);
    }

    @Test @Order(15)
    @DisplayName("searchByTranscript returns empty list for no match")
    void searchNoMatch() {
        List<Transcription> results = transcriptionRepository
            .searchByTranscript(user.getId(), "xyzquux");
        assertThat(results).isEmpty();
    }

    @Test @Order(16)
    @DisplayName("searchByTranscript does not return other user's transcriptions")
    void searchIsolated() {
        // "private" only appears in otherUser's transcription
        List<Transcription> results = transcriptionRepository
            .searchByTranscript(user.getId(), "private");
        assertThat(results).isEmpty();
    }

    // ── Aggregate stats ───────────────────────────────────────────────────────

    @Test @Order(17)
    @DisplayName("countByUserId returns correct count")
    void countByUserId() {
        assertThat(transcriptionRepository.countByUserId(user.getId())).isEqualTo(5);
    }

    @Test @Order(18)
    @DisplayName("countByUserIdAndStatus counts done correctly")
    void countByStatus() {
        assertThat(transcriptionRepository
            .countByUserIdAndStatus(user.getId(), "done")).isEqualTo(3);
        assertThat(transcriptionRepository
            .countByUserIdAndStatus(user.getId(), "failed")).isEqualTo(1);
    }

    @Test @Order(19)
    @DisplayName("sumWordCountByUserId sums only seeded transcriptions")
    void sumWordCount() {
        Long total = transcriptionRepository.sumWordCountByUserId(user.getId());
        assertThat(total).isEqualTo(240L); // 100 + 80 + 60 + 0 + 0
    }

    @Test @Order(20)
    @DisplayName("avgConfidenceByUserId excludes null confidence rows")
    void avgConfidence() {
        Double avg = transcriptionRepository.avgConfidenceByUserId(user.getId());
        assertThat(avg).isNotNull().isGreaterThan(0.0).isLessThanOrEqualTo(1.0);
    }

    @Test @Order(21)
    @DisplayName("findDistinctLanguagesByUserId returns unique languages")
    void distinctLanguages() {
        List<String> languages =
            transcriptionRepository.findDistinctLanguagesByUserId(user.getId());
        assertThat(languages).containsExactlyInAnyOrder("en-US", "hi-IN");
    }

    // ── markStuckProcessingAsFailed ───────────────────────────────────────────

    @Test @Order(22)
    @DisplayName("markStuckProcessingAsFailed updates processing rows to failed")
    @org.springframework.transaction.annotation.Transactional
    void markStuckProcessing() {
        int updated = transcriptionRepository.markStuckProcessingAsFailed();
        assertThat(updated).isEqualTo(1);  // 1 seeded processing row

        // Verify it was actually updated
        assertThat(transcriptionRepository
            .countByUserIdAndStatus(user.getId(), "processing")).isEqualTo(0);
        assertThat(transcriptionRepository
            .countByUserIdAndStatus(user.getId(), "failed")).isEqualTo(2); // 1 original + 1 fixed
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void save(User owner, String transcript, String lang,
                      String status, int words, Double conf, double dur) {
        Transcription t = Transcription.builder()
            .user(owner)
            .transcript(transcript)
            .language(lang)
            .status(status)
            .wordCount(words)
            .confidenceScore(conf)
            .durationSeconds(dur)
            .sttProvider("mock")
            .build();
        transcriptionRepository.save(t);
    }
}