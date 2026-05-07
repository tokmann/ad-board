package com.adboard.repository.queries;

public final class UserQueries {

  private UserQueries() {}

  public static final String FIND_BY_EMAIL = "SELECT u FROM User u WHERE u.email = :email";
  public static final String FIND_BY_USERNAME = "SELECT u FROM User u WHERE u.username = :username";
  public static final String FIND_BY_ID = "SELECT u FROM User u WHERE u.id = :id";
}
