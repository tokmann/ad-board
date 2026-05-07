package com.adboard.repository.queries;

public final class MessageQueries {

  private MessageQueries() {}

  public static final String FIND_BY_CONVERSATION =
      "SELECT m FROM Message m LEFT JOIN FETCH m.sender WHERE m.conversation.id = :convId ORDER BY m.createdAt ASC";

  public static final String COUNT_BY_CONVERSATION =
      "SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :convId";
}
