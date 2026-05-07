package com.adboard.repository;

import com.adboard.entity.reference.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

  List<Category> findAll();
  Optional<Category> findById(Long id);
}
