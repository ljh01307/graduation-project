package com.graduation.service;

import com.graduation.entity.Product;
import com.graduation.entity.Review;
import com.graduation.repository.ProductRepository;
import com.graduation.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import org.springframework.data.domain.Page;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ModelServiceClient modelServiceClient;

    public Long resolveTargetUserId(Long currentUserId, String currentRole, Long manageUserId) {
        if ("ADMIN".equals(currentRole) && manageUserId != null) {
            return manageUserId;
        }
        return currentUserId;
    }

    private Product findProductWithUser(Long productId, Long userId) {
        return productRepository.findByIdAndUserId(productId, userId)
                .orElseThrow(() -> new RuntimeException("商品不存在或无权限"));
    }

    public int uploadReviewsFromCSV(MultipartFile file, Long productId, Long userId) throws Exception {
        Product product = findProductWithUser(productId, userId);

        List<Review> reviews = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), "UTF-8"))) {

            CSVParser csvParser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(reader);

            List<CSVRecord> records = csvParser.getRecords();

            for (CSVRecord record : records) {
                String content = null;

                try {
                    if (record.isMapped("content")) {
                        content = record.get("content");
                    } else if (record.isMapped("评论内容")) {
                        content = record.get("评论内容");
                    } else if (record.isMapped("内容")) {
                        content = record.get("内容");
                    }
                } catch (IllegalArgumentException e) {
                }

                if (content == null && record.size() > 0) {
                    content = record.get(0);
                }

                if (content != null && !content.trim().isEmpty()) {
                    Review review = new Review();
                    review.setProduct(product);
                    review.setContent(content.trim());
                    review.setAnalyzed(false);
                    reviews.add(review);
                }
            }

            csvParser.close();
        }

        reviewRepository.saveAll(reviews);
        return reviews.size();
    }

    public List<Review> uploadReviews(Long productId, List<String> contents, Long userId) {
        Product product = findProductWithUser(productId, userId);

        List<Review> reviews = contents.stream().map(content -> {
            Review review = new Review();
            review.setProduct(product);
            review.setContent(content);
            review.setAnalyzed(false);
            return review;
        }).toList();

        return reviewRepository.saveAll(reviews);
    }

    public void analyzeReviews(Long productId, Long userId) {
        findProductWithUser(productId, userId);

        List<Review> reviews = reviewRepository.findByProductIdAndAnalyzed(productId, false);
        if (reviews.isEmpty()) {
            return;
        }

        List<String> contents = reviews.stream()
                .map(Review::getContent)
                .collect(Collectors.toList());

        List<ModelServiceClient.SentimentResult> results = modelServiceClient.batchPredict(contents);

        for (int i = 0; i < reviews.size(); i++) {
            Review review = reviews.get(i);
            ModelServiceClient.SentimentResult result = results.get(i);
            review.setSentimentLabel(result.getLabel());
            review.setConfidence(result.getConfidence());
            review.setAnalyzed(true);
            review.setAnalyzeTime(LocalDateTime.now());
        }

        reviewRepository.saveAll(reviews);
    }

    public Map<String, Long> getSentimentCount(Long productId, Long userId) {
        findProductWithUser(productId, userId);
        List<Review> reviews = reviewRepository.findByProductId(productId);

        long positive = reviews.stream()
                .filter(r -> r.getAnalyzed() && r.getSentimentLabel() == 1)
                .count();

        long negative = reviews.stream()
                .filter(r -> r.getAnalyzed() && r.getSentimentLabel() == 0)
                .count();

        Map<String, Long> result = new HashMap<>();
        result.put("positive", positive);
        result.put("negative", negative);
        return result;
    }

    public Map<String, Object> getWeeklyStats(Long productId, int weeks, Long userId) {
        findProductWithUser(productId, userId);
        List<Review> reviews = reviewRepository.findByProductId(productId);

        List<Review> analyzedReviews = reviews.stream()
                .filter(Review::getAnalyzed)
                .collect(Collectors.toList());

        Map<String, Map<String, Long>> weeklyStats = new LinkedHashMap<>();

        LocalDateTime now = LocalDateTime.now();

        for (int i = weeks - 1; i >= 0; i--) {
            LocalDateTime weekStart = now.minusWeeks(i).with(DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS);
            LocalDateTime weekEnd = weekStart.plusDays(6).with(LocalTime.MAX);

            String weekLabel = weekStart.format(DateTimeFormatter.ofPattern("MM.dd")) +
                              " - " +
                              weekEnd.format(DateTimeFormatter.ofPattern("MM.dd"));

            long positive = analyzedReviews.stream()
                    .filter(r -> !r.getAnalyzeTime().isBefore(weekStart)
                              && !r.getAnalyzeTime().isAfter(weekEnd)
                              && r.getSentimentLabel() == 1)
                    .count();

            long negative = analyzedReviews.stream()
                    .filter(r -> !r.getAnalyzeTime().isBefore(weekStart)
                              && !r.getAnalyzeTime().isAfter(weekEnd)
                              && r.getSentimentLabel() == 0)
                    .count();

            Map<String, Long> weekData = new HashMap<>();
            weekData.put("positive", positive);
            weekData.put("negative", negative);
            weeklyStats.put(weekLabel, weekData);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("weeks", weeklyStats.keySet());
        result.put("positive", weeklyStats.values().stream().map(m -> m.get("positive")).collect(Collectors.toList()));
        result.put("negative", weeklyStats.values().stream().map(m -> m.get("negative")).collect(Collectors.toList()));

        return result;
    }

    public Map<String, Object> getWeeklyStats(Long productId, LocalDateTime startTime, LocalDateTime endTime, Long userId) {
        findProductWithUser(productId, userId);
        List<Review> reviews;
        if (startTime != null && endTime != null) {
            reviews = reviewRepository.findByProductIdAndUploadTimeBetween(productId, startTime, endTime);
        } else {
            reviews = reviewRepository.findByProductId(productId);
        }

        List<Review> analyzedReviews = reviews.stream()
                .filter(Review::getAnalyzed)
                .collect(Collectors.toList());

        Map<String, Map<String, Long>> weeklyStats = new LinkedHashMap<>();

        LocalDateTime current = startTime.truncatedTo(ChronoUnit.DAYS);
        LocalDateTime end = endTime.truncatedTo(ChronoUnit.DAYS);

        while (!current.isAfter(end)) {
            LocalDateTime weekStartRaw = current.with(DayOfWeek.MONDAY);
            LocalDateTime weekEndRaw = weekStartRaw.plusDays(6).with(LocalTime.MAX);

            final LocalDateTime weekStart = weekStartRaw.isBefore(startTime) ? startTime : weekStartRaw;
            final LocalDateTime weekEnd = weekEndRaw.isAfter(endTime) ? endTime : weekEndRaw;

            String weekLabel = weekStart.toLocalDate().format(DateTimeFormatter.ofPattern("MM.dd")) +
                              " - " +
                              weekEnd.toLocalDate().format(DateTimeFormatter.ofPattern("MM.dd"));

            long positive = analyzedReviews.stream()
                    .filter(r -> r.getAnalyzeTime() != null
                              && !r.getAnalyzeTime().isBefore(weekStart)
                              && !r.getAnalyzeTime().isAfter(weekEnd)
                              && r.getSentimentLabel() == 1)
                    .count();

            long negative = analyzedReviews.stream()
                    .filter(r -> r.getAnalyzeTime() != null
                              && !r.getAnalyzeTime().isBefore(weekStart)
                              && !r.getAnalyzeTime().isAfter(weekEnd)
                              && r.getSentimentLabel() == 0)
                    .count();

            Map<String, Long> weekData = new HashMap<>();
            weekData.put("positive", positive);
            weekData.put("negative", negative);
            weeklyStats.put(weekLabel, weekData);

            current = weekStart.plusWeeks(1);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("weeks", weeklyStats.keySet());
        result.put("positive", weeklyStats.values().stream().map(m -> m.get("positive")).collect(Collectors.toList()));
        result.put("negative", weeklyStats.values().stream().map(m -> m.get("negative")).collect(Collectors.toList()));

        return result;
    }

    public Map<String, Object> getWordCloudData(Long productId, int topN, Long userId) {
        findProductWithUser(productId, userId);
        List<Review> reviews = reviewRepository.findByProductId(productId);

        List<String> positiveTexts = reviews.stream()
                .filter(r -> r.getAnalyzed() && r.getSentimentLabel() == 1)
                .map(Review::getContent)
                .collect(Collectors.toList());

        List<String> negativeTexts = reviews.stream()
                .filter(r -> r.getAnalyzed() && r.getSentimentLabel() == 0)
                .map(Review::getContent)
                .collect(Collectors.toList());

        Map<String, Object> request = new HashMap<>();
        request.put("positive_texts", positiveTexts);
        request.put("negative_texts", negativeTexts);
        request.put("top_n", topN);

        return modelServiceClient.getWordCloudData(request);
    }

    public Map<String, Object> getProductOverview(Long productId, Long userId) {
        findProductWithUser(productId, userId);
        List<Review> reviews = reviewRepository.findByProductId(productId);

        long total = reviews.size();
        long analyzed = reviews.stream().filter(Review::getAnalyzed).count();
        long positive = reviews.stream()
                .filter(r -> r.getAnalyzed() && r.getSentimentLabel() == 1)
                .count();
        long negative = reviews.stream()
                .filter(r -> r.getAnalyzed() && r.getSentimentLabel() == 0)
                .count();

        double positiveRate = analyzed > 0 ? (double) positive / analyzed * 100 : 0;

        Map<String, Object> overview = new HashMap<>();
        overview.put("total", total);
        overview.put("analyzedReviews", analyzed);
        overview.put("positiveCount", positive);
        overview.put("negativeCount", negative);
        overview.put("positiveRate", Math.round(positiveRate * 100) / 100.0);

        return overview;
    }

    public Map<String, Object> getProductOverview(Long productId, LocalDateTime startTime, LocalDateTime endTime, Long userId) {
        findProductWithUser(productId, userId);
        List<Review> reviews;
        if (startTime != null && endTime != null) {
            reviews = reviewRepository.findByProductIdAndUploadTimeBetween(productId, startTime, endTime);
        } else {
            reviews = reviewRepository.findByProductId(productId);
        }

        long total = reviews.size();
        long analyzed = reviews.stream().filter(Review::getAnalyzed).count();
        long positive = reviews.stream()
                .filter(r -> r.getAnalyzed() && r.getSentimentLabel() == 1)
                .count();
        long negative = reviews.stream()
                .filter(r -> r.getAnalyzed() && r.getSentimentLabel() == 0)
                .count();

        double positiveRate = analyzed > 0 ? (double) positive / analyzed * 100 : 0;

        Map<String, Object> overview = new HashMap<>();
        overview.put("total", total);
        overview.put("analyzedReviews", analyzed);
        overview.put("positiveCount", positive);
        overview.put("negativeCount", negative);
        overview.put("positiveRate", Math.round(positiveRate * 100) / 100.0);

        return overview;
    }

    public Map<String, Object> getReviewsByPage(
            Long productId,
            Integer sentimentLabel,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int page,
            int size,
            String sortBy,
            String sortDir,
            Long userId) {

        findProductWithUser(productId, userId);

        org.springframework.data.domain.Sort.Direction direction =
            "desc".equalsIgnoreCase(sortDir)
                ? org.springframework.data.domain.Sort.Direction.DESC
                : org.springframework.data.domain.Sort.Direction.ASC;

        org.springframework.data.domain.Pageable pageable =
            org.springframework.data.domain.PageRequest.of(page, size, direction, sortBy);

        Page<Review> reviewPage = reviewRepository.findByFilters(
            productId, sentimentLabel, startTime, endTime, pageable);

        List<Map<String, Object>> reviews = new ArrayList<>();
        for (Review review : reviewPage.getContent()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", review.getId());
            item.put("content", review.getContent());
            item.put("sentimentLabel", review.getSentimentLabel());
            item.put("confidence", review.getConfidence());
            item.put("analyzed", review.getAnalyzed());
            item.put("uploadTime", review.getUploadTime());
            item.put("analyzeTime", review.getAnalyzeTime());
            item.put("productId", review.getProduct().getId());
            item.put("productCategory", review.getProduct().getCategory());
            reviews.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("reviews", reviews);
        result.put("totalElements", reviewPage.getTotalElements());
        result.put("totalPages", reviewPage.getTotalPages());
        result.put("currentPage", reviewPage.getNumber());
        result.put("size", reviewPage.getSize());

        return result;
    }

    public Map<String, Object> getAllReviewsByPage(
            Long productId,
            Integer sentimentLabel,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int page,
            int size,
            String sortBy,
            String sortDir,
            Long userId) {

        org.springframework.data.domain.Sort.Direction direction =
            "desc".equalsIgnoreCase(sortDir)
                ? org.springframework.data.domain.Sort.Direction.DESC
                : org.springframework.data.domain.Sort.Direction.ASC;

        org.springframework.data.domain.Pageable pageable =
            org.springframework.data.domain.PageRequest.of(page, size, direction, sortBy);

        Page<Review> reviewPage;

        if (productId != null) {
            findProductWithUser(productId, userId);
            reviewPage = reviewRepository.findByFilters(
                productId, sentimentLabel, startTime, endTime, pageable);
        } else {
            reviewPage = reviewRepository.findAllFilters(
                userId, sentimentLabel, startTime, endTime, pageable);
        }

        List<Map<String, Object>> reviews = new ArrayList<>();
        for (Review review : reviewPage.getContent()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", review.getId());
            item.put("content", review.getContent());
            item.put("sentimentLabel", review.getSentimentLabel());
            item.put("confidence", review.getConfidence());
            item.put("analyzed", review.getAnalyzed());
            item.put("uploadTime", review.getUploadTime());
            item.put("analyzeTime", review.getAnalyzeTime());
            item.put("productId", review.getProduct().getId());
            item.put("productCategory", review.getProduct().getCategory());
            reviews.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("reviews", reviews);
        result.put("totalElements", reviewPage.getTotalElements());
        result.put("totalPages", reviewPage.getTotalPages());
        result.put("currentPage", reviewPage.getNumber());
        result.put("size", reviewPage.getSize());

        return result;
    }

    public Review updateSentimentLabel(Long reviewId, Integer sentimentLabel, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        findProductWithUser(review.getProduct().getId(), userId);

        review.setSentimentLabel(sentimentLabel);
        review.setAnalyzed(true);
        review.setConfidence(1.0);
        review.setAnalyzeTime(LocalDateTime.now());

        return reviewRepository.save(review);
    }

    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        findProductWithUser(review.getProduct().getId(), userId);

        reviewRepository.delete(review);
    }

    public Map<String, Object> getKeywordAttribution(Long productId, int topN, Long userId) {
        findProductWithUser(productId, userId);
        List<Review> reviews = reviewRepository.findByProductId(productId);

        List<String> positiveTexts = reviews.stream()
                .filter(r -> r.getAnalyzed() && r.getSentimentLabel() == 1)
                .map(Review::getContent)
                .collect(Collectors.toList());

        List<String> negativeTexts = reviews.stream()
                .filter(r -> r.getAnalyzed() && r.getSentimentLabel() == 0)
                .map(Review::getContent)
                .collect(Collectors.toList());

        Map<String, Object> request = new HashMap<>();
        request.put("positive_texts", positiveTexts);
        request.put("negative_texts", negativeTexts);
        request.put("top_n", topN);

        return modelServiceClient.getKeywordAttribution(request);
    }

    public long countUnanalyzed(Long productId, Long userId) {
        findProductWithUser(productId, userId);
        return reviewRepository.countUnanalyzedByProductId(productId);
    }

    public long countUnanalyzedAll(Long userId) {
        return reviewRepository.countUnanalyzedAll(userId);
    }

    public void analyzeAllReviews(Long userId) {
        List<Review> unanalyzed = reviewRepository.findByUserIdAndAnalyzed(userId, false);
        if (unanalyzed.isEmpty()) return;

        List<String> texts = unanalyzed.stream()
                .map(Review::getContent)
                .collect(Collectors.toList());

        List<ModelServiceClient.SentimentResult> predictions = modelServiceClient.batchPredict(texts);

        if (predictions != null) {
            for (int i = 0; i < predictions.size() && i < unanalyzed.size(); i++) {
                Review review = unanalyzed.get(i);
                ModelServiceClient.SentimentResult pred = predictions.get(i);

                review.setAnalyzed(true);
                review.setSentimentLabel(pred.getLabel());
                review.setConfidence(pred.getConfidence());
                review.setAnalyzeTime(LocalDateTime.now());
            }
        }

        reviewRepository.saveAll(unanalyzed);
    }
}
