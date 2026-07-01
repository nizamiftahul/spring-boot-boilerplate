package com.example.spring_boot_boilerplate.user.seeder;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.spring_boot_boilerplate.user.entity.Role;
import com.example.spring_boot_boilerplate.user.entity.User;
import com.example.spring_boot_boilerplate.user.repository.UserRepository;

@Component
public class UserSeeder implements CommandLineRunner {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public UserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Seed initial users
        if (userRepository.count() == 0) {
            User user1 = new User("john_doe", passwordEncoder.encode("password123"), "john_doe@example.com",
                    Role.ADMIN);
            User user2 = new User("jane_smith", passwordEncoder.encode("securepass"), "jane_smith@example.com",
                    Role.USER);
            userRepository.save(user1);
            userRepository.save(user2);
        }
    }

}
