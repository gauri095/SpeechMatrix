package com.labmentix.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labmentix.speechmatrix.dto.AuthDto;
import com.labmentix.speechmatrix.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
@DisplayName("AuthController integration tests")
class AuthControllerTest {

    @Autowired MockMvc        mockMvc;
    @Autowired ObjectMapper   objectMapper;
    @Autowired UserRepository userRepository;

    // Shared across tests in this class
    private static String savedToken;

    @BeforeEach
    void cleanUp() {
        // Only wipe before the first test; subsequent tests reuse the saved user
    }

    @AfterAll
    static void tearDown(@Autowired UserRepository repo) {
        repo.deleteAll();
    }

    // â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    // REGISTER
    // â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @Order(1)
    @DisplayName("POST /register â?? success returns 201 + token")
    void registerSuccess() throws Exception {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setName("Ananya Kapoor");
        req.setEmail("ananya@sttapp.com");
        req.setPassword("SecurePass1");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.type").value("Bearer"))
            .andExpect(jsonPath("$.email").value("ananya@sttapp.com"))
            .andExpect(jsonPath("$.name").value("Ananya Kapoor"))
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.expiresInMs").value(86400000))
            .andReturn();

        // Save token for later tests
        String body = result.getResponse().getContentAsString();
        savedToken = objectMapper.readTree(body).get("token").asText();
    }

    @Test
    @Order(2)
    @DisplayName("POST /register â?? duplicate email returns 400")
    void registerDuplicateEmail() throws Exception {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setName("Another Person");
        req.setEmail("ananya@sttapp.com");   // already taken
        req.setPassword("SecurePass1");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value(containsString("already registered")));
    }

    @Test
    @Order(3)
    @DisplayName("POST /register â?? missing name returns 400 with field error")
    void registerMissingName() throws Exception {
        String body = """
            { "email": "test@test.com", "password": "SecurePass1" }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.name").exists());
    }

    @Test
    @Order(4)
    @DisplayName("POST /register â?? weak password returns 400 with detail")
    void registerWeakPassword() throws Exception {
        String body = """
            { "name": "Test", "email": "weak@test.com", "password": "abc" }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.password").exists());
    }

    @Test
    @Order(5)
    @DisplayName("POST /register â?? invalid email format returns 400")
    void registerInvalidEmail() throws Exception {
        String body = """
            { "name": "Test", "email": "notanemail", "password": "SecurePass1" }
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.email").exists());
    }

    // â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    // LOGIN
    // â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @Order(6)
    @DisplayName("POST /login â?? correct credentials return 200 + token")
    void loginSuccess() throws Exception {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest();
        req.setEmail("ananya@sttapp.com");
        req.setPassword("SecurePass1");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.type").value("Bearer"))
            .andExpect(jsonPath("$.email").value("ananya@sttapp.com"));
    }

    @Test
    @Order(7)
    @DisplayName("POST /login â?? wrong password returns 401")
    void loginWrongPassword() throws Exception {
        String body = """
            { "email": "ananya@sttapp.com", "password": "WrongPass99" }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @Order(8)
    @DisplayName("POST /login â?? unknown email returns 401")
    void loginUnknownEmail() throws Exception {
        String body = """
            { "email": "nobody@test.com", "password": "SecurePass1" }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized());
    }

    // â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    // PROTECTED ENDPOINTS
    // â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @Order(9)
    @DisplayName("GET /me â?? valid token returns 200")
    void meWithValidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + savedToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("ananya@sttapp.com"))
            .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    @Order(10)
    @DisplayName("GET /me â€” no token returns 401 JSON (not HTML redirect)")
    void meWithNoToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @Order(11)
    @DisplayName("GET /me â€” tampered token returns 401")
    void meWithBadToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer totally.fake.token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(12)
    @DisplayName("GET /profile â€” returns profile with stats")
    void getProfile() throws Exception {
        mockMvc.perform(get("/api/auth/profile")
                .header("Authorization", "Bearer " + savedToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("ananya@sttapp.com"))
            .andExpect(jsonPath("$.totalTranscriptions").isNumber())
            .andExpect(jsonPath("$.totalWords").isNumber());
    }

    @Test
    @Order(13)
    @DisplayName("POST /change-password â€” wrong current password returns 401")
    void changePasswordWrongCurrent() throws Exception {
        String body = """
            { "currentPassword": "WrongPass99", "newPassword": "NewSecure1" }
            """;

        mockMvc.perform(post("/api/auth/change-password")
                .header("Authorization", "Bearer " + savedToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(14)
    @DisplayName("POST /logout â€” blacklists token, subsequent call returns 401")
    void logoutThenReject() throws Exception {
        // 1. Logout
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + savedToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Logged out successfully"));

        // 2. Same token must now be rejected
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + savedToken))
            .andExpect(status().isUnauthorized());
    }
}
