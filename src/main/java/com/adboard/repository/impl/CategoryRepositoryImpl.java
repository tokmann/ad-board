package com.adboard.repository.impl;

import com.adboard.entity.reference.Category;
import com.adboard.repository.CategoryRepository;
import com.adboard.repository.queries.CategoryQueries;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CategoryRepositoryImpl implements CategoryRepository {

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public List<Category> findAll() {
    return entityManager.createQuery(CategoryQueries.FIND_ALL, Category.class).getResultList();
  }

  @Override
  public Optional<Category> findById(Long id) {
    try {
      return Optional.of(entityManager.createQuery(CategoryQueries.FIND_BY_ID, Category.class)
          .setParameter("id", id)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
}
