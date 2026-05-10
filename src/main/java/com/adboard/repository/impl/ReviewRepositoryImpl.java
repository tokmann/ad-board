package com.adboard.repository.impl;

import com.adboard.entity.Review;
import com.adboard.repository.ReviewRepository;
import com.adboard.repository.queries.ReviewQueries;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ReviewRepositoryImpl implements ReviewRepository {

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public List<Review> findBySellerId(Long sellerId, int page, int size) {
    return entityManager.createQuery(ReviewQueries.FIND_BY_SELLER_ID, Review.class)
        .setParameter("sellerId", sellerId)
        .setFirstResult(page * size)
        .setMaxResults(size)
        .getResultList();
  }

  @Override
  public long countBySellerId(Long sellerId) {
    return entityManager.createQuery(ReviewQueries.COUNT_BY_SELLER_ID, Long.class)
        .setParameter("sellerId", sellerId)
        .getSingleResult();
  }

  @Override
  public Optional<Review> findByReviewerIdAndSellerId(Long reviewerId, Long sellerId) {
    try {
      return Optional.of(entityManager.createQuery(ReviewQueries.FIND_BY_REVIEWER_AND_SELLER, Review.class)
          .setParameter("reviewerId", reviewerId)
          .setParameter("sellerId", sellerId)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  @Override
  public Double findAverageRatingBySellerId(Long sellerId) {
    return entityManager.createQuery(ReviewQueries.FIND_AVERAGE_RATING_BY_SELLER_ID, Double.class)
        .setParameter("sellerId", sellerId)
        .getSingleResult();
  }

  @Override
  public void save(Review review) {
    if (review.getId() == null) {
      entityManager.persist(review);
    } else {
      entityManager.merge(review);
    }
  }
}
