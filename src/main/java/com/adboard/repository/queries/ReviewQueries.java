package com.adboard.repository.queries;

public final class ReviewQueries {

  private ReviewQueries() {}

  public static final String FIND_BY_SELLER_ID =
      "SELECT r FROM Review r LEFT JOIN FETCH r.reviewer WHERE r.seller.id = :sellerId ORDER BY r.createdAt DESC";

  public static final String COUNT_BY_SELLER_ID =
      "SELECT COUNT(r) FROM Review r WHERE r.seller.id = :sellerId";

  public static final String FIND_BY_REVIEWER_AND_SELLER =
      "SELECT r FROM Review r WHERE r.reviewer.id = :reviewerId AND r.seller.id = :sellerId";

  public static final String FIND_AVERAGE_RATING_BY_SELLER_ID =
      "SELECT AVG(r.rating) FROM Review r WHERE r.seller.id = :sellerId";
}
