package com.nizamiftahul.spring_boilerplate.modules.user.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsername(String username);

	boolean existsByEmail(String email);
}
