package com.adboard.repository;

import com.adboard.entity.User;

import java.util.Optional;

public interface UserRepository {

  Optional<User> findByEmail(String email);
  Optional<User> findByUsername(String username);
  Optional<User> findById(Long id);
  void save(User user);
}
