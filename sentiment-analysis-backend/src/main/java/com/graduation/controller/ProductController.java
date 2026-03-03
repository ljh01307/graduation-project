package com.graduation.controller;

import com.graduation.entity.Product;
import com.graduation.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/product")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    @PostMapping("/add")
    public Product addCategory(@RequestBody Map<String, String> payload) {
        String category = payload.get("category");
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("商品类别不能为空");
        }
        return productService.addCategory(category);
    }
    
    @GetMapping("/list")
    public List<Product> getAllCategories() {
        return productService.getAllCategories();
    }
    
    @GetMapping("/list-with-stats")
    public Map<String, Object> getAllCategoriesWithStats() {
        return productService.getAllCategoriesWithStats();
    }
    
    @PutMapping("/update/{id}")
    public Map<String, Object> updateCategory(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        String category = payload.get("category");
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("商品类别不能为空");
        }
        Product product = productService.updateCategory(id, category);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("product", product);
        return result;
    }
    
    @DeleteMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id) {
        productService.deleteCategory(id);
        return "删除成功";
    }
}