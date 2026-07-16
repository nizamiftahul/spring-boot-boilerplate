package com.nizamiftahul.spring_boilerplate.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final SecretKey signingKey;
	private final long accessTokenExpirationMillis;

	public JwtService(
			@Value("${jwt.secret}") String secret,
			@Value("${jwt.access-token-expiration:3600000}") long accessTokenExpirationMillis) {
		this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessTokenExpirationMillis = accessTokenExpirationMillis;
	}

	public String generateAccessToken(String username, String role) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(username)
				.claim("role", role)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusMillis(accessTokenExpirationMillis)))
				.signWith(signingKey)
				.compact();
	}

	public long getAccessTokenExpirationMillis() {
		return accessTokenExpirationMillis;
	}

	public String extractUsername(String token) {
		return parseClaims(token).getSubject();
	}

	public String extractRole(String token) {
		return parseClaims(token).get("role", String.class);
	}

	public boolean isValid(String token) {
		try {
			Claims claims = parseClaims(token);
			return claims.getExpiration().after(Date.from(Instant.now()));
		} catch (JwtException | IllegalArgumentException ex) {
			return false;
		}
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(signingKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
