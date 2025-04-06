package com.example.demo.UserRepository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);  // Already exists

    // Optional: If you need to fetch users by email as well in the future
    Optional<User> findByEmail(String email);
}
