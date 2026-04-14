package com.graduation.service;

import com.graduation.entity.Product;
import com.graduation.entity.User;
import com.graduation.repository.ProductRepository;
import com.graduation.repository.ReviewRepository;
import com.graduation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    public Long resolveTargetUserId(Long currentUserId, String currentRole, Long manageUserId) {
        if ("ADMIN".equals(currentRole) && manageUserId != null) {
            return manageUserId;
        }
        return currentUserId;
    }

    public Product addCategory(String category, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Product product = new Product();
        product.setCategory(category);
        product.setUser(user);
        return productRepository.save(product);
    }

    public List<Product> getAllCategories(Long userId) {
        return productRepository.findByUserId(userId);
    }

    public Map<String, Object> getAllCategoriesWithStats(Long userId) {
        List<Product> products = productRepository.findByUserId(userId);
        List<Map<String, Object>> result = new java.util.ArrayList<>();

        for (Product product : products) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", product.getId());
            item.put("category", product.getCategory());
            item.put("createTime", product.getCreateTime());

            long totalReviews = reviewRepository.countByProductId(product.getId());
            long positiveCount = reviewRepository.countPositiveByProductId(product.getId());
            long negativeCount = reviewRepository.countNegativeByProductId(product.getId());
            long analyzedCount = positiveCount + negativeCount;
            double positiveRate = analyzedCount > 0 ? (double) positiveCount / analyzedCount * 100 : 0;

            item.put("reviewCount", totalReviews);
            item.put("analyzedCount", analyzedCount);
            item.put("positiveCount", positiveCount);
            item.put("negativeCount", negativeCount);
            item.put("positiveRate", Math.round(positiveRate * 100) / 100.0);

            result.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("products", result);
        return response;
    }

    public Product updateCategory(Long id, String category, Long userId) {
        Product product = productRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("商品不存在或无权限"));
        product.setCategory(category);
        return productRepository.save(product);
    }

    public void deleteCategory(Long id, Long userId) {
        Product product = productRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("商品不存在或无权限"));
        productRepository.delete(product);
    }
}
