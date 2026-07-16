package com.nizamiftahul.spring_boilerplate.modules.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nizamiftahul.spring_boilerplate.modules.user.domain.RefreshToken;
import com.nizamiftahul.spring_boilerplate.modules.user.domain.RefreshTokenRepository;
import com.nizamiftahul.spring_boilerplate.modules.user.domain.Role;
import com.nizamiftahul.spring_boilerplate.modules.user.domain.User;
import com.nizamiftahul.spring_boilerplate.modules.user.domain.UserRepository;
import com.nizamiftahul.spring_boilerplate.modules.user.web.dto.LoginRequest;
import com.nizamiftahul.spring_boilerplate.modules.user.web.dto.RefreshRequest;
import com.nizamiftahul.spring_boilerplate.modules.user.web.dto.RegisterRequest;
import com.nizamiftahul.spring_boilerplate.platform.exception.DomainException;
import com.nizamiftahul.spring_boilerplate.platform.security.JwtService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

	private UserRepository userRepository;
	private RefreshTokenRepository refreshTokenRepository;
	private PasswordEncoder passwordEncoder;
	private AuthenticationManager authenticationManager;
	private JwtService jwtService;
	private ApplicationEventPublisher eventPublisher;
	private AuthService authService;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		refreshTokenRepository = mock(RefreshTokenRepository.class);
		passwordEncoder = mock(PasswordEncoder.class);
		authenticationManager = mock(AuthenticationManager.class);
		jwtService = mock(JwtService.class);
		eventPublisher = mock(ApplicationEventPublisher.class);
		authService = new AuthService(
				userRepository,
				refreshTokenRepository,
				passwordEncoder,
				authenticationManager,
				jwtService,
				eventPublisher,
				2_592_000_000L);
	}

	@Test
	void register_createsUserAndPublishesEventAndIssuesTokens() {
		RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123");
		when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("hashed");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(jwtService.generateAccessToken("alice", "USER")).thenReturn("access-token");
		when(jwtService.getAccessTokenExpirationMillis()).thenReturn(3_600_000L);

		var response = authService.register(request);

		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(response.refreshToken()).isNotBlank();
		verify(eventPublisher).publishEvent(any(Object.class));
		verify(refreshTokenRepository).save(any(RefreshToken.class));
	}

	@Test
	void register_rejectsDuplicateEmail() {
		RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "password123");
		when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(request)).isInstanceOf(DomainException.class);
	}

	@Test
	void login_authenticatesAndIssuesTokens() {
		LoginRequest request = new LoginRequest("alice", "password123");
		User user = new User("alice", "alice@example.com", "hashed", Role.USER);
		when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
		when(jwtService.generateAccessToken("alice", "USER")).thenReturn("access-token");
		when(jwtService.getAccessTokenExpirationMillis()).thenReturn(3_600_000L);

		var response = authService.login(request);

		assertThat(response.accessToken()).isEqualTo("access-token");
	}

	@Test
	void refresh_rejectsRevokedToken() {
		RefreshToken revoked = new RefreshToken("some-hash", 1L, Instant.now().plusSeconds(60));
		revoked.revoke();
		when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(revoked));

		assertThatThrownBy(() -> authService.refresh(new RefreshRequest("raw-token")))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void refresh_rejectsUnknownToken() {
		when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.refresh(new RefreshRequest("raw-token")))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void logout_revokesMatchingToken() {
		RefreshToken token = new RefreshToken("some-hash", 1L, Instant.now().plusSeconds(60));
		when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

		authService.logout(new RefreshRequest("raw-token"));

		assertThat(token.isRevoked()).isTrue();
		verify(refreshTokenRepository).save(token);
	}
}
