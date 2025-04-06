package com.example.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder; // Use PasswordEncoder interface
import org.springframework.stereotype.Component;

import com.example.demo.UserRepository.UserRepository;
import com.example.demo.model.Role;
import com.example.demo.model.User;

@Component
public class AdminSetup implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Use PasswordEncoder

    public AdminSetup(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Check if there's already an admin user
        if (userRepository.findByUsername("admin").isEmpty()) {
            // Create a new admin user
            User adminUser = new User("admin", passwordEncoder.encode("adminpassword"), null, Role.ADMIN);
            userRepository.save(adminUser);
            System.out.println("Admin account created successfully.");
        } else {
            System.out.println("Admin account already exists.");
        }
    }
}
