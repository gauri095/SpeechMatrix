package com.labmentix.speechmatrix.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * Type-safe binding for all app.upload.* properties.
 */
@Configuration
@ConfigurationProperties(prefix = "app.upload")
@Data
public class UploadProperties {

    /** Root directory for audio uploads */
    private String dir = "uploads";

    /** Comma-separated allowed file extensions */
    private String allowedExtensions = "mp3,wav,m4a,flac,ogg,webm,mp4";

    /** Maximum file size in bytes (default 50 MB) */
    private long maxSizeBytes = 52_428_800L;

    /** Returns the allowed extensions as a Set for fast lookup */
    public Set<String> getAllowedExtensionSet() {
        return Set.of(allowedExtensions.split(","));
    }

    /** Returns max size as MB for display in error messages */
    public long getMaxSizeMb() {
        return maxSizeBytes / 1_048_576;
    }
}
