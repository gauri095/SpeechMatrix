package com.labmentix.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labmentix.speechmatrix.dto.AuthDto;
import com.labmentix.speechmatrix.model.Transcription;
import com.labmentix.speechmatrix.model.User;
import com.labmentix.speechmatrix.repository.TranscriptionRepository;
import com.labmentix.speechmatrix.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("History & Search end-to-end tests")
class HistoryAndSearchE2ETest {

    @Autowired MockMvc                 mockMvc;
    @Autowired ObjectMapper            objectMapper;
    @Autowired UserRepository          userRepository;
    @Autowired TranscriptionRepository transcriptionRepository;
    @Autowired PasswordEncoder         passwordEncoder;

    static String token;
    static Long   userId;

    @BeforeEach
    void setUp() throws Exception {
        if (token != null) return;  // only run once

        transcriptionRepository.deleteAll();
        userRepository.deleteAll();

        // Create a test user
        User user = userRepository.save(User.builder()
            .name("E2E Tester")
            .email("e2e@test.com")
            .password(passwordEncoder.encode("TestPass1"))
            .build());
        userId = user.getId();

        // Seed transcriptions
        seed(user, "Spring Boot microservices architecture guide",    "en-US", "done",    250, 0.97, 45.0);
        seed(user, "PostgreSQL indexing and query optimisation tips", "en-US", "done",    180, 0.95, 30.0);
        seed(user, "React hooks and state management patterns",       "en-US", "done",    160, 0.94, 25.0);
        seed(user, "Hindi language voice transcription namaskar",     "hi-IN", "done",    90,  0.91, 15.0);
        seed(user, "Failed audio file — corrupted recording",         "en-US", "failed",  0,   null, 0.0);

        // Login and get token
        AuthDto.LoginRequest login = new AuthDto.LoginRequest();
        login.setEmail("e2e@test.com");
        login.setPassword("TestPass1");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andReturn();

        token = objectMapper.readTree(
            result.getResponse().getContentAsString()).get("token").asText();
    }

    @AfterAll
    static void cleanup(@Autowired TranscriptionRepository tr,
                        @Autowired UserRepository ur) {
        tr.deleteAll();
        ur.deleteAll();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/speech/history
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(1)
    @DisplayName("GET /history returns all 5 seeded transcriptions")
    void historyAll() throws Exception {
        mockMvc.perform(get("/api/speech/history")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalElements").value(5))
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.content[0].transcriptPreview").isNotEmpty())
            .andExpect(jsonPath("$.data.sort.field").value("createdAt"))
            .andExpect(jsonPath("$.data.sort.direction").value("desc"));
    }

    @Test @Order(2)
    @DisplayName("GET /history?size=2 returns page of 2 with correct metadata")
    void historyPaginated() throws Exception {
        mockMvc.perform(get("/api/speech/history?page=0&size=2")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", hasSize(2)))
            .andExpect(jsonPath("$.data.totalElements").value(5))
            .andExpect(jsonPath("$.data.totalPages").value(3))
            .andExpect(jsonPath("$.data.first").value(true))
            .andExpect(jsonPath("$.data.last").value(false));
    }

    @Test @Order(3)
    @DisplayName("GET /history?page=2&size=2 returns last page with 1 item")
    void historyLastPage() throws Exception {
        mockMvc.perform(get("/api/speech/history?page=2&size=2")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", hasSize(1)))
            .andExpect(jsonPath("$.data.last").value(true))
            .andExpect(jsonPath("$.data.first").value(false));
    }

    @Test @Order(4)
    @DisplayName("GET /history?status=done returns only done transcriptions")
    void historyFilterByStatus() throws Exception {
        mockMvc.perform(get("/api/speech/history?status=done")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(4))
            .andExpect(jsonPath("$.data.content[*].status",
                everyItem(equalTo("done"))));
    }

    @Test @Order(5)
    @DisplayName("GET /history?status=failed returns only failed transcriptions")
    void historyFilterFailed() throws Exception {
        mockMvc.perform(get("/api/speech/history?status=failed")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.content[0].status").value("failed"));
    }

    @Test @Order(6)
    @DisplayName("GET /history?language=hi-IN returns only Hindi transcription")
    void historyFilterByLanguage() throws Exception {
        mockMvc.perform(get("/api/speech/history?language=hi-IN")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.content[0].language").value("hi-IN"));
    }

    @Test @Order(7)
    @DisplayName("GET /history?status=done&language=en-US combines filters")
    void historyFilterStatusAndLanguage() throws Exception {
        mockMvc.perform(get("/api/speech/history?status=done&language=en-US")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(3));
    }

    @Test @Order(8)
    @DisplayName("GET /history?size=200 is capped at 50")
    void historySizeCapped() throws Exception {
        mockMvc.perform(get("/api/speech/history?size=200")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.size").value(lessThanOrEqualTo(50)));
    }

    @Test @Order(9)
    @DisplayName("GET /history without token returns 401")
    void historyNoToken() throws Exception {
        mockMvc.perform(get("/api/speech/history"))
            .andExpect(status().isUnauthorized());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/speech/{id}
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(10)
    @DisplayName("GET /{id} returns full transcript for valid owned id")
    void getByIdFound() throws Exception {
        Long id = firstId();

        mockMvc.perform(get("/api/speech/" + id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(id))
            .andExpect(jsonPath("$.data.transcript").isNotEmpty())
            .andExpect(jsonPath("$.data.status").exists())
            .andExpect(jsonPath("$.data.wordCount").exists())
            .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test @Order(11)
    @DisplayName("GET /{id} returns 404 for non-existent id")
    void getByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/speech/99999")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test @Order(12)
    @DisplayName("GET /{id} returns 404 for id belonging to another user")
    void getByIdWrongUser() throws Exception {
        // Create another user's transcription
        User other = userRepository.save(User.builder()
            .name("Other").email("other@test.com").password("hashed").build());
        Transcription t = transcriptionRepository.save(Transcription.builder()
            .user(other).transcript("Private note").status("done")
            .language("en-US").build());

        // Try to access with our token
        mockMvc.perform(get("/api/speech/" + t.getId())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/speech/search
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(13)
    @DisplayName("GET /search?q=Spring returns matching results with highlight")
    void searchFindsMatch() throws Exception {
        mockMvc.perform(get("/api/speech/search?q=Spring")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.query").value("Spring"))
            .andExpect(jsonPath("$.data.content[0].highlightedPreview")
                .value(containsString("<mark>")));
    }

    @Test @Order(14)
    @DisplayName("GET /search?q=optimisation finds case-insensitive match")
    void searchCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/speech/search?q=OPTIMISATION")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test @Order(15)
    @DisplayName("GET /search?q=xyz returns empty results (no match)")
    void searchNoMatch() throws Exception {
        mockMvc.perform(get("/api/speech/search?q=xyz123notexist")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(0))
            .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test @Order(16)
    @DisplayName("GET /search?q=a returns 400 (query too short)")
    void searchQueryTooShort() throws Exception {
        mockMvc.perform(get("/api/speech/search?q=a")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest());
    }

    @Test @Order(17)
    @DisplayName("GET /search paginated returns correct page metadata")
    void searchPaginated() throws Exception {
        mockMvc.perform(get("/api/speech/search?q=audio&page=0&size=1")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.size").value(1));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/speech/stats
    // ══════════════════════════════════════════════════════════════════════════

    @Test @Order(18)
    @DisplayName("GET /stats returns correct aggregated statistics")
    void statsCorrect() throws Exception {
        mockMvc.perform(get("/api/speech/stats")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalTranscriptions").value(5))
            .andExpect(jsonPath("$.data.doneCount").value(4))
            .andExpect(jsonPath("$.data.failedCount").value(1))
            .andExpect(jsonPath("$.data.totalWords").value(680))   // 250+180+160+90
            .andExpect(jsonPath("$.data.totalDurationSeconds").value(115.0))
            .andExpect(jsonPath("$.data.avgConfidence").isNumber())
            .andExpect(jsonPath("$.data.languages").isArray())
            .andExpect(jsonPath("$.data.languages", hasItems("en-US", "hi-IN")));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void seed(User user, String transcript, String lang, String status,
                      int words, Double conf, double dur) {
        transcriptionRepository.save(Transcription.builder()
            .user(user)
            .transcript(transcript)
            .language(lang)
            .status(status)
            .wordCount(words)
            .confidenceScore(conf)
            .durationSeconds(dur)
            .sttProvider("mock")
            .build());
    }

    private Long firstId() {
        return transcriptionRepository
            .findByUserIdOrderByCreatedAtDesc(userId, org.springframework.data.domain.PageRequest.of(0, 1))
            .getContent().get(0).getId();
    }
}