package com.github.harrydimas.leetcode_solution.user.repos;

import com.github.harrydimas.leetcode_solution.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);
}