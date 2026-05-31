package com.labmentix.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtUtils unit tests")
class JwtUtilsTest {

    @Mock  TokenBlacklistService blacklistService;
    @InjectMocks JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret",
            "TestSecretKeyForUnitTestsOnly1234567890ABCDEF");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 3_600_000L);
    }

    @Test
    @DisplayName("generateTokenFromEmail produces a valid non-empty token")
    void generateToken() {
        String token = jwtUtils.generateTokenFromEmail("priya@sttapp.com");
        assertThat(token).isNotBlank().contains(".");
    }

    @Test
    @DisplayName("getEmailFromToken extracts the correct subject")
    void extractEmail() {
        String token = jwtUtils.generateTokenFromEmail("ravi@sttapp.com");
        assertThat(jwtUtils.getEmailFromToken(token)).isEqualTo("ravi@sttapp.com");
    }

    @Test
    @DisplayName("validateToken returns true for a fresh valid token")
    void validateValid() {
        when(blacklistService.isBlacklisted(any())).thenReturn(false);
        String token = jwtUtils.generateTokenFromEmail("valid@test.com");
        assertThat(jwtUtils.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken returns false for a malformed token")
    void validateMalformed() {
        assertThat(jwtUtils.validateToken("not.a.valid.jwt")).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false when token is blacklisted")
    void validateBlacklisted() {
        String token = jwtUtils.generateTokenFromEmail("logout@test.com");
        when(blacklistService.isBlacklisted(token)).thenReturn(true);
        assertThat(jwtUtils.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("getExpiryMs returns a future timestamp")
    void expiryInFuture() {
        String token = jwtUtils.generateTokenFromEmail("exp@test.com");
        long expiry = jwtUtils.getExpiryMs(token);
        assertThat(expiry).isGreaterThan(System.currentTimeMillis());
    }
}
