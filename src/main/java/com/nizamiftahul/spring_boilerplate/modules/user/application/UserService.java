package com.nizamiftahul.spring_boilerplate.modules.user.application;

import com.nizamiftahul.spring_boilerplate.modules.user.api.UserApi;
import com.nizamiftahul.spring_boilerplate.modules.user.api.UserSummary;
import com.nizamiftahul.spring_boilerplate.modules.user.domain.User;
import com.nizamiftahul.spring_boilerplate.modules.user.domain.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserApi {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public Optional<UserSummary> findByUsername(String username) {
		return userRepository.findByUsername(username).map(this::toSummary);
	}

	@Override
	public boolean existsByEmail(String email) {
		return userRepository.existsByEmail(email);
	}

	@Override
	public Optional<UserSummary> getById(Long id) {
		return userRepository.findById(id).map(this::toSummary);
	}

	private UserSummary toSummary(User user) {
		return new UserSummary(user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
	}
}
