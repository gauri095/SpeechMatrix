package com.labmentix.speechmatrix.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AudioStorageService unit tests")
class AudioStorageServiceTest {

    @TempDir
    Path tempDir;

    AudioStorageService service;

    // Minimal valid MP3 header bytes
    private static final byte[] MP3_BYTES = {
        0x49, 0x44, 0x33, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x54, 0x45, 0x53, 0x54, 0x20, 0x41, 0x55, 0x44, 0x49, 0x4F
    };

    @BeforeEach
    void setUp() {
        service = new AudioStorageService();
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
    }

    // â??â?? store â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("store() saves file and returns a path string")
    void storeReturnsPath() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "interview.mp3", "audio/mpeg", MP3_BYTES);

        String path = service.store(file, 1L);

        assertThat(path).isNotBlank().endsWith(".mp3");
        assertThat(new File(path)).exists();
    }

    @Test
    @DisplayName("store() creates user-specific subdirectory")
    void storeCreatesUserDir() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "note.mp3", "audio/mpeg", MP3_BYTES);

        String path = service.store(file, 42L);

        assertThat(path).contains("42");
        assertThat(new File(path).getParentFile().getName()).isEqualTo("42");
    }

    @Test
    @DisplayName("store() sanitises dangerous filename characters")
    void storeSanitisesFilename() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "../../etc/passwd.mp3", "audio/mpeg", MP3_BYTES);

        String path = service.store(file, 1L);

        // Path traversal characters must be removed
        assertThat(path).doesNotContain("..");
        assertThat(new File(path)).exists();
    }

    @Test
    @DisplayName("store() two files with same name get unique paths")
    void storeProducesUniqueNames() {
        MockMultipartFile f1 = new MockMultipartFile(
            "file", "audio.mp3", "audio/mpeg", MP3_BYTES);
        MockMultipartFile f2 = new MockMultipartFile(
            "file", "audio.mp3", "audio/mpeg", MP3_BYTES);

        String path1 = service.store(f1, 1L);
        String path2 = service.store(f2, 1L);

        assertThat(path1).isNotEqualTo(path2);
    }

    @Test
    @DisplayName("store() writes correct content to disk")
    void storeWritesCorrectContent() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.mp3", "audio/mpeg", MP3_BYTES);

        String path = service.store(file, 1L);

        byte[] written = Files.readAllBytes(Path.of(path));
        assertThat(written).isEqualTo(MP3_BYTES);
    }

    // â??â?? storeBytes â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("storeBytes() saves byte array with given extension")
    void storeBytesWorks() {
        String path = service.storeBytes(MP3_BYTES, 1L, "webm");

        assertThat(path).endsWith(".webm");
        assertThat(new File(path)).exists();
        assertThat(new File(path).length()).isEqualTo(MP3_BYTES.length);
    }

    // â??â?? load â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("load() returns existing file")
    void loadExistingFile() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "load.mp3", "audio/mpeg", MP3_BYTES);
        String path = service.store(file, 1L);

        File loaded = service.load(path);

        assertThat(loaded).exists();
        assertThat(loaded.length()).isEqualTo(MP3_BYTES.length);
    }

    @Test
    @DisplayName("load() throws when file doesn't exist")
    void loadMissingFileThrows() {
        assertThatThrownBy(() -> service.load("/nonexistent/path/file.mp3"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("not found");
    }

    // â??â?? delete â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("delete() removes the file from disk")
    void deleteRemovesFile() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "delete.mp3", "audio/mpeg", MP3_BYTES);
        String path = service.store(file, 1L);
        assertThat(new File(path)).exists();

        service.delete(path);

        assertThat(new File(path)).doesNotExist();
    }

    @Test
    @DisplayName("delete() with null path does not throw")
    void deleteNullPathSilent() {
        assertThatCode(() -> service.delete(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("delete() with already-deleted path does not throw")
    void deleteAlreadyGoneSilent() {
        assertThatCode(() -> service.delete("/nonexistent/file.mp3"))
            .doesNotThrowAnyException();
    }

    // â??â?? deleteAllForUser â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("deleteAllForUser() removes all files for that user")
    void deleteAllForUserRemovesDir() {
        MockMultipartFile f1 = new MockMultipartFile(
            "file", "a.mp3", "audio/mpeg", MP3_BYTES);
        MockMultipartFile f2 = new MockMultipartFile(
            "file", "b.mp3", "audio/mpeg", MP3_BYTES);
        service.store(f1, 99L);
        service.store(f2, 99L);

        service.deleteAllForUser(99L);

        assertThat(tempDir.resolve("99").toFile()).doesNotExist();
    }
}
