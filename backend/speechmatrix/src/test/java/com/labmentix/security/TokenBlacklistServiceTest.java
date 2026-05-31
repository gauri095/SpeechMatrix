package com.labmentix.security;

import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TokenBlacklistService unit tests")
class TokenBlacklistServiceTest {

    TokenBlacklistService service;

    @BeforeEach
    void setUp() { service = new TokenBlacklistService(); }

    // ── blacklist + isBlacklisted ─────────────────────────────────────────────

    @Test
    @DisplayName("Token not blacklisted initially")
    void notBlacklistedByDefault() {
        assertThat(service.isBlacklisted("fresh.token")).isFalse();
    }

    @Test
    @DisplayName("Blacklisted token is recognised")
    void blacklistedIsRecognised() {
        long futureExpiry = Instant.now().toEpochMilli() + 3_600_000;
        service.blacklist("my.token", futureExpiry);
        assertThat(service.isBlacklisted("my.token")).isTrue();
    }

    @Test
    @DisplayName("Different tokens are independent")
    void tokensAreIndependent() {
        long expiry = Instant.now().toEpochMilli() + 3_600_000;
        service.blacklist("token-A", expiry);
        assertThat(service.isBlacklisted("token-A")).isTrue();
        assertThat(service.isBlacklisted("token-B")).isFalse();
    }

    @Test
    @DisplayName("Multiple tokens can be blacklisted")
    void multipleTokens() {
        long expiry = Instant.now().toEpochMilli() + 3_600_000;
        service.blacklist("token-1", expiry);
        service.blacklist("token-2", expiry);
        service.blacklist("token-3", expiry);
        assertThat(service.isBlacklisted("token-1")).isTrue();
        assertThat(service.isBlacklisted("token-2")).isTrue();
        assertThat(service.isBlacklisted("token-3")).isTrue();
    }

    // ── purgeExpired ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("purgeExpired removes tokens whose JWT has expired")
    void purgeRemovesExpiredTokens() {
        // Expired tokens (expiry in the past)
        long pastExpiry = Instant.now().toEpochMilli() - 1000;
        service.blacklist("expired-1", pastExpiry);
        service.blacklist("expired-2", pastExpiry);

        // Still-valid token (expiry in the future)
        long futureExpiry = Instant.now().toEpochMilli() + 3_600_000;
        service.blacklist("valid-token", futureExpiry);

        service.purgeExpired();

        assertThat(service.isBlacklisted("expired-1")).isFalse();
        assertThat(service.isBlacklisted("expired-2")).isFalse();
        assertThat(service.isBlacklisted("valid-token")).isTrue();
    }

    @Test
    @DisplayName("purgeExpired on empty blacklist does not throw")
    void purgeEmptyBlacklist() {
        assertThatCode(() -> service.purgeExpired()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("After purge, re-blacklisting a token works")
    void reBlacklistAfterPurge() {
        long pastExpiry   = Instant.now().toEpochMilli() - 1000;
        long futureExpiry = Instant.now().toEpochMilli() + 3_600_000;

        service.blacklist("token", pastExpiry);
        service.purgeExpired();
        assertThat(service.isBlacklisted("token")).isFalse();

        service.blacklist("token", futureExpiry);
        assertThat(service.isBlacklisted("token")).isTrue();
    }
}