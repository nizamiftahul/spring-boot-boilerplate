package com.nizamiftahul.spring_boilerplate.modules.user.web;

import com.nizamiftahul.spring_boilerplate.modules.user.api.UserApi;
import com.nizamiftahul.spring_boilerplate.modules.user.api.UserSummary;
import com.nizamiftahul.spring_boilerplate.platform.exception.ResourceNotFoundException;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserApi userApi;

	public UserController(UserApi userApi) {
		this.userApi = userApi;
	}

	@GetMapping("/me")
	public UserSummary me(Principal principal) {
		return userApi.findByUsername(principal.getName())
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + principal.getName()));
	}
}
