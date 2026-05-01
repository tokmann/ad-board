package com.adboard.repository.impl;

import com.adboard.entity.reference.Role;
import com.adboard.repository.RoleRepository;
import com.adboard.repository.queries.RoleQueries;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RoleRepositoryImpl implements RoleRepository {

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public Optional<Role> findByName(String name) {
    try {
      Role role = entityManager.createQuery(RoleQueries.FIND_BY_NAME, Role.class)
          .setParameter("name", name)
          .getSingleResult();
      return Optional.of(role);
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
}
