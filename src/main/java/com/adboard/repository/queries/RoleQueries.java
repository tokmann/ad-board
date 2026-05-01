package com.adboard.repository.queries;

public final class RoleQueries {

  private RoleQueries() {}

  public static final String FIND_BY_NAME = "SELECT r FROM Role r WHERE r.name = :name";
}
