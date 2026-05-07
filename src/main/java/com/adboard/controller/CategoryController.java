package com.adboard.controller;

import com.adboard.dto.response.PageResponse;
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

@Slf4j
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Directory of ad categories")
public class CategoryController {

  private final CategoryService categoryService;

  @Operation(summary = "Get a list of all categories")
  @GetMapping
  public ResponseEntity<PageResponse<CategoryDto>> getAllCategories() {
    log.debug("REST request to get all categories");
    PageResponse<CategoryDto> categories = categoryService.getAllCategories();
    return ResponseEntity.ok(categories);
  }
}