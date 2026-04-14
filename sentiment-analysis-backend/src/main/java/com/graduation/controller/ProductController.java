package com.graduation.controller;

import com.graduation.common.GlobalResponse;
import com.graduation.entity.Product;
import com.graduation.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping("/add")
    public GlobalResponse<?> addCategory(@RequestBody Map<String, String> payload,
                                        @RequestAttribute Long userId,
                                        @RequestAttribute String role,
                                        @RequestParam(required = false) Long manageUserId) {
        String category = payload.get("category");
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("商品类别不能为空");
        }
        Long targetUserId = productService.resolveTargetUserId(userId, role, manageUserId);
        Product product = productService.addCategory(category, targetUserId);
        return GlobalResponse.success("商品添加成功", product);
    }

    @GetMapping("/list")
    public GlobalResponse<?> getAllCategories(@RequestAttribute Long userId,
                                              @RequestAttribute String role,
                                              @RequestParam(required = false) Long manageUserId) {
        Long targetUserId = productService.resolveTargetUserId(userId, role, manageUserId);
        List<Product> products = productService.getAllCategories(targetUserId);
        return GlobalResponse.success(products);
    }

    @GetMapping("/list-with-stats")
    public GlobalResponse<?> getAllCategoriesWithStats(@RequestAttribute Long userId,
                                                        @RequestAttribute String role,
                                                        @RequestParam(required = false) Long manageUserId) {
        Long targetUserId = productService.resolveTargetUserId(userId, role, manageUserId);
        Map<String, Object> data = productService.getAllCategoriesWithStats(targetUserId);
        return GlobalResponse.success(data);
    }

    @PutMapping("/update/{id}")
    public GlobalResponse<?> updateCategory(@PathVariable Long id,
                                           @RequestBody Map<String, String> payload,
                                           @RequestAttribute Long userId,
                                           @RequestAttribute String role,
                                           @RequestParam(required = false) Long manageUserId) {
        String category = payload.get("category");
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("商品类别不能为空");
        }
        Long targetUserId = productService.resolveTargetUserId(userId, role, manageUserId);
        Product product = productService.updateCategory(id, category, targetUserId);
        return GlobalResponse.success("商品更新成功", product);
    }

    @DeleteMapping("/delete/{id}")
    public GlobalResponse<?> deleteCategory(@PathVariable Long id,
                                            @RequestAttribute Long userId,
                                            @RequestAttribute String role,
                                            @RequestParam(required = false) Long manageUserId) {
        Long targetUserId = productService.resolveTargetUserId(userId, role, manageUserId);
        productService.deleteCategory(id, targetUserId);
        return GlobalResponse.success("商品删除成功", null);
    }
}
