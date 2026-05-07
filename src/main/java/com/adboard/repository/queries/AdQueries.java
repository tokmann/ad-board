package com.adboard.repository.queries;

public final class AdQueries {

  private AdQueries() {}

  public static final String FIND_BY_ID = "SELECT a FROM Ad a LEFT JOIN FETCH a.seller WHERE a.id = :id";

  public static final String FIND_ACTIVE_BY_ID =
      "SELECT a FROM Ad a LEFT JOIN FETCH a.seller LEFT JOIN FETCH a.category WHERE a.id = :id AND a.status = 'ACTIVE'";

  public static final String FIND_BY_SELLER_EMAIL =
      "SELECT a FROM Ad a WHERE a.seller.email = :email ORDER BY a.createdAt DESC";

  public static final String COUNT_BY_SELLER_EMAIL =
      "SELECT COUNT(a) FROM Ad a WHERE a.seller.email = :email";

  public static final String FIND_SOLD_BY_SELLER_EMAIL =
      "SELECT a FROM Ad a WHERE a.seller.email = :sellerEmail AND a.status = 'SOLD' ORDER BY a.soldAt DESC";

  public static final String COUNT_SOLD_BY_SELLER_EMAIL =
      "SELECT COUNT(a) FROM Ad a WHERE a.seller.email = :sellerEmail AND a.status = 'SOLD'";
}
