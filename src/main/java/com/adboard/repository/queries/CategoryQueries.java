package com.adboard.repository.queries;

public final class CategoryQueries {

  private CategoryQueries() {}

  public static final String FIND_ALL = "SELECT c FROM Category c ORDER BY c.name ASC";
  public static final String FIND_BY_ID = "SELECT c FROM Category c WHERE c.id = :id";
}