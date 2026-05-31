package com.labmentix.speechmatrix.util;

import com.labmentix.speechmatrix.exception.InvalidAudioFileException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AudioFileValidator unit tests")
class AudioFileValidatorTest {

    AudioFileValidator validator;

    // Real MP3 magic bytes (ID3 header)
    private static final byte[] MP3_MAGIC = { 0x49, 0x44, 0x33, 0x03, 0x00 };

    // Real WAV magic bytes (RIFF....WAVE)
    private static final byte[] WAV_MAGIC = {
        0x52, 0x49, 0x46, 0x46,  // "RIFF"
        0x24, 0x08, 0x00, 0x00,  // file size (placeholder)
        0x57, 0x41, 0x56, 0x45   // "WAVE"
    };

    // Real FLAC magic bytes
    private static final byte[] FLAC_MAGIC = { 0x66, 0x4C, 0x61, 0x43 };

    // Real OGG magic bytes
    private static final byte[] OGG_MAGIC = { 0x4F, 0x67, 0x67, 0x53 };

    // Fake bytes (EXE header)
    private static final byte[] EXE_MAGIC = { 0x4D, 0x5A, 0x00, 0x00 };

    @BeforeEach
    void setUp() {
        validator = new AudioFileValidator();
        ReflectionTestUtils.setField(validator, "maxSizeBytes", 52_428_800L);
        ReflectionTestUtils.setField(validator, "allowedExtensionsRaw",
            "mp3,wav,m4a,flac,ogg,webm,mp4");
    }

    // â??â?? Empty / null â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("Empty file throws InvalidAudioFileException")
    void emptyFileThrows() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.mp3", "audio/mpeg", new byte[0]);
        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOf(InvalidAudioFileException.class)
            .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("Null original filename throws")
    void nullFilenameThrows() {
        MockMultipartFile file = new MockMultipartFile(
            "file", null, "audio/mpeg", MP3_MAGIC);
        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOf(InvalidAudioFileException.class);
    }

    // â??â?? Size â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @Test
    @DisplayName("File over 50 MB throws with size info in message")
    void oversizedFileThrows() {
        ReflectionTestUtils.setField(validator, "maxSizeBytes", 1000L);
        byte[] bigData = new byte[1001];
        System.arraycopy(MP3_MAGIC, 0, bigData, 0, MP3_MAGIC.length);
        MockMultipartFile file = new MockMultipartFile(
            "file", "big.mp3", "audio/mpeg", bigData);

        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOf(InvalidAudioFileException.class)
            .hasMessageContaining("MB");
    }

    @Test
    @DisplayName("File exactly at limit passes")
    void fileSizeAtLimitPasses() {
        ReflectionTestUtils.setField(validator, "maxSizeBytes", 1000L);
        byte[] data = new byte[1000];
        System.arraycopy(MP3_MAGIC, 0, data, 0, MP3_MAGIC.length);
        MockMultipartFile file = new MockMultipartFile(
            "file", "ok.mp3", "audio/mpeg", data);
        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    // â??â?? Extension â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    @ParameterizedTest(name = "Rejects .{0} files")
    @ValueSource(strings = {"exe", "pdf", "txt", "zip", "jpg", "php"})
    @DisplayName("Unsupported extensions rejected")
    void unsupportedExtensionRejected(String ext) {
        MockMultipartFile file = new MockMultipartFile(
            "file", "file." + ext, "application/octet-stream", MP3_MAGIC);
        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOf(InvalidAudioFileException.class)
            .hasMessageContaining("Unsupported");
    }

    @ParameterizedTest(name = "Accepts .{0}")
    @ValueSource(strings = {"mp3", "wav", "flac", "ogg"})
    @DisplayName("Supported audio extensions pass extension check")
    void supportedExtensionPasses(String ext) {
        byte[] magic = switch (ext) {
            case "mp3"  -> MP3_MAGIC;
            case "wav"  -> WAV_MAGIC;
            case "flac" -> FLAC_MAGIC;
            case "ogg"  -> OGG_MAGIC;
            default     -> MP3_MAGIC;
        };
        MockMultipartFile file = new MockMultipartFile(
            "file", "audio." + ext, "audio/" + ext, magic);
        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    // â”€â”€ Magic bytes â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("EXE disguised as MP3 is rejected")
    void executableDisguisedAsMp3Rejected() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "malicious.mp3", "audio/mpeg", EXE_MAGIC);
        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOf(InvalidAudioFileException.class)
            .hasMessageContaining("content does not match");
    }

    @Test
    @DisplayName("Real WAV file passes magic byte check")
    void realWavPasses() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "audio.wav", "audio/wav", WAV_MAGIC);
        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("RIFF without WAVE header (AVI) is rejected as WAV")
    void riffWithoutWaveRejected() {
        byte[] avi = {
            0x52, 0x49, 0x46, 0x46,  // "RIFF"
            0x00, 0x00, 0x00, 0x00,
            0x41, 0x56, 0x49, 0x20   // "AVI " not "WAVE"
        };
        MockMultipartFile file = new MockMultipartFile(
            "file", "video.wav", "audio/wav", avi);
        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOf(InvalidAudioFileException.class);
    }

    @Test
    @DisplayName("WebM bypasses magic byte check (trusted by extension)")
    void webmBypassesMagicCheck() {
        // WebM has complex EBML header â€” we trust by extension
        byte[] someBytes = new byte[]{ 0x1A, 0x45, 0x44, 0x46, 0x01 };
        MockMultipartFile file = new MockMultipartFile(
            "file", "recording.webm", "audio/webm", someBytes);
        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    // â”€â”€ getMimeType â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("getMimeType returns correct MIME for each format")
    void getMimeType() {
        assertThat(validator.getMimeType("audio.mp3")).isEqualTo("audio/mpeg");
        assertThat(validator.getMimeType("audio.wav")).isEqualTo("audio/wav");
        assertThat(validator.getMimeType("audio.flac")).isEqualTo("audio/flac");
        assertThat(validator.getMimeType("audio.ogg")).isEqualTo("audio/ogg");
        assertThat(validator.getMimeType("audio.m4a")).isEqualTo("audio/mp4");
        assertThat(validator.getMimeType("audio.webm")).isEqualTo("audio/webm");
    }

    @Test
    @DisplayName("getMimeType falls back to audio/wav for unknown extension")
    void getMimeTypeFallback() {
        assertThat(validator.getMimeType("audio.xyz")).isEqualTo("audio/wav");
    }
}
