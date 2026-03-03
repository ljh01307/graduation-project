package com.graduation.controller;

import com.graduation.entity.Review;
import com.graduation.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/review")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping("/upload")
    public List<Review> uploadReviews(@RequestBody Map<String, Object> payload) {
        Long productId = Long.valueOf(payload.get("productId").toString());
        @SuppressWarnings("unchecked")
        List<String> contents = (List<String>) payload.get("contents");
        return reviewService.uploadReviews(productId, contents);
    }

    @PostMapping("/analyze/{productId}")
    public String analyzeReviews(@PathVariable Long productId) {
        reviewService.analyzeReviews(productId);
        return "分析任务已启动，请稍后查看结果";
    }

    @GetMapping("/stats/{productId}")
    public Map<String, Long> getStats(@PathVariable Long productId) {
        return reviewService.getSentimentCount(productId);
    }

    @GetMapping("/weekly/{productId}")
    public Map<String, Object> getWeeklyStats(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "4") int weeks) {
        return reviewService.getWeeklyStats(productId, weeks);
    }

    @GetMapping("/wordcloud/{productId}")
    public Map<String, Object> getWordCloudData(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "50") int topN) {
        return reviewService.getWordCloudData(productId, topN);
    }

    @GetMapping("/overview/{productId}")
    public Map<String, Object> getProductOverview(
            @PathVariable Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        if (startTime != null && endTime != null) {
            return reviewService.getProductOverview(productId, startTime, endTime);
        }
        return reviewService.getProductOverview(productId);
    }

    @GetMapping("/list/{productId}")
    public Map<String, Object> getReviewsByPage(
            @PathVariable Long productId,
            @RequestParam(required = false) Integer sentimentLabel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "uploadTime") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        return reviewService.getReviewsByPage(
            productId, sentimentLabel, startTime, endTime, page, size, sortBy, sortDir);
    }

    @GetMapping("/list-all")
    public Map<String, Object> getAllReviewsByPage(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer sentimentLabel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "uploadTime") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        return reviewService.getAllReviewsByPage(
            productId, sentimentLabel, startTime, endTime, page, size, sortBy, sortDir);
    }

    @PutMapping("/update-sentiment/{reviewId}")
    public Map<String, Object> updateSentimentLabel(
            @PathVariable Long reviewId,
            @RequestBody Map<String, Object> payload) {
        Integer sentimentLabel = (Integer) payload.get("sentimentLabel");
        if (sentimentLabel == null || (sentimentLabel != 0 && sentimentLabel != 1)) {
            throw new IllegalArgumentException("情感标签必须为0或1");
        }
        Review review = reviewService.updateSentimentLabel(reviewId, sentimentLabel);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("review", review);
        return result;
    }

    @DeleteMapping("/delete/{reviewId}")
    public Map<String, Object> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "删除成功");
        return result;
    }

    @GetMapping("/keyword-attribution/{productId}")
    public Map<String, Object> getKeywordAttribution(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "20") int topN) {
        return reviewService.getKeywordAttribution(productId, topN);
    }

    @GetMapping("/unanalyzed-count/{productId}")
    public Map<String, Object> getUnanalyzedCount(@PathVariable Long productId) {
        long count = reviewService.countUnanalyzed(productId);
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return result;
    }

    @GetMapping("/unanalyzed-count")
    public Map<String, Object> getUnanalyzedCountAll() {
        long count = reviewService.countUnanalyzedAll();
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return result;
    }

    @PostMapping("/analyze-all")
    public String analyzeAllReviews() {
        reviewService.analyzeAllReviews();
        return "分析任务已启动，请稍后查看结果";
    }

    @PostMapping("/upload/csv")
    public Map<String, Object> uploadReviewsByCSV(
            @RequestParam("file") MultipartFile file,
            @RequestParam("productId") Long productId) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            int count = reviewService.uploadReviewsFromCSV(file, productId);
            
            result.put("code", 200);
            result.put("message", "上传成功");
            result.put("count", count);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "上传失败: " + e.getMessage());
        }
        
        return result;
    }
}