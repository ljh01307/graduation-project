package com.graduation.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "review")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne  // 多个评论属于一个商品
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;  // 关联的商品

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;   // 评论文本

    private Integer sentimentLabel;  // 情感标签：1好评，0差评（分析后填充）

    private Double confidence;       // 置信度（分析后填充）

    @Column(name = "is_analyzed")
    private Boolean analyzed = false;  // 是否已分析

    @Column(name = "upload_time")
    private LocalDateTime uploadTime = LocalDateTime.now();  // 上传时间

    @Column(name = "analyze_time")
    private LocalDateTime analyzeTime;  // 分析时间（分析后填充）

    // 必须的 getter 和 setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getSentimentLabel() { return sentimentLabel; }
    public void setSentimentLabel(Integer sentimentLabel) { this.sentimentLabel = sentimentLabel; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public Boolean getAnalyzed() { return analyzed; }
    public void setAnalyzed(Boolean analyzed) { this.analyzed = analyzed; }

    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }

    public LocalDateTime getAnalyzeTime() { return analyzeTime; }
    public void setAnalyzeTime(LocalDateTime analyzeTime) { this.analyzeTime = analyzeTime; }
}