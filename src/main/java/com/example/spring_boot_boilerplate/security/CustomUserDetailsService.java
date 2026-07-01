package com.example.spring_boot_boilerplate.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.spring_boot_boilerplate.user.entity.User;
import com.example.spring_boot_boilerplate.user.service.UserService;

/**
 * Custom User Details Service.
 * 
 * Single Responsibility: Load user details by username for authentication.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserService userService;

    /**
     * Constructor injection of BCryptPasswordEncoder.
     */
    public CustomUserDetailsService(UserService userService) {
        this.userService = userService;
        logger.info("CustomUserDetailsService initialized");
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
        logger.debug("Loading user by username: {}", username);

        User user = userService.findByUsername(username);

        logger.debug("User loaded successfully: {}", username);
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())
                // .disabled(!user.isEnabled())
                .build();
    }
}
