package com.nizamiftahul.spring_boilerplate.modules.user.application;

import com.nizamiftahul.spring_boilerplate.modules.user.domain.User;
import com.nizamiftahul.spring_boilerplate.modules.user.domain.UserRepository;
import java.util.List;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	private final UserRepository userRepository;

	public UserDetailsServiceImpl(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
		return org.springframework.security.core.userdetails.User.builder()
				.username(user.getUsername())
				.password(user.getPasswordHash())
				.authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
				.build();
	}
}
