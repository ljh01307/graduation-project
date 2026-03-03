package com.graduation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map; 

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

@Service
public class ModelServiceClient {
    
    private final String PYTHON_SERVICE_URL = "http://127.0.0.1:5000";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    //单条评论情感预测

    public SentimentResult predict(String text) {
        try {
            String url = PYTHON_SERVICE_URL + "/predict";
            
            // 构造请求体
            Map<String, String> request = new HashMap<>();
            request.put("text", text);
            
            // 发送POST请求
            String response = restTemplate.postForObject(url, request, String.class);
            
            // 解析JSON响应
            JsonNode json = objectMapper.readTree(response);
            
            SentimentResult result = new SentimentResult();
            result.setText(json.get("text").asText());
            result.setSentiment(json.get("sentiment").asText());
            result.setLabel(json.get("label").asInt());
            result.setConfidence(json.get("confidence").asDouble());
            
            return result;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 批量评论预测
    public List<SentimentResult> batchPredict(List<String> texts) {
        String url = PYTHON_SERVICE_URL + "/batch_predict";
        
        // 构造请求体
        Map<String, List<String>> request = new HashMap<>();
        request.put("texts", texts);
        
        try {
            // 发送POST请求
            String response = restTemplate.postForObject(url, request, String.class);
            
            // 解析JSON响应
            JsonNode root = objectMapper.readTree(response);
            JsonNode resultsNode = root.get("results");
            
            List<SentimentResult> results = new ArrayList<>();
            if (resultsNode != null && resultsNode.isArray()) {
                for (JsonNode node : resultsNode) {
                    SentimentResult result = new SentimentResult();
                    // 根据实际返回字段调整
                    result.setText(node.get("text").asText());
                    result.setLabel(node.get("label").asInt());
                    result.setConfidence(node.get("confidence").asDouble());
                    result.setSentiment(node.get("sentiment").asText());
                    results.add(result);
                }
            }
            return results;
            
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList(); // 返回空列表，避免空指针
        }
    }
    
    //预测结果封装类
    
    public static class SentimentResult {
        private String text;
        private String sentiment;
        private Integer label;
        private Double confidence;
        
        // getter和setter
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public String getSentiment() { return sentiment; }
        public void setSentiment(String sentiment) { this.sentiment = sentiment; }
        
        public Integer getLabel() { return label; }
        public void setLabel(Integer label) { this.label = label; }
        
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
        
        @Override
        public String toString() {
            return String.format("SentimentResult{text='%s', sentiment='%s', label=%d, confidence=%.2f}",
                    text, sentiment, label, confidence);
        }
    }

    //词云
    public Map<String, Object> getWordCloudData(Map<String, Object> request) {
        String url = PYTHON_SERVICE_URL + "/wordcloud";
        
        try {
            System.out.println("调用Python服务URL: " + url);
            System.out.println("请求参数: " + request);
            
            String response = restTemplate.postForObject(url, request, String.class);
            System.out.println("Python服务返回原始数据: " + response);
            
            JsonNode json = objectMapper.readTree(response);
            
            Map<String, Object> result = new HashMap<>();
            
            // 正面词云数据 - Python返回的是name和value
            List<Map<String, Object>> positiveWords = new ArrayList<>();
            JsonNode positiveNode = json.get("positive");
            if (positiveNode != null && positiveNode.isArray()) {
                for (JsonNode node : positiveNode) {
                    Map<String, Object> word = new HashMap<>();
                    // Python用的是name，不是word
                    word.put("word", node.get("name").asText());
                    // Python用的是value，不是frequency或count
                    word.put("frequency", node.get("value").asInt());
                    positiveWords.add(word);
                }
            }
            
            // 负面词云数据
            List<Map<String, Object>> negativeWords = new ArrayList<>();
            JsonNode negativeNode = json.get("negative");
            if (negativeNode != null && negativeNode.isArray()) {
                for (JsonNode node : negativeNode) {
                    Map<String, Object> word = new HashMap<>();
                    word.put("word", node.get("name").asText());
                    word.put("frequency", node.get("value").asInt());
                    negativeWords.add(word);
                }
            }
            
            result.put("positive", positiveWords);
            result.put("negative", negativeWords);
            
            System.out.println("处理后的正面词云数据: " + positiveWords);
            System.out.println("处理后的负面词云数据: " + negativeWords);
            
            return result;
            
        } catch (Exception e) {
            System.err.println("调用Python服务失败: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> empty = new HashMap<>();
            empty.put("positive", Collections.emptyList());
            empty.put("negative", Collections.emptyList());
            return empty;
        }
    }

    public Map<String, Object> getKeywordAttribution(Map<String, Object> request) {
        String url = PYTHON_SERVICE_URL + "/keyword_attribution";
        
        try {
            System.out.println("调用Python关键词归因服务: " + url);
            
            String response = restTemplate.postForObject(url, request, String.class);
            
            JsonNode json = objectMapper.readTree(response);
            
            Map<String, Object> result = new HashMap<>();
            
            List<Map<String, Object>> positiveKeywords = new ArrayList<>();
            JsonNode positiveNode = json.get("positive_keywords");
            if (positiveNode != null && positiveNode.isArray()) {
                for (JsonNode node : positiveNode) {
                    Map<String, Object> keyword = new HashMap<>();
                    keyword.put("keyword", node.get("keyword").asText());
                    keyword.put("count", node.get("count").asInt());
                    keyword.put("examples", parseExamples(node.get("examples")));
                    positiveKeywords.add(keyword);
                }
            }
            
            List<Map<String, Object>> negativeKeywords = new ArrayList<>();
            JsonNode negativeNode = json.get("negative_keywords");
            if (negativeNode != null && negativeNode.isArray()) {
                for (JsonNode node : negativeNode) {
                    Map<String, Object> keyword = new HashMap<>();
                    keyword.put("keyword", node.get("keyword").asText());
                    keyword.put("count", node.get("count").asInt());
                    keyword.put("examples", parseExamples(node.get("examples")));
                    negativeKeywords.add(keyword);
                }
            }
            
            result.put("positive_keywords", positiveKeywords);
            result.put("negative_keywords", negativeKeywords);
            
            return result;
            
        } catch (Exception e) {
            System.err.println("调用Python关键词归因服务失败: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> empty = new HashMap<>();
            empty.put("positive_keywords", Collections.emptyList());
            empty.put("negative_keywords", Collections.emptyList());
            return empty;
        }
    }
    
    private List<String> parseExamples(JsonNode examplesNode) {
        List<String> examples = new ArrayList<>();
        if (examplesNode != null && examplesNode.isArray()) {
            for (JsonNode example : examplesNode) {
                examples.add(example.asText());
            }
        }
        return examples;
    }
}