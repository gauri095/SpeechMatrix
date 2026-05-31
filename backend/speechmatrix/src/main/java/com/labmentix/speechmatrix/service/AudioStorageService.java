package com.labmentix.speechmatrix.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

/**
 * Handles all audio file I/O on the local filesystem.
 *
 * Storage layout:
 *   uploads/
 *     <userId>/
 *       <uuid>_<sanitisedOriginalName>.mp3
 *
 * Organising by userId ensures:
 *  - Easy per-user cleanup / quota management
 *  - No filename collisions between users
 *  - Simple "delete all uploads for user X" operation
 *
 * In production (Week 5), replace the local Path with an S3 client.
 * Only this service needs changing â?? the rest of the app is unaffected.
 */
@Service
@Slf4j
public class AudioStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // â??â?? Store â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    /**
     * Saves a MultipartFile under uploads/<userId>/<uuid>_<filename>.
     *
     * @return the relative file path stored in the DB, e.g.
     *         "uploads/3/a1b2c3d4_interview.mp3"
     */
    public String store(MultipartFile file, Long userId) {
        Path userDir = resolveUserDir(userId);
        ensureDirectoryExists(userDir);

        String safeName  = sanitise(file.getOriginalFilename());
        String uniqueName = UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                + "_" + safeName;
        Path destination = userDir.resolve(uniqueName);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored audio: {} ({} bytes)",
                destination, file.getSize());
            return destination.toString();     // relative path saved in DB
        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to save audio file: " + e.getMessage(), e);
        }
    }

    /**
     * Saves raw bytes (e.g. from microphone recording).
     *
     * @param extension  file extension without dot: "webm", "wav"
     * @return relative file path
     */
    public String storeBytes(byte[] bytes, Long userId, String extension) {
        Path userDir = resolveUserDir(userId);
        ensureDirectoryExists(userDir);

        String uniqueName = UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                + "_recording." + extension;
        Path destination = userDir.resolve(uniqueName);

        try {
            Files.write(destination, bytes);
            log.info("Stored recording: {} ({} bytes)", destination, bytes.length);
            return destination.toString();
        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to save recording: " + e.getMessage(), e);
        }
    }

    // â??â?? Load â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    /** Resolves a stored path string to a java.io.File for reading. */
    public File load(String storedPath) {
        File file = new File(storedPath);
        if (!file.exists()) {
            throw new RuntimeException("Audio file not found: " + storedPath);
        }
        return file;
    }

    // â??â?? Delete â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    /** Deletes the audio file from disk. Fails silently if already gone. */
    public void delete(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) return;
        try {
            boolean deleted = Files.deleteIfExists(Path.of(storedPath));
            if (deleted) log.info("Deleted audio file: {}", storedPath);
            else         log.debug("Audio file already gone: {}", storedPath);
        } catch (IOException e) {
            log.warn("Could not delete audio file {}: {}", storedPath, e.getMessage());
        }
    }

    /** Deletes all uploads for a given user (used when account is deleted). */
    public void deleteAllForUser(Long userId) {
        Path userDir = resolveUserDir(userId);
        if (!Files.exists(userDir)) return;
        try (var stream = Files.walk(userDir)) {
            stream.sorted(java.util.Comparator.reverseOrder())
                  .map(Path::toFile)
                  .forEach(File::delete);
            log.info("Deleted all uploads for user {}", userId);
        } catch (IOException e) {
            log.warn("Error deleting user {} uploads: {}", userId, e.getMessage());
        }
    }

    // â??â?? Helpers â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??â??

    private Path resolveUserDir(Long userId) {
        return Path.of(uploadDir, String.valueOf(userId));
    }

    private void ensureDirectoryExists(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException(
                "Cannot create upload directory: " + dir, e);
        }
    }

    /**
     * Strips path traversal characters and trims the name.
     * e.g. "../../etc/passwd.mp3" â?? "etcpasswd.mp3"
     */
    private String sanitise(String filename) {
        if (filename == null) return "audio.bin";
        return filename
            .replaceAll("[^a-zA-Z0-9._-]", "_")  // only safe chars
            .replaceAll("_{2,}", "_")              // collapse multiple _
            .toLowerCase();
    }
}
