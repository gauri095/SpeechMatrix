package com.labmentix.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labmentix.speechmatrix.dto.AuthDto;
import com.labmentix.speechmatrix.model.User;
import com.labmentix.speechmatrix.repository.TranscriptionRepository;
import com.labmentix.speechmatrix.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
@DisplayName("Error handling end-to-end integration tests")
class ErrorHandlingE2ETest {

    @Autowired MockMvc         mockMvc;
    @Autowired ObjectMapper    objectMapper;
    @Autowired UserRepository  userRepository;
    @Autowired TranscriptionRepository transcriptionRepository;
    @Autowired PasswordEncoder passwordEncoder;

    static String token;

    @BeforeEach
    void setUp() throws Exception {
        if (token != null) return;

        transcriptionRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(User.builder()
            .name("Error Tester").email("error@test.com")
            .password(passwordEncoder.encode("TestPass1")).build());

        AuthDto.LoginRequest login = new AuthDto.LoginRequest();
        login.setEmail("error@test.com");
        login.setPassword("TestPass1");

        MvcResult r = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
            .andReturn();

        token = objectMapper.readTree(
            r.getResponse().getContentAsString()).get("token").asText();
    }

    @AfterAll
    static void cleanup(@Autowired TranscriptionRepository tr,
                        @Autowired UserRepository ur) {
        tr.deleteAll(); ur.deleteAll();
    }

    // ── Error response shape ──────────────────────────────────────────────────

    @Test @Order(1)
    @DisplayName("Every error response has timestamp, status, error, message, path")
    void errorResponseShape() throws Exception {
        mockMvc.perform(get("/api/speech/99999")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").isNotEmpty())
            .andExpect(jsonPath("$.path").value("/api/speech/99999"));
    }

    @Test @Order(2)
    @DisplayName("X-Request-ID header is present in every response")
    void requestIdInResponse() throws Exception {
        mockMvc.perform(get("/api/speech/history")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Request-ID"));
    }

    @Test @Order(3)
    @DisplayName("Custom X-Request-ID is echoed back")
    void customRequestIdEchoed() throws Exception {
        mockMvc.perform(get("/api/speech/ping")
                .header("X-Request-ID", "test-correlation-id"))
            .andExpect(header().string("X-Request-ID", "test-correlation-id"));
    }

    // ── 400 errors ────────────────────────────────────────────────────────────

    @Test @Order(4)
    @DisplayName("400 — Register with weak password includes field details")
    void registerWeakPassword400() throws Exception {
        String body = """
            {"name":"Test","email":"new@test.com","password":"weak"}
            """;
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation Failed"))
            .andExpect(jsonPath("$.details.password").exists());
    }

    @Test @Order(5)
    @DisplayName("400 — Register with invalid email format")
    void registerInvalidEmail400() throws Exception {
        String body = """
            {"name":"Test","email":"notanemail","password":"TestPass1"}
            """;
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.email").exists());
    }

    @Test @Order(6)
    @DisplayName("400 — Search query too short returns 400")
    void searchQueryTooShort400() throws Exception {
        mockMvc.perform(get("/api/speech/search?q=a")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest());
    }

    // ── 401 errors ────────────────────────────────────────────────────────────

    @Test @Order(7)
    @DisplayName("401 — No token returns JSON 401, not HTML redirect")
    void noTokenReturns401Json() throws Exception {
        mockMvc.perform(get("/api/speech/history"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test @Order(8)
    @DisplayName("401 — Tampered JWT returns 401")
    void tamperedTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/speech/history")
                .header("Authorization", "Bearer tampered.jwt.token"))
            .andExpect(status().isUnauthorized());
    }

    @Test @Order(9)
    @DisplayName("401 — Wrong password returns 401 with hint")
    void wrongPasswordHint() throws Exception {
        String body = """
            {"email":"error@test.com","password":"WrongPass99"}
            """;
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.hint").isNotEmpty());
    }

    // ── 404 errors ────────────────────────────────────────────────────────────

    @Test @Order(10)
    @DisplayName("404 — Non-existent transcription ID")
    void transcriptionNotFound404() throws Exception {
        mockMvc.perform(get("/api/speech/999999")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value(containsString("999999")));
    }

    // ── 405 errors ────────────────────────────────────────────────────────────

    @Test @Order(11)
    @DisplayName("405 — DELETE on /history returns 405 with allowed methods")
    void wrongMethodOnHistory405() throws Exception {
        mockMvc.perform(delete("/api/speech/history")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.status").value(405))
            .andExpect(jsonPath("$.hint").value(containsString("GET")));
    }

    // ── 415 errors ────────────────────────────────────────────────────────────

    @Test @Order(12)
    @DisplayName("415 — JSON body to multipart endpoint returns 415")
    void wrongContentType415() throws Exception {
        mockMvc.perform(post("/api/speech/upload")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"file\":\"fake\"}"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.status").value(415))
            .andExpect(jsonPath("$.hint").value(containsString("multipart")));
    }

    // ── 422 errors ────────────────────────────────────────────────────────────

    @Test @Order(13)
    @DisplayName("422 — Upload TXT file returns 422 with format hint")
    void uploadWrongFormat422() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "notes.txt", "text/plain", "hello world".getBytes());

        mockMvc.perform(multipart("/api/speech/upload")
                .file(file)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.status").value(422))
            .andExpect(jsonPath("$.hint").value(containsString("MP3")));
    }

    // ── 501 errors ────────────────────────────────────────────────────────────

    @Test @Order(14)
    @DisplayName("501 — POST /record returns 501 (not yet implemented)")
    void recordNotImplemented501() throws Exception {
        mockMvc.perform(post("/api/speech/record")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(new byte[]{0x01, 0x02}))
            .andExpect(status().isNotImplemented())
            .andExpect(jsonPath("$.status").value(501))
            .andExpect(jsonPath("$.hint").isNotEmpty());
    }

    // ── Duplicate email ───────────────────────────────────────────────────────

    @Test @Order(15)
    @DisplayName("400 — Duplicate email returns 400 with 'already registered' message")
    void duplicateEmail400() throws Exception {
        // Register first time
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"First","email":"dup@test.com","password":"TestPass1"}
                    """))
            .andExpect(status().isCreated());

        // Register again with same email
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Second","email":"dup@test.com","password":"TestPass1"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("already registered")));
    }
}