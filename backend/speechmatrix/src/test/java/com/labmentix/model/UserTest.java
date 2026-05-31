package com.labmentix.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User entity tests")
class UserTest {

    @Test
    @DisplayName("Builder creates user with correct fields")
    void builderSetsFields() {
        User user = User.builder()
                .name("Riya Sharma")
                .email("riya@example.com")
                .password("hashed_password")
                .build();

        assertThat(user.getName()).isEqualTo("Riya Sharma");
        assertThat(user.getEmail()).isEqualTo("riya@example.com");
        assertThat(user.getPassword()).isEqualTo("hashed_password");
        assertThat(user.getTranscriptions()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("addTranscription sets bidirectional relationship")
    void addTranscriptionLinksBothSides() {
        User user = User.builder()
                .name("Test")
                .email("test@test.com")
                .password("pass")
                .build();

        Transcription t = new Transcription();
        t.setTranscript("Hello world");

        user.addTranscription(t);

        assertThat(user.getTranscriptions()).hasSize(1);
        assertThat(t.getUser()).isSameAs(user);
    }

    @Test
    @DisplayName("removeTranscription unlinks both sides")
    void removeTranscriptionUnlinksBothSides() {
        User user = User.builder()
                .name("Test")
                .email("test@test.com")
                .password("pass")
                .build();

        Transcription t = new Transcription();
        user.addTranscription(t);
        user.removeTranscription(t);

        assertThat(user.getTranscriptions()).isEmpty();
        assertThat(t.getUser()).isNull();
    }
}
