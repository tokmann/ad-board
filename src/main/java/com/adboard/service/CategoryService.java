package com.adboard.service;

import com.adboard.dto.mapper.CategoryMapper;
import com.adboard.dto.response.PageResponse;
import com.adboard.dto.response.category.CategoryDto;
import com.adboard.entity.reference.Category;
import com.adboard.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

  private final CategoryRepository categoryRepository;
  private final CategoryMapper categoryMapper;

  @Transactional(readOnly = true)
  public PageResponse<CategoryDto> getAllCategories() {
    List<Category> categories = categoryRepository.findAll();

    List<CategoryDto> content = categories.stream()
        .map(categoryMapper::toDto)
        .toList();

    log.info("Successfully retrieved {} categories", content.size());

    return new PageResponse<>(content, 0, content.size(), content.size(), 1);
  }
}
