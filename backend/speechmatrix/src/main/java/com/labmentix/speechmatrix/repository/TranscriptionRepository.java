package com.labmentix.speechmatrix.repository;

import com.labmentix.speechmatrix.model.Transcription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranscriptionRepository extends JpaRepository<Transcription, Long> {

    Page<Transcription> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Transcription> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT t FROM Transcription t WHERE t.user.id = :userId " +
           "AND LOWER(t.transcript) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Transcription> searchByTranscript(@Param("userId") Long userId,
                                           @Param("query") String query);

    long countByUserId(Long userId);

    @Query("SELECT SUM(t.wordCount) FROM Transcription t WHERE t.user.id = :userId")
    Long sumWordCountByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(t.durationSeconds) FROM Transcription t WHERE t.user.id = :userId")
    Double sumDurationByUserId(@Param("userId") Long userId);

    // Filter: status + language combined
Page<Transcription> findByUserIdAndStatusAndLanguageOrderByCreatedAtDesc(
    Long userId, String status, String language, Pageable pageable);

// Filter: language only
Page<Transcription> findByUserIdAndLanguageOrderByCreatedAtDesc(
    Long userId, String language, Pageable pageable);

// Filter: by date range
@Query("""
    SELECT t FROM Transcription t
    WHERE t.user.id = :userId
      AND t.createdAt BETWEEN :from AND :to
    ORDER BY t.createdAt DESC
    """)
Page<Transcription> findByUserIdAndDateRange(
    @Param("userId") Long userId,
    @Param("from")   LocalDateTime from,
    @Param("to")     LocalDateTime to,
    Pageable pageable);

// Search with pagination
@Query("""
    SELECT t FROM Transcription t
    WHERE t.user.id = :userId
      AND LOWER(t.transcript) LIKE LOWER(CONCAT('%', :query, '%'))
    ORDER BY t.createdAt DESC
    """)
Page<Transcription> searchByTranscriptPaged(
    @Param("userId") Long userId,
    @Param("query")  String query,
    Pageable pageable);

// Stats aggregates
long countByUserIdAndStatus(Long userId, String status);

@Query("SELECT COALESCE(AVG(t.confidenceScore), 0.0) FROM Transcription t " +
       "WHERE t.user.id = :userId AND t.confidenceScore IS NOT NULL")
Double avgConfidenceByUserId(@Param("userId") Long userId);

@Query("SELECT DISTINCT t.language FROM Transcription t " +
       "WHERE t.user.id = :userId ORDER BY t.language")
List<String> findDistinctLanguagesByUserId(@Param("userId") Long userId);
}
