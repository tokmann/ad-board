package com.adboard.controller;

import com.adboard.dto.response.category.CategoryDto;
import com.adboard.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Directory of ad categories")
public class CategoryController {

  private final CategoryService categoryService;

  @Operation(summary = "Get a list of all categories")
  @GetMapping
  public ResponseEntity<List<CategoryDto>> getAllCategories() {
    log.info("Fetching all categories");
    List<CategoryDto> categories = categoryService.getAllCategories();
    log.info("Categories fetched: count={}", categories.size());
    return ResponseEntity.ok(categories);
  }
}