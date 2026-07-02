package com.example.spring_boot_boilerplate.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.example.spring_boot_boilerplate.auth.dto.AuthResponse;
import com.example.spring_boot_boilerplate.auth.dto.LoginRequest;
import com.example.spring_boot_boilerplate.auth.service.AuthFacade;
import com.example.spring_boot_boilerplate.auth.service.RefreshTokenService;
import com.example.spring_boot_boilerplate.auth.service.impl.AuthFacadeImpl;
import com.example.spring_boot_boilerplate.common.exception.UnauthorizedException;
import com.example.spring_boot_boilerplate.security.JwtTokenProvider;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Unit tests for AuthFacade.
 * Validates token creation, rotation, revocation, and validation logic.
 */
@ExtendWith(MockitoExtension.class)
class AuthFacadeTest {

    private AuthFacade authFacade;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        authFacade = new AuthFacadeImpl(authenticationManager, jwtTokenProvider, refreshTokenService);
    }

    @Test
    @DisplayName("Login with valid credentials returns access token and sets refresh token cookie")
    void shouldLoginSuccessfully() {
        String username = "testuser";
        String accessToken = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString();

        LoginRequest loginRequest = new LoginRequest(username, "password123");
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(),
                loginRequest.getPassword());

        HttpServletResponse response = mock(HttpServletResponse.class);

        Authentication auth = mock(Authentication.class);

        when(authenticationManager.authenticate(eq(authenticationToken))).thenReturn(auth);
        when(jwtTokenProvider.createAccessToken(eq(username), anyList())).thenReturn(accessToken);
        when(refreshTokenService.enforceConcurrentLimit(eq(username))).thenReturn(false);
        when(refreshTokenService.create(eq(username))).thenReturn(refreshToken);

        AuthResponse resp = authFacade.login(loginRequest, response);

        assertNotNull(resp);
        assertEquals(accessToken, resp.getAccessToken());
        assertEquals(username, resp.getUsername());

        verify(authenticationManager, times(1)).authenticate(eq(authenticationToken));
        verify(jwtTokenProvider, times(1)).createAccessToken(eq(username), anyList());
        verify(refreshTokenService, times(1)).enforceConcurrentLimit(eq(username));
        verify(refreshTokenService, times(1)).create(eq(username));
        verify(refreshTokenService, times(1)).setCookie(eq(response), eq(refreshToken));
    }

    @Test
    @DisplayName("Login with invalid credentials throws exception")
    void shouldThrowExceptionWhenLoginFails() {
        String username = "testuser";

        LoginRequest loginRequest = new LoginRequest(username, "password123");
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(),
                loginRequest.getPassword());

        HttpServletResponse response = mock(HttpServletResponse.class);

        when(authenticationManager.authenticate(eq(authenticationToken)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> {
            authFacade.login(loginRequest, response);
        });

        verify(authenticationManager, times(1)).authenticate(eq(authenticationToken));
        verify(jwtTokenProvider, never()).createAccessToken(anyString(), anyList());
        verify(refreshTokenService, never()).enforceConcurrentLimit(anyString());
        verify(refreshTokenService, never()).create(anyString());
        verify(refreshTokenService, never()).setCookie(any(HttpServletResponse.class), anyString());
    }

    @Test
    @DisplayName("Refresh token validation returns access token and rotates refresh token")
    void shouldRefreshTokenSuccessfully() {
        String username = "testuser";
        String oldRefreshToken = UUID.randomUUID().toString();
        String newToken = UUID.randomUUID().toString();
        String newRefreshToken = UUID.randomUUID().toString();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(refreshTokenService.getFromCookie(eq(request))).thenReturn(oldRefreshToken);
        when(refreshTokenService.validate(eq(oldRefreshToken))).thenReturn(username);
        when(jwtTokenProvider.createAccessToken(eq(username), anyList())).thenReturn(newToken);
        when(refreshTokenService.rotate(eq(oldRefreshToken))).thenReturn(newRefreshToken);

        AuthResponse resp = authFacade.refresh(request, response);

        assertNotNull(resp);
        assertEquals(newToken, resp.getAccessToken());
        assertEquals(username, resp.getUsername());

        verify(refreshTokenService, times(1)).getFromCookie(eq(request));
        verify(refreshTokenService, times(1)).validate(eq(oldRefreshToken));
        verify(jwtTokenProvider, times(1)).createAccessToken(eq(username), anyList());
        verify(refreshTokenService, times(1)).rotate(eq(oldRefreshToken));
        verify(refreshTokenService, times(1)).setCookie(eq(response), eq(newRefreshToken));
    }

    @Test
    @DisplayName("Refresh token validation with invalid token throws exception")
    void shouldThrowExceptionWhenRefreshTokenIsInvalid() {
        String oldRefreshToken = UUID.randomUUID().toString();

        when(refreshTokenService.getFromCookie(any(HttpServletRequest.class))).thenReturn(oldRefreshToken);
        when(refreshTokenService.validate(eq(oldRefreshToken)))
                .thenThrow(new UnauthorizedException("Unauthorized: Invalid or expired refresh token"));

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        assertThrows(RuntimeException.class, () -> authFacade.refresh(request, response));

        verify(refreshTokenService, times(1)).getFromCookie(eq(request));
        verify(refreshTokenService, times(1)).validate(eq(oldRefreshToken));
        verify(jwtTokenProvider, never()).createAccessToken(anyString(), anyList());
        verify(refreshTokenService, never()).rotate(anyString());
        verify(refreshTokenService, never()).setCookie(any(HttpServletResponse.class), anyString());
    }

}
