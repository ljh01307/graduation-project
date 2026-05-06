package com.graduation.service;

import com.graduation.entity.User;
import com.graduation.entity.Product;
import com.graduation.entity.Review;
import com.graduation.repository.UserRepository;
import com.graduation.repository.ProductRepository;
import com.graduation.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public List<Map<String, Object>> getAllUsers() {
        return userRepository.findAll().stream()
            .map(user -> {
                Map<String, Object> item = new java.util.HashMap<>();
                item.put("id", user.getId());
                item.put("username", user.getUsername());
                item.put("role", user.getRole());
                item.put("createTime", user.getCreateTime());
                item.put("productCount", getProductCount(user.getId()));
                return item;
            })
            .collect(Collectors.toList());
    }

    public Map<String, Object> addUser(String username, String password, String role) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (username.length() < 3 || username.length() > 20) {
            throw new RuntimeException("用户名长度需在3-20个字符之间");
        }
        if (password == null || password.length() < 6) {
            throw new RuntimeException("密码至少6位");
        }
        if (userRepository.existsByUsername(username.trim())) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role != null && role.equals("ADMIN") ? "ADMIN" : "USER");

        userRepository.save(user);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("createTime", user.getCreateTime());
        return result;
    }

    public void updateUser(Long userId, String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (username.length() < 3 || username.length() > 20) {
            throw new RuntimeException("用户名长度需在3-20个字符之间");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (userRepository.existsByUsername(username.trim()) && !user.getUsername().equals(username.trim())) {
            throw new RuntimeException("用户名已存在");
        }

        user.setUsername(username.trim());
        userRepository.save(user);
    }

    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("用户不存在");
        }

        List<Product> products = productRepository.findByUserId(userId);
        for (Product product : products) {
            List<Review> reviews = reviewRepository.findByProductId(product.getId());
            reviewRepository.deleteAll(reviews);
        }
        productRepository.deleteAll(products);

        userRepository.deleteById(userId);
    }

    private int getProductCount(Long userId) {
        return productRepository.findByUserId(userId).size();
    }
}
