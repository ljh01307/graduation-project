package com.graduation.repository;

import com.graduation.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    @Query("SELECT r FROM Review r WHERE r.product.id = :productId")
    List<Review> findByProductId(@Param("productId") Long productId);
    
    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND r.analyzed = :analyzed")
    List<Review> findByProductIdAndAnalyzed(@Param("productId") Long productId, @Param("analyzed") Boolean analyzed);
    
    List<Review> findByAnalyzed(Boolean analyzed);
    
    @Query("SELECT r FROM Review r WHERE r.product.id = :productId")
    Page<Review> findByProductId(@Param("productId") Long productId, Pageable pageable);
    
    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND r.sentimentLabel = :sentimentLabel")
    Page<Review> findByProductIdAndSentimentLabel(@Param("productId") Long productId, @Param("sentimentLabel") Integer sentimentLabel, Pageable pageable);
    
    @Query("SELECT r FROM Review r WHERE r.product.id = :productId " +
           "AND (:sentimentLabel IS NULL OR r.sentimentLabel = :sentimentLabel) " +
           "AND (:startTime IS NULL OR r.uploadTime >= :startTime) " +
           "AND (:endTime IS NULL OR r.uploadTime <= :endTime)")
    Page<Review> findByFilters(
        @Param("productId") Long productId,
        @Param("sentimentLabel") Integer sentimentLabel,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        Pageable pageable
    );

    @Query("SELECT r FROM Review r WHERE 1=1 " +
           "AND (:sentimentLabel IS NULL OR r.sentimentLabel = :sentimentLabel) " +
           "AND (:startTime IS NULL OR r.uploadTime >= :startTime) " +
           "AND (:endTime IS NULL OR r.uploadTime <= :endTime)")
    Page<Review> findAllFilters(
        @Param("sentimentLabel") Integer sentimentLabel,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        Pageable pageable
    );
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId")
    long countByProductId(@Param("productId") Long productId);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.sentimentLabel = 1 AND r.analyzed = true")
    long countPositiveByProductId(@Param("productId") Long productId);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.sentimentLabel = 0 AND r.analyzed = true")
    long countNegativeByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.analyzed = false")
    long countUnanalyzedByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.analyzed = false")
    long countUnanalyzedAll();

    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND r.uploadTime BETWEEN :startTime AND :endTime")
    List<Review> findByProductIdAndUploadTimeBetween(
        @Param("productId") Long productId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}