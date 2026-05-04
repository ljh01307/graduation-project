package com.graduation.controller;

import com.graduation.common.GlobalResponse;
import com.graduation.entity.Review;
import com.graduation.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping("/upload")
    public GlobalResponse<?> uploadReviews(@RequestBody Map<String, Object> payload,
                                         @RequestAttribute Long userId,
                                         @RequestAttribute String role,
                                         @RequestParam(required = false) Long manageUserId) {
        Long targetUserId = reviewService.resolveTargetUserId(userId, role, manageUserId);
        Long productId = Long.valueOf(payload.get("productId").toString());
        @SuppressWarnings("unchecked")
        List<String> contents = (List<String>) payload.get("contents");
        List<Review> reviews = reviewService.uploadReviews(productId, contents, targetUserId);
        return GlobalResponse.success("评论上传成功", reviews);
    }

    @PostMapping("/analyze/{productId}")
    public GlobalResponse<?> analyzeReviews(@PathVariable Long productId,
                                          @RequestAttribute Long userId,
                                          @RequestAttribute String role,
                                          @RequestParam(required = false) Long manageUserId) {
        Long targetUserId = reviewService.resolveTargetUserId(userId, role, manageUserId);
        reviewService.analyzeReviews(productId, targetUserId);
        return GlobalResponse.success("分析任务已启动，请稍后查看结果", null);
    }

    @GetMapping("/stats/{productId}")
    public GlobalResponse<?> getStats(@PathVariable Long productId,
                                      @RequestAttribute Long userId,
                                      @RequestAttribute String role,
                                      @RequestParam(required = false) Long manageUserId) {
        Long targetUserId = reviewService.resolveTargetUserId(userId, role, manageUserId);
        Map<String, Long> stats = reviewService.getSentimentCount(productId, targetUserId);
        return GlobalResponse.success(stats);
    }

    @GetMapping("/weekly/{productId}")
    public GlobalResponse<?> getWeeklyStats(@PathVariable Long productId,
                                            @RequestParam(defaultValue = "4") int weeks,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                                            @RequestAttribute Long userId,
                                            @RequestAttribute String role,
                                            @RequestParam(required = false) Long manageUserId) {
        Long targetUserId = reviewService.resolveTargetUserId(userId, role, manageUserId);
        Map<String, Object> data;
        if (startTime != null && endTime != null) {
            data = reviewService.getWeeklyStats(productId, startTime, endTime, targetUserId);
        } else {
            data = reviewService.getWeeklyStats(productId, weeks, targetUserId);
        }
        return GlobalResponse.success(data);
    }

    @GetMapping("/wordcloud/{productId}")
    public GlobalResponse<?> getWordCloudData(@PathVariable Long productId,
                                              @RequestParam(defaultValue = "50") int topN,
                                              @RequestAttribute Long userId,
                                              @RequestAttribute String role,
                                              @RequestParam(required = false) Long manageUserId) {
        Long targetUserId = reviewService.resolveTargetUserId(userId, role, manageUserId);
        Map<String, Object> data = reviewService.getWordCloudData(productId, topN, targetUserId);
        return GlobalResponse.success(data);
    }

    @GetMapping("/overview/{productId}")
    public GlobalResponse<?> getProductOverview(@PathVariable Long productId,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                                                @RequestAttribute Long userId,
                                                @RequestAttribute String role,
                                                @RequestParam(required = false) Long manageUserId) {
        Long targetUserId = reviewService.resolveTargetUserId(userId, role, manageUserId);
        Map<String, Object> data;
        if (startTime != null && endTime != null) {
            data = reviewService.getProductOverview(productId, startTime, endTime, targetUserId);
        } else {
            data = reviewService.getProductOverview(productId, targetUserId);
        }
        return GlobalResponse.success(data);
    }

    @GetMapping("/list/{productId}")
    public GlobalResponse<?> getReviewsByPage(@PathVariable Long productId,
                                               @RequestParam(required = false) Integer sentimentLabel,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @RequestParam(defaultValue = "uploadTime") String sortBy,
                                               @RequestParam(defaultValue = "DESC") String sortDir,
                                               @RequestAttribute Long userId,
                                               @RequestAttribute String role,
                                               @RequestParam(required = false) Long manageUserId) {
        Long targetUserId = reviewService.resolveTargetUserId(userId, role, manageUserId);
        Map<String, Object> data = reviewService.getReviewsByPage(
            productId, sentimentLabel, startTime, endTime, page, size, sortBy, sortDir, targetUserId);
        return GlobalResponse.success(data);
    }

    @GetMapping("/list-all")
    public GlobalResponse<?> getAllReviewsByPage(@RequestParam(required = false) Long productId,
                                                  @RequestParam(required = false) Integer sentimentLabel,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  @RequestParam(defaultValue = "uploadTime") String sortBy,
                                                  @RequestParam(defaultValue = "DESC") String sortDir,
                                                  @RequestAttribute Long userId,
                                                  @RequestAttribute String role,
                                                  @RequestParam(required = false) Long manageUserId) {
        Long targetUserId = reviewService.resolveTargetUserId(userId, role, manageUserId);
        Map<String, Object> data = reviewService.getAllReviewsByPage(
            productId, sentimentLabel, startTime, endTime, page, size, sortBy, sortDir, targetUserId);
        return GlobalResponse.success(data);
    }

    @PutMapping("/update-sentiment/{reviewId}")
    public GlobalResponse<?> updateSentimentLabel(@PathVariable Long reviewId,
                                                   @RequestBody Map<String, Object> payload,
                                                   @RequestAttribute Long userId,
                                                   @RequestAttribute String role,
                                                   @RequestParam(required = false) Long manageUserId) {
        Long targetUserId = reviewService.resolveTargetUserId(userId, role, manageUserId);
        Integer sentimentLabel = (Integer) payload.get("sentimentLabel");
        if (sentimentLabel == null || (sentimentLabel != 0 && sentimentLabel != 1)) {
            throw new IllegalArgumentException("情感标签必须为0或1");
        }
        Review review = reviewService.updateSentimentLabel(reviewId, sentimentLabel, targetUserId);
        return GlobalResponse.success("情感标签更新成功", review);
    }

    @DeleteMapping("/delete/{reviewId}")
    public GlobalResponse<?> deleteReview(@PathVariable Long reviewId,
                                            @RequestAttribute Long userId,
                                            @RequestAttribute String role,
                                            @RequestParam(required = false) Long manageUserId) {
        Long targetUserId = reviewService.resolveTargetUserId(userId, role, manageUserId);
        reviewService.deleteReview(reviewId, targetUserId);
        return GlobalResponse.success("评论删除成功", null);
    }

    @GetMapping("/keyword-attribution/{productId}")
    public GlobalResponse<?> getKeywordAttribution(@PathVariable Long productId,
                                                    @RequestParam(defaultValue = "20") int topN,
                                                    @RequestAttribute Long userId,
                                                    @RequestAttribute String role,
                                                    @RequestParam(required = false) Long manageUserId) {
        Long targetUserId = reviewService.resolveTargetUserId(userId, role, manageUserId);
        Map<String, Object> data = reviewService.getKeywordAttribution(productId, topN, targetUserId);
        return GlobalResponse.success(data);
    }

    @GetMapping("/unanalyzed-count/{productId}")
    public GlobalResponse<?> getUnanalyzedCount(@PathVariable Long productId,
                                                 @RequestAttribute Long userId,
                                                 @RequestAttribute String role,
                                                 @RequestParam(required = false) Long manageUserId) {
        Long targetUserId = reviewService.resolveTargetUserId(userId, role, manageUserId);
        long count = reviewService.countUnanalyzed(productId, targetUserId);
        return GlobalResponse.success(count);
    }

    @GetMapping("/unanalyzed-count")
    public GlobalResponse<?> getUnanalyzedCountAll(@RequestAttribute Long userId,
                                                    @RequestAttribute String role,
                                                    @RequestParam(required = false) Long manageUserId) {
        Long targetUserId = reviewService.resolveTargetUserId(userId, role, manageUserId);
        long count = reviewService.countUnanalyzedAll(targetUserId);
        return GlobalResponse.success(count);
    }

    @PostMapping("/analyze-all")
    public GlobalResponse<?> analyzeAllReviews(@RequestAttribute Long userId,
                                               @RequestAttribute String role,
                                               @RequestParam(required = false) Long manageUserId) {
        Long targetUserId = reviewService.resolveTargetUserId(userId, role, manageUserId);
        reviewService.analyzeAllReviews(targetUserId);
        return GlobalResponse.success("分析任务已启动，请稍后查看结果", null);
    }
}
