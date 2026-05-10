package com.adboard.repository;

import com.adboard.entity.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository {

  List<Review> findBySellerId(Long sellerId, int page, int size);
  long countBySellerId(Long sellerId);
  Optional<Review> findByReviewerIdAndSellerId(Long reviewerId, Long sellerId);
  Double findAverageRatingBySellerId(Long sellerId);
  void save(Review review);
}
