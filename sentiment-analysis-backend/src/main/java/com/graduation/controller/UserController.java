package com.graduation.controller;

import com.graduation.common.GlobalResponse;
import com.graduation.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public GlobalResponse<?> getAllUsers(@RequestAttribute String role) {
        if (!"ADMIN".equals(role)) {
            return GlobalResponse.error(403, "无权限访问");
        }

        return GlobalResponse.success(userService.getAllUsers());
    }

    @PostMapping
    public GlobalResponse<?> addUser(@RequestAttribute String role, @RequestBody Map<String, Object> request) {
        if (!"ADMIN".equals(role)) {
            return GlobalResponse.error(403, "无权限访问");
        }

        String username = (String) request.get("username");
        String password = (String) request.get("password");
        String userRole = (String) request.get("role");

        Map<String, Object> data = userService.addUser(username, password, userRole);
        return GlobalResponse.success("用户添加成功", data);
    }

    @PutMapping("/{id}")
    public GlobalResponse<?> updateUser(@RequestAttribute String role, @PathVariable Long id, @RequestBody Map<String, Object> request) {
        if (!"ADMIN".equals(role)) {
            return GlobalResponse.error(403, "无权限访问");
        }

        String username = (String) request.get("username");
        userService.updateUser(id, username);
        return GlobalResponse.success("用户更新成功", null);
    }

    @DeleteMapping("/{id}")
    public GlobalResponse<?> deleteUser(@RequestAttribute String role, @RequestAttribute Long userId, @PathVariable Long id) {
        if (!"ADMIN".equals(role)) {
            return GlobalResponse.error(403, "无权限访问");
        }

        if (userId.equals(id)) {
            return GlobalResponse.error("不能删除自己");
        }

        userService.deleteUser(id);
        return GlobalResponse.success("用户删除成功", null);
    }
}
