package com.labmentix.speechmatrix.util;

import com.sttapp.dto.AuthDto;
import com.sttapp.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UserMapper unit tests")
class UserMapperTest {

    UserMapper mapper = new UserMapper();

    User user = User.builder()
            .id(1L).name("Priya Nair")
            .email("priya@sttapp.com").password("$2a$12$hashed")
            .build();

    // â??â?? toProfileMap â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("toProfileMap includes all expected keys")
    void profileMapKeys() {
        Map<String, Object> profile = mapper.toProfileMap(user, 5L, 1200L, 300.0);
        assertThat(profile).containsKeys(
            "id", "name", "email", "createdAt",
            "totalTranscriptions", "totalWords", "totalDurationSecs");
    }

    @Test
    @DisplayName("toProfileMap NEVER includes password")
    void profileMapExcludesPassword() {
        Map<String, Object> profile = mapper.toProfileMap(user, 0L, 0L, 0.0);
        assertThat(profile).doesNotContainKey("password");

        // Also check no value contains the hash
        profile.values().forEach(v ->
            assertThat(v == null || !v.toString().startsWith("$2a$"))
                .as("Password hash must not appear in profile response")
                .isTrue());
    }

    @Test
    @DisplayName("toProfileMap sets correct stats values")
    void profileMapStats() {
        Map<String, Object> profile = mapper.toProfileMap(user, 7L, 2500L, 540.5);
        assertThat(profile.get("totalTranscriptions")).isEqualTo(7L);
        assertThat(profile.get("totalWords")).isEqualTo(2500L);
        assertThat(profile.get("totalDurationSecs")).isEqualTo(540.5);
    }

    // â??â?? toAuthResponse â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("toAuthResponse sets token, id, name, email")
    void authResponseFields() {
        AuthDto.AuthResponse r = mapper.toAuthResponse(user, "my.jwt.token");
        assertThat(r.getToken()).isEqualTo("my.jwt.token");
        assertThat(r.getId()).isEqualTo(1L);
        assertThat(r.getName()).isEqualTo("Priya Nair");
        assertThat(r.getEmail()).isEqualTo("priya@sttapp.com");
    }

    @Test
    @DisplayName("toAuthResponse type is Bearer")
    void authResponseTypeIsBearer() {
        assertThat(mapper.toAuthResponse(user, "token").getType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("toAuthResponse NEVER includes password")
    void authResponseExcludesPassword() {
        AuthDto.AuthResponse r = mapper.toAuthResponse(user, "token");
        // Verify via toString / reflection that password isn't leaked
        assertThat(r.toString()).doesNotContain("$2a$");
        assertThat(r.toString()).doesNotContain("hashed");
    }
}
