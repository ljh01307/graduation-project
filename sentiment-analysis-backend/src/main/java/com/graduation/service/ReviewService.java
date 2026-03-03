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

    // 注入模型服务客户端
    @Autowired
    private ModelServiceClient modelServiceClient;

    //批量上传评论 文件格式：CSV
    public int uploadReviewsFromCSV(MultipartFile file, Long productId) throws Exception {
        // 检查商品是否存在
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        
        List<Review> reviews = new ArrayList<>();
        
        // 解析CSV文件
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), "UTF-8"))) {
            
            CSVParser csvParser = CSVFormat.DEFAULT.builder()
                .setHeader()  
                .setSkipHeaderRecord(true)  // 跳过标题行
                .build()
                .parse(reader);
            
            // 获取所有记录
            List<CSVRecord> records = csvParser.getRecords();
            
            for (CSVRecord record : records) {
                // 获取评论内容（假设列名为"content"或"评论内容"）
                String content = null;
                
                // 方式1：通过列名获取
                try {
                    if (record.isMapped("content")) {
                        content = record.get("content");
                    } else if (record.isMapped("评论内容")) {
                        content = record.get("评论内容");
                    } else if (record.isMapped("内容")) {
                        content = record.get("内容");
                    }
                } catch (IllegalArgumentException e) {
                    // 列名不存在，忽略
                }
                
                // 方式2：如果没有列名或获取失败，取第一列
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
        
        // 批量保存
        reviewRepository.saveAll(reviews);
        return reviews.size();
    }
    
    // 批量上传评论 文件格式：JSON
    public List<Review> uploadReviews(Long productId, List<String> contents) {
        // 找到对应的商品
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));

        // 将每个评论内容转换为 Review 对象
        List<Review> reviews = contents.stream().map(content -> {
            Review review = new Review();
            review.setProduct(product);
            review.setContent(content);
            review.setAnalyzed(false);      // 默认未分析
            // 其他字段（sentimentLabel, confidence, analyzeTime）保持 null
            return review;
        }).toList();

        // 批量保存
        return reviewRepository.saveAll(reviews);
    }

    // 分析指定商品的所有未分析评论
    public void analyzeReviews(Long productId) {
        // 查询该商品下未分析的评论
        List<Review> reviews = reviewRepository.findByProductIdAndAnalyzed(productId, false);
        if (reviews.isEmpty()) {
            return;
        }

        // 提取评论内容列表
        List<String> contents = reviews.stream()
                .map(Review::getContent)
                .collect(Collectors.toList());

        // 调用 Python 批量预测
        List<ModelServiceClient.SentimentResult> results = modelServiceClient.batchPredict(contents);

        // 更新评论（假设返回结果的顺序与 contents 一致）
        for (int i = 0; i < reviews.size(); i++) {
            Review review = reviews.get(i);
            ModelServiceClient.SentimentResult result = results.get(i);
            review.setSentimentLabel(result.getLabel());
            review.setConfidence(result.getConfidence());
            review.setAnalyzed(true);
            review.setAnalyzeTime(LocalDateTime.now());
        }

        // 批量保存
        reviewRepository.saveAll(reviews);
    }

    // 统计某商品的正负面评论数量
    public Map<String, Long> getSentimentCount(Long productId) {
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

    //获取每周数据
    public Map<String, Object> getWeeklyStats(Long productId, int weeks) {
        // 查询该商品所有已分析的评论
        List<Review> reviews = reviewRepository.findByProductId(productId);
        
        // 过滤出已分析的评论
        List<Review> analyzedReviews = reviews.stream()
                .filter(Review::getAnalyzed)
                .collect(Collectors.toList());
        
        // 按周分组统计
        Map<String, Map<String, Long>> weeklyStats = new LinkedHashMap<>();
        
        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        
        // 生成最近weeks周的标签（如："第1周", "第2周" 或具体日期范围）
        for (int i = weeks - 1; i >= 0; i--) {
            LocalDateTime weekStart = now.minusWeeks(i).with(DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS);
            LocalDateTime weekEnd = weekStart.plusDays(6).with(LocalTime.MAX);
            
            String weekLabel = weekStart.format(DateTimeFormatter.ofPattern("MM.dd")) + 
                              " - " + 
                              weekEnd.format(DateTimeFormatter.ofPattern("MM.dd"));
            
            // 统计该周的正负面评论
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
        
        // 4. 构造返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("weeks", weeklyStats.keySet());
        result.put("positive", weeklyStats.values().stream().map(m -> m.get("positive")).collect(Collectors.toList()));
        result.put("negative", weeklyStats.values().stream().map(m -> m.get("negative")).collect(Collectors.toList()));
        
        return result;
    }

    //词云图
    public Map<String, Object> getWordCloudData(Long productId, int topN) {
        // 查询该商品所有已分析的评论
        List<Review> reviews = reviewRepository.findByProductId(productId);
        
        // 分离正面和负面评论
        List<String> positiveTexts = reviews.stream()
                .filter(r -> r.getAnalyzed() && r.getSentimentLabel() == 1)
                .map(Review::getContent)
                .collect(Collectors.toList());
        
        List<String> negativeTexts = reviews.stream()
                .filter(r -> r.getAnalyzed() && r.getSentimentLabel() == 0)
                .map(Review::getContent)
                .collect(Collectors.toList());
        
        // 调用Python服务进行分词和词频统计
        // 构造请求参数
        Map<String, Object> request = new HashMap<>();
        request.put("positive_texts", positiveTexts);
        request.put("negative_texts", negativeTexts);
        request.put("top_n", topN);
        
        // 调用Python服务的词云接口
        return modelServiceClient.getWordCloudData(request);
    }

    public Map<String, Object> getProductOverview(Long productId) {
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
    
    public Map<String, Object> getProductOverview(Long productId, LocalDateTime startTime, LocalDateTime endTime) {
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
            String sortDir) {
        
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
            String sortDir) {
        
        org.springframework.data.domain.Sort.Direction direction = 
            "desc".equalsIgnoreCase(sortDir) 
                ? org.springframework.data.domain.Sort.Direction.DESC 
                : org.springframework.data.domain.Sort.Direction.ASC;
        
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(page, size, direction, sortBy);
        
        Page<Review> reviewPage;
        
        if (productId != null) {
            reviewPage = reviewRepository.findByFilters(
                productId, sentimentLabel, startTime, endTime, pageable);
        } else {
            reviewPage = reviewRepository.findAllFilters(
                sentimentLabel, startTime, endTime, pageable);
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

    public Review updateSentimentLabel(Long reviewId, Integer sentimentLabel) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        
        review.setSentimentLabel(sentimentLabel);
        review.setAnalyzed(true);
        review.setConfidence(1.0);
        review.setAnalyzeTime(LocalDateTime.now());
        
        return reviewRepository.save(review);
    }
    
    public void deleteReview(Long reviewId) {
        reviewRepository.deleteById(reviewId);
    }
    
    public Map<String, Object> getKeywordAttribution(Long productId, int topN) {
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

    public long countUnanalyzed(Long productId) {
        return reviewRepository.countUnanalyzedByProductId(productId);
    }

    public long countUnanalyzedAll() {
        return reviewRepository.countUnanalyzedAll();
    }

    public void analyzeAllReviews() {
        List<Review> unanalyzed = reviewRepository.findByAnalyzed(false);
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