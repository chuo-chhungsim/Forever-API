package com.forever.foreverapi.modules.user.repository;

import com.forever.foreverapi.modules.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import com.forever.foreverapi.modules.user.model.Role;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByRole(Role role);
}
