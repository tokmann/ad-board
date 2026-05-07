package com.adboard.repository.queries;

public final class CommentQueries {

  private CommentQueries() {}

  public static final String FIND_ROOT_BY_AD_ID =
      "SELECT c FROM Comment c LEFT JOIN FETCH c.author WHERE c.ad.id = :adId AND c.parentComment IS NULL ORDER BY c.createdAt ASC";

  public static final String FIND_REPLIES_BY_PARENT_ID =
      "SELECT c FROM Comment c LEFT JOIN FETCH c.author WHERE c.parentComment.id = :parentId ORDER BY c.createdAt ASC";

  public static final String FIND_BY_ID_WITH_AUTHOR =
      "SELECT c FROM Comment c LEFT JOIN FETCH c.author WHERE c.id = :id";

  public static final String COUNT_ROOT_BY_AD_ID =
      "SELECT COUNT(c) FROM Comment c WHERE c.ad.id = :adId AND c.parentComment IS NULL";
}
