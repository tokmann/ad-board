package com.adboard.repository;

import com.adboard.entity.reference.Role;

import java.util.Optional;

public interface RoleRepository {

  Optional<Role> findByName(String name);
}
