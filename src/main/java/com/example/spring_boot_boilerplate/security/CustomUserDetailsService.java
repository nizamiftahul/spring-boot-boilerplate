package com.example.spring_boot_boilerplate.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom User Details Service.
 * 
 * Loads user information from backing store. Currently uses in-memory
 * user database for development. In production, replace with database
 * lookups via UserRepository (Dependency Inversion).
 * 
 * Single Responsibility: Load user details by username for authentication.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final BCryptPasswordEncoder passwordEncoder;

    // In-memory user database for development (replace with DB in production)
    private final Map<String, UserDetails> users;

    /**
     * Constructor injection of BCryptPasswordEncoder.
     * Initializes in-memory user store with demo users.
     */
    public CustomUserDetailsService(BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.users = initializeDemoUsers();
        logger.info("CustomUserDetailsService initialized with {} demo users", users.size());
    }

    /**
     * Load user by username. Implements UserDetailsService contract.
     * 
     * Fail-safe: Throws UsernameNotFoundException if user not found (expected
     * behavior).
     * 
     * @param username the username
     * @return UserDetails object
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails user = users.get(username);

        if (user == null) {
            logger.warn("User not found: {}", username);
            throw new UsernameNotFoundException("User not found: " + username);
        }

        logger.debug("User loaded successfully: {}", username);
        return user;
    }

    /**
     * Initialize demo users for development.
     * In production, fetch from database via UserRepository.
     * 
     * @return map of username to UserDetails
     */
    private Map<String, UserDetails> initializeDemoUsers() {
        Map<String, UserDetails> demoUsers = new HashMap<>();

        // Demo user with ROLE_USER
        demoUsers.put("user",
                User.builder()
                        .username("user")
                        .password(passwordEncoder.encode("password"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .build());

        // Demo admin with ROLE_ADMIN
        demoUsers.put("admin",
                User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .authorities(
                                new SimpleGrantedAuthority("ROLE_ADMIN"),
                                new SimpleGrantedAuthority("ROLE_USER"))
                        .build());

        return demoUsers;
    }
}
