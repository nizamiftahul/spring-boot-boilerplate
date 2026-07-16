package com.nizamiftahul.spring_boilerplate.modules.user.api;

import java.util.Optional;

public interface UserApi {

	Optional<UserSummary> findByUsername(String username);

	boolean existsByEmail(String email);

	Optional<UserSummary> getById(Long id);
}
