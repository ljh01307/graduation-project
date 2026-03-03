package com.graduation.service;

import com.graduation.entity.Product;
import com.graduation.repository.ProductRepository;
import com.graduation.repository.ReviewRepository;
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
    
    public Product addCategory(String category) {
        Product product = new Product();
        product.setCategory(category);
        return productRepository.save(product);
    }
    
    public List<Product> getAllCategories() {
        return productRepository.findAll();
    }
    
    public Map<String, Object> getAllCategoriesWithStats() {
        List<Product> products = productRepository.findAll();
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
    
    public Product updateCategory(Long id, String category) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        product.setCategory(category);
        return productRepository.save(product);
    }
    
    public void deleteCategory(Long id) {
        productRepository.deleteById(id);
    }
}