package com.labmentix.speechmatrix.util;

import com.labmentix.speechmatrix.exception.InvalidAudioFileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * Validates uploaded audio files before they are saved or sent to the STT API.
 *
 * Three layers of defence:
 *
 *  1. Null / empty check          â?? reject blank files immediately
 *  2. Size check                  â?? reject files over configured limit
 *  3. Extension check             â?? allow only declared audio extensions
 *  4. Magic-byte check            â?? read first 12 bytes to confirm real format
 *                                   (prevents .exe renamed to .mp3)
 *
 * Why magic bytes?
 *  File extensions are user-controlled and trivially spoofed.
 *  Every binary audio format starts with a known byte signature.
 *  We read just 12 bytes â?? fast and no temp file needed.
 */
@Component
@Slf4j
public class AudioFileValidator {

    // Injected from application.properties
    @Value("${app.upload.max-size-bytes:52428800}")
    private long maxSizeBytes;   // default 50 MB

    @Value("${app.upload.allowed-extensions:mp3,wav,m4a,flac,ogg,webm,mp4}")
    private String allowedExtensionsRaw;

    // â??â?? Magic byte signatures â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??
    // Each entry: display-name â?? first N bytes that identify the format
    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
        "MP3 (ID3)",  new byte[]{ 0x49, 0x44, 0x33 },               // "ID3"
        "MP3 (sync)", new byte[]{ (byte)0xFF, (byte)0xFB },          // sync + MPEG1 L3
        "MP3 (sync2)",new byte[]{ (byte)0xFF, (byte)0xF3 },
        "WAV",        new byte[]{ 0x52, 0x49, 0x46, 0x46 },          // "RIFF"
        "FLAC",       new byte[]{ 0x66, 0x4C, 0x61, 0x43 },          // "fLaC"
        "OGG",        new byte[]{ 0x4F, 0x67, 0x67, 0x53 },          // "OggS"
        "M4A/MP4",    new byte[]{ 0x00, 0x00, 0x00 }                 // box size prefix (partial)
    );

    // Formats that are valid but have no reliable magic bytes
    // (WebM starts with EBML header â?? complex to check, trusted by extension)
    private static final Set<String> TRUST_BY_EXTENSION = Set.of("webm");

    // â??â?? Public: validate everything â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    /**
     * Run all checks. Throws InvalidAudioFileException if anything fails.
     * Returns quietly if the file is valid.
     */
    public void validate(MultipartFile file) {
        checkNotEmpty(file);
        checkSize(file);
        String ext = checkExtension(file);
        if (!TRUST_BY_EXTENSION.contains(ext)) {
            checkMagicBytes(file, ext);
        }
        log.debug("Audio file validated: name={} size={} ext={}",
            file.getOriginalFilename(), file.getSize(), ext);
    }

    // â??â?? Public: MIME type for STT API â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    /** Returns the correct MIME type string for the audio format. */
    public String getMimeType(String filename) {
        return switch (getExtension(filename)) {
            case "mp3"  -> "audio/mpeg";
            case "wav"  -> "audio/wav";
            case "m4a"  -> "audio/mp4";
            case "flac" -> "audio/flac";
            case "ogg"  -> "audio/ogg";
            case "webm" -> "audio/webm";
            case "mp4"  -> "audio/mp4";
            default     -> "audio/wav";
        };
    }

    // â??â?? Private checks â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â”€â”€â”€

    private void checkNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidAudioFileException(
                "Audio file is empty or missing. Please upload an audio file.");
        }
        if (file.getOriginalFilename() == null
                || file.getOriginalFilename().isBlank()) {
            throw new InvalidAudioFileException(
                "File has no name. Please select a valid audio file.");
        }
    }

    private void checkSize(MultipartFile file) {
        if (file.getSize() > maxSizeBytes) {
            long   limitMb   = maxSizeBytes / 1_048_576;
            double actualMb  = file.getSize() / 1_048_576.0;
            throw new InvalidAudioFileException(String.format(
                "File too large: %.1f MB. Maximum allowed size is %d MB.",
                actualMb, limitMb));
        }
    }

    private String checkExtension(MultipartFile file) {
        String ext = getExtension(file.getOriginalFilename());
        Set<String> allowed = getAllowedExtensions();

        if (!allowed.contains(ext)) {
            throw new InvalidAudioFileException(String.format(
                "Unsupported file format: .%s. Allowed formats: %s",
                ext, String.join(", ", allowed)));
        }
        return ext;
    }

    private void checkMagicBytes(MultipartFile file, String ext) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = is.readNBytes(12);
            if (header.length < 3) {
                throw new InvalidAudioFileException(
                    "File is too small to be a valid audio file.");
            }

            boolean matched = MAGIC_BYTES.values().stream()
                .anyMatch(magic -> startsWith(header, magic));

            // WAV has RIFF header but WEBP / AVI also use RIFF â€” verify it's WAVE
            if (ext.equals("wav") && matched) {
                matched = header.length >= 12
                    && header[8] == 0x57  // W
                    && header[9] == 0x41  // A
                    && header[10] == 0x56 // V
                    && header[11] == 0x45;// E
            }

            if (!matched) {
                log.warn("Magic byte check failed for: {} (ext={})",
                    file.getOriginalFilename(), ext);
                throw new InvalidAudioFileException(
                    "File content does not match its extension. " +
                    "Please upload a real audio file.");
            }

        } catch (InvalidAudioFileException e) {
            throw e;
        } catch (IOException e) {
            throw new InvalidAudioFileException(
                "Could not read file: " + e.getMessage());
        }
    }

    // â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }

    private Set<String> getAllowedExtensions() {
        return Set.of(allowedExtensionsRaw.split(","));
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}
