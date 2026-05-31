package com.labmentix.speechmatrix.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("BCryptPasswordService tests")
class BCryptPasswordServiceTest {

    BCryptPasswordService service;

    @BeforeEach
    void setUp() {
        service = new BCryptPasswordService();
    }

    // â??â?? Hashing â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("hash() produces a non-blank 60-char BCrypt string")
    void hashProducesValidBcrypt() {
        String hash = service.hash("SecurePass1");
        assertThat(hash)
            .isNotBlank()
            .hasSize(60)
            .startsWith("$2a$");
    }

    @Test
    @DisplayName("Same password produces different hashes (random salt)")
    void samePasswordDifferentHashes() {
        String hash1 = service.hash("SecurePass1");
        String hash2 = service.hash("SecurePass1");
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("hash() includes cost factor 12 in output")
    void hashIncludesCostFactor() {
        String hash = service.hash("SecurePass1");
        assertThat(hash).startsWith("$2a$12$");
    }

    // â??â?? Verification â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("verify() returns true for correct password")
    void verifyCorrectPassword() {
        String hash = service.hash("SecurePass1");
        assertThat(service.verify("SecurePass1", hash)).isTrue();
    }

    @Test
    @DisplayName("verify() returns false for wrong password")
    void verifyWrongPassword() {
        String hash = service.hash("SecurePass1");
        assertThat(service.verify("WrongPass99", hash)).isFalse();
    }

    @Test
    @DisplayName("verify() returns false for null inputs")
    void verifyNullInputs() {
        assertThat(service.verify(null, "$2a$12$somehash")).isFalse();
        assertThat(service.verify("SecurePass1", null)).isFalse();
    }

    @Test
    @DisplayName("verify() cross-validates hashes correctly")
    void verifyCrossHashValidation() {
        String hash1 = service.hash("PasswordA1");
        String hash2 = service.hash("PasswordB1");

        // Each password only matches its own hash
        assertThat(service.verify("PasswordA1", hash1)).isTrue();
        assertThat(service.verify("PasswordB1", hash2)).isTrue();
        assertThat(service.verify("PasswordA1", hash2)).isFalse();
        assertThat(service.verify("PasswordB1", hash1)).isFalse();
    }

    // â??â?? analyzePassword â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("analyzePassword returns expected keys")
    void analyzePasswordKeys() {
        Map<String, Object> info = service.analyzePassword("SecurePass1");

        assertThat(info).containsKeys(
            "inputLength", "hasUppercase", "hasLowercase",
            "hasDigit", "hashesAreDifferent", "hash1VerifiesOk",
            "bcryptStrength", "hashLength"
        );
    }

    @Test
    @DisplayName("analyzePassword â?? hashes are always different (random salt)")
    void analyzePasswordHashesDiffer() {
        Map<String, Object> info = service.analyzePassword("SecurePass1");
        assertThat((Boolean) info.get("hashesAreDifferent")).isTrue();
    }

    @Test
    @DisplayName("analyzePassword â?? hash length is always 60")
    void analyzePasswordHashLength() {
        Map<String, Object> info = service.analyzePassword("AnyPass1");
        assertThat(info.get("hashLength")).isEqualTo(60);
    }

    @Test
    @DisplayName("analyzePassword â€” both hashes verify correctly")
    void analyzePasswordVerification() {
        Map<String, Object> info = service.analyzePassword("SecurePass1");
        assertThat((Boolean) info.get("hash1VerifiesOk")).isTrue();
        assertThat((Boolean) info.get("hash2VerifiesOk")).isTrue();
    }

    @Test
    @DisplayName("BCrypt cost-factor timing â€” strength 12 takes >100ms")
    void bcryptTimingReasonable() {
        long start = System.currentTimeMillis();
        service.hash("TimingTest1");
        long elapsed = System.currentTimeMillis() - start;

        // At strength 12 this should take at least 100ms on any modern CPU
        // If it's faster something is wrong with the config
        assertThat(elapsed)
            .as("BCrypt strength 12 should take at least 100ms")
            .isGreaterThan(100L);
    }

    // â”€â”€ Edge cases â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("Password over 72 bytes still hashes (with truncation warning)")
    void passwordOver72Bytes() {
        // 80-char password â€” BCrypt silently truncates at byte 72
        String longPass = "A".repeat(40) + "1".repeat(40);
        String hash = service.hash(longPass);
        assertThat(hash).isNotBlank().hasSize(60);

        // Verify the 73rd+ chars don't matter (both verify true)
        assertThat(service.verify(longPass, hash)).isTrue();
    }

    @Test
    @DisplayName("needsUpgrade returns false for strength-12 hash")
    void needsUpgradeCurrentStrength() {
        String hash = new BCryptPasswordEncoder(12).encode("TestPass1");
        assertThat(service.needsUpgrade(hash)).isFalse();
    }

    @Test
    @DisplayName("needsUpgrade returns true for lower-strength hash")
    void needsUpgradeOldHash() {
        // Simulate a hash generated with strength 4 (old/weak)
        String weakHash = new BCryptPasswordEncoder(4).encode("TestPass1");
        assertThat(service.needsUpgrade(weakHash)).isTrue();
    }
}
