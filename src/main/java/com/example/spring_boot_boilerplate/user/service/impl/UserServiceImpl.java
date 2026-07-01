package com.example.spring_boot_boilerplate.user.service.impl;

import org.springframework.stereotype.Service;

import com.example.spring_boot_boilerplate.user.entity.User;
import com.example.spring_boot_boilerplate.user.repository.UserRepository;
import com.example.spring_boot_boilerplate.user.service.UserService;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
}
