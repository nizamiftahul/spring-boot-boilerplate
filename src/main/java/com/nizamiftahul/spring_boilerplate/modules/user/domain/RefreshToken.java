package com.nizamiftahul.spring_boilerplate.modules.user.domain;

import com.nizamiftahul.spring_boilerplate.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseEntity {

	@Column(name = "token_hash", nullable = false, unique = true)
	private String tokenHash;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(nullable = false)
	private boolean revoked = false;

	protected RefreshToken() {
	}

	public RefreshToken(String tokenHash, Long userId, Instant expiresAt) {
		this.tokenHash = tokenHash;
		this.userId = userId;
		this.expiresAt = expiresAt;
		this.revoked = false;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public Long getUserId() {
		return userId;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public boolean isRevoked() {
		return revoked;
	}

	public void revoke() {
		this.revoked = true;
	}

	public boolean isExpired() {
		return Instant.now().isAfter(expiresAt);
	}
}
