package com.graduation.service;

import com.graduation.entity.User;
import com.graduation.repository.UserRepository;
import com.graduation.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostConstruct
    public void initDefaultUsers() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            userRepository.save(admin);
        }

        if (!userRepository.existsByUsername("default123")) {
            User defaultUser = new User();
            defaultUser.setUsername("default123");
            defaultUser.setPassword(passwordEncoder.encode("default123"));
            defaultUser.setRole("USER");
            userRepository.save(defaultUser);
        }
    }

    public Map<String, Object> login(String username, String password, Boolean remember) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        boolean rememberMe = remember != null && remember;
        String token = jwtUtil.generateToken(user.getUsername(), user.getId(), user.getRole(), rememberMe);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("userId", user.getId());
        return result;
    }

    public Map<String, Object> register(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER");

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername(), user.getId(), user.getRole(), false);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("userId", user.getId());
        return result;
    }

    public java.util.List<Map<String, Object>> getAllUsers() {
        return userRepository.findAll().stream()
            .map(user -> {
                java.util.Map<String, Object> item = new java.util.HashMap<>();
                item.put("id", user.getId());
                item.put("username", user.getUsername());
                item.put("role", user.getRole());
                item.put("createTime", user.getCreateTime());
                return item;
            })
            .collect(java.util.stream.Collectors.toList());
    }

    public void updateUsername(Long userId, String newUsername) {
        if (newUsername == null || newUsername.trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (newUsername.length() < 3 || newUsername.length() > 20) {
            throw new RuntimeException("用户名长度需在3-20个字符之间");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (userRepository.existsByUsername(newUsername.trim()) && !user.getUsername().equals(newUsername.trim())) {
            throw new RuntimeException("用户名已存在");
        }
        user.setUsername(newUsername.trim());
        userRepository.save(user);
    }

    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        if (oldPassword == null || oldPassword.isEmpty()) {
            throw new RuntimeException("原密码不能为空");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            throw new RuntimeException("新密码不能为空");
        }
        if (newPassword.length() < 6) {
            throw new RuntimeException("新密码至少6位");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
