package com.example.spring_boot_boilerplate.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.spring_boot_boilerplate.auth.entity.RefreshToken;
import com.example.spring_boot_boilerplate.auth.repository.RefreshTokenRepository;
import com.example.spring_boot_boilerplate.auth.service.AuthService;
import com.example.spring_boot_boilerplate.auth.service.impl.AuthServiceImpl;
import com.example.spring_boot_boilerplate.security.JwtTokenProvider;

/**
 * Unit tests for AuthService.
 * Validates token creation, rotation, revocation, and validation logic.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private AuthService authService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private static final long EXPIRY_DURATION_MS = 7 * 24 * 60 * 60 * 1000; // 7 days

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(jwtTokenProvider, refreshTokenRepository);
    }

    @Test
    @DisplayName("Create refresh token calls repository save")
    void testCreateRefreshToken() {
        String username = "testuser";

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String token = authService.createRefreshToken(username, EXPIRY_DURATION_MS);

        assertNotNull(token);
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Validate existing refresh token returns username")
    void testValidateRefreshToken() {
        String username = "testuser";
        String token = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsername(username);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(refreshToken));

        String result = authService.validateRefreshToken(token);

        assertEquals(username, result);
    }

    @Test
    @DisplayName("Validate expired refresh token returns null")
    void testValidateExpiredRefreshToken() {
        String token = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setExpiresAt(Instant.now().minusSeconds(3600)); // Expired

        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(refreshToken));

        String result = authService.validateRefreshToken(token);

        assertNull(result);
    }

    @Test
    @DisplayName("Validate non-existent refresh token returns null")
    void testValidateNonExistentRefreshToken() {
        String token = UUID.randomUUID().toString();

        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.empty());

        String result = authService.validateRefreshToken(token);

        assertNull(result);
    }

    @Test
    @DisplayName("Rotate refresh token deletes old and creates new")
    void testRotateRefreshToken() {
        String oldToken = UUID.randomUUID().toString();
        String username = "testuser";
        RefreshToken oldRefreshToken = new RefreshToken();
        oldRefreshToken.setId(UUID.randomUUID());
        oldRefreshToken.setUsername(username);
        oldRefreshToken.setToken(oldToken);
        oldRefreshToken.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByToken(oldToken))
                .thenReturn(Optional.of(oldRefreshToken));
        doNothing().when(refreshTokenRepository).delete(oldRefreshToken);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String newToken = authService.rotateRefreshToken(oldToken, EXPIRY_DURATION_MS);

        assertNotNull(newToken);
        assertNotEquals(oldToken, newToken);
        verify(refreshTokenRepository, times(1)).delete(oldRefreshToken);
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Revoke single refresh token deletes from database")
    void testRevokeRefreshToken() {
        String token = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setToken(token);

        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.of(refreshToken));
        doNothing().when(refreshTokenRepository).delete(refreshToken);

        authService.revokeRefreshToken(token);

        verify(refreshTokenRepository, times(1)).delete(refreshToken);
    }

    @Test
    @DisplayName("Revoke all user tokens deletes all for username")
    void testRevokeAllUserTokens() {
        String username = "testuser";

        doNothing().when(refreshTokenRepository).deleteByUsername(username);

        authService.revokeAllUserTokens(username);

        verify(refreshTokenRepository, times(1)).deleteByUsername(username);
    }

    @Test
    @DisplayName("Rotate returns null for non-existent token")
    void testRotateNonExistentToken() {
        String token = UUID.randomUUID().toString();

        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.empty());

        String result = authService.rotateRefreshToken(token, EXPIRY_DURATION_MS);

        assertNull(result);
    }
}
