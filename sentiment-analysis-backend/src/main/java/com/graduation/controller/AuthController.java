package com.graduation.controller;

import com.graduation.common.GlobalResponse;
import com.graduation.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public GlobalResponse<?> login(@RequestBody Map<String, Object> request) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        Boolean remember = (Boolean) request.get("remember");

        Map<String, Object> data = authService.login(username, password, remember);
        return GlobalResponse.success("登录成功", data);
    }

    @PostMapping("/register")
    public GlobalResponse<?> register(@RequestBody Map<String, Object> request) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");

        Map<String, Object> data = authService.register(username, password);
        return GlobalResponse.success("注册成功", data);
    }

    @GetMapping("/users")
    public GlobalResponse<?> getAllUsers(@RequestAttribute String role) {
        if (!"ADMIN".equals(role)) {
            return GlobalResponse.error(403, "无权限访问");
        }

        return GlobalResponse.success(authService.getAllUsers());
    }

    @PutMapping("/username")
    public GlobalResponse<?> updateUsername(@RequestAttribute Long userId, @RequestBody Map<String, Object> request) {
        String newUsername = (String) request.get("username");
        authService.updateUsername(userId, newUsername);
        return GlobalResponse.success("用户名修改成功", null);
    }

    @PutMapping("/password")
    public GlobalResponse<?> updatePassword(@RequestAttribute Long userId, @RequestBody Map<String, Object> request) {
        String oldPassword = (String) request.get("oldPassword");
        String newPassword = (String) request.get("newPassword");
        authService.updatePassword(userId, oldPassword, newPassword);
        return GlobalResponse.success("密码修改成功", null);
    }
}
