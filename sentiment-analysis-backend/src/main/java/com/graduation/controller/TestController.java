package com.graduation.controller;

import com.graduation.service.ModelServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin // 允许所有来源（仅开发用！）
@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @Autowired
    private ModelServiceClient modelServiceClient;
    
    @GetMapping("/ping")
    public String ping() {
        return "Java后端服务正常！";
    }
    
    @PostMapping("/predict")
    public Map<String, Object> predict(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        
        // 调用Python模型服务
        ModelServiceClient.SentimentResult result = modelServiceClient.predict(text);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", result);
        
        return response;
    }
}