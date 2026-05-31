package com.labmentix.speechmatrix.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "transcriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transcription {
    // Add these constants at the top of the class:
public static final String STATUS_PENDING    = "pending";
public static final String STATUS_PROCESSING = "processing";
public static final String STATUS_DONE       = "done";
public static final String STATUS_FAILED     = "failed";

// Add these fields:
@Column(nullable = false, length = 20)
@Builder.Default
private String status = STATUS_PENDING;

@UpdateTimestamp
@Column(name = "updated_at")
private LocalDateTime updatedAt;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "audio_file")
    private String audioFile;

    @Column(columnDefinition = "TEXT")
    private String transcript;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    private String language;

    @Column(name = "speaker_count")
    private Integer speakerCount;

    @Column(name = "duration_seconds")
    private Double durationSeconds;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "stt_provider")
    private String sttProvider;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
