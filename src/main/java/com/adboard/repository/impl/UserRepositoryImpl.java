package com.adboard.repository.impl;

import com.adboard.entity.User;
import com.adboard.repository.UserRepository;
import com.adboard.repository.queries.UserQueries;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public Optional<User> findByEmail(String email) {
    try {
      User user = entityManager.createQuery(UserQueries.FIND_BY_EMAIL, User.class)
          .setParameter("email", email)
          .getSingleResult();
      return Optional.of(user);
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<User> findByUsername(String username) {
    try {
      User user = entityManager.createQuery(UserQueries.FIND_BY_USERNAME, User.class)
          .setParameter("username", username)
          .getSingleResult();
      return Optional.of(user);
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<User> findById(Long id) {
    try {
      User user = entityManager.createQuery(UserQueries.FIND_BY_ID, User.class)
          .setParameter("id", id)
          .getSingleResult();
      return Optional.of(user);
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  @Override
  public void save(User user) {
    if (user.getId() == null) {
      entityManager.persist(user);
    } else {
      entityManager.merge(user);
    }
  }
}
