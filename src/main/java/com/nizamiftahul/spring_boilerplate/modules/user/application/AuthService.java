package com.nizamiftahul.spring_boilerplate.modules.user.application;

import com.nizamiftahul.spring_boilerplate.modules.user.domain.RefreshToken;
import com.nizamiftahul.spring_boilerplate.modules.user.domain.RefreshTokenRepository;
import com.nizamiftahul.spring_boilerplate.modules.user.domain.Role;
import com.nizamiftahul.spring_boilerplate.modules.user.domain.User;
import com.nizamiftahul.spring_boilerplate.modules.user.domain.UserRepository;
import com.nizamiftahul.spring_boilerplate.modules.user.event.UserRegisteredEvent;
import com.nizamiftahul.spring_boilerplate.modules.user.web.dto.AuthResponse;
import com.nizamiftahul.spring_boilerplate.modules.user.web.dto.LoginRequest;
import com.nizamiftahul.spring_boilerplate.modules.user.web.dto.RefreshRequest;
import com.nizamiftahul.spring_boilerplate.modules.user.web.dto.RegisterRequest;
import com.nizamiftahul.spring_boilerplate.platform.exception.DomainException;
import com.nizamiftahul.spring_boilerplate.platform.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final ApplicationEventPublisher eventPublisher;
	private final long refreshTokenExpirationMillis;

	public AuthService(
			UserRepository userRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			JwtService jwtService,
			ApplicationEventPublisher eventPublisher,
			@Value("${jwt.refresh-token-expiration:2592000000}") long refreshTokenExpirationMillis) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.eventPublisher = eventPublisher;
		this.refreshTokenExpirationMillis = refreshTokenExpirationMillis;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new DomainException("Email already registered: " + request.email());
		}
		User user = new User(request.username(), request.email(), passwordEncoder.encode(request.password()), Role.USER);
		user = userRepository.save(user);
		eventPublisher.publishEvent(new UserRegisteredEvent(user.getId(), user.getUsername(), user.getEmail()));
		return issueTokenPair(user);
	}

	@Transactional
	public AuthResponse login(LoginRequest request) {
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.username(), request.password()));
		User user = userRepository.findByUsername(request.username())
				.orElseThrow(() -> new DomainException("Invalid credentials"));
		return issueTokenPair(user);
	}

	@Transactional
	public AuthResponse refresh(RefreshRequest request) {
		String presentedHash = hash(request.refreshToken());
		RefreshToken existing = refreshTokenRepository.findByTokenHash(presentedHash)
				.orElseThrow(() -> new DomainException("Invalid refresh token"));
		if (existing.isRevoked() || existing.isExpired()) {
			throw new DomainException("Invalid refresh token");
		}
		existing.revoke();
		refreshTokenRepository.save(existing);
		User user = userRepository.findById(existing.getUserId())
				.orElseThrow(() -> new DomainException("Invalid refresh token"));
		return issueTokenPair(user);
	}

	@Transactional
	public void logout(RefreshRequest request) {
		String presentedHash = hash(request.refreshToken());
		refreshTokenRepository.findByTokenHash(presentedHash).ifPresent(token -> {
			token.revoke();
			refreshTokenRepository.save(token);
		});
	}

	private AuthResponse issueTokenPair(User user) {
		String accessToken = jwtService.generateAccessToken(user.getUsername(), user.getRole().name());
		String rawRefreshToken = generateRawToken();
		RefreshToken refreshToken = new RefreshToken(
				hash(rawRefreshToken), user.getId(), Instant.now().plusMillis(refreshTokenExpirationMillis));
		refreshTokenRepository.save(refreshToken);
		return new AuthResponse(accessToken, rawRefreshToken, jwtService.getAccessTokenExpirationMillis() / 1000);
	}

	private String generateRawToken() {
		byte[] bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hash(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 not available", ex);
		}
	}
}
