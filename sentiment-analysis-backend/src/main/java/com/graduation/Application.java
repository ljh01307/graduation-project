package com.graduation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 开关注解，告诉 Spring Boot 这是启动类
@SpringBootApplication

public class Application {

    public static void main(String[] args) {
        // 启动 Spring Boot 容器，开启 Web 服务
        SpringApplication.run(Application.class, args);
        System.out.println("情感分析后端服务启动成功！");
        System.out.println("访问地址: http://localhost:8080");
        // ============= 新增结束 =============
    }
}