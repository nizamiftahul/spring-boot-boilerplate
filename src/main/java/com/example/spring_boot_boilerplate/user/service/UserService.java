package com.example.spring_boot_boilerplate.user.service;

import com.example.spring_boot_boilerplate.user.entity.User;

public interface UserService {
    User findByUsername(String username);
}
