package com.adboard.repository.queries;

public final class ConversationQueries {

  private ConversationQueries() {}

  public static final String FIND_BY_AD_AND_USERS =
      "SELECT c FROM Conversation c WHERE c.ad.id = :adId " +
          "AND ((c.buyer.id = :user1Id AND c.seller.id = :user2Id) " +
          "OR (c.buyer.id = :user2Id AND c.seller.id = :user1Id))";

  public static final String CHECK_PARTICIPANT =
      "SELECT COUNT(c) FROM Conversation c WHERE c.id = :convId " +
          "AND (c.buyer.id = :userId OR c.seller.id = :userId)";

  public static final String COUNT_ALL_CONVERSATIONS = "SELECT COUNT(c) FROM Conversation c";
}
